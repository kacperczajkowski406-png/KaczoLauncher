'use strict';

// Jeśli coś pójdzie nie tak, pokaż komunikat na ekranie zamiast pustego okna.
let fatalShown = false;
function showFatal(title, detail) {
  const v = document.getElementById('view');
  if (!v || fatalShown) return; // pierwszy błąd jest zwykle przyczyną – nie nadpisuj go kolejnymi
  fatalShown = true;
  v.textContent = '';
  const h = document.createElement('h1'); h.textContent = title;
  const pre = document.createElement('pre');
  pre.textContent = String(detail);
  pre.style.whiteSpace = 'pre-wrap'; pre.style.color = '#f87171'; pre.style.font = '13px/1.5 monospace';
  const hint = document.createElement('p'); hint.className = 'sub';
  hint.textContent = typeof t === 'function' ? t('fatal.hint') : 'F12';
  v.append(h, hint, pre);
}
window.addEventListener('error', (e) => showFatal('Error', (e.error && e.error.stack) || e.message));
window.addEventListener('unhandledrejection', (e) => showFatal('Error', (e.reason && e.reason.stack) || e.reason));
if (!window.api) showFatal('No connection', 'window.api is undefined – preload.js did not load.');

// ───────────────────────── narzędzia ─────────────────────────

const bridge = window.api; // nie nazywaj tego `api` – window.api jest stałą właściwością (contextBridge)

/** Tworzy element DOM. Teksty zawsze trafiają jako textContent – żadnego innerHTML z danych z sieci. */
function el(tag, props, ...kids) {
  const node = document.createElement(tag);
  let deferredValue;
  for (const [k, v] of Object.entries(props || {})) {
    if (v === undefined || v === null || v === false) continue;
    if (k === 'class') node.className = v;
    else if (k === 'value') deferredValue = v;
    else if (k.startsWith('on') && typeof v === 'function') node.addEventListener(k.slice(2).toLowerCase(), v);
    else if (k === 'disabled' || k === 'checked' || k === 'hidden' || k === 'open' || k === 'readOnly') node[k] = !!v;
    else node.setAttribute(k, v === true ? '' : v);
  }
  for (const kid of kids.flat(Infinity)) {
    if (kid === null || kid === undefined || kid === false) continue;
    node.append(kid.nodeType ? kid : document.createTextNode(String(kid)));
  }
  if (deferredValue !== undefined) node.value = deferredValue;
  return node;
}

const SVG_ATTR = 'viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"';
const ICONS = {
  play: `<svg ${SVG_ATTR}><polygon points="6 4 20 12 6 20 6 4"/></svg>`,
  box: `<svg ${SVG_ATTR}><path d="M21 8l-9-5-9 5v8l9 5 9-5V8z"/><path d="M3 8l9 5 9-5M12 13v8"/></svg>`,
  puzzle: `<svg ${SVG_ATTR}><path d="M10 4a2 2 0 114 0v1h4a1 1 0 011 1v4h-1a2 2 0 100 4h1v4a1 1 0 01-1 1h-4v-1a2 2 0 10-4 0v1H6a1 1 0 01-1-1v-4h1a2 2 0 100-4H5V6a1 1 0 011-1h4V4z"/></svg>`,
  user: `<svg ${SVG_ATTR}><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0116 0"/></svg>`,
  gear: `<svg ${SVG_ATTR}><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 00.3 1.8l.1.1a2 2 0 11-2.8 2.8l-.1-.1a1.7 1.7 0 00-1.8-.3 1.7 1.7 0 00-1 1.5V21a2 2 0 11-4 0v-.1a1.7 1.7 0 00-1.1-1.5 1.7 1.7 0 00-1.8.3l-.1.1a2 2 0 11-2.8-2.8l.1-.1a1.7 1.7 0 00.3-1.8 1.7 1.7 0 00-1.5-1H3a2 2 0 110-4h.1a1.7 1.7 0 001.5-1.1 1.7 1.7 0 00-.3-1.8l-.1-.1a2 2 0 112.8-2.8l.1.1a1.7 1.7 0 001.8.3H9a1.7 1.7 0 001-1.5V3a2 2 0 114 0v.1a1.7 1.7 0 001 1.5 1.7 1.7 0 001.8-.3l.1-.1a2 2 0 112.8 2.8l-.1.1a1.7 1.7 0 00-.3 1.8V9a1.7 1.7 0 001.5 1H21a2 2 0 110 4h-.1a1.7 1.7 0 00-1.5 1z"/></svg>`,
  crown: `<svg ${SVG_ATTR}><path d="M3 8l4.5 4L12 5l4.5 7L21 8l-2 11H5L3 8z"/></svg>`,
};
function icon(name) {
  const span = document.createElement('span');
  span.innerHTML = ICONS[name]; // statyczny, zaufany SVG
  return span.firstChild;
}

