const fs = require('fs');
const path = require('path');
const { P } = require('./paths');
const { getJson } = require('./http');
const { T } = require('./lang');

const META = 'https://meta.fabricmc.net/v2/versions';

/** Lista wersji Minecrafta obsługiwanych przez Fabric. */
async function gameVersions() {
  const list = await getJson(`${META}/game`);
  return list.map((v) => v.version);
}

/**
 * Upewnia się, że profil Fabric dla danej wersji MC leży w folderze versions
 * i zwraca jego id (używane jako `version.custom` w minecraft-launcher-core).
 */
/** Najnowszy już pobrany profil Fabric dla danej wersji gry (do uruchomienia bez internetu). */
function localProfile(mc) {
  try {
    const root = path.join(P.common, 'versions');
    const suffix = `-${mc}`;
    return fs
      .readdirSync(root)
      .filter((d) => d.startsWith('fabric-loader-') && d.endsWith(suffix) && fs.existsSync(path.join(root, d, `${d}.json`)))
      .sort((a, b) => a.localeCompare(b, undefined, { numeric: true }))
      .pop() || null;
  } catch {
    return null;
  }
}

async function ensureFabric(mc) {
  let loaders;
  try {
    loaders = await getJson(`${META}/loader/${encodeURIComponent(mc)}`);
  } catch (e) {
    const local = e && e.network ? localProfile(mc) : null;
    if (local) return local;
    throw e;
  }
  if (!Array.isArray(loaders) || !loaders.length) {
    throw new Error(T('fabric.unsupported', { mc }));
  }
  const pick = loaders.find((l) => l.loader.stable) || loaders[0];
  const loaderVersion = pick.loader.version;
  const id = `fabric-loader-${loaderVersion}-${mc}`;

  const dir = path.join(P.common, 'versions', id);
  const file = path.join(dir, `${id}.json`);
  if (!fs.existsSync(file)) {
    const profile = await getJson(
      `${META}/loader/${encodeURIComponent(mc)}/${encodeURIComponent(loaderVersion)}/profile/json`
    );
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(file, JSON.stringify(profile, null, 2));
  }
  return id;
}

module.exports = { ensureFabric, gameVersions, localProfile };
