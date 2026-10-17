const { getJson } = require('./http');

const MANIFEST = 'https://launchermeta.mojang.com/mc/game/version_manifest_v2.json';
let cache = null;

async function manifest() {
  if (cache && Date.now() - cache.t < 10 * 60 * 1000) return cache.data;
  const data = await getJson(MANIFEST);
  cache = { t: Date.now(), data };
  return data;
}

function parse(id) {
  const m = /^(\d+)\.(\d+)(?:\.(\d+))?/.exec(String(id));
  return m ? [Number(m[1]), Number(m[2]), Number(m[3] || 0)] : null;
}

/** Launcher obsługuje wydania od 1.21 wzwyż (starsze wersje zostały usunięte z listy). */
function isSupported(id) {
  const v = parse(id);
  if (!v) return false;
  return v[0] >= 26 || (v[0] === 1 && v[1] >= 21);
}

async function listVersions() {
  const m = await manifest();
  return m.versions
    .filter((v) => v.type === 'release' && isSupported(v.id))
    .map(({ id, type, releaseTime }) => ({ id, type, releaseTime }));
}

/** Jaka wersja Javy jest potrzebna – czytamy to prosto z pliku wersji Mojang. */
async function javaMajorFor(mcId) {
  try {
    const m = await manifest();
    const entry = m.versions.find((v) => v.id === mcId);
    if (entry) {
      const json = await getJson(entry.url);
      if (json.javaVersion && json.javaVersion.majorVersion) return json.javaVersion.majorVersion;
      return 8;
    }
  } catch {
    /* brak sieci – użyj heurystyki poniżej */
  }
  return guessJava(mcId);
}

function guessJava(mcId) {
  const v = parse(mcId);
  if (!v) return 21;
  const [major, minor, patch] = v;
  if (major >= 26) return 25;
  if (minor >= 21 || (minor === 20 && patch >= 5)) return 21;
  if (minor >= 18) return 17;
  if (minor === 17) return 16;
  return 8;
}

module.exports = { listVersions, javaMajorFor, guessJava, isSupported, parse };
