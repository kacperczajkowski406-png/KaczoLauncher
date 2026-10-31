const fs = require('fs');
const path = require('path');
const { P, safeJoin } = require('./paths');
const { getJson, downloadFile } = require('./http');
const { T } = require('./lang');

const API = 'https://api.modrinth.com/v2';

// Pakiet optymalizacyjny (Fabric) – instalowany automatycznie na każdej wersji, dla której istnieje.
const PACK_SLUGS = ['fabric-api', 'sodium', 'lithium', 'ferrite-core', 'immediatelyfast', 'entityculling', 'modernfix', 'dynamic-fps'];
const PACK_STATE = '.kaczo-pack.json';
const MISSING_TTL = 24 * 60 * 60 * 1000; // mod bez wersji dla danej gry sprawdzamy ponownie najwcześniej po 24 h
const HUD_FILE = 'kaczo-hud.jar';

function instanceDir(inst) {
  // id jest generowane przez launcher, ale i tak nie ufamy mu bezwarunkowo
  return path.join(P.instances, path.basename(inst.id));
}

function modsDir(inst) {
  const dir = path.join(instanceDir(inst), 'mods');
  fs.mkdirSync(dir, { recursive: true });
  return dir;
}

function listInstalled(inst) {
  const dir = modsDir(inst);
  return fs
    .readdirSync(dir)
    .filter((f) => f.endsWith('.jar') || f.endsWith('.jar.disabled'))
    .map((file) => ({
      file,
      enabled: file.endsWith('.jar'),
      name: file.replace(/\.jar(\.disabled)?$/, ''),
      size: fs.statSync(path.join(dir, file)).size,
    }))
    .sort((a, b) => a.name.localeCompare(b.name));
}

function toggle(inst, file) {
  const dir = modsDir(inst);
  const from = safeJoin(dir, file);
  const to = from.endsWith('.disabled') ? from.slice(0, -'.disabled'.length) : from + '.disabled';
  fs.renameSync(from, to);
}

function remove(inst, file) {
  fs.rmSync(safeJoin(modsDir(inst), file), { force: true });
}

function addLocal(inst, srcPath) {
  if (!srcPath.toLowerCase().endsWith('.jar')) throw new Error(T('mods.pickJar'));
  fs.copyFileSync(srcPath, safeJoin(modsDir(inst), path.basename(srcPath)));
}

function requireFabric(inst) {
  if (inst.loader !== 'fabric') throw new Error(T('mods.needFabric'));
}

async function search(inst, { query = '', offset = 0 } = {}) {
  requireFabric(inst);
  const facets = JSON.stringify([['project_type:mod'], ['categories:fabric'], [`versions:${inst.mc}`]]);
  const params = new URLSearchParams({
    query: String(query).slice(0, 100),
    facets,
    limit: '20',
    offset: String(Math.max(0, Number(offset) || 0)),
    index: query ? 'relevance' : 'downloads',
  });
  const res = await getJson(`${API}/search?${params}`);
  return {
    total: res.total_hits,
    hits: res.hits.map((h) => ({
      id: h.project_id,
      title: h.title,
      description: h.description,
      icon: h.icon_url || '',
      downloads: h.downloads,
      author: h.author,
    })),
  };
}

async function latestVersion(projectId, inst) {
  const params = new URLSearchParams({
    loaders: JSON.stringify(['fabric']),
    game_versions: JSON.stringify([inst.mc]),
  });
  const versions = await getJson(`${API}/project/${encodeURIComponent(projectId)}/version?${params}`);
  if (!versions.length) throw new Error(T('mods.notFound', { mc: inst.mc }));
  // Modrinth zwraca od najnowszej; wolimy stabilne wydanie przed alfą/betą
  return versions.find((v) => v.version_type === 'release') || versions[0];
}

/** Brak internetu / timeout – nie ma sensu odpytywać kolejnych modów. */
function isNetworkError(e) {
  if (e && e.network) return true;
  const code = e && e.cause && e.cause.code;
  return (
    ['ENOTFOUND', 'ECONNREFUSED', 'ETIMEDOUT', 'ECONNRESET', 'EAI_AGAIN', 'UND_ERR_CONNECT_TIMEOUT'].includes(code) ||
    (e && e.name === 'TimeoutError') ||
    /fetch failed|timed out|aborted/i.test(String((e && e.message) || ''))
  );
}