const SPLASH = {
  pl: ['Kwa!', 'Teraz z kaczkami!', 'Też nie śpisz?', '100% pikseli!', 'Uwaga na creepera!', 'Pamiętaj o łóżku!', 'Więcej FPS!', 'Nie kop prosto w dół!', 'Świeży build!', 'Wykopane z miłością!'],
  en: ['Quack!', 'Now with ducks!', 'Still awake?', '100% pixels!', 'Watch out for creepers!', 'Remember a bed!', 'More FPS!', 'Never dig straight down!', 'Fresh build!', 'Mined with love!'],
};
const pickSplash = () => { const a = SPLASH[S && S.settings.lang === 'en' ? 'en' : 'pl']; return a[Math.floor(Math.random() * a.length)]; };
// ───── aktualizacje w aplikacji ─────
let U = null;
function updText(u) {
  if (!u || !u.supported) return t('upd.dev');
  switch (u.status) {
    case 'checking': return t('upd.checking');
    case 'downloading': return t('upd.downloading', { v: u.version, p: u.percent });
    case 'ready': return t('upd.ready', { v: u.version });
    case 'none': return t('upd.none');
    case 'error': return t('upd.error', { msg: u.error });
    default: return t('upd.idle');
  }
}
function buildUpdateCard() {
  const busy = U && (U.status === 'checking' || U.status === 'downloading');
  const kids = [el('h2', {}, t('upd.title')),
    el('p', { class: 'small muted' }, t('upd.version', { v: (S && S.appVersion) || '?' })),
    el('p', { class: 'small' }, updText(U))];
  if (U && U.status === 'downloading') kids.push(el('div', { class: 'bar' }, el('i', { style: 'width:' + U.percent + '%' })));
  kids.push(el('div', { class: 'row actions' },
    U && U.status === 'ready'
      ? el('button', { class: 'btn primary', onclick: () => act('update:install') }, t('upd.install'))
      : el('button', { class: 'btn', disabled: !U || !U.supported || busy, onclick: async () => { const r = await act('update:check'); if (r) { U = r; refreshUpdate(); } } }, t('upd.check'))));
  return el('div', { class: 'card', id: 'upd-card' }, ...kids);
}
function refreshUpdate() {
  const old = document.getElementById('upd-card');
  if (old) old.replaceWith(buildUpdateCard());
  renderSidebar();
}

const THEMES = ['glass', 'neon', 'mc'];
const applyTheme = () => {
  const th = S && THEMES.includes(S.settings.theme) ? S.settings.theme : 'glass';
  document.documentElement.dataset.theme = th;
  for (const id of ['glass', 'mc']) { const l = document.getElementById('theme-' + id); if (l) l.disabled = th !== id; }
};
const locale = () => (S && S.settings.lang === 'en' ? 'en-GB' : 'pl-PL');
const fmtSize = (b) => (b > 1048576 ? (b / 1048576).toFixed(1) + ' MB' : Math.max(1, Math.round(b / 1024)) + ' KB');
const fmtNum = (n) => new Intl.NumberFormat(locale(), { notation: 'compact' }).format(n);
const fmtGb = (mb) => {
  const s = (mb / 1024).toFixed(mb % 1024 ? 1 : 0);
  return (locale() === 'pl-PL' ? s.replace('.', ',') : s) + ' GB';
};
const fmtDate = (ts) => (ts ? new Date(ts).toLocaleDateString(locale(), { day: 'numeric', month: 'short', year: 'numeric' }) : t('play.never'));

function toast(msg, kind = '') {
  const node = el('div', { class: 'toast ' + kind }, msg);
  document.getElementById('toasts').append(node);
  setTimeout(() => node.remove(), kind === 'error' ? 7000 : 3500);
}

function openModal(title, body, actions) {
  const root = document.getElementById('modal-root');
  const close = () => { backdrop.remove(); document.removeEventListener('keydown', onKey); };
  const onKey = (e) => { if (e.key === 'Escape') close(); };
  const backdrop = el('div', { class: 'backdrop', onmousedown: (e) => { if (e.target === backdrop) close(); } },
    el('div', { class: 'modal' }, el('h2', {}, title), body, el('div', { class: 'modal-actions' }, actions(close))));
  document.addEventListener('keydown', onKey);
  root.append(backdrop);
  return close;
}

async function act(channel, ...args) {
  try {
    return await bridge.invoke(channel, ...args);
  } catch (e) {
    toast(e.message || String(e), 'error');
    return undefined;
  }
}
async function actState(channel, ...args) {
  const s = await act(channel, ...args);
  if (s) { S = s; render(); }
  return s;
}

// ───────────────────────── stan ─────────────────────────

let S = null;                 // stan z głównego procesu (konta, instancje, ustawienia)
let page = 'play';
let versions = null;          // wydania MC obsługiwane przez launcher (1.21+)
let fabricVersions = null;    // wersje obsługiwane przez Fabric
const game = { busy: false, running: false, text: '', progress: null, logs: [] };
const modsUI = { tab: 'installed', query: '', installed: null, results: null, total: 0, loading: false, installing: new Set(), forInstance: null };
let msBusy = false;

const selInstance = () => S.instances.find((i) => i.id === S.selectedInstance);
const selAccount = () => S.accounts.find((a) => a.id === S.selectedAccount);

async function ensureVersions(needFabric) {
  if (!versions) versions = await act('versions:list');
  if (needFabric && !fabricVersions) fabricVersions = await act('versions:fabric');
  return !!versions && (!needFabric || !!fabricVersions);
}

// ───────────────────────── wspólne elementy ─────────────────────────

const STEVE_HEAD = 'https://mc-heads.net/avatar/MHF_Steve/64';
const headUrl = (a) => (a.type === 'microsoft' ? `https://mc-heads.net/avatar/${encodeURIComponent(a.uuid)}/64` : STEVE_HEAD);

/** Głowa skina: premium – skin gracza, offline – głowa Steve’a. Litera jest awaryjnym zamiennikiem. */
function avatar(account, size) {
  const box = el('div', { class: 'avatar' + (size ? ' ' + size : '') }, (account.name || '?')[0].toUpperCase());
  const img = el('img', { src: headUrl(account), alt: '' });
  img.addEventListener('error', () => img.remove());
  box.append(img);
  return box;
}

const ownerBadge = () => el('span', { class: 'badge owner' }, icon('crown'), t('acc.owner'));
const typeBadge = (a) => el('span', { class: 'badge ' + (a.type === 'microsoft' ? 'green' : 'blue') }, a.type === 'microsoft' ? t('acc.premium') : t('acc.offline'));
const loaderLabel = (l) => (l === 'fabric' ? t('inst.fabricShort') : t('inst.vanillaShort'));

