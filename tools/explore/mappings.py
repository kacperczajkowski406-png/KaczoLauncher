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
ycols, yclasses = parse_tiny(yl)      # intermediary, named

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
for yc in yclasses:
    off, inter, named = yc['n']
    mojname = off2moj.get(off.replace('/', '.'))
    ycn = named.replace('/', '.')
    if mojname: out['classes'][ycn] = mojname
    for d, n in yc['m']:
        mname = mm.get((off, n[0], d))
        if mname: out['methods'][f'{ycn}#{n[2]}#{d}'] = mname
    for d, n in yc['f']:
        fname = mf.get((off, n[0]))
        if fname: out['fields'][f'{ycn}#{n[2]}'] = fname

import os
os.makedirs('out', exist_ok=True)
json.dump(out, open('out/map.json', 'w'), indent=0)
print(len(out['classes']), len(out['methods']), len(out['fields']))
