const path = require('path'), fs = require('fs'), os = require('os');
process.env.APPDATA = fs.mkdtempSync(path.join(os.tmpdir(), 'kt-'));
const Mod = require('module'); const orig = Mod._load;
Mod._load = function (r, ...a) { if (r === 'electron') return { app: { getPath: () => process.env.APPDATA } }; return orig.call(this, r, ...a); };
const mods = require('../../src/mods');
const { P } = require('../../src/paths');
(async () => {
  for (const mc of ['1.21.4', '1.21.1']) {
    const inst = { id: 'i' + mc, mc, loader: 'fabric', shaders: 'complementary-reimagined' };
    const logs = [];
    await mods.ensureShaders(inst, (m) => logs.push(m));
    const dir = path.join(P.instances, inst.id);
    const ls = (d) => { try { return fs.readdirSync(path.join(dir, d)).join(','); } catch { return '-'; } };
    console.log(`::notice title=shd ${mc}::${logs.join(' | ')} | mods=${ls('mods')} | packs=${ls('shaderpacks')} | iris=${(() => { try { return fs.readFileSync(path.join(dir, 'config', 'iris.properties'), 'utf8').replace(/\n/g, ';'); } catch { return '-'; } })()}`);
  }
})().catch((e) => { console.log('::error title=shd::' + e); process.exit(1); });