/** Karta profilu: głowa, nick, typ konta i – dla właściciela – korona. */
function profileChip(account, big) {
  return el('div', { class: 'profile' + (big ? ' big' : '') + (account.owner ? ' is-owner' : '') },
    avatar(account, big ? 'lg' : ''),
    el('div', { class: 'meta' },
      el('b', {}, account.name),
      el('div', { class: 'badges' }, account.owner && ownerBadge(), typeBadge(account))));
}

function emptyState(title, text, buttonText, onClick) {
  return el('div', { class: 'empty card' }, el('b', {}, title), el('p', {}, text),
    buttonText && el('button', { class: 'btn primary', onclick: onClick }, buttonText));
}

// ───────────────────────── ekran: Graj ─────────────────────────

function buildControls() {
  const inst = selInstance();
  const acc = selAccount();
  if (game.running) {
    return el('button', { class: 'play-btn stop', onclick: () => act('game:stop') }, t('play.stop'));
  }
  return el('button', { class: 'play-btn', disabled: game.busy || !inst || !acc, onclick: startGame },
    game.busy ? el('span', {}, el('span', { class: 'spin' }), t('play.preparing')) : t('play.btn'));
}

async function startGame() {
  game.busy = true; game.logs = []; game.text = t('play.start'); game.progress = null;
  syncPlay();
  try {
    S = await bridge.invoke('game:play');
  } catch (e) {
    toast(e.message, 'error');
    game.text = t('play.error', { msg: e.message });
  }
  game.busy = false;
  game.progress = null;
  syncPlay();
  if (S) renderSidebar();
}

// animowane wejście do gry: pełnoekranowa nakładka z logo, pierścieniami i paskiem postępu
let launchEl = null, launchTimer = null;
function syncLaunch() {
  if (game.busy && !launchEl) {
    launchEl = el('div', { class: 'launch' },
      el('div', { class: 'launch-stage' },
        el('i', { class: 'ring r1' }), el('i', { class: 'ring r2' }), el('i', { class: 'ring r3' }),
        el('img', { class: 'launch-logo', src: 'logo.svg', alt: '' })),
      el('div', { class: 'launch-title' }, 'KACZO CLIENT'),
      el('div', { class: 'launch-status' }),
      el('div', { class: 'launch-bar' }, el('i')));
    document.body.append(launchEl);
    if (launchTimer) { clearTimeout(launchTimer); launchTimer = null; }
  }
  if (!launchEl) return;
  launchEl.querySelector('.launch-status').textContent = (game.progress && game.progress.label) || game.text || '';
  const fill = launchEl.querySelector('.launch-bar i');
  if (game.progress && game.progress.pct !== null) { fill.classList.remove('indet'); fill.style.width = game.progress.pct + '%'; }
  else { fill.classList.add('indet'); fill.style.width = ''; }
  if (!game.busy && !launchTimer) {
    const ok = game.running;
    launchEl.classList.add(ok ? 'done' : 'fail');
    const el0 = launchEl;
    launchTimer = setTimeout(() => {
      el0.classList.add('out');
      setTimeout(() => { el0.remove(); if (launchEl === el0) launchEl = null; launchTimer = null; }, 500);
    }, ok ? 1300 : 350);
  }
}

function syncPlay() {
  syncLaunch();
  if (page !== 'play') return;
  const controls = document.getElementById('play-controls');
  if (controls) controls.replaceChildren(buildControls());
  const status = document.getElementById('play-status');
  if (status) status.textContent = game.text;
  const bar = document.getElementById('play-bar');
  if (bar) {
    bar.hidden = !(game.busy || game.progress);
    const fill = bar.firstChild;
    if (game.progress && game.progress.pct !== null) {
      bar.classList.remove('indeterminate');
      fill.style.width = game.progress.pct + '%';
    } else {
      bar.classList.add('indeterminate');
      fill.style.width = '';
    }
  }
  const label = document.getElementById('play-bar-label');
  if (label) label.textContent = game.progress ? game.progress.label : '';
}

function appendLog(line) {
  game.logs.push(line);
  if (game.logs.length > 500) game.logs.shift();
  const log = document.getElementById('log');
  if (log) {
    log.append(document.createTextNode(line + '\n'));
    log.scrollTop = log.scrollHeight;
  }
}

