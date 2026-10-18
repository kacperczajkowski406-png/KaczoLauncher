#!/usr/bin/env python3
"""
Migracja kodu moda z nazw yarn (1.21.x) na nazwy Mojang (Minecraft 26.x, bez obfuskacji).

  migrate26.py gen  <map.json> <classes.txt> <src_java_dir> <out_java_dir> [overlay_dir]
      Etap 1: zamienia nazwy klas (importy i użycia), kopiuje nakładkę ręcznie pisanych plików.
  migrate26.py fix  <map.json> <out_java_dir> <build.log> <mc.jar>
      Etap 2: czyta błędy javac „cannot find symbol” i zamienia nazwy metod/pól na właściwe (po typie odbiorcy).
      Wypisuje liczbę poprawek; kod wyjścia 0 = coś poprawiono, 3 = nic do poprawienia.
"""
import json, os, re, subprocess, sys, shutil, collections

# nazwy, które w 26.x różnią się od mapowań Mojang z 1.21.11 (lub brak ich w mapie)
CLASS_OVERRIDES = {
    'net.minecraft.client.gui.DrawContext': 'net.minecraft.client.gui.GuiGraphicsExtractor',
    'net.minecraft.util.Identifier': 'net.minecraft.resources.Identifier',
    'net.minecraft.client.gui.RotatingCubeMapRenderer': 'net.minecraft.client.renderer.Panorama',
}

def load_map(p):
    m = json.load(open(p))
    classes = dict(m['classes']); classes.update(CLASS_OVERRIDES)
    inv = collections.defaultdict(list)
    for y, mo in classes.items(): inv[mo].append(y)
    ymem = collections.defaultdict(lambda: collections.defaultdict(set))
    for k, v in m['methods'].items():
        c, n, d = k.split('#'); ymem[c][n].add(v)
    for k, v in m['fields'].items():
        c, n = k.split('#'); ymem[c][n].add(v)
    return classes, inv, ymem

def simple(fq): return fq.rsplit('.', 1)[-1]

# ───────────────────────── etap 1 ─────────────────────────
def gen(args):
    mapp, clsfile, src, out = args[:4]
    overlay = args[4] if len(args) > 4 else None
    classes, inv, ymem = load_map(mapp)
    have = set(l.strip().replace('/', '.') for l in open(clsfile))
    bysimple = collections.defaultdict(list)
    for c in have:
        if '$' not in c: bysimple[simple(c)].append(c)
    def tr(fq):
        mo = classes.get(fq)
        if mo and mo in have: return mo
        if mo and mo not in have:
            cand = bysimple.get(simple(mo), [])
            if len(cand) == 1: return cand[0]
        return mo or fq
    shutil.rmtree(out, ignore_errors=True)
    missing = set()
    fq_re = re.compile(r'\b(net\.minecraft(?:\.[a-z0-9_]+)+\.[A-Z]\w*|com\.mojang(?:\.[a-z0-9_]+)+\.[A-Z]\w*)')
    imp_re = re.compile(r'(?m)^import ((?:net\.minecraft|com\.mojang)[\w.]*);')
    for root, _, files in os.walk(src):
        for f in files:
            if not f.endswith('.java'): continue
            p = os.path.join(root, f)
            s = open(p, encoding='utf8').read()
            simple_map = {}
            def one(fq):
                n = tr(fq)
                if n not in have and (fq.startswith('net.minecraft') or fq.startswith('com.mojang')): missing.add(f'{fq} -> {n}')
                if simple(fq) != simple(n): simple_map[simple(fq)] = simple(n)
                return n
            s = imp_re.sub(lambda m: 'import ' + one(m.group(1)) + ';', s)
            cut = s.rfind('\nimport ')
            cut = s.find('\n', cut + 1) if cut >= 0 else 0
            head, body = s[:cut], s[cut:]
            body = fq_re.sub(lambda m: one(m.group(1)), body)
            for a_, b_ in sorted(simple_map.items(), key=lambda x: -len(x[0])):
                body = re.sub(r'(?<![\w.])' + re.escape(a_) + r'(?![\w])', b_, body)
            dest = os.path.join(out, os.path.relpath(p, src))
            os.makedirs(os.path.dirname(dest), exist_ok=True)
            open(dest, 'w', encoding='utf8').write(head + body)
    if overlay and os.path.isdir(overlay):
        for root, _, files in os.walk(overlay):
            for f in files:
                p = os.path.join(root, f); dest = os.path.join(out, os.path.relpath(p, overlay))
                os.makedirs(os.path.dirname(dest), exist_ok=True); shutil.copyfile(p, dest)
    for x in sorted(missing): print('BRAK KLASY:', x)
    print('gen ok')

