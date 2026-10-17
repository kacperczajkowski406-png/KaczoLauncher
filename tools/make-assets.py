#!/usr/bin/env python3
"""
Generuje wszystkie grafiki Kaczo Client z jednej neonowej kaczki narysowanej samymi liniami:
logo/ikonę launchera, logo menu gry, panoramę tła, neonowe przyciski i ikonę .ico.

Wymaga: playwright (z Chromium) i Pillow.   Uruchom:  python3 tools/make-assets.py
"""
import asyncio, math, os, random, sys
from pathlib import Path
from playwright.async_api import async_playwright
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
PACK = ROOT / "kaczo-pack" / "assets" / "minecraft"
GUI = PACK / "textures" / "gui"

YELLOW, YELLOW_CORE = "#ffe14a", "#fff7c8"
ORANGE, ORANGE_CORE = "#ff8f3a", "#ffe3c6"
PINK, PINK_CORE = "#ff4fd8", "#ffd3f5"
CYAN, CYAN_CORE = "#35e6ff", "#d9fbff"

# Kaczka w siatce 128x128, zwrócona w prawo. Każdy element: (ścieżka, kolor, kolor rdzenia)
DUCK = [
    # korpus + szyja + głowa jednym obrysem
    ("M17 52 C26 58 31 62 40 64 L58 62 L67.7 60.3 A23 23 0 1 1 100.3 60.3 "
     "C104 64 108 72 106 82 C103 100 86 110 64 110 C38 110 22 96 21 76 C21 66 20 59 17 52 Z",
     YELLOW, YELLOW_CORE),
    # dziób i linia dzioba
    ("M105.9 37 C116 35 125 41 126 47 C125 54 115 57 104.7 54", ORANGE, ORANGE_CORE),
    ("M105 46 C111 47 118 47 125 46.5", ORANGE, ORANGE_CORE),
    # skrzydło
    ("M43 76 C52 68 72 68 82 80 C76 94 54 96 43 76 Z", ORANGE, ORANGE_CORE),
    # pikselowe okulary
    ("M71 36 H103 V41 H98 V46 H91 V49 H85 V46 H79 V43 H71 Z", PINK, PINK_CORE),
    # woda
    ("M6 114 C20 106 32 106 46 114 C60 122 72 122 86 114 C100 106 112 106 122 112", CYAN, CYAN_CORE),
    ("M18 124 C30 119 40 119 52 124 C64 129 74 129 86 124", CYAN, CYAN_CORE),
]

FILTER = """
<filter id="glow" x="-40%" y="-40%" width="180%" height="180%" color-interpolation-filters="sRGB">
  <feGaussianBlur in="SourceGraphic" stdDeviation="1.6" result="b1"/>
  <feGaussianBlur in="SourceGraphic" stdDeviation="5" result="b2"/>
  <feMerge><feMergeNode in="b2"/><feMergeNode in="b2"/><feMergeNode in="b1"/><feMergeNode in="SourceGraphic"/></feMerge>
</filter>
<filter id="tglow" x="-20%" y="-60%" width="140%" height="220%" color-interpolation-filters="sRGB">
  <feGaussianBlur in="SourceGraphic" stdDeviation="4" result="b1"/>
  <feGaussianBlur in="SourceGraphic" stdDeviation="12" result="b2"/>
  <feMerge><feMergeNode in="b2"/><feMergeNode in="b2"/><feMergeNode in="b1"/><feMergeNode in="SourceGraphic"/></feMerge>
</filter>
"""


def duck(transform="", width=3.2, core=1.2):
    glow = "".join(
        f'<path d="{d}" stroke="{c}" stroke-width="{width}"/>' for d, c, _ in DUCK)
    cores = "".join(
        f'<path d="{d}" stroke="{k}" stroke-width="{core}"/>' for d, _, k in DUCK)
    return (f'<g transform="{transform}" fill="none" stroke-linecap="round" stroke-linejoin="round">'
            f'<g filter="url(#glow)">{glow}</g>{cores}</g>')


def neon_text(x, y, text, size, color, core, length=None, weight=900, spacing=0):
    attrs = (f'x="{x}" y="{y}" text-anchor="middle" font-family="Inter, Segoe UI, sans-serif" '
             f'font-weight="{weight}" font-size="{size}" letter-spacing="{spacing}"')
    if length:
        attrs += f' textLength="{length}" lengthAdjust="spacing"'
    return (f'<text {attrs} fill="{color}" filter="url(#tglow)">{text}</text>'
            f'<text {attrs} fill="{core}">{text}</text>')


def svg(w, h, body, defs="", bg=""):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">'
            f'<defs>{FILTER}{defs}</defs>{bg}{body}</svg>')


# ───────────────────────── poszczególne grafiki ─────────────────────────