function renderPlay(view) {
  const inst = selInstance();
  const acc = selAccount();

  view.append(el('div', { class: 'row page-head' },
    el('div', {}, el('h1', {}, t('play.title')), el('p', { class: 'sub' }, t('play.sub'))),
    el('span', { class: 'spacer' }),
    acc && profileChip(acc)));

  if (!S.instances.length) {
    view.append(emptyState(t('play.emptyTitle'), t('play.emptyText'), t('play.emptyBtn'), () => { go('instances'); instanceModal(); }));
    return;
  }

  const instSelect = el('select', { value: S.selectedInstance, onchange: (e) => actState('instance:select', e.target.value) },
    S.instances.map((i) => el('option', { value: i.id }, `${i.name} — ${i.mc} ${loaderLabel(i.loader)}`)));
  const accSelect = S.accounts.length
    ? el('select', { value: S.selectedAccount, onchange: (e) => actState('account:select', e.target.value) },
      S.accounts.map((a) => el('option', { value: a.id }, `${a.name} (${a.type === 'microsoft' ? t('play.premium') : t('play.offline')})${a.owner ? ' ★' : ''}`)))
    : el('button', { class: 'btn', onclick: () => go('accounts') }, t('play.addAccount'));

  const hasHud = inst.loader === 'fabric' && S.hudVersions.includes(inst.mc);

  view.append(
    el('div', { class: 'card hero' },
      el('div', { class: 'hero-info' },
        el('div', { class: 'meta' }, el('span', { class: 'badge green' }, 'Minecraft ' + inst.mc), el('span', { class: 'badge blue' }, loaderLabel(inst.loader)),
          el('span', { class: 'badge' }, fmtGb(inst.ramMb || S.settings.ramMb)), hasHud && el('span', { class: 'badge pink' }, t('play.hud'))),
        el('h1', {}, inst.name),
        el('span', { class: 'muted small' }, t('play.last', { date: fmtDate(inst.lastPlayed) }))),
      el('div', { class: 'hero-brand' },
        el('img', { src: 'duck-neon.svg', alt: '', class: 'hero-duck' }),
        el('div', { class: 'wordmark' }, el('span', { class: 'neon-yellow' }, 'KACZO'), el('span', { class: 'neon-cyan' }, 'CLIENT'), el('em', { class: 'splash' }, pickSplash()))),
      el('div', { class: 'hotbar', 'aria-hidden': 'true' }, ['🗡', '⛏', '🪓', '🍖', '🧭'].map((g, i) => el('i', { class: i === 0 ? 'on' : '' }, g)))),
    el('div', { class: 'play-row' },
      el('div', { id: 'play-controls' }, buildControls()),
      el('div', { class: 'selectors' }, instSelect, accSelect)),
    el('div', { class: 'progress' },
      el('div', { id: 'play-bar', class: 'bar indeterminate', hidden: true }, el('i')),
      el('div', { id: 'play-bar-label', class: 'status small' }),
      el('div', { id: 'play-status', class: 'status' }, game.text)),
    el('details', { class: 'log' }, el('summary', {}, t('play.log')), el('div', { id: 'log' }, game.logs.join('\n') + (game.logs.length ? '\n' : ''))));
}

// ───────────────────────── ekran: Instancje ─────────────────────────

function versionOptions(loader, current) {
  const fabricSet = loader === 'fabric' ? new Set(fabricVersions) : null;
  const list = versions.filter((v) => !fabricSet || fabricSet.has(v.id));
  // instancja utworzona wcześniej na starszej wersji nadal może ją pokazać
  if (current && !list.some((v) => v.id === current)) list.unshift({ id: current, type: 'release' });
  return list;
}

async function instanceModal(inst, forceLoader) {
  const isEdit = !!inst;
  const form = {
    name: inst ? inst.name : '',
    loader: forceLoader || (inst ? inst.loader : 'fabric'),
    mc: inst ? inst.mc : '',
    ramMb: inst ? inst.ramMb : 0,
    optimize: inst ? inst.optimize !== false : true,
  };
  if (!(await ensureVersions(form.loader === 'fabric'))) return;

  const nameInput = el('input', { type: 'text', maxlength: '40', placeholder: t('inst.namePh'), value: form.name, oninput: (e) => { form.name = e.target.value; } });
  const verSelect = el('select', { onchange: (e) => { form.mc = e.target.value; } });
  const ramLabel = el('span', { class: 'muted' });
  const ramInput = el('input', {
    type: 'range', min: '0', max: '32', step: '0.5', value: String(form.ramMb / 1024),
    oninput: (e) => { form.ramMb = Math.round(Number(e.target.value) * 1024); updateRam(); },
  });
  const updateRam = () => { ramLabel.textContent = form.ramMb ? fmtGb(form.ramMb) : t('inst.ramDefault', { gb: fmtGb(S.settings.ramMb) }); };

  const optimizeRow = el('div', { class: 'field' },
    el('label', { class: 'check' }, el('input', { type: 'checkbox', checked: form.optimize, onchange: (e) => { form.optimize = e.target.checked; } }), t('inst.optimize')),
    el('p', { class: 'small muted hint' }, t('inst.optimizeHint')));

  const refreshVersions = () => {
    const opts = versionOptions(form.loader, inst && inst.mc);
    verSelect.replaceChildren(...opts.map((v) => el('option', { value: v.id }, v.id)));
    form.mc = opts.some((o) => o.id === form.mc) ? form.mc : (opts[0] ? opts[0].id : '');
    verSelect.value = form.mc;
    optimizeRow.hidden = form.loader !== 'fabric';
  };

  const segVanilla = el('button', { type: 'button' }, t('inst.vanilla'));
  const segFabric = el('button', { type: 'button' }, t('inst.fabric'));
  const paintSeg = () => {
    segVanilla.classList.toggle('active', form.loader === 'vanilla');
    segFabric.classList.toggle('active', form.loader === 'fabric');
  };
  const setLoader = async (loader) => {
    if (loader === 'fabric' && !(await ensureVersions(true))) return;
    form.loader = loader; paintSeg(); refreshVersions();
  };
  segVanilla.onclick = () => setLoader('vanilla');
  segFabric.onclick = () => setLoader('fabric');

  paintSeg(); refreshVersions(); updateRam();

  const body = el('div', {},
    el('label', { class: 'field' }, el('span', {}, t('inst.name')), nameInput),
    el('div', { class: 'field' }, el('span', { class: 'small muted' }, t('inst.loader')), el('div', { class: 'segmented' }, segVanilla, segFabric)),
    el('label', { class: 'field' }, el('span', {}, t('inst.version')), verSelect, el('p', { class: 'small muted hint' }, t('inst.versionHint'))),
    optimizeRow,
    el('label', { class: 'field' }, el('span', {}, t('inst.ram'), ramLabel), ramInput),
    isEdit && el('p', { class: 'small muted' }, t('inst.editNote')));

  openModal(isEdit ? t('inst.editTitle') : t('inst.newTitle'), body, (close) => [
    el('button', { class: 'btn ghost', onclick: close }, t('c.cancel')),
    el('button', { class: 'btn primary', onclick: async () => {
      const ok = await actState(isEdit ? 'instance:update' : 'instance:create', ...(isEdit ? [inst.id, form] : [form]));
      if (ok) { modsUI.installed = null; modsUI.results = null; close(); }
    } }, isEdit ? t('c.save') : t('c.create')),
  ]);
}

