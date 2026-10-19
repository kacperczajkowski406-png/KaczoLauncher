// Aktualizacje w aplikacji: launcher sam sprawdza GitHub Releases, pobiera nową wersję w tle
// i instaluje ją po kliknięciu „Uruchom ponownie i zaktualizuj” (albo przy zamknięciu aplikacji).
// Mod Kaczo HUD jest częścią instalatora, więc aktualizuje się razem z aplikacją.
// Aktualizacja sprawdza się od razu po uruchomieniu i instaluje automatycznie (gdy nie trwa gra).

const state = { status: 'idle', version: '', percent: 0, error: '', supported: false };
let updater = null;
let notify = () => {};
let timer = null;

function set(patch) {
  Object.assign(state, patch);
  notify({ ...state });
}

let beforeInstall = () => {};
function init(app, emit, canInstall = () => true, onBeforeInstall = () => {}) {
  beforeInstall = onBeforeInstall;
  notify = (s) => emit('update:state', s);
  if (!app.isPackaged) return; // w trybie developerskim (npm start) aktualizacji nie ma
  try {
    ({ autoUpdater: updater } = require('electron-updater'));
  } catch (e) {
    console.error('[update] brak modułu electron-updater:', e.message);
    return;
  }
  state.supported = true;
  updater.autoDownload = true;
  updater.autoInstallOnAppQuit = true;
  updater.on('checking-for-update', () => set({ status: 'checking', error: '' }));
  updater.on('update-available', (info) => set({ status: 'downloading', version: info.version, percent: 0 }));
  updater.on('update-not-available', () => set({ status: 'none' }));
  updater.on('download-progress', (p) => set({ status: 'downloading', percent: Math.round(p.percent || 0) }));
  updater.on('update-downloaded', (info) => {
    set({ status: 'ready', version: info.version, percent: 100 });
    // automatyczna instalacja: zaraz po pobraniu (o ile gra nie jest uruchomiona), bez klikania
    const tryInstall = () => {
      if (canInstall()) { try { try { beforeInstall(); } catch { /* */ } updater.quitAndInstall(true, true); } catch { /* spróbuje przy zamknięciu */ } }
      else setTimeout(tryInstall, 30000);
    };
    setTimeout(tryInstall, 2500);
  });
  updater.on('error', (e) => set({ status: 'error', error: String((e && e.message) || e).slice(0, 200) }));
  setTimeout(check, 800);
  timer = setInterval(check, 4 * 3600 * 1000);
  if (timer.unref) timer.unref();
}

async function check() {
  if (!updater) return { ...state };
  if (state.status === 'downloading' || state.status === 'ready') return { ...state };
  try { await updater.checkForUpdates(); } catch (e) { set({ status: 'error', error: String((e && e.message) || e).slice(0, 200) }); }
  return { ...state };
}

function install() {
  if (!updater || state.status !== 'ready') throw new Error('Brak pobranej aktualizacji');
  try { beforeInstall(); } catch { /* */ }
  updater.quitAndInstall(true, true);
}

module.exports = { init, check, install, get: () => ({ ...state }) };
