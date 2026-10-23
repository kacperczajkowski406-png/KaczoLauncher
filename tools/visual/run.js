// Uruchamia prawdziwego Minecrafta (Fabric + Kaczo HUD) na wirtualnym ekranie i robi zrzuty ekranu.
const fs = require('fs'), path = require('path'), https = require('https'), { execSync, spawn } = require('child_process');
const { Client, Authenticator } = require('minecraft-launcher-core');

const MC = process.env.MC, JAR = process.env.JAR, STYLE = process.env.STYLE || '8';
const ROOT = path.resolve('mc-root'), GAME = path.resolve('mc-game'), OUT = path.resolve('shots');
fs.mkdirSync(path.join(GAME, 'mods'), { recursive: true }); fs.mkdirSync(OUT, { recursive: true });

function get(url) {
  return new Promise((res, rej) => https.get(url, { headers: { 'User-Agent': 'kaczo-visual-test' } }, (r) => {
    if (r.statusCode >= 300 && r.statusCode < 400 && r.headers.location) return res(get(r.headers.location));
    const b = []; r.on('data', (c) => b.push(c)); r.on('end', () => res(Buffer.concat(b)));
  }).on('error', rej));
}
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const shot = (name) => { try { execSync(`import -window root ${path.join(OUT, name + '.png')}`, { env: process.env }); console.log('zrzut', name); } catch (e) { console.log('brak zrzutu', e.message); } };

(async () => {
  // Fabric loader
  const loaders = JSON.parse((await get(`https://meta.fabricmc.net/v2/versions/loader/${MC}`)).toString());
  const lv = loaders.find((l) => l.loader.stable).loader.version;
  const prof = (await get(`https://meta.fabricmc.net/v2/versions/loader/${MC}/${lv}/profile/json`)).toString();
  const id = JSON.parse(prof).id;
  fs.mkdirSync(path.join(ROOT, 'versions', id), { recursive: true });
  fs.writeFileSync(path.join(ROOT, 'versions', id, id + '.json'), prof);
  // Fabric API + nasz mod
  const q = encodeURIComponent(`["${MC}"]`), l = encodeURIComponent('["fabric"]');
  const vs = JSON.parse((await get(`https://api.modrinth.com/v2/project/fabric-api/version?game_versions=${q}&loaders=${l}`)).toString());
  const file = vs[0].files.find((f) => f.primary) || vs[0].files[0];
  fs.writeFileSync(path.join(GAME, 'mods', file.filename), await get(file.url));
  fs.copyFileSync(JAR, path.join(GAME, 'mods', 'kaczo-hud.jar'));
  // ustawienia moda: wybrany styl nieba
  const home = process.env.HOME; fs.mkdirSync(path.join(home, '.kaczo-client'), { recursive: true });
  fs.writeFileSync(path.join(home, '.kaczo-client', 'hud-settings.json'), JSON.stringify({ mods: { sky: { enabled: true, vals: { style: STYLE } } } }));
  fs.writeFileSync(path.join(GAME, 'options.txt'), 'onboardAccessibility:false\ntutorialStep:none\nguiScale:2\nmaxFps:30\nrenderDistance:6\nlang:pl_pl\n');

  const launcher = new Client();
  const log = [];
  launcher.on('debug', (m) => { log.push(String(m)); });
  launcher.on('data', (m) => { log.push(String(m)); console.log(String(m).slice(0, 300)); });
  launcher.on('close', (c) => { console.log('gra zamknięta, kod', c); });
  const proc = await launcher.launch({
    authorization: Authenticator.getAuth('Kaczo'),
    root: ROOT, javaPath: 'java',
    version: { number: MC, type: 'release', custom: id },
    memory: { max: '2G', min: '512M' },
    overrides: { gameDirectory: GAME },
    customArgs: process.env.WORLD === '1' ? ['--quickPlaySingleplayer', 'KaczoTest'] : [],
  });
  if (!proc) { console.log('launch zwrócił null'); process.exit(1); }
  const t0 = Date.now();
  for (const s of (process.env.SHOTS || '45,75,110,150').split(',').map(Number)) {
    await sleep(Math.max(0, t0 + s * 1000 - Date.now()));
    shot('t' + s);
  }
  try { fs.copyFileSync(path.join(GAME, 'logs', 'latest.log'), path.join(OUT, 'latest.log')); } catch { /* */ }
  const cr = path.join(GAME, 'crash-reports');
  try { for (const f of fs.readdirSync(cr)) fs.copyFileSync(path.join(cr, f), path.join(OUT, f)); } catch { /* brak */ }
  fs.writeFileSync(path.join(OUT, 'launcher.log'), log.join('\n').slice(-60000));
  try { proc.kill(); } catch { /* */ }
  process.exit(0);
})().catch((e) => { console.error(e); process.exit(1); });
