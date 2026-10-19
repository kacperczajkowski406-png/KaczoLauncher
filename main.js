const { app, BrowserWindow, ipcMain, dialog, shell, Menu, clipboard } = require('electron');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');

const { P } = require('./src/paths');
const store = require('./src/store');
const auth = require('./src/auth');
const versions = require('./src/versions');
const fabric = require('./src/fabric');
const mods = require('./src/mods');
const game = require('./src/game');
const owner = require('./src/owner');
const gameopts = require('./src/gameopts');
const resourcepack = require('./src/resourcepack');
const updater = require('./src/updater');
const nowplaying = require('./src/nowplaying');
const { T, GAME_LANG } = require('./src/lang');

let win = null;

if (process.platform === 'win32') app.setAppUserModelId('pl.kaczo.launcher');

if (!app.requestSingleInstanceLock()) {
  app.quit();
}

function createWindow() {
  win = new BrowserWindow({
    width: 1100,
    height: 720,
    minWidth: 920,
    minHeight: 620,
    backgroundColor: '#0b1022',
    title: 'Kaczo Client',
    icon: path.join(__dirname, 'build', 'icon.png'),
    titleBarStyle: 'hidden',
    titleBarOverlay: { color: '#0b1022', symbolColor: '#d6e4ee', height: 38 },
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
  });
  win.loadFile(path.join(__dirname, 'renderer', 'index.html'));

  // Diagnostyka: F12 lub Ctrl+Shift+I otwiera konsolę, a błędy strony trafiają też do terminala.
  win.webContents.on('before-input-event', (event, input) => {
    if (input.type === 'keyDown' && (input.key === 'F12' || (input.control && input.shift && input.key.toLowerCase() === 'i'))) {
      win.webContents.toggleDevTools();
    }
  });
  win.webContents.on('console-message', (...args) => {
    const e = args[0];
    console.log('[okno]', e && e.message !== undefined ? e.message : args[2]);
  });
  win.webContents.on('preload-error', (_e, file, err) => console.error('[preload] błąd w', file, err));
  win.webContents.on('did-fail-load', (_e, code, desc) => console.error('[okno] nie załadowano strony:', code, desc));
  win.webContents.on('render-process-gone', (_e, d) => console.error('[okno] proces okna zakończył się:', d.reason));

  // Żadnych nowych okien ani nawigacji poza naszą stroną.
  win.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  win.webContents.on('will-navigate', (e) => e.preventDefault());
  win.on('closed', () => { win = null; });
}

app.whenReady().then(() => {
  if (process.platform !== 'darwin') Menu.setApplicationMenu(null);
  nowplaying.start((ch, p) => emit(ch, p));
  app.on('before-quit', () => nowplaying.stop());
  updater.init(app, (ch, p) => emit(ch, p), () => !game.isRunning());
  purgeReservedOfflineAccounts();
  createWindow();
  app.on('activate', () => { if (!BrowserWindow.getAllWindows().length) createWindow(); });
});

app.on('second-instance', () => {
  if (win) { if (win.isMinimized()) win.restore(); win.focus(); }
});
app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });

// ───────────────────────── pomocnicze ─────────────────────────