/**
 * Instaluje mod wraz z wymaganymi zależnościami (np. Fabric API).
 * Zwraca { file, added } – nazwę pliku głównego modu oraz listę nowo pobranych plików.
 */
async function installProject(inst, projectId, onStatus = () => {}, seen = new Set(), depth = 0) {
  requireFabric(inst);
  if (seen.has(projectId) || depth > 4) return { file: null, added: [] };
  seen.add(projectId);

  const version = await latestVersion(projectId, inst);
  const file = version.files.find((f) => f.primary) || version.files[0];
  if (!file || !file.filename.endsWith('.jar')) throw new Error(T('mods.noJar'));

  const dir = modsDir(inst);
  const dest = safeJoin(dir, file.filename);
  const added = [];

  if (!fs.existsSync(dest) && !fs.existsSync(dest + '.disabled')) {
    onStatus(file.filename);
    await downloadFile(file.url, dest, { sha1: file.hashes && file.hashes.sha1 });
    added.push(file.filename);
  }

  for (const dep of version.dependencies || []) {
    if (dep.dependency_type === 'required' && dep.project_id) {
      const sub = await installProject(inst, dep.project_id, onStatus, seen, depth + 1);
      added.push(...sub.added);
    }
  }
  return { file: file.filename, added };
}

async function install(inst, projectId, onStatus, seen, depth) {
  return (await installProject(inst, projectId, onStatus, seen, depth)).added;
}

function readState(inst) {
  try {
    const st = JSON.parse(fs.readFileSync(path.join(instanceDir(inst), PACK_STATE), 'utf8'));
    return { mc: st.mc || null, files: st.files || {}, missing: st.missing || {} };
  } catch {
    return { mc: null, files: {}, missing: {} };
  }
}

function writeState(inst, state) {
  fs.mkdirSync(instanceDir(inst), { recursive: true });
  fs.writeFileSync(path.join(instanceDir(inst), PACK_STATE), JSON.stringify(state, null, 2));
}

/**
 * Dba o to, by pakiet optymalizacyjny był zainstalowany w wersji pasującej do instancji.
 * Mody niedostępne dla danej wersji są po prostu pomijane.
 */
async function ensurePack(inst, log = () => {}, slugs = PACK_SLUGS) {
  if (inst.loader !== 'fabric') return { installed: [], skipped: [] };
  const dir = modsDir(inst);
  let state = readState(inst);

  if (state.mc !== inst.mc) {
    // zmieniono wersję gry – usuń nasze stare pliki, żeby nie powodowały błędów
    for (const f of Object.values(state.files)) {
      fs.rmSync(path.join(dir, path.basename(f)), { force: true });
      fs.rmSync(path.join(dir, path.basename(f)) + '.disabled', { force: true });
    }
    state = { mc: inst.mc, files: {}, missing: {} };
  }

  const installed = [];
  const skipped = [];
  for (const slug of slugs) {
    const known = state.files[slug];
    if (known && (fs.existsSync(path.join(dir, known)) || fs.existsSync(path.join(dir, known + '.disabled')))) continue;
    if (state.missing[slug] && Date.now() - state.missing[slug] < MISSING_TTL) continue; // niedawno sprawdzone: brak wersji
    try {
      const res = await installProject(inst, slug, () => {});
      if (res.file) state.files[slug] = res.file;
      delete state.missing[slug];
      installed.push(...(res.added.length ? res.added : [res.file]).filter(Boolean));
    } catch (e) {
      skipped.push(`${slug} (${e && e.message ? e.message : e})`);
      if (isNetworkError(e)) break; // offline: nie czekaj na timeout każdego kolejnego modu
      state.missing[slug] = Date.now(); // brak wersji / błąd serwera – spróbuj ponownie za jakiś czas
    }
  }
  writeState(inst, state);

  if (installed.length) log(T('pack.installed', { list: installed.join(', ') }));
  else if (!skipped.length) log(T('pack.ok'));
  if (skipped.length) log(T('pack.skipped', { list: skipped.join('; ') }));
  return { installed, skipped };
}

