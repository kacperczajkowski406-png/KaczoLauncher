const fs = require('fs');
const path = require('path');

// Wygląd menu gry rysuje teraz mod Kaczo HUD (kod, bez texture packa).
// Ten moduł tylko sprząta po starych wersjach launchera, które instalowały pakiet „KaczoMenu”.
const PACK_FOLDER = 'KaczoMenu';
const PACK_ENTRY = 'file/' + PACK_FOLDER;

/** Usuwa stary pakiet menu z instancji. */
function remove(gameDir) {
  fs.rmSync(path.join(gameDir, 'resourcepacks', PACK_FOLDER), { recursive: true, force: true });
}

module.exports = { remove, PACK_ENTRY, PACK_FOLDER };