function renderInstances(view) {
  view.append(el('h1', {}, t('inst.title')), el('p', { class: 'sub' }, t('inst.sub')));
  const grid = el('div', { class: 'grid' });

  for (const inst of S.instances) {
    const selected = inst.id === S.selectedInstance;
    const hasHud = inst.loader === 'fabric' && S.hudVersions.includes(inst.mc);
    grid.append(el('div', { class: 'card inst' + (selected ? ' selected' : ''), onclick: () => actState('instance:select', inst.id) },
      el('div', { class: 'row' }, el('div', { class: 'name' }, inst.name), el('span', { class: 'spacer' }), selected && el('span', { class: 'badge green' }, t('inst.current'))),
      el('div', { class: 'row wrap' }, el('span', { class: 'badge' }, inst.mc), el('span', { class: 'badge blue' }, loaderLabel(inst.loader)),
        el('span', { class: 'badge' }, fmtGb(inst.ramMb || S.settings.ramMb)), hasHud && el('span', { class: 'badge pink' }, t('inst.hudOn'))),
      el('span', { class: 'small muted' }, t('play.last', { date: fmtDate(inst.lastPlayed) })),
      el('div', { class: 'inst-actions', onclick: (e) => e.stopPropagation() },
        el('button', { class: 'btn sm', onclick: () => instanceModal(inst) }, t('c.edit')),
        el('button', { class: 'btn sm', onclick: () => act('instance:openFolder', inst.id) }, t('c.folder')),
        el('button', { class: 'btn sm danger', onclick: () => removeInstanceModal(inst) }, t('c.delete')))));
  }
  grid.append(el('button', { class: 'card add-card', onclick: () => instanceModal() }, t('inst.new')));
  view.append(grid);
}

function removeInstanceModal(inst) {
  let deleteFiles = false;
  openModal(t('inst.deleteTitle'), el('div', {},
    el('p', {}, t('inst.deleteText', { name: inst.name })),
    el('label', { class: 'check' }, el('input', { type: 'checkbox', onchange: (e) => { deleteFiles = e.target.checked; } }), t('inst.deleteFiles'))),
  (close) => [
    el('button', { class: 'btn ghost', onclick: close }, t('c.cancel')),
    el('button', { class: 'btn danger', onclick: async () => { if (await actState('instance:remove', inst.id, deleteFiles)) { modsUI.installed = null; close(); } } }, t('c.delete')),
  ]);
}

// ───────────────────────── ekran: Mody ─────────────────────────

async function loadInstalled() {
  const inst = selInstance();
  if (!inst) return;
  modsUI.installed = (await act('mods:list', inst.id)) || [];
  if (page === 'mods') render();
}

async function runSearch(reset) {
  const inst = selInstance();
  if (!inst || inst.loader !== 'fabric') return;
  modsUI.loading = true;
  if (reset) { modsUI.results = []; modsUI.total = 0; }
  render();
  const res = await act('mods:search', inst.id, { query: modsUI.query, offset: reset ? 0 : modsUI.results.length });
  modsUI.loading = false;
  if (res) { modsUI.results = (reset ? [] : modsUI.results).concat(res.hits); modsUI.total = res.total; }
  render();
}

async function installMod(hit) {
  const inst = selInstance();
  modsUI.installing.add(hit.id); render();
  const res = await act('mods:install', inst.id, hit.id);
  modsUI.installing.delete(hit.id);
  if (res) {
    modsUI.installed = res.mods;
    toast(res.added.length ? t('mods.done', { list: res.added.join(', ') }) : t('mods.already'), 'ok');
  }
  render();
}

function renderMods(view) {
  const inst = selInstance();
  view.append(el('h1', {}, t('mods.title')));
  if (!inst) {
    view.append(emptyState(t('mods.noInstance'), t('mods.noInstanceText'), t('mods.createInstance'), () => { go('instances'); instanceModal(undefined, 'fabric'); }));
    return;
  }
  if (modsUI.forInstance !== inst.id) { modsUI.forInstance = inst.id; modsUI.installed = null; modsUI.results = null; modsUI.query = ''; }

  view.append(el('p', { class: 'sub' }, t('mods.instance', { name: inst.name, mc: inst.mc, loader: loaderLabel(inst.loader) })));

  if (inst.loader !== 'fabric') {
    view.append(el('div', { class: 'banner' }, t('mods.vanillaBanner'),
      el('button', { class: 'btn sm', onclick: () => instanceModal(inst, 'fabric') }, t('mods.switch'))));
  }

  view.append(el('div', { class: 'tabs' },
    el('div', { class: 'segmented' },
      el('button', { class: modsUI.tab === 'installed' ? 'active' : '', onclick: () => { modsUI.tab = 'installed'; render(); } }, t('mods.tabInstalled')),
      el('button', { class: modsUI.tab === 'browse' ? 'active' : '', onclick: () => { modsUI.tab = 'browse'; if (!modsUI.results && inst.loader === 'fabric') runSearch(true); else render(); } }, t('mods.tabBrowse')))));

  if (modsUI.tab === 'installed') renderInstalled(view, inst); else renderBrowse(view, inst);
}