handleLyrics();
function handleLyrics() {
  const dir = path.join(P.base, 'lyrics');
  fs.mkdirSync(dir, { recursive: true });
  ipcMain.handle('lyrics:get', async (_e, artist, title) => {
    try {
      const clean = (x) => String(x || '').replace(/[\\/:*?"<>|]/g, '').trim();
      for (const n of [`${clean(artist)} - ${clean(title)}.lrc`, `${clean(title)}.lrc`]) {
        const f = path.join(dir, path.basename(n));
        if (fs.existsSync(f)) return { ok: true, data: { text: fs.readFileSync(f, 'utf8'), dir } };
      }
      // brak lokalnego pliku → publiczna baza LRCLIB (zsynchronizowane teksty), wynik zapisujemy w cache
      const https = require('https');
      const url = `https://lrclib.net/api/get?artist_name=${encodeURIComponent(artist || '')}&track_name=${encodeURIComponent(title || '')}`;
      const body = await new Promise((res) => {
        const rq = https.get(url, { headers: { 'User-Agent': 'KaczoClient (https://github.com/kacperczajkowski406-png/KaczoLauncher)' }, timeout: 6000 }, (r) => {
          if (r.statusCode !== 200) { r.resume(); return res(''); }
          let d = ''; r.setEncoding('utf8'); r.on('data', (c) => { d += c; }); r.on('end', () => res(d));
        });
        rq.on('timeout', () => { rq.destroy(); res(''); }); rq.on('error', () => res(''));
      });
      let text = '';
      try { text = JSON.parse(body).syncedLyrics || ''; } catch { /* brak */ }
      if (text) { try { fs.writeFileSync(path.join(dir, path.basename(`${clean(artist)} - ${clean(title)}.lrc`)), text); } catch { /* */ } }
      return { ok: true, data: { text, dir } };
    } catch (e) { return { ok: false, error: String(e.message || e) }; }
  });
}
function handle(channel, fn) {
  ipcMain.handle(channel, async (event, ...args) => {
    try {
      if (!event.senderFrame || !event.senderFrame.url.startsWith('file://')) {
        throw new Error('Unauthorized call');
      }
      return { ok: true, data: await fn(...args) };
    } catch (e) {
      return { ok: false, error: String((e && e.message) || e) };
    }
  });
}

function emit(channel, payload) {
  if (win && !win.isDestroyed()) win.webContents.send(channel, payload);
}

/** Konta offline z nickiem właściciela (np. dodane ręcznie w pliku danych) są usuwane przy starcie. */
function purgeReservedOfflineAccounts() {
  const d = store.load();
  const before = d.accounts.length;
  d.accounts = d.accounts.filter((a) => !(a.type === 'offline' && owner.isReservedName(a.name)));
  if (d.accounts.length !== before) {
    if (!d.accounts.some((a) => a.id === d.selectedAccount)) d.selectedAccount = d.accounts.length ? d.accounts[0].id : null;
    store.save();
  }
}

function selectedAccount() {
  const d = store.load();
  return d.accounts.find((a) => a.id === d.selectedAccount) || null;
}

function requireOwner() {
  if (!owner.isOwnerAccount(selectedAccount())) throw new Error(T('owner.denied'));
}

function publicState() {
  const d = store.load();
  return {
    accounts: d.accounts.map((a) => ({ id: a.id, type: a.type, name: a.name, uuid: a.uuid, owner: owner.isOwnerAccount(a) })),
    selectedAccount: d.selectedAccount,
    isOwner: owner.isOwnerAccount(selectedAccount()),
    ownerName: owner.OWNER.name,
    instances: d.instances,
    selectedInstance: d.selectedInstance,
    settings: d.settings,
    hudVersions: mods.hudVersions(),
    dataDir: P.base,
    appVersion: app.getVersion ? app.getVersion() : '',
    update: updater.get(),
    running: game.isRunning(),
  };
}

function getInstance(id) {
  const inst = store.load().instances.find((i) => i.id === id);
  if (!inst) throw new Error(T('inst.notFound'));
  return inst;
}

function cleanInstanceInput(input) {
  const name = String(input.name || '').trim().slice(0, 40);
  if (!name) throw new Error(T('inst.name'));
  const mc = String(input.mc || '').trim();
  if (!/^[\w.\-]{1,40}$/.test(mc)) throw new Error(T('inst.version'));
  const loader = input.loader === 'fabric' ? 'fabric' : 'vanilla';
  let ramMb = Number(input.ramMb) || 0; // 0 = użyj ustawienia globalnego
  if (ramMb !== 0) ramMb = Math.min(65536, Math.max(1024, Math.round(ramMb)));
  return { name, mc, loader, ramMb, optimize: input.optimize !== false };
}

// ───────────────────────── stan i wersje ─────────────────────────

handle('app:init', () => publicState());
handle('update:state', () => updater.get());
handle('update:check', () => updater.check());
handle('update:install', () => { updater.install(); return true; });
handle('versions:list', () => versions.listVersions());
handle('versions:fabric', () => fabric.gameVersions());

// ───────────────────────── konta ─────────────────────────

handle('account:microsoft', async () => {
  const account = await auth.loginMicrosoft();
  const d = store.load();
  const i = d.accounts.findIndex((a) => a.id === account.id);
  if (i >= 0) d.accounts[i] = account; else d.accounts.push(account);
  d.selectedAccount = account.id;
  store.save();
  return publicState();
});

handle('account:offline', (name) => {
  const account = auth.createOffline(name); // rzuca błąd dla nicku właściciela i zablokowanych
  const d = store.load();
  if (!d.accounts.some((a) => a.id === account.id)) d.accounts.push(account);
  d.selectedAccount = account.id;
  store.save();
  return publicState();
});

handle('account:select', (id) => {
  const d = store.load();
  if (!d.accounts.some((a) => a.id === id)) throw new Error(T('acct.notFound'));
  d.selectedAccount = id;
  store.save();
  return publicState();
});

handle('account:remove', (id) => {
  const d = store.load();
  d.accounts = d.accounts.filter((a) => a.id !== id);
  if (d.selectedAccount === id) d.selectedAccount = d.accounts.length ? d.accounts[0].id : null;
  store.save();
  return publicState();
});

// ───────────────────────── instancje ─────────────────────────

/** Od razu po utworzeniu/zmianie instancji: mody optymalizacyjne + Kaczo HUD (nie czekamy na pierwszy start gry). */
const preparing = new Map(); // id instancji → trwające przygotowanie (kolejka, żeby nic nie nakładało się na siebie)

function prepareInstance(inst) {
  const prev = preparing.get(inst.id) || Promise.resolve();
  const next = prev.then(() => doPrepare(inst)).catch(() => {});
  preparing.set(inst.id, next);
  return next;
}

async function doPrepare(inst) {
  if (inst.loader !== 'fabric' || game.isRunning()) return;
  const log = (l) => emit('game:log', String(l));
  try {
    emit('game:state', { text: T('inst.modsPrep'), busy: true });
    mods.installHud(inst, log);
    if (inst.optimize !== false) await mods.ensurePack(inst, log);
    emit('game:state', { text: T('inst.modsDone'), busy: false });
  } catch (e) {
    log(String((e && e.message) || e));
    emit('game:state', { text: String((e && e.message) || e), busy: false });
  }
}

handle('instance:create', (input) => {
  const data = cleanInstanceInput(input);
  const slug = data.name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '').slice(0, 20) || 'instance';
  const inst = {
    id: `${slug}-${crypto.randomBytes(4).toString('hex')}`,
    ...data,
    created: Date.now(),
    lastPlayed: null,
  };
  const d = store.load();
  d.instances.push(inst);
  d.selectedInstance = inst.id;
  fs.mkdirSync(mods.instanceDir(inst), { recursive: true });
  store.save();
  prepareInstance(inst);
  return publicState();
});

handle('instance:update', (id, input) => {
  const inst = getInstance(id);
  Object.assign(inst, cleanInstanceInput(input));
  store.save();
  prepareInstance(inst);
  return publicState();
});

handle('instance:remove', (id, deleteFiles) => {
  const d = store.load();
  const inst = getInstance(id);
  if (game.isRunning()) throw new Error(T('inst.closeGame'));
  d.instances = d.instances.filter((i) => i.id !== id);
  if (d.selectedInstance === id) d.selectedInstance = d.instances.length ? d.instances[0].id : null;
  if (deleteFiles) fs.rmSync(mods.instanceDir(inst), { recursive: true, force: true });
  store.save();
  return publicState();
});

handle('instance:select', (id) => {
  getInstance(id);
  store.load().selectedInstance = id;
  store.save();
  return publicState();
});

handle('instance:openFolder', async (id) => {
  const dir = mods.instanceDir(getInstance(id));
  fs.mkdirSync(dir, { recursive: true });
  await shell.openPath(dir);
  return true;
});

// ───────────────────────── mody ─────────────────────────

handle('mods:list', (id) => mods.listInstalled(getInstance(id)));
handle('mods:toggle', (id, file) => { mods.toggle(getInstance(id), file); return mods.listInstalled(getInstance(id)); });
handle('mods:remove', (id, file) => { mods.remove(getInstance(id), file); return mods.listInstalled(getInstance(id)); });

handle('mods:addLocal', async (id) => {
  const inst = getInstance(id);
  const res = await dialog.showOpenDialog(win, {
    title: T('dlg.mods'),
    filters: [{ name: T('dlg.modsFilter'), extensions: ['jar'] }],
    properties: ['openFile', 'multiSelections'],
  });
  if (!res.canceled) res.filePaths.forEach((p) => mods.addLocal(inst, p));
  return mods.listInstalled(inst);
});

function importReport(r) {
  const lines = [];
  if (r.copied.length) lines.push(`Skopiowano: ${r.copied.length}`);
  if (r.updated.length) lines.push(`Pobrano nowsze wersje: ${r.updated.length}`);
  if (r.skipped.length) lines.push(`Pominięto: ${r.skipped.length}\n` + r.skipped.slice(0, 12).join('\n'));
  return lines.join('\n') || 'Nie znaleziono modów do zaimportowania.';
}

handle('mods:importFolder', async (id) => {
  const inst = getInstance(id);
  const res = await dialog.showOpenDialog(win, { title: 'Wybierz folder z modami (np. mods z innego klienta)', properties: ['openDirectory'] });
  if (res.canceled || !res.filePaths[0]) return mods.listInstalled(inst);
  const r = await mods.importFrom(inst, res.filePaths[0]);
  await dialog.showMessageBox(win, { type: 'info', message: 'Import modów zakończony', detail: importReport(r) });
  return mods.listInstalled(inst);
});

handle('mods:importInstance', async (id, fromId) => {
  const inst = getInstance(id), from = getInstance(fromId);
  const r = await mods.importFromInstance(inst, from);
  await dialog.showMessageBox(win, { type: 'info', message: `Mody z „${from.name}” (${from.mc}) przeniesione na ${inst.mc}`, detail: importReport(r) });
  return mods.listInstalled(inst);
});

handle('mods:search', (id, opts) => mods.search(getInstance(id), opts || {}));

handle('mods:install', async (id, projectId) => {
  const inst = getInstance(id);
  const added = await mods.install(inst, String(projectId));
  return { added, mods: mods.listInstalled(inst) };
});

// ───────────────────────── ustawienia ─────────────────────────

handle('settings:save', (input) => {
  const d = store.load();
  const ramMb = Math.min(65536, Math.max(1024, Math.round(Number(input.ramMb) || 4096)));
  const javaPath = String(input.javaPath || '').trim();
  if (javaPath && !fs.existsSync(javaPath)) throw new Error(T('settings.javaMissing'));
  const lang = input.lang === 'en' ? 'en' : 'pl';
  d.settings = { ...d.settings, ramMb, javaPath, minimizeOnLaunch: !!input.minimizeOnLaunch, lang,
    menuStyle: 'vanilla', discord: input.discord !== false,
    theme: ['glass', 'neon', 'mc'].includes(input.theme) ? input.theme : 'glass' };
  store.save();
  try { win && win.setTitleBarOverlay && win.setTitleBarOverlay(d.settings.theme === 'mc' ? { color: '#3b2a1a', symbolColor: '#ffffff' } : { color: '#0b1022', symbolColor: '#d6e4ee' }); } catch { /* inne systemy */ }
  return publicState();
});

handle('settings:pickJava', async () => {
  const res = await dialog.showOpenDialog(win, { title: T('dlg.java'), properties: ['openFile'] });
  return res.canceled ? null : res.filePaths[0];
});

handle('app:openData', async () => { await shell.openPath(P.base); return true; });

// ───────────────────────── panel właściciela ─────────────────────────
// Uwaga: to funkcje lokalne (na Twoim komputerze). Launcher nie ma serwera, więc nie steruje innymi graczami.

function diagnostics() {
  const d = store.load();
  const lines = [
    `Kaczo Client ${app.getVersion()} | Electron ${process.versions.electron} | Node ${process.versions.node}`,
    `OS: ${process.platform} ${process.arch} | lang: ${d.settings.lang} (game: ${GAME_LANG[d.settings.lang]})`,
    `Data dir: ${P.base}`,
    `Kaczo HUD builds: ${mods.hudVersions().join(', ') || '—'}`,
    '',
    'Accounts:',
    ...d.accounts.map((a) => `  - ${a.name} [${a.type}] ${a.uuid}${owner.isOwnerAccount(a) ? ' (OWNER)' : ''}`),
    '',
    'Instances:',
    ...d.instances.map((i) => `  - ${i.name}: ${i.mc} ${i.loader}, RAM ${i.ramMb || d.settings.ramMb} MB, optimize=${i.optimize !== false}`),
    '',
    `Blocked nicknames: ${d.settings.blockedNicks.join(', ') || '—'}`,
  ];
  return lines.join('\n');
}

handle('owner:diagnostics', () => { requireOwner(); return diagnostics(); });
handle('owner:copyDiagnostics', () => { requireOwner(); clipboard.writeText(diagnostics()); return true; });
handle('owner:openLogs', async () => { requireOwner(); await shell.openPath(P.logs); return true; });
handle('owner:devtools', () => { requireOwner(); if (win) win.webContents.openDevTools({ mode: 'detach' }); return true; });

handle('owner:resetOptions', (id) => {
  requireOwner();
  const inst = getInstance(id);
  const dir = mods.instanceDir(inst);
  gameopts.apply(dir, { lang: GAME_LANG[store.load().settings.lang], menuStyle: 'vanilla', packEntry: resourcepack.PACK_ENTRY, force: true });
  return T('owner.reset');
});

handle('owner:discordId', (id) => {
  requireOwner();
  const v = String(id || '').trim();
  if (v && !/^\d{15,25}$/.test(v)) throw new Error(T('own.discordBad'));
  const d = store.load();
  d.settings.discordAppId = v;
  store.save();
  return publicState();
});

handle('owner:clearJava', () => {
  requireOwner();
  if (game.isRunning()) throw new Error(T('inst.closeGame'));
  for (const name of fs.readdirSync(P.runtime)) fs.rmSync(path.join(P.runtime, name), { recursive: true, force: true });
  return true;
});

handle('owner:blockNick', (nick) => {
  requireOwner();
  const n = String(nick || '').trim();
  if (!auth.NICK_RE.test(n)) throw new Error(T('err.nick'));
  const d = store.load();
  if (!d.settings.blockedNicks.some((x) => x.toLowerCase() === n.toLowerCase())) d.settings.blockedNicks.push(n);
  // usuń już zapisane konta offline z tym nickiem
  d.accounts = d.accounts.filter((a) => !(a.type === 'offline' && owner.isBlockedByOwner(a.name)));
  if (!d.accounts.some((a) => a.id === d.selectedAccount)) d.selectedAccount = d.accounts.length ? d.accounts[0].id : null;
  store.save();
  return publicState();
});

handle('owner:unblockNick', (nick) => {
  requireOwner();
  const d = store.load();
  d.settings.blockedNicks = d.settings.blockedNicks.filter((x) => x.toLowerCase() !== String(nick).toLowerCase());
  store.save();
  return publicState();
});

// ───────────────────────── gra ─────────────────────────

let logStream = null;
function openLog() {
  try { if (logStream) logStream.end(); } catch { /* ignore */ }
  logStream = fs.createWriteStream(path.join(P.logs, 'latest.log'), { flags: 'w' });
}

handle('game:play', async () => {
  const d = store.load();
  const inst = d.instances.find((i) => i.id === d.selectedInstance);
  if (!inst) throw new Error(T('inst.none'));
  const account = d.accounts.find((a) => a.id === d.selectedAccount);
  if (!account) throw new Error(T('acct.none'));

  await (preparing.get(inst.id) || Promise.resolve());
  openLog();
  await game.play({
    instance: inst,
    account,
    settings: d.settings,
    emit: (channel, payload) => {
      if (channel === 'game:crash') { onCrash(payload); return; }
      emit(channel, payload);
      if (channel === 'game:log' && logStream) logStream.write(String(payload).replace(/\n?$/, '\n'));
      if (channel === 'game:state' && payload.closed && win && !win.isDestroyed()) {
        win.show();
        if (win.isMinimized()) win.restore();
      }
    },
    onAccountChanged: () => store.save(),
  });

  inst.lastPlayed = Date.now();
  store.save();
  if (d.settings.minimizeOnLaunch && win) win.minimize();
  return publicState();
});

/** Po awarii gry: pokazuje krótki opis i pozwala wysłać raport (link trafia do schowka). */
async function onCrash(c) {
  try { fs.writeFileSync(path.join(P.logs, 'last-crash.txt'), c.report || c.summary || ''); } catch { /* ignoruj */ }
  if (!win || win.isDestroyed()) return;
  const r = await dialog.showMessageBox(win, {
    type: 'error',
    title: 'Kaczo Client – awaria gry',
    message: `Gra zakończyła się błędem (Minecraft ${c.mc}).`,
    detail: (c.summary || 'Brak szczegółów.') + (c.retry ? '\n\nUruchamiam grę ponownie bez moda Kaczo HUD.' : ''),
    buttons: ['Wyślij raport (skopiuj link)', 'Zamknij'],
    defaultId: 0, cancelId: 1,
  });
  if (r.response !== 0) return;
  try {
    const res = await fetch('https://api.mclo.gs/1/log', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'User-Agent': 'KaczoClient/1.0' },
      body: new URLSearchParams({ content: (c.report || c.summary || 'brak').slice(0, 9_000_000) }),
    });
    const j = await res.json();
    if (j && j.success && j.url) {
      clipboard.writeText(j.url);
      dialog.showMessageBox(win, { type: 'info', message: 'Link do raportu skopiowany do schowka – wklej go w czacie.', detail: j.url });
    } else throw new Error((j && j.error) || 'błąd serwera');
  } catch (e) {
    clipboard.writeText(c.report || c.summary || '');
    dialog.showMessageBox(win, { type: 'info', message: 'Nie udało się wysłać raportu – cała treść jest w schowku.', detail: String((e && e.message) || e) });
  }
}

handle('game:stop', () => { game.stop(); return true; });