/** Wersje MC, dla których w folderze kaczo-mod leży zbudowany mod Kaczo HUD. */
function hudVersions(dir = P.hudMods) {
  try {
    return fs
      .readdirSync(dir)
      .map((f) => /^kaczo-hud-(.+)\.jar$/.exec(f))
      .filter(Boolean)
      .map((m) => m[1]);
  } catch {
    return [];
  }
}

/** Kopiuje mod Kaczo HUD pasujący do wersji instancji (albo usuwa niepasujący). */
function installHud(inst, log = () => {}, dir = P.hudMods) {
  if (inst.loader !== 'fabric') return false;
  const target = path.join(modsDir(inst), HUD_FILE);
  const source = path.join(dir, `kaczo-hud-${inst.mc}.jar`);
  if (!fs.existsSync(source)) {
    fs.rmSync(target, { force: true }); // jar od innej wersji mógłby zepsuć start gry
    log(T('hud.missing', { mc: inst.mc }));
    return false;
  }
  fs.writeFileSync(target, fs.readFileSync(source));
  log(T('hud.installed', { mc: inst.mc }));
  return true;
}

/** Usuwa mod Kaczo HUD z instancji (tryb bezpieczny po awarii). */
function removeHud(inst) {
  fs.rmSync(path.join(modsDir(inst), HUD_FILE), { force: true });
}

const crypto = require('crypto');

/**
 * Import modów z innego folderu (np. z innego klienta) albo z innej instancji. Każdy plik sprawdzamy w Modrinth po sumie sha1:
 *  – jeśli ten sam plik działa na wersji tej instancji, kopiujemy go bez pobierania,
 *  – jeśli ma wersję dla tej gry, pobieramy odpowiednią (zamiast starej),
 *  – jeśli takiej wersji nie ma (albo mod nie jest na Modrinth), pomijamy go i zgłaszamy w raporcie.
 */
async function importFrom(inst, srcDir, onStatus = () => {}) {
  requireFabric(inst);
  const files = fs.readdirSync(srcDir).filter((f) => f.endsWith('.jar'));
  const result = { copied: [], updated: [], skipped: [] };
  if (!files.length) return result;
  const hashes = {};
  for (const f of files) hashes[crypto.createHash('sha1').update(fs.readFileSync(path.join(srcDir, f))).digest('hex')] = f;
  let known = {};
  try {
    const res = await fetch(`${API}/version_files`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'User-Agent': 'KaczoClient/1.0' },
      body: JSON.stringify({ hashes: Object.keys(hashes), algorithm: 'sha1' }),
      signal: AbortSignal.timeout(30000),
    });
    if (res.ok) known = await res.json();
  } catch { /* bez sieci – kopiujemy tylko pliki, które na pewno pasują */ }
  const dir = modsDir(inst);
  const seen = new Set();
  for (const [hash, f] of Object.entries(hashes)) {
    const v = known[hash];
    onStatus(f);
    try {
      if (!v) { result.skipped.push(`${f} (nieznany mod – pominięto)`); continue; }
      if (v.game_versions.includes(inst.mc) && v.loaders.includes('fabric')) {
        fs.copyFileSync(path.join(srcDir, f), safeJoin(dir, f));
        result.copied.push(f);
      } else {
        if (seen.has(v.project_id)) continue;
        seen.add(v.project_id);
        const added = await installProject(inst, v.project_id, onStatus, new Set(), 0);
        result.updated.push(added.file || f);
      }
    } catch (e) {
      result.skipped.push(`${f} (brak wersji na ${inst.mc})`);
    }
  }
  return result;
}

function importFromInstance(inst, fromInst, onStatus) {
  return importFrom(inst, modsDir(fromInst), onStatus);
}