function renderInstalled(view, inst) {
  if (!modsUI.installed) { loadInstalled(); view.append(el('p', { class: 'muted' }, el('span', { class: 'spin' }), t('c.loading'))); return; }
  view.append(el('div', { class: 'row' },
    el('button', { class: 'btn', onclick: async () => { const l = await act('mods:addLocal', inst.id); if (l) { modsUI.installed = l; render(); } } }, t('mods.addJar')),
    el('button', { class: 'btn', onclick: () => act('instance:openFolder', inst.id) }, t('mods.openFolder')),
    el('span', { class: 'spacer' }), el('span', { class: 'muted small' }, t('mods.count', { n: modsUI.installed.length }))));
  const list = el('div', { class: 'list gap-top' });
  if (!modsUI.installed.length) list.append(emptyState(t('mods.noneTitle'), t('mods.noneText')));
  for (const m of modsUI.installed) {
    list.append(el('div', { class: 'item' + (m.enabled ? '' : ' off') },
      el('div', { class: 'info' }, el('b', {}, m.name), el('p', {}, fmtSize(m.size) + (m.enabled ? '' : ' · ' + t('mods.disabled')))),
      el('label', { class: 'switch' }, el('input', { type: 'checkbox', checked: m.enabled, onchange: async () => { const l = await act('mods:toggle', inst.id, m.file); if (l) { modsUI.installed = l; render(); } } }), el('span')),
      el('button', { class: 'btn sm danger', onclick: async () => { const l = await act('mods:remove', inst.id, m.file); if (l) { modsUI.installed = l; render(); } } }, t('c.delete'))));
  }
  view.append(list);
}

function renderBrowse(view, inst) {
  if (inst.loader !== 'fabric') return;
  const input = el('input', { type: 'search', placeholder: t('mods.searchPh', { mc: inst.mc }), value: modsUI.query,
    oninput: (e) => { modsUI.query = e.target.value; },
    onkeydown: (e) => { if (e.key === 'Enter') runSearch(true); } });
  view.append(el('div', { class: 'row' }, input, el('button', { class: 'btn primary', onclick: () => runSearch(true) }, t('c.search'))));

  const list = el('div', { class: 'list gap-top' });
  for (const hit of modsUI.results || []) {
    const busy = modsUI.installing.has(hit.id);
    list.append(el('div', { class: 'item' },
      hit.icon ? el('img', { class: 'mod-icon', src: hit.icon, alt: '', loading: 'lazy' }) : el('div', { class: 'mod-icon' }),
      el('div', { class: 'info' }, el('b', {}, hit.title), el('p', {}, hit.description), el('span', { class: 'small muted' }, `${hit.author} · ${t('mods.downloads', { n: fmtNum(hit.downloads) })}`)),
      el('button', { class: 'btn primary sm', disabled: busy, onclick: () => installMod(hit) }, busy ? el('span', {}, el('span', { class: 'spin' }), t('mods.installing')) : t('mods.install'))));
  }
  if (modsUI.loading) list.append(el('p', { class: 'muted' }, el('span', { class: 'spin' }), t('mods.searching')));
  else if (modsUI.results && !modsUI.results.length) list.append(emptyState(t('mods.noResults'), t('mods.noResultsText', { q: modsUI.query, mc: inst.mc })));
  view.append(list);
  if (!modsUI.loading && modsUI.results && modsUI.results.length < modsUI.total) {
    view.append(el('div', { class: 'row gap-top' }, el('button', { class: 'btn', onclick: () => runSearch(false) }, t('c.more'))));
  }
}

// ───────────────────────── ekran: Konta ─────────────────────────

function renderAccounts(view) {
  view.append(el('h1', {}, t('acc.title')), el('p', { class: 'sub' }, t('acc.sub')));

  const nick = el('input', { type: 'text', maxlength: '16', placeholder: t('acc.nickPh'), onkeydown: (e) => { if (e.key === 'Enter') addOffline(); } });
  const addOffline = async () => { if (await actState('account:offline', nick.value)) toast(t('acc.offlineAdded'), 'ok'); };

  view.append(el('div', { class: 'cols' },
    el('div', { class: 'card' },
      el('h2', {}, t('acc.premiumTitle')),
      el('p', { class: 'muted small' }, t('acc.premiumText')),
      el('button', { class: 'btn primary', disabled: msBusy, onclick: async () => {
        msBusy = true; render();
        const ok = await actState('account:microsoft');
        msBusy = false; if (ok) toast(t('acc.signedIn'), 'ok'); render();
      } }, msBusy ? el('span', {}, el('span', { class: 'spin' }), t('acc.waiting')) : t('acc.login'))),
    el('div', { class: 'card' },
      el('h2', {}, t('acc.offlineTitle')),
      el('p', { class: 'muted small' }, t('acc.offlineText')),
      el('div', { class: 'row' }, nick, el('button', { class: 'btn', onclick: addOffline }, t('c.add'))))));

  view.append(el('h2', {}, t('acc.saved')));
  if (!S.accounts.length) { view.append(emptyState(t('acc.noneTitle'), t('acc.noneText'))); return; }
  const list = el('div', { class: 'list' });
  for (const a of S.accounts) {
    const selected = a.id === S.selectedAccount;
    list.append(el('div', { class: 'item' + (a.owner ? ' owner-item' : '') }, avatar(a),
      el('div', { class: 'info' }, el('b', {}, a.name), el('div', { class: 'badges' }, a.owner && ownerBadge(), typeBadge(a))),
      selected ? el('span', { class: 'badge green' }, t('c.selected')) : el('button', { class: 'btn sm', onclick: () => actState('account:select', a.id) }, t('c.select')),
      el('button', { class: 'btn sm danger', onclick: () => actState('account:remove', a.id) }, t('c.delete'))));
  }
  view.append(list);
}

