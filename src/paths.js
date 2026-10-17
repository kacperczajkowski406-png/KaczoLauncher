const { app } = require('electron');
const path = require('path');
const fs = require('fs');

const base = path.join(app.getPath('appData'), 'KaczoLauncher');

const P = {
  base,
  common: path.join(base, 'minecraft'),     // wspólne: versions, libraries, assets
  instances: path.join(base, 'instances'),  // osobny folder gry dla każdej instancji
  runtime: path.join(base, 'runtime'),      // pobrane środowiska Java
  logs: path.join(base, 'logs'),            // log ostatniego uruchomienia gry
  data: path.join(base, 'launcher.json'),   // konta, instancje, ustawienia
  // zasoby dostarczane razem z launcherem
  menuPack: path.join(__dirname, '..', 'kaczo-pack'),
  hudMods: path.join(__dirname, '..', 'kaczo-mod'),
};

for (const dir of [P.base, P.common, P.instances, P.runtime, P.logs]) {
  fs.mkdirSync(dir, { recursive: true });
}

/** Bezpieczne łączenie ścieżki – nazwa pliku nigdy nie wyjdzie poza katalog. */
function safeJoin(dir, name) {
  const clean = path.basename(String(name));
  if (!clean || clean === '.' || clean === '..') throw new Error('Invalid file name');
  return path.join(dir, clean);
}

module.exports = { P, safeJoin };
