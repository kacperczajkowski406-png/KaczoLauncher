#!/usr/bin/env python3
"""Buduje tablicę nazw yarn -> Mojang (dla MC 1.21.11) łącząc yarn, intermediary i oficjalne mapowania Mojang.
Wynik: out/map.json  {classes:{yarn:mojang}, methods:{"yarnCls#name#desc":mojang}, fields:{...}}"""
import io, json, re, sys, urllib.request, zipfile, collections

MC = '1.21.11'; YARN = '1.21.11+build.6'

def get(url):
    req = urllib.request.Request(url, headers={'User-Agent': 'kaczo-tools'})
    return urllib.request.urlopen(req, timeout=120).read()

def tiny_from_jar(url):
    z = zipfile.ZipFile(io.BytesIO(get(url)))
    return z.read('mappings/mappings.tiny').decode().split('\n')

def parse_tiny(lines):
    hdr = lines[0].split('\t'); cols = hdr[3:]
    classes = []; cur = None
    for l in lines[1:]:
        if not l: continue
        if l[0] == 'c':
            p = l.split('\t'); cur = {'n': p[1:], 'm': [], 'f': []}; classes.append(cur)
        elif l.startswith('\tm\t'):
            p = l.split('\t'); cur['m'].append((p[2], p[3:]))
        elif l.startswith('\tf\t'):
            p = l.split('\t'); cur['f'].append((p[2], p[3:]))
    return cols, classes

yl = tiny_from_jar(f'https://maven.fabricmc.net/net/fabricmc/yarn/{YARN}/yarn-{YARN}-mergedv2.jar')
il = tiny_from_jar(f'https://maven.fabricmc.net/net/fabricmc/intermediary/{MC}/intermediary-{MC}-v2.jar')
ycols, yclasses = parse_tiny(yl)      # intermediary, named
icols, iclasses = parse_tiny(il)      # official, intermediary

inter2off = {c['n'][1]: c for c in iclasses}
# per-class: intermediary -> (official class, {inter method name+desc -> official name}, {inter field -> official})
off_m = {}; off_f = {}
for c in iclasses:
    for d, n in c['m']: off_m[(c['n'][1], n[1], d)] = n[0]     # key: inter class, inter method, official desc
    for d, n in c['f']: off_f[(c['n'][1], n[1])] = n[0]

# Mojang proguard
man = json.loads(get('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
vurl = [v['url'] for v in man['versions'] if v['id'] == MC][0]
vj = json.loads(get(vurl))
moj = get(vj['downloads']['client_mappings']['url']).decode().split('\n')

moj_cls = {}            # mojang dotted -> official dotted
off2moj = {}
cur = None; members = collections.defaultdict(lambda: {'m': [], 'f': []})
for l in moj:
    if not l or l.startswith('#'): continue
    if not l.startswith(' '):
        a, b = l.rstrip(':').split(' -> ')
        moj_cls[a] = b; off2moj[b] = a; cur = b
    else:
        s = l.strip()
        m = re.match(r'(?:\d+:\d+:)?(\S+) (\S+?)(\(.*\))? -> (\S+)$', s)
        if not m: continue
        typ, name, args, obf = m.groups()
        if args is None: members[cur]['f'].append((name, obf, typ))
        else: members[cur]['m'].append((name, obf, typ, args[1:-1]))

PRIM = {'int': 'I', 'long': 'J', 'double': 'D', 'float': 'F', 'boolean': 'Z', 'byte': 'B', 'char': 'C', 'short': 'S', 'void': 'V'}
def desc_of(t):
    dims = 0
    while t.endswith('[]'): dims += 1; t = t[:-2]
    if t in PRIM: d = PRIM[t]
    else:
        off = moj_cls.get(t, t)
        d = 'L' + off.replace('.', '/') + ';'
    return '[' * dims + d

# Mojang method lookup: (official class dotted, official name, official desc) -> mojang name
mm = {}; mf = {}
for ocls, mem in members.items():
    for name, obf, typ, args in mem['m']:
        ad = ''.join(desc_of(a) for a in args.split(',') if a)
        mm[(ocls.replace('.', '/'), obf, '(' + ad + ')' + desc_of(typ))] = name
    for name, obf, typ in mem['f']:
        mf[(ocls.replace('.', '/'), obf)] = name

out = {'classes': {}, 'methods': {}, 'fields': {}}
miss = 0
for yc in yclasses:
    inter, named = yc['n']
    ic = inter2off.get(inter)
    if not ic: continue
    off = ic['n'][0]
    mojname = off2moj.get(off.replace('/', '.'))
    ycn = named.replace('/', '.')
    if mojname: out['classes'][ycn] = mojname
    for d, n in yc['m']:
        # d jest deskryptorem w nazwach intermediary – szukamy oficjalnego przez tablicę intermediary
        pass
    # metody: dopasuj po intermediary (nazwa unikalna w klasie+desc)
    for d, n in yc['m']:
        key = (inter, n[0])
        # znajdź oficjalną nazwę i deskryptor
        for d2, n2 in ic['m']:
            if n2[1] == n[0]:
                mname = mm.get((off, n2[0], d2))
                if mname: out['methods'][f'{ycn}#{n[1]}#{d2}'] = mname
                break
    for d, n in yc['f']:
        for d2, n2 in ic['f']:
            if n2[1] == n[0]:
                fname = mf.get((off, n2[0]))
                if fname: out['fields'][f'{ycn}#{n[1]}'] = fname
                break

import os
os.makedirs('out', exist_ok=True)
json.dump(out, open('out/map.json', 'w'), indent=0)
print(len(out['classes']), len(out['methods']), len(out['fields']))