# ───────────────────────── etap 2 ─────────────────────────
_sup = {}
def supers(jar, cls):
    """Klasa + wszystkie nadklasy/interfejsy (kolejność BFS)."""
    out = []; todo = [cls]
    while todo:
        c = todo.pop(0)
        if c in out: continue
        out.append(c)
        if c not in _sup:
            r = subprocess.run(['javap', '-p', '-cp', jar, c], capture_output=True, text=True)
            head = ''
            for l in r.stdout.split('\n'):
                if ' class ' in l or ' interface ' in l or l.startswith(('class ', 'interface ')) or ' enum ' in l or ' record ' in l:
                    head = l; break
            names = []
            ext = re.search(r'\bextends\s+(.+?)(?:\s+implements\s+|\s*\{|$)', head)
            imp = re.search(r'\bimplements\s+(.+?)\s*\{', head)
            for grp in (ext, imp):
                if grp:
                    txt = re.sub(r'<[^<>]*>', '', re.sub(r'<[^<>]*>', '', re.sub(r'<[^<>]*>', '', grp.group(1))))
                    names += [n.strip() for n in txt.split(',') if n.strip()]
            _sup[c] = names
        todo += _sup[c]
    return out

_members = {}
def members(jar, cls):
    if cls not in _members:
        r = subprocess.run(['javap', '-p', '-cp', jar, cls], capture_output=True, text=True)
        meth = collections.defaultdict(list); fld = set()
        for l in r.stdout.split('\n'):
            l = l.strip().rstrip(';')
            m = re.match(r'.*?([\w$]+)\((.*)\)(?:\s+throws .*)?$', l)
            if m and '(' in l:
                params = m.group(2); n = 0 if not params.strip() else len(re.sub(r'<[^<>]*>', '', re.sub(r'<[^<>]*>', '', params)).split(','))
                meth[m.group(1)].append(n)
            else:
                m2 = re.match(r'.*?([\w$]+)$', l)
                if m2 and '(' not in l and l and not l.startswith(('Compiled', 'public class', 'public abstract', 'public final', 'public interface', 'public enum', 'public record', '}')) : fld.add(m2.group(1))
        _members[cls] = (meth, fld)
    return _members[cls]

ERR = re.compile(r'^(?P<file>\S+\.java):(?P<line>\d+): error: cannot find symbol$')

