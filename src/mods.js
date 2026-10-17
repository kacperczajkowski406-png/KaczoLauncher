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

module.exports = {
  instanceDir, modsDir, listInstalled, toggle, remove, addLocal, search, install,
  ensurePack, installHud, hudVersions, PACK_SLUGS,
};