// ───────────────────────── ekran: Ustawienia ─────────────────────────

function renderSettings(view) {
  const f = { ramMb: S.settings.ramMb, javaPath: S.settings.javaPath, minimizeOnLaunch: S.settings.minimizeOnLaunch, lang: S.settings.lang, discord: S.settings.discord, theme: S.settings.theme || 'glass' };
  view.append(el('h1', {}, t('set.title')), el('p', { class: 'sub' }, t('set.sub')));

  const ramLabel = el('span', { class: 'muted' }, fmtGb(f.ramMb));
  const javaInput = el('input', { type: 'text', readOnly: true, placeholder: t('set.javaPh'), value: f.javaPath });

  const langBtn = (code, label) => el('button', { class: f.lang === code ? 'active' : '', onclick: async () => {
    f.lang = code;
    if (await actState('settings:save', f)) toast(t('set.saved'), 'ok');
  } }, label);

  const themeBtn = (code, label) => el('button', { class: f.theme === code ? 'active' : '', onclick: async () => {
    f.theme = code;
    if (await actState('settings:save', f)) toast(t('set.saved'), 'ok');
  } }, label);

  const hudList = S.hudVersions.length ? S.hudVersions.join(', ') : t('set.hudNone');

  view.append(el('div', { class: 'card' },
    el('div', { class: 'field' }, el('span', { class: 'small muted' }, t('set.language')),
      el('div', { class: 'segmented' }, langBtn('pl', 'Polski'), langBtn('en', 'English')),
      el('p', { class: 'small muted hint' }, t('set.languageHint'))),
    el('label', { class: 'field' }, el('span', {}, t('set.ram'), ramLabel),
      el('input', { type: 'range', min: '1', max: '32', step: '0.5', value: String(f.ramMb / 1024), oninput: (e) => { f.ramMb = Math.round(Number(e.target.value) * 1024); ramLabel.textContent = fmtGb(f.ramMb); } })),
    el('div', { class: 'field' }, el('span', { class: 'small muted' }, t('set.java')),
      el('div', { class: 'row' }, javaInput,
        el('button', { class: 'btn', onclick: async () => { const p = await act('settings:pickJava'); if (p) { f.javaPath = p; javaInput.value = p; } } }, t('c.choose')),
        el('button', { class: 'btn ghost', onclick: () => { f.javaPath = ''; javaInput.value = ''; } }, t('c.clear')))),
    el('div', { class: 'field' }, el('span', { class: 'small muted' }, t('set.theme')),
      el('div', { class: 'segmented' }, themeBtn('glass', t('set.themeGlass')), themeBtn('neon', t('set.themeNeon')), themeBtn('mc', t('set.themeMc')))),
    el('p', { class: 'small muted hint' }, t('set.menuModHint')),
    el('label', { class: 'check' }, el('input', { type: 'checkbox', checked: f.discord !== false, onchange: (e) => { f.discord = e.target.checked; } }), t('set.discord')),
    el('label', { class: 'check' }, el('input', { type: 'checkbox', checked: f.minimizeOnLaunch, onchange: (e) => { f.minimizeOnLaunch = e.target.checked; } }), t('set.minimize')),
    el('div', { class: 'row actions' },
      el('button', { class: 'btn primary', onclick: async () => { if (await actState('settings:save', f)) toast(t('set.saved'), 'ok'); } }, t('set.save')),
      el('button', { class: 'btn', onclick: () => act('app:openData') }, t('set.openData')))));

  view.append(el('div', { class: 'cols gap-top' },
    el('div', { class: 'card' }, el('h2', {}, t('set.defaultsTitle')), el('p', { class: 'muted small' }, t('set.defaultsText'))),
    el('div', { class: 'card' }, el('h2', {}, t('set.hudTitle')), el('p', { class: 'muted small' }, t('set.hudText', { list: hudList })))));

  view.append(buildUpdateCard());
  view.append(el('p', { class: 'small muted' }, t('set.dataDir', { dir: S.dataDir })));
}

// ───────────────────────── ekran: Panel właściciela ─────────────────────────