PIXEL_DUCK = [
    "........................",
    "..............KKKKKK....",
    ".............KYYYYYYK...",
    "............KYWWYYYYYK..",
    "............KYWYYEEYYYK.",
    "............KYYYYEEYYKKK",
    "............KYYYYYYYKOOOK",
    "....K.......KYYYYYYKOooOK",
    "...KYK......KYYYYYYKKKKK.",
    "..KYYYK....KYYYYYYK.....",
    "..KYYYYKKKKKYYYYYYK.....",
    "...KYYYYYYYYYYYYYYK.....",
    "...KYYYYYYYYYYYYYYYK....",
    "..KYYYYYyyyyyyYYYYYK....",
    "..KYYYYyyyyyyyyyYYYK....",
    "..KYYYYYyyyyyyyYYYYK....",
    "...KYYYYYYYYYYYYYYK.....",
    "....KYYYYYYYYYYYYK......",
    ".....KKYYYYYYYYKK.......",
    ".......KKKKKKKK.........",
]
PIXEL_PAL = {"K": "#3a260a", "Y": "#ffd83a", "y": "#e8b420", "W": "#fff4aa", "E": "#1e160c", "O": "#ff8a1f", "o": "#c8600f"}
PW, PH = 25, len(PIXEL_DUCK)


def pixel_duck(x, y, scale):
    """Pikselowa kaczka w stylu Minecrafta (bez neonów) jako prostokąty SVG."""
    out = []
    for r, row in enumerate(PIXEL_DUCK):
        c = 0
        while c < len(row):
            ch = row[c]
            if ch not in PIXEL_PAL:
                c += 1
                continue
            e = c
            while e < len(row) and row[e] == ch:
                e += 1
            out.append(f'<rect x="{x + c * scale}" y="{y + r * scale}" width="{(e - c) * scale}" height="{scale}" fill="{PIXEL_PAL[ch]}"/>')
            c = e
    return f'<g shape-rendering="crispEdges">{"".join(out)}</g>'


def tile_svg(size=128):
    """Ikona: jasnoniebieski kafelek z pikselową kaczką."""
    defs = ('<linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">'
            '<stop offset="0" stop-color="#8fd3ff"/><stop offset="1" stop-color="#4fa9ee"/></linearGradient>')
    bg = ('<rect width="128" height="128" rx="26" fill="url(#sky)"/>'
          '<rect x="2" y="2" width="124" height="124" rx="24" fill="none" stroke="#ffffff" stroke-opacity="0.45" stroke-width="3"/>')
    s = svg(128, 128, pixel_duck(14, 26, 4), defs, bg)
    return s.replace('width="128" height="128" viewBox', f'width="{size}" height="{size}" viewBox', 1)


def duck_only_svg():
    return svg(100, 80, pixel_duck(0, 0, 4))


def title_logo_svg():
    """Logo menu gry 1024x256 (górne 176 px to właściwe logo): sama pikselowa kaczka."""
    return svg(1024, 256, pixel_duck(412, 8, 8))


def edition_svg():
    return svg(512, 64, "")


def pack_icon_svg():
    return tile_svg(128)


def panorama_face(i):
    rnd = random.Random(1000 + i)
    S = 1024
    parts = []
    defs = []
    if i < 4:  # boki: niebo → poświata horyzontu → podłoga z siatką
        defs.append('<linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">'
                    '<stop offset="0" stop-color="#04060f"/><stop offset="0.28" stop-color="#0a0e2a"/>'
                    '<stop offset="0.43" stop-color="#2a1260"/><stop offset="0.5" stop-color="#c2279c"/>'
                    '<stop offset="0.5" stop-color="#0a0b1f"/><stop offset="1" stop-color="#05060f"/></linearGradient>')
        parts.append(f'<rect width="{S}" height="{S}" fill="url(#sky)"/>')
        for _ in range(70):  # gwiazdy tylko nad horyzontem
            x, y, r = rnd.uniform(0, S), rnd.uniform(0, 400), rnd.choice([1, 1, 1.5, 2])
            parts.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{r}" fill="#cfe9ff" opacity="{rnd.uniform(.35, .9):.2f}"/>')
        # poziome linie podłogi (gęstnieją ku horyzontowi)
        for k in range(1, 15):
            y = 512 + 512 * (k / 15) ** 2.2
            op = 0.25 + 0.5 * (k / 15)
            parts.append(f'<line x1="0" y1="{y:.1f}" x2="{S}" y2="{y:.1f}" stroke="{PINK}" stroke-width="2.4" opacity="{op:.2f}"/>')
        for k in range(0, 17):  # pionowe, równo – brzegi ścian pasują do siebie
            x = k * S / 16
            parts.append(f'<line x1="{x:.1f}" y1="512" x2="{x:.1f}" y2="{S}" stroke="{PINK}" stroke-width="2" opacity="0.28"/>')
        parts.append(f'<g filter="url(#glow)"><line x1="0" y1="512" x2="{S}" y2="512" stroke="{CYAN}" stroke-width="6"/></g>'
                     f'<line x1="0" y1="512" x2="{S}" y2="512" stroke="{CYAN_CORE}" stroke-width="2"/>')
        # tło bez kaczki: kaczki są w logo, a obracająca się panorama nie może zasłaniać napisu
    elif i == 4:  # góra: gwiazdy
        parts.append(f'<rect width="{S}" height="{S}" fill="#04060f"/>')
        for _ in range(160):
            x, y, r = rnd.uniform(0, S), rnd.uniform(0, S), rnd.choice([1, 1, 1.5, 2, 2.5])
            parts.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{r}" fill="#cfe9ff" opacity="{rnd.uniform(.3, .95):.2f}"/>')
    else:  # dół: siatka
        parts.append(f'<rect width="{S}" height="{S}" fill="#05060f"/>')
        for k in range(0, 17):
            p = k * S / 16
            parts.append(f'<line x1="{p:.1f}" y1="0" x2="{p:.1f}" y2="{S}" stroke="{PINK}" stroke-width="2" opacity="0.45"/>'
                         f'<line x1="0" y1="{p:.1f}" x2="{S}" y2="{p:.1f}" stroke="{PINK}" stroke-width="2" opacity="0.45"/>')
    return svg(S, S, "".join(parts), "".join(defs))


