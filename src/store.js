const fs = require('fs');
const { app, safeStorage } = require('electron');
const { P } = require('./paths');

function defaultLang() {
  try {
    return String(app.getLocale() || '').toLowerCase().startsWith('pl') ? 'pl' : 'en';
  } catch {
    return 'pl';
  }
}

function defaults() {
  return {
    accounts: [],
    selectedAccount: null,
    instances: [],
    selectedInstance: null,
    settings: { ramMb: 4096, javaPath: '', minimizeOnLaunch: true, lang: defaultLang(), blockedNicks: [], menuStyle: 'kaczo', discord: true, discordAppId: '', theme: 'glass' },
  };
}

let data = null;

function load() {
  if (data) return data;
  const base = defaults();
  try {
    const raw = JSON.parse(fs.readFileSync(P.data, 'utf8'));
    data = { ...base, ...raw, settings: { ...base.settings, ...(raw.settings || {}) } };
  } catch {
    data = base;
  }
  if (data.settings.lang !== 'pl' && data.settings.lang !== 'en') data.settings.lang = base.settings.lang;
  if (data.settings.menuStyle !== 'vanilla') data.settings.menuStyle = 'kaczo';
  if (data.settings.themeV !== 2) { data.settings.theme = 'glass'; data.settings.themeV = 2; } // nowy domyślny wygląd
  if (!['glass', 'neon', 'mc'].includes(data.settings.theme)) data.settings.theme = 'glass';
  if (!Array.isArray(data.settings.blockedNicks)) data.settings.blockedNicks = [];
  return data;
}

function save() {
  const tmp = P.data + '.tmp';
  fs.writeFileSync(tmp, JSON.stringify(data, null, 2));
  fs.renameSync(tmp, P.data);
}

/** Tokeny odświeżania są szyfrowane kluczem systemowym (DPAPI / Keychain / libsecret), jeśli dostępny. */
function seal(text) {
  if (safeStorage.isEncryptionAvailable()) {
    return 'enc:' + safeStorage.encryptString(text).toString('base64');
  }
  return 'raw:' + Buffer.from(text, 'utf8').toString('base64');
}

function unseal(value) {
  const [kind, b64] = String(value).split(/:(.+)/);
  const buf = Buffer.from(b64, 'base64');
  return kind === 'enc' ? safeStorage.decryptString(buf) : buf.toString('utf8');
}

module.exports = { load, save, seal, unseal };
