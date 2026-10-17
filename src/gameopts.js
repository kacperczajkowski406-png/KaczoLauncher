const fs = require('fs');
const path = require('path');

// Ustawienia, które launcher wpisuje do options.txt przy PIERWSZYM uruchomieniu instancji
// (potem gracz może je dowolnie zmieniać w grze).
const FIRST_RUN = {
  narrator: '0',                    // narrator wyłączony
  guiScale: '4',                    // skala interfejsu 4
  soundCategory_music: '0.0',       // muzyka wyłączona
  onboardAccessibility: 'false',    // bez ekranu powitalnego o ułatwieniach (narrator)
  tutorialStep: 'none',             // bez podpowiedzi samouczka
};

// Druga partia ustawień startowych (dla nowych i dla starszych instancji – raz): bez VSync, bez efektów FOV
// i trzęsienia od obrażeń, pole widzenia 110 („Quake Pro”).
const FIRST_RUN_V2 = {
  enableVsync: 'false',
  fovEffectScale: '0.0',
  damageTiltStrength: '0.0',
  fov: '110.0',
};

const MARKER = '.kaczo-defaults';
const MARKER_V2 = '.kaczo-defaults-v2';

function splitLines(text) {
  const lines = String(text).split(/\r?\n/);
  while (lines.length && lines[lines.length - 1] === '') lines.pop();
  return lines;
}

function setKey(lines, key, value) {
  const prefix = key + ':';
  const i = lines.findIndex((l) => l.startsWith(prefix));
  const line = prefix + value;
  if (i >= 0) lines[i] = line; else lines.push(line);
}

function getKey(lines, key) {
  const prefix = key + ':';
  const l = lines.find((x) => x.startsWith(prefix));
  return l === undefined ? undefined : l.slice(prefix.length);
}

function addToList(lines, key, entry, fallback) {
  let list;
  try {
    list = JSON.parse(getKey(lines, key) || JSON.stringify(fallback));
    if (!Array.isArray(list)) list = fallback.slice();
  } catch {
    list = fallback.slice();
  }
  if (!list.includes(entry)) list.push(entry);
  setKey(lines, key, JSON.stringify(list));
}

/**
 * Dodaje pakiet zasobów do listy włączonych pakietów (zachowując pozostałe).
 * Wpis w `incompatibleResourcePacks` sprawia, że gra nie wyrzuci pakietu z listy, nawet gdyby uznała
 * jego numer formatu za niezgodny z wersją.
 */
function enablePack(lines, entry) {
  addToList(lines, 'resourcePacks', entry, ['vanilla']);
  addToList(lines, 'incompatibleResourcePacks', entry, []);
}

function removeFromList(lines, key, entry) {
  const raw = getKey(lines, key);
  if (raw === undefined) return;
  try {
    const list = JSON.parse(raw);
    if (Array.isArray(list)) setKey(lines, key, JSON.stringify(list.filter((x) => x !== entry)));
  } catch { /* uszkodzony wpis – zostaw */ }
}

function disablePack(lines, entry) {
  removeFromList(lines, 'resourcePacks', entry);
  removeFromList(lines, 'incompatibleResourcePacks', entry);
}

const MENU_MARKER = '.kaczo-menu';

/**
 * Zapisuje options.txt w folderze gry.
 *  - język (`lang`) jest ustawiany przy każdym uruchomieniu – zgodnie z językiem launchera,
 *  - ustawienia startowe i pakiet zasobów – tylko za pierwszym razem (lub przy `force`).
 */
function apply(gameDir, { lang, packEntry, menuStyle = 'kaczo', force = false } = {}) {
  fs.mkdirSync(gameDir, { recursive: true });
  const file = path.join(gameDir, 'options.txt');
  const marker = path.join(gameDir, MARKER);
  const lines = fs.existsSync(file) ? splitLines(fs.readFileSync(file, 'utf8')) : [];

  if (lang) setKey(lines, 'lang', lang);

  const first = force || !fs.existsSync(marker);
  if (first) {
    for (const [k, v] of Object.entries(FIRST_RUN)) setKey(lines, k, v);
  }

  const marker2 = path.join(gameDir, MARKER_V2);
  const first2 = force || !fs.existsSync(marker2);
  if (first2) for (const [k, v] of Object.entries(FIRST_RUN_V2)) setKey(lines, k, v);

  // styl menu: pakiet jest dopisywany/odpisywany tylko gdy styl się ZMIENIŁ (albo przy resecie),
  // żeby nie nadpisywać wyboru gracza z menu zasobów w grze
  const styleFile = path.join(gameDir, MENU_MARKER);
  let last = null;
  try { last = fs.readFileSync(styleFile, 'utf8').trim(); } catch { /* brak */ }
  if (packEntry && (force || last !== menuStyle)) {
    if (menuStyle === 'vanilla') disablePack(lines, packEntry);
    else enablePack(lines, packEntry);
    fs.writeFileSync(styleFile, menuStyle);
  }

  fs.writeFileSync(file, lines.join('\n') + '\n');
  if (first) fs.writeFileSync(marker, new Date().toISOString());
  if (first2) fs.writeFileSync(marker2, new Date().toISOString());
  return { first };
}

module.exports = { apply, FIRST_RUN, FIRST_RUN_V2, MARKER, setKey, getKey, splitLines };