def menu_background_svg(size=64):
    body = (f'<rect width="{size}" height="{size}" fill="#080c1c"/>'
            f'<rect x="0" y="0" width="{size}" height="{size}" fill="none" stroke="#15204a" stroke-width="2"/>'
            f'<circle cx="{size // 2}" cy="{size // 2}" r="1.6" fill="#35e6ff" opacity="0.55"/>')
    return svg(size, size, body)


def button_svg(border, fill, fill2, w=400, h=40, line=None, accent=None):
    """Płaski, ciemny przycisk (jak w nowoczesnych klientach): lekko zaokrąglony, cienka ramka,
    delikatny gradient i wąski kolorowy pasek akcentu u dołu."""
    line = line or border
    defs = (f'<linearGradient id="g" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{fill}"/>'
            f'<stop offset="1" stop-color="{fill2}"/></linearGradient>')
    acc = (f'<rect x="14" y="{h - 5}" width="{w - 28}" height="3" rx="1.5" fill="{accent}"/>' if accent else '')
    body = (f'<rect x="1.5" y="1.5" width="{w - 3}" height="{h - 3}" rx="8" fill="url(#g)" '
            f'stroke="{line}" stroke-width="3"/>' + acc)
    return svg(w, h, body, defs=defs)


def star_path(cx, cy, ro, ri, n=5):
    pts = []
    for i in range(n * 2):
        r = ro if i % 2 == 0 else ri
        a = -math.pi / 2 + math.pi * i / n
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return "M" + " L".join(f"{x:.1f} {y:.1f}" for x, y in pts) + " Z"


def gear_path(cx, cy, r_out, r_in, teeth):
    pts = []
    for i in range(teeth):
        a0 = 2 * math.pi * i / teeth
        step = 2 * math.pi / teeth
        for frac, r in ((0.0, r_in), (0.18, r_out), (0.5 - 0.18, r_out), (0.5, r_in)):
            a = a0 + step * frac
            pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return "M" + " L".join(f"{x:.1f} {y:.1f}" for x, y in pts) + " Z"


# ikony przycisków (czcionka bitmapowa, znaki prywatne U+E000…): pełne, białe, czytelne
ICONS = [
    ['<circle cx="32" cy="20" r="11" fill="#fff"/>', '<path d="M10 58 C10 40 20 35 32 35 C44 35 54 40 54 58 Z" fill="#fff"/>'],
    ['<circle cx="22" cy="21" r="9" fill="#fff"/>', '<circle cx="43" cy="25" r="7.5" fill="#fff"/>',
     '<path d="M4 57 C4 41 12 36 22 36 C32 36 38 41 38 57 Z" fill="#fff"/>',
     '<path d="M41 37 C52 37 60 41 60 57 H44 C44 48 43 42 41 37 Z" fill="#fff"/>'],
    [f'<path d="{gear_path(32, 32, 28, 20, 8)}" fill="#fff" fill-rule="evenodd"/>', '<circle cx="32" cy="32" r="9" fill="#101522"/>'],
    ['<path d="M19 14 A23 23 0 1 0 45 14" fill="none" stroke="#fff" stroke-width="8"/>', '<path d="M32 4 V30" stroke="#fff" stroke-width="8"/>'],
    [f'<path d="{star_path(32, 34, 29, 12)}" fill="#fff"/>'],
]


