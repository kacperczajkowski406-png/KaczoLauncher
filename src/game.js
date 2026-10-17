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
async function play({ instance, account, settings, emit, onAccountChanged }) {
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
      mods.installHud(instance, log);
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
    launcher.on('close', (code) => {
      current = null;
      discord.stop();
      emit('game:state', { text: T('game.closed', { code }), running: false, closed: true });
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
      discord.start(settings.discordAppId, {
        details: 'Kaczo Client',
        state: `Minecraft ${instance.mc}`,
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

module.exports = { play, stop, isRunning };