def fix(args):
    mapp, out, log, jar = args[:4]
    classes, inv, ymem = load_map(mapp)
    allc = [l.strip().replace('/', '.') for l in subprocess.run(['unzip', '-Z1', jar], capture_output=True, text=True).stdout.split('\n') if l.endswith('.class')]
    allc = [c[:-6] for c in allc]
    bysimple = collections.defaultdict(list)
    for c in allc: bysimple[simple(c.replace('$', '.')).split('.')[-1]].append(c)
    lines = open(log, encoding='utf8', errors='replace').read().split('\n')
    edits = collections.defaultdict(list)   # file -> [(line, col, old, new)]
    unresolved = []
    i = 0
    while i < len(lines):
        m = ERR.match(re.sub(r'^\s*', '', lines[i])) if lines[i].strip().endswith('cannot find symbol') else None
        if not m:
            i += 1; continue
        src_line = lines[i + 1] if i + 1 < len(lines) else ''
        caret = lines[i + 2] if i + 2 < len(lines) else ''
        sym = loc = None
        for j in range(i + 3, min(i + 8, len(lines))):
            t = lines[j].strip()
            if t.startswith('symbol:'): sym = t[len('symbol:'):].strip()
            if t.startswith('location:'): loc = t[len('location:'):].strip()
        i += 3
        if not sym: continue
        kind, rest = sym.split(None, 1) if ' ' in sym else (sym, '')
        if kind not in ('method', 'variable'): unresolved.append((m.group('file'), m.group('line'), sym, loc)); continue
        name = re.match(r'[\w$]+', rest).group(0)
        nargs = None
        if kind == 'method':
            ar = re.search(r'\((.*)\)', rest)
            ptxt = ar.group(1) if ar else ''
            nargs = 0 if not ptxt.strip() else len(re.sub(r'<[^<>]*>', '', ptxt).split(','))
        # typ odbiorcy
        tname = None
        if loc:
            lm = re.search(r'(?:of type|class|interface|enum)\s+([\w.$<>?, ]+)$', loc)
            if lm: tname = re.sub(r'<.*', '', lm.group(1)).strip()
        cands = []
        if tname:
            tn = tname.replace('$', '.')
            if tn in allc: cands = [tn]
            else:
                cands = bysimple.get(simple(tn).split('.')[-1], [])
                # klasa własna (pl.kaczo...) – szukamy jej nadklasy w pliku
                if not cands:
                    ownf = None
                    for root, _, fs in os.walk(out):
                        if simple(tn) + '.java' in fs: ownf = os.path.join(root, simple(tn) + '.java')
                    if ownf:
                        em = re.search(r'class\s+\w+(?:<[^>]*>)?\s+extends\s+([\w.]+)', open(ownf, encoding='utf8').read())
                        if em:
                            ex = em.group(1)
                            cands = bysimple.get(simple(ex), [])
                            if not cands:       # np. KScreen -> Screen
                                ownf2 = None
                                for root, _, fs in os.walk(out):
                                    if simple(ex) + '.java' in fs: ownf2 = os.path.join(root, simple(ex) + '.java')
                                if ownf2:
                                    em2 = re.search(r'class\s+\w+(?:<[^>]*>)?\s+extends\s+([\w.]+)', open(ownf2, encoding='utf8').read())
                                    if em2: cands = bysimple.get(simple(em2.group(1)), [])
        new = None
        for c in cands:
            for anc in supers(jar, c.replace('.', '.')):
                for yc in inv.get(anc, []):
                    for mo in ymem.get(yc, {}).get(name, ()):
                        meth, fld = members(jar, anc)
                        if mo == name: continue
                        ok = (mo in meth and (nargs is None or nargs in meth[mo])) if kind == 'method' else (mo in fld)
                        if ok and not new: new = mo
                if new: break
            if new: break
        if not new:
            unresolved.append((m.group('file'), m.group('line'), sym, loc)); continue
        col = caret.find('^')
        ln = int(m.group('line'))
        edits[m.group('file')].append((ln, col, name, new))
    changed = 0
    for f, es in edits.items():
        path = f if os.path.isabs(f) else None
        if not path or not os.path.exists(path):
            # javac pokazuje ścieżkę bezwzględną; w razie czego szukamy po nazwie
            base = os.path.basename(f)
            for root, _, fs in os.walk(out):
                if base in fs: path = os.path.join(root, base)
        if not path or not os.path.exists(path): continue
        L = open(path, encoding='utf8').read().split('\n')
        for ln, col, old, new in sorted(set(es), key=lambda x: (-x[0], -x[1])):
            s = L[ln - 1]
            k = s.find(old, max(0, col))
            if k < 0: k = s.find(old)
            if k < 0: continue
            L[ln - 1] = s[:k] + new + s[k + len(old):]; changed += 1
        open(path, 'w', encoding='utf8').write('\n'.join(L))
    for u in unresolved[:60]: print('NIEROZWIĄZANE:', u)
    print(f'poprawek: {changed}, nierozwiązanych: {len(unresolved)}')
    sys.exit(0 if changed else 3)

if __name__ == '__main__':
    {'gen': gen, 'fix': fix}[sys.argv[1]](sys.argv[2:])
