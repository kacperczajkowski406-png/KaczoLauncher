"""Generuje własne (oryginalne) pikselowe tekstury w stylu klocków: ziemia, kamień, deski, trawa, niebo z chmurami."""
import random
from PIL import Image
OUT = 'renderer/tex/'

def noise_tile(base, spread, seed, size=16, scale=4, specks=None):
    r = random.Random(seed)
    im = Image.new('RGB', (size, size))
    for y in range(size):
        for x in range(size):
            d = r.randint(-spread, spread)
            im.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in base))
    if specks:
        col, n = specks
        for _ in range(n):
            im.putpixel((r.randrange(size), r.randrange(size)), col)
    return im.resize((size * scale, size * scale), Image.NEAREST)

noise_tile((92, 64, 40), 12, 1, specks=((60, 40, 24), 18)).save(OUT + 'dirt.png')
noise_tile((112, 112, 112), 9, 2, specks=((84, 84, 84), 22)).save(OUT + 'stone.png')
noise_tile((70, 70, 74), 7, 3, specks=((50, 50, 54), 22)).save(OUT + 'stone-dark.png')

# deski: poziome słoje
r = random.Random(4)
pl = Image.new('RGB', (16, 16))
for y in range(16):
    row = (156, 120, 70) if (y // 4) % 2 == 0 else (142, 108, 62)
    for x in range(16):
        d = r.randint(-6, 6)
        c = tuple(max(0, min(255, v + d)) for v in row)
        if y % 4 == 3: c = tuple(v - 30 for v in c)
        pl.putpixel((x, y), c)
pl.resize((64, 64), Image.NEAREST).save(OUT + 'planks.png')

# pasek trawy: zielony wierzch + ziemia pod spodem (16 px szerokości, powtarzany w poziomie)
r = random.Random(5)
g = Image.new('RGB', (16, 16))
for y in range(16):
    for x in range(16):
        if y < 4 or (y < 6 and r.random() < 0.5):
            base = (96, 168, 56); d = r.randint(-14, 14)
        else:
            base = (92, 64, 40); d = r.randint(-12, 12)
        g.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in base))
g.resize((64, 64), Image.NEAREST).save(OUT + 'grass.png')

# niebo z blokowymi chmurami (szerokie, powtarzalne)
W, H = 128, 64
sky = Image.new('RGB', (W, H))
for y in range(H):
    t = y / H
    sky_c = (int(110 + 60 * t), int(170 + 50 * t), int(255 - 10 * t))
    for x in range(W): sky.putpixel((x, y), sky_c)
r = random.Random(6)
for _ in range(7):
    cx, cy = r.randrange(W), r.randrange(6, 40)
    for dx in range(-r.randint(6, 11), r.randint(6, 11)):
        for dy in range(-1, r.randint(1, 3)):
            sky.putpixel(((cx + dx) % W, cy + dy), (240, 246, 255))
sky.resize((W * 4, H * 4), Image.NEAREST).save(OUT + 'sky.png')
print('ok')
