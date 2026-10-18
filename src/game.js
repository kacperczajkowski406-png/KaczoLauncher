const fs = require('fs');
const path = require('path');
const { Client } = require('minecraft-launcher-core');
const { P } = require('./paths');
const { authorize } = require('./auth');
const { ensureJava } = require('./java');
const { ensureFabric } = require('./fabric');
const { javaMajorFor } = require('./versions');
const { T, GAME_LANG } = require('./lang');
const gameopts = require('./gameopts');
const resourcepack = require('./resourcepack');
const mods = require('./mods');
const discord = require('./discord');

let current = null; // { proc, instanceId }

function isRunning() {
  return current !== null;
}

function stop() {
  if (current && current.proc) current.proc.kill();
}

/**
 * Przygotowuje wszystko (logowanie, Java, loader, mody, ustawienia) i uruchamia grę.
 * `emit(channel, payload)` wysyła zdarzenia do interfejsu.
 */
async function play(opts) {
  const { instance, account, settings, emit, onAccountChanged, safe = false } = opts;
  if (current) throw new Error(T('game.already'));
  current = { proc: null, instanceId: instance.id };

  const state = (text, extra = {}) => emit('game:state', { text, ...extra });
  const progress = (label, value, max) => emit('game:progress', { label, value, max });
  const log = (line) => emit('game:log', String(line));

  try {
    state(T('game.login'), { busy: true });
    const authorization = await authorize(account);
    onAccountChanged();

    const gameDirectory = path.join(P.instances, path.basename(instance.id));
    fs.mkdirSync(gameDirectory, { recursive: true });

    // ustawienia startowe gry, język (zgodny z launcherem) i neonowy pakiet menu
    // wygląd menu rysuje teraz mod (bez texture packa) – stary pakiet launchera jest usuwany z instancji
    resourcepack.remove(gameDirectory);
    gameopts.apply(gameDirectory, {
      lang: GAME_LANG[settings.lang] || 'en_us',
      menuStyle: 'vanilla',
      packEntry: resourcepack.PACK_ENTRY,
    });

    state(T('game.java'), { busy: true });
    const major = await javaMajorFor(instance.mc);
    const javaPath = await ensureJava(major, settings, (label, done, total) => {
      state(label + '…', { busy: true });
      progress(label, done, total);
    });

    let custom;
    if (instance.loader === 'fabric') {
      state(T('game.fabric'), { busy: true });
      custom = await ensureFabric(instance.mc);

      if (instance.optimize !== false) {
        state(T('game.mods'), { busy: true });
        await mods.ensurePack(instance, log);
      }
      if (safe) { mods.removeHud(instance); log('Kaczo HUD wyłączony (tryb bezpieczny)'); }
      else mods.installHud(instance, log);
    }

    const ram = Math.max(1024, Number(instance.ramMb || settings.ramMb) || 4096);
    const launcher = new Client();

    launcher.on('debug', (m) => emit('game:log', String(m)));
    launcher.on('data', (m) => emit('game:log', String(m)));
    launcher.on('progress', (e) => {
      if (e && e.total) progress(T('game.dl', { name: e.type || '…' }), e.task, e.total);
    });
    launcher.on('download-status', (e) => {
      if (e && e.total) progress(T('game.dl', { name: e.name || '…' }), e.current, e.total);
    });
    const startedAt = Date.now();
    launcher.on('close', (rawCode) => {
      const code = Number(rawCode) | 0; // 4294967295 -> -1
      current = null;
      discord.stop();
      const crashed = code !== 0 && code !== 130 && code !== 143;
      let diag = null;
      if (crashed) {
        try { diag = diagnose(gameDirectory, startedAt); } catch { /* brak raportu */ }
      }
      emit('game:state', { text: T('game.closed', { code }), running: false, closed: true });
      if (crashed) {
        const hudInvolved = !!(diag && /kaczohud|pl\.kaczo/i.test(diag.report));
        const retry = !safe && instance.loader === 'fabric' && Date.now() - startedAt < 5 * 60 * 1000 && (hudInvolved || !diag || diag.report.length === 0);
        emit('game:crash', { code, summary: diag ? diag.summary : '', report: diag ? diag.report : '', hudInvolved, retry, mc: instance.mc });
        if (retry) {
          emit('game:state', { text: 'Kaczo HUD powodował błąd – uruchamiam grę ponownie bez niego…', busy: true });
          setTimeout(() => play({ ...opts, safe: true }).catch((e) => emit('game:state', { text: String((e && e.message) || e), running: false, closed: true })), 800);
        }
      }
    });

    state(T('game.files'), { busy: true });
    const proc = await launcher.launch({
      authorization,
      root: P.common,
      javaPath,
      version: custom
        ? { number: instance.mc, type: 'release', custom }
        : { number: instance.mc, type: 'release' },
      memory: { max: `${ram}M`, min: '512M' },
      overrides: { gameDirectory },
    });

    if (!proc) {
      current = null;
      throw new Error(T('game.failed'));
    }
    current.proc = proc;
    if (settings.discord !== false) {
      discord.start(settings.discordAppId || discord.defaultId(), {
        details: 'Gra w Kaczo Client',
        state: `Minecraft ${instance.mc}${instance.loader === 'fabric' ? ' • Fabric' : ''}`,
        timestamps: { start: Math.floor(Date.now() / 1000) },
        assets: { large_image: 'logo', large_text: 'Kaczo Client' },
      });
    }
    emit('game:state', { text: T('game.running'), running: true });
    return true;
  } catch (e) {
    current = null;
    throw e;
  }
}

/** Zbiera raport z awarii gry: najnowszy crash-report (z ostatnich minut) albo koniec latest.log. */
function diagnose(gameDirectory, since) {
  let report = '';
  const dir = path.join(gameDirectory, 'crash-reports');
  try {
    const files = fs.readdirSync(dir).map((f) => ({ f, t: fs.statSync(path.join(dir, f)).mtimeMs }))
      .filter((x) => x.t >= since - 5000).sort((a, b) => b.t - a.t);
    if (files.length) report = fs.readFileSync(path.join(dir, files[0].f), 'utf8');
  } catch { /* brak folderu */ }
  if (!report) {
    try {
      const log = fs.readFileSync(path.join(gameDirectory, 'logs', 'latest.log'), 'utf8').split('\n');
      report = log.slice(-150).join('\n');
    } catch { /* brak logu */ }
  }
  const lines = report.split('\n');
  const key = lines.filter((l) => /Description:|Caused by|Exception|Error|Mixin|kaczohud/i.test(l)).slice(0, 6).join('\n');
  return { summary: key.slice(0, 700), report: report.slice(0, 200000) };
}

module.exports = { play, stop, isRunning };