// ───────────── prawdziwe shadery (Iris + paczka shaderów z Modrinth) ─────────────
const SHADER_PACKS = {
  'toggle': 'Kaczo Shaders (przełącznik w modzie)',
  'kaczo': 'Kaczo Shaders',
  'complementary-reimagined': 'Complementary Reimagined',
  'complementary-unbound': 'Complementary Unbound',
  'bsl-shaders': 'BSL',
  'solas-shader': 'Solas',
};

function setIrisProps(gameDir, props) {
  const file = path.join(gameDir, 'config', 'iris.properties');
  fs.mkdirSync(path.dirname(file), { recursive: true });
  let lines = [];
  try { lines = fs.readFileSync(file, 'utf8').split(/\r?\n/).filter((l) => l.trim()); } catch { /* nowy plik */ }
  for (const [k, v] of Object.entries(props)) {
    const i = lines.findIndex((l) => l.startsWith(k + '='));
    if (i >= 0) lines[i] = `${k}=${v}`; else lines.push(`${k}=${v}`);
  }
  fs.writeFileSync(file, lines.join('\n') + '\n');
}

/** Włącza shadery: instaluje Iris i wybraną paczkę, ustawia ją jako aktywną. */
async function ensureShaders(inst, log = () => {}) {
  const slug = inst.shaders === undefined ? 'toggle' : inst.shaders;      // domyślnie: Iris + paczka gotowe, włączasz w modzie
  if (inst.loader !== 'fabric' || !slug || !SHADER_PACKS[slug]) return disableShaders(inst);
  try {
    const gameDir = instanceDir(inst);
    await installProject(inst, 'iris', () => {});
    if (slug === 'toggle') {
      // Iris i Sodium są zainstalowane; shadery włącza się w modzie (Prawy Shift → Shaders). Nie ruszamy aktualnego stanu.
      const f = path.join(gameDir, 'config', 'iris.properties');
      if (!fs.existsSync(f)) setIrisProps(gameDir, { enableShaders: 'false' });
      log('Shadery: Iris gotowy – włączysz je w menu moda (Prawy Shift → Shaders)');
      return;
    }
    if (slug === 'kaczo') {
      const dst = path.join(gameDir, 'shaderpacks', 'KaczoShaders');
      fs.rmSync(dst, { recursive: true, force: true });
      fs.cpSync(path.join(P.shaders, 'KaczoShaders'), dst, { recursive: true });
      setIrisProps(gameDir, { enableShaders: 'true', shaderPack: 'KaczoShaders' });
      log('Shadery: Kaczo Shaders');
      return;
    }
    const params = new URLSearchParams({ loaders: JSON.stringify(['iris']), game_versions: JSON.stringify([inst.mc]) });
    const versions = await getJson(`${API}/project/${encodeURIComponent(slug)}/version?${params}`);
    if (!versions.length) { log(`Shadery: brak paczki ${SHADER_PACKS[slug]} dla ${inst.mc}`); return; }
    const v = versions.find((x) => x.version_type === 'release') || versions[0];
    const file = v.files.find((f) => f.primary) || v.files[0];
    const dest = safeJoin(path.join(gameDir, 'shaderpacks'), file.filename);
    if (!fs.existsSync(dest)) {
      fs.mkdirSync(path.dirname(dest), { recursive: true });
      await downloadFile(file.url, dest, { sha1: file.hashes && file.hashes.sha1 });
    }
    setIrisProps(gameDir, { enableShaders: 'true', shaderPack: file.filename });
    log(`Shadery: ${SHADER_PACKS[slug]} (${file.filename})`);
  } catch (e) {
    log('Shadery: nie udało się ustawić – ' + (e && e.message ? e.message : e));
  }
}

function disableShaders(inst) {
  try {
    const f = path.join(instanceDir(inst), 'config', 'iris.properties');
    if (fs.existsSync(f)) setIrisProps(instanceDir(inst), { enableShaders: 'false' });
  } catch { /* nieważne */ }
}

module.exports = {
  ensureShaders, disableShaders, SHADER_PACKS,
  importFrom, importFromInstance,
  removeHud,
  instanceDir, modsDir, listInstalled, toggle, remove, addLocal, search, install,
  ensurePack, installHud, hudVersions, PACK_SLUGS,
};