function renderOwner(view) {
  const acc = selAccount();
  const inst = selInstance();

  view.append(el('h1', {}, el('span', { class: 'crown' }, icon('crown')), t('own.title')), el('p', { class: 'sub' }, t('own.sub')));

  const idInput = el('input', { type: 'text', value: S.settings.discordAppId || '', placeholder: '1234567890123456789' });
  const feat = (title, text, ...controls) => el('div', { class: 'item' }, el('div', { class: 'info' }, el('b', {}, title), el('p', { class: 'small muted' }, text), ...controls));
  view.append(el('div', { class: 'card' },
    el('h2', {}, t('own.featTitle')), el('p', { class: 'muted small' }, t('own.featSub')),
    feat(t('own.featDiscord'), t('own.featDiscordText'), el('div', { class: 'row' }, idInput,
      el('button', { class: 'btn sm', onclick: async () => { if (await actState('owner:discordId', idInput.value)) toast(t('own.discordSaved'), 'ok'); } }, t('own.discordSave')))),
    feat(t('own.featOpt'), t('own.featOptText')),
    feat(t('own.featOpts'), t('own.featOptsText'))));

  view.append(el('div', { class: 'card owner-card' },
    el('span', { class: 'small muted' }, t('own.profile')),
    acc && profileChip(acc, true),
    acc && el('code', { class: 'uuid' }, acc.uuid)));

  view.append(el('div', { class: 'cols gap-top' },
    el('div', { class: 'card' },
      el('h2', {}, t('own.diagTitle')),
      el('div', { class: 'stack' },
        el('button', { class: 'btn', onclick: async () => { if (await act('owner:copyDiagnostics')) toast(t('own.copied'), 'ok'); } }, t('own.copyDiag')),
        el('button', { class: 'btn', onclick: () => act('owner:openLogs') }, t('own.openLogs')),
        el('button', { class: 'btn', onclick: () => act('owner:devtools') }, t('own.devtools'))),
      el('p', { class: 'small muted gap-top' }, t('own.hudStatus', { list: S.hudVersions.join(', ') || '—' }))),
    el('div', { class: 'card' },
      el('h2', {}, t('own.gameTitle')),
      el('div', { class: 'stack' },
        el('button', { class: 'btn danger', disabled: !game.running, onclick: () => act('game:stop') }, t('own.stopGame')),
        el('button', { class: 'btn', disabled: !inst, onclick: async () => { const m = await act('owner:resetOptions', inst.id); if (m) toast(m, 'ok'); } }, t('own.resetOptions')),
        el('button', { class: 'btn', onclick: async () => { if (await act('owner:clearJava')) toast(t('own.javaCleared'), 'ok'); } }, t('own.clearJava'))))));

  const nick = el('input', { type: 'text', maxlength: '16', placeholder: t('own.blockedPh'), onkeydown: (e) => { if (e.key === 'Enter') block(); } });
  const block = async () => { if (await actState('owner:blockNick', nick.value)) nick.value = ''; };
  const blocked = S.settings.blockedNicks || [];
  view.append(el('div', { class: 'card gap-top' },
    el('h2', {}, t('own.blockedTitle')),
    el('p', { class: 'muted small' }, t('own.blockedText')),
    el('div', { class: 'row' }, nick, el('button', { class: 'btn', onclick: block }, t('own.block'))),
    el('div', { class: 'list gap-top' },
      el('div', { class: 'item reserved' }, el('div', { class: 'info' }, el('b', {}, S.ownerName), el('p', {}, 'OWNER')), ownerBadge()),
      blocked.length ? blocked.map((n) => el('div', { class: 'item' }, el('div', { class: 'info' }, el('b', {}, n)),
        el('button', { class: 'btn sm', onclick: () => actState('owner:unblockNick', n) }, t('own.unblock'))))
        : el('p', { class: 'muted small' }, t('own.blockedNone')))));

  view.append(el('p', { class: 'small muted gap-top' }, t('own.note')));
}

// ───────────────────────── szkielet i nawigacja ─────────────────────────

const PAGES = [
  ['play', 'nav.play', 'play', renderPlay],
  ['instances', 'nav.instances', 'box', renderInstances],
  ['mods', 'nav.mods', 'puzzle', renderMods],
  ['accounts', 'nav.accounts', 'user', renderAccounts],
  ['settings', 'nav.settings', 'gear', renderSettings],
  ['owner', 'nav.owner', 'crown', renderOwner, true],
];

const visiblePages = () => PAGES.filter((p) => !p[4] || S.isOwner);

function go(p) { page = p; render(); }

function renderSidebar() {
  const nav = document.getElementById('nav');
  nav.replaceChildren(...visiblePages().map(([id, label, ic, , ownerOnly]) =>
    el('button', { class: 'nav-item' + (page === id ? ' active' : '') + (ownerOnly ? ' owner-nav' : ''), onclick: () => go(id) }, icon(ic), t(label))));

  const foot = document.getElementById('sidebar-foot');
  const acc = selAccount();
  foot.replaceChildren(acc ? profileChip(acc) : el('button', { class: 'btn', onclick: () => go('accounts') }, t('acc.add')));
  document.getElementById('brand-sub').textContent = 'CLIENT';
  if (U && (U.status === 'ready' || U.status === 'downloading')) {
    foot.prepend(U.status === 'ready'
      ? el('button', { class: 'btn primary upd-chip', onclick: () => act('update:install') }, t('upd.banner', { v: U.version }))
      : el('div', { class: 'upd-chip small muted' }, t('upd.bannerDl', { p: U.percent })));
  }
}

function render() {
  if (!S) return;
  applyTheme();
  setLang(S.settings.lang);
  if (page === 'owner' && !S.isOwner) page = 'play';
  renderSidebar();
  const view = document.getElementById('view');
  const scroll = view.scrollTop;
  const fresh = el('div');
  PAGES.find((p) => p[0] === page)[3](fresh);
  view.replaceChildren(...fresh.childNodes);
  view.scrollTop = scroll;
  if (page === 'play') syncPlay(); // elementy są już w dokumencie – dopiero teraz można je zsynchronizować
}

// ───────────────────────── start ─────────────────────────

bridge.on('game:state', (p) => {
  game.text = p.text || game.text;
  if (p.busy !== undefined) game.busy = !!p.busy;
  if (p.running !== undefined) { game.running = !!p.running; if (p.running) { game.busy = false; game.progress = null; } }
  if (p.closed) { game.running = false; game.busy = false; game.progress = null; }
  syncPlay();
});

bridge.on('game:progress', (p) => {
  game.progress = { label: p.label, pct: p.max ? Math.min(100, Math.round((p.value / p.max) * 100)) : null };
  syncPlay();
});

bridge.on('game:log', (line) => appendLog(line));
bridge.on('update:state', (p) => { U = p; if (S) S.update = p; refreshUpdate(); });

(async () => {
  try {
    const timer = setTimeout(() => showFatal('Launcher not responding', 'The main process did not answer app:init within 10 s. Check the terminal where npm start is running.'), 10000);
    S = await bridge.invoke('app:init');
    U = S.update || null;
    clearTimeout(timer);
    game.running = S.running;
    render();
  } catch (e) {
    showFatal('Launcher failed to start', e.message);
  }
})();