def icons_svg(cell=64):
    parts = []
    for i, shapes in enumerate(ICONS):
        parts.append(f'<g transform="translate({i * cell} 0)" stroke-linecap="round" stroke-linejoin="round">{"".join(shapes)}</g>')
    return svg(cell * len(ICONS), cell, "".join(parts))


SPLASHES = [
    "Kwa!", "Kaczo Client!", "Neon is life!", "Quack!", "Płyń z prądem!", "Made with 100% duck!",
    "Rubber ducks approve!", "Nie karm kreeperów!", "Kwa kwa kwa!", "Fabric + Kaczo!", "Duck mode: ON",
    "Uwaga na wodę!", "Neon never sleeps!", "Better with mods!", "Bread is the best loot!",
]


# ───────────────────────── rasteryzacja ─────────────────────────

async def render(page, svg_markup, path, w, h, transparent=True):
    await page.set_viewport_size({"width": w, "height": h})
    await page.set_content(
        f'<html><body style="margin:0;background:transparent">{svg_markup}</body></html>')
    await page.wait_for_timeout(150)
    path.parent.mkdir(parents=True, exist_ok=True)
    await page.screenshot(path=str(path), omit_background=transparent, clip={"x": 0, "y": 0, "width": w, "height": h})


async def main():
    (ROOT / "renderer").mkdir(exist_ok=True)
    (ROOT / "build").mkdir(exist_ok=True)

    # SVG-i używane bezpośrednio przez launcher
    (ROOT / "renderer" / "logo.svg").write_text(tile_svg(128), encoding="utf-8")
    (ROOT / "renderer" / "duck-neon.svg").write_text(duck_only_svg(), encoding="utf-8")

    async with async_playwright() as p:
        browser = await p.chromium.launch()
        page = await browser.new_page()

        await render(page, tile_svg(512), ROOT / "build" / "icon.png", 512, 512)
        await render(page, pack_icon_svg(), ROOT / "kaczo-pack" / "pack.png", 128, 128)
        await render(page, title_logo_svg(), GUI / "title" / "minecraft.png", 1024, 256)
        await render(page, edition_svg(), GUI / "title" / "edition.png", 512, 64)
        spr = GUI / "sprites" / "widget"
        await render(page, button_svg("#232b40", "#0f1424", "#080b16", accent="#ff8f3a"), spr / "button.png", 400, 40)
        await render(page, button_svg(ORANGE, "#2a1d12", "#120c08", accent=YELLOW), spr / "button_highlighted.png", 400, 40)
        await render(page, button_svg("#1a1f30", "#0c0f1a", "#0a0c14"), spr / "button_disabled.png", 400, 40)
        await render(page, icons_svg(64), GUI.parent / "font" / "kaczo_icons.png", 320, 64)
        await browser.close()

    nine = '{"gui":{"scaling":{"type":"nine_slice","width":200,"height":20,"border":4}}}'
    for n in ("button", "button_highlighted", "button_disabled"):
        (GUI / "sprites" / "widget" / f"{n}.png.mcmeta").write_text(nine)

    # ikony zamiast napisów: czcionka bitmapowa + puste/ikonowe teksty w plikach językowych
    font = PACK / "font"
    font.mkdir(parents=True, exist_ok=True)
    import json
    (font / "default.json").write_text(json.dumps({"providers": [{
        "type": "bitmap", "file": "minecraft:font/kaczo_icons.png", "ascent": 11, "height": 16,
        "chars": ["\ue000\ue001\ue002\ue003\ue004"]}]}, indent=2), encoding="utf-8")
    langs = {
        "en_us": ["Singleplayer", "Multiplayer", "Options...", "Quit Game", "Minecraft Realms"],
        "pl_pl": ["Gra jednoosobowa", "Gra wieloosobowa", "Opcje...", "Wyjd\u017a z gry", "Minecraft Realms"],
    }
    keys = ["menu.singleplayer", "menu.multiplayer", "menu.options", "menu.quit", "menu.online"]
    (PACK / "lang").mkdir(exist_ok=True)
    for code, names in langs.items():
        data = {k: f"{chr(0xE000 + i)} {n}" for i, (k, n) in enumerate(zip(keys, names))}
        (PACK / "lang" / f"{code}.json").write_text(json.dumps(data, indent=2), encoding="utf-8")

    texts = PACK / "texts"
    texts.mkdir(parents=True, exist_ok=True)
    (texts / "splashes.txt").write_text("\n".join(SPLASHES) + "\n", encoding="utf-8")

    # ikona .ico (Windows) z wielu rozmiarów
    img = Image.open(ROOT / "build" / "icon.png").convert("RGBA")
    img.save(ROOT / "build" / "icon.ico", sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
    print("OK: grafiki wygenerowane w", ROOT)


asyncio.run(main())
