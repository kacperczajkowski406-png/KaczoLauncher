package pl.kaczo.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * „Niebo Kaczo”: ozdobne niebo rysowane jako półprzezroczysta warstwa na górnej części ekranu (nad linią horyzontu).
 * To nie jest prawdziwy shader – nakładka działa na wszystkich wersjach gry i podąża za obrotem kamery.
 * Style: kaczkowy deszcz, zorza, gwiazdy, zachód słońca, cukierkowe, matrix, tęcza.
 */
public final class Sky {
    private Sky() { }

    public static final String[] STYLES = {"Duck rain", "Aurora", "Starfield", "Sunset", "Candy", "Matrix", "Rainbow", "Storm", "Liquid glass", "Star trails"};

    // 9x8 pikseli: y = żółty, o = dziób, e = oko, w = jasny połysk
    private static final String[] DUCK = {
            "..yyy....",
            ".yywyy...",
            ".yyeyyoo.",
            ".yyyyoo..",
            "..yyy....",
            ".yyyyyyy.",
            "yyyyyyyyy",
            ".yyyyyyy.",
    };

    private static float hash(int n) {
        n = (n << 13) ^ n;
        return ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 2147483647f;
    }

    private static int argb(int a, int rgb) { return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF); }

    private static int lerp(int c1, int c2, float t) {
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (r << 16) | (g << 8) | b;
    }

    /** Pionowy gradient wypełniany pasami (działa tak samo we wszystkich wersjach). */
    private static void vgrad(DrawContext ctx, int w, int y0, int y1, int cTop, int cBot, int alpha) {
        int h = y1 - y0;
        if (h <= 0) return;
        int step = Math.max(1, h / 110);
        for (int y = 0; y < h; y += step) {
            ctx.fill(0, y0 + y, w, Math.min(y1, y0 + y + step), argb(alpha, lerp(cTop, cBot, y / (float) h)));
        }
    }


    /** Prawdziwe niebo świata: miesza kolor nieba z palety stylu (wołane z mixina na ClientWorld). */
    public static int worldColor(int rgb) {
        try {
            Module m = Modules.byId("sky");
            if (true) return rgb;      // prawdziwe niebo zostaje bez zmian (stary styl nieba usunięty)
            if (!m.enabled() || !m.bool("world")) return rgb;
            int style = m.num("style");
            int[][] pal = { {0x6EC6FF,0xFFF3B0},{0x0A1A33,0x1B4A7A},{0x0B1A33,0x1D3A5C},{0xFF8A3D,0x2B1A5E},{0xFFB3E6,0xB3E6FF},{0x021A06,0x00A030},{0x6A5CFF,0xFF7AA8},{0x3A1F5E,0x8A5A9C},{0x000000,0x0A0A10},{0x03060A,0x0A130E} };
            int[] p = pal[Math.max(0, Math.min(pal.length - 1, style))];
            float t = (System.nanoTime() / 1_000_000_000f) * (m.num("speed") / 50f) * 0.15f;
            float wave = 0.5f + 0.5f * (float) Math.sin(t);
            int target = lerp(p[0], p[1], wave);
            float lum = (((rgb >> 16) & 255) * 0.3f + ((rgb >> 8) & 255) * 0.59f + (rgb & 255) * 0.11f) / 255f;
            float bright = style >= 8 ? 1f : Math.max(0.12f, Math.min(1f, lum / 0.55f));       // zachowaj cykl dzień/noc (czarne style są zawsze ciemne)
            int tr = (int) (((target >> 16) & 255) * bright), tg = (int) (((target >> 8) & 255) * bright), tb = (int) ((target & 255) * bright);
            return lerp(rgb & 0xFFFFFF, (tr << 16) | (tg << 8) | tb, Math.min(1f, m.num("intensity") / 100f * 0.95f));
        } catch (Throwable e) { return rgb; }
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        Module m = Modules.byId("sky");
        if (!m.enabled() || mc.player == null || mc.world == null) return;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        float pitch = mc.player.getPitch(), yaw = mc.player.getYaw();
        double fov = Math.max(30, mc.options.getFov().getValue());
        double half = Math.tan(Math.toRadians(fov / 2));
        int horizon = (int) Math.round(h / 2.0 + Math.tan(Math.toRadians(pitch)) * (h / 2.0) / half);

        int style = m.num("style");
        float t = (System.nanoTime() / 1_000_000_000f) * (m.num("speed") / 50f);
        Shader.render(ctx, mc, m, t);
    }

    private static void ducks(DrawContext ctx, int w, int horizon, float t, float px, float py, int count, int size, int strength, boolean main) {
        int s = Math.max(1, Math.min(5, size));
        int dw = 9 * s, dh = 8 * s;
        int n = Math.max(1, count);
        for (int i = 0; i < n; i++) {
            float r1 = hash(i * 7 + 1), r2 = hash(i * 13 + 5), r3 = hash(i * 29 + 11);
            float fall = 12f + r2 * 22f;
            float sway = (float) Math.sin(t * (0.6f + r3) + i) * (6f + 8f * r3);
            float x = ((r1 * (w + dw * 2) + px * (0.4f + r3 * 0.6f) + sway) % (w + dw * 2) + (w + dw * 2)) % (w + dw * 2) - dw;
            float span = horizon + dh * 2;
            float y = ((t * fall + r3 * span + py * (0.3f + r2)) % span + span) % span - dh;
            if (y < 0 || y + dh > horizon) continue;
            // wyblakanie przy krawędziach strefy nieba
            float edge = Math.min(1f, Math.min(y / 14f, (horizon - y - dh) / 14f));
            int a = Math.round(255 * strength / 100f * Math.max(0.1f, edge));
            if (a < 6) continue;
            boolean flip = r1 > 0.5f;
            for (int ry = 0; ry < DUCK.length; ry++) {
                String row = DUCK[ry];
                for (int rx = 0; rx < 9; rx++) {
                    char c = row.charAt(flip ? 8 - rx : rx);
                    if (c == '.') continue;
                    int col = switch (c) { case 'o' -> 0xFF8F1F; case 'e' -> 0x1A1A1A; case 'w' -> 0xFFF6A8; default -> 0xFFD92E; };
                    int sx = (int) x + rx * s, sy = (int) y + ry * s;
                    ctx.fill(sx, sy, sx + s, sy + s, argb(a, col));
                }
            }
        }
    }

    private static void aurora(DrawContext ctx, int w, int horizon, float t, float px, int strength) {
        int a = Math.round(strength / 100f * 90);
        for (int x = 0; x < w; x += 3) {
            float u = (x + px) / 70f;
            float wave = (float) (Math.sin(u + t * 0.6) * 0.5 + Math.sin(u * 0.37 - t * 0.4) * 0.5);
            int top = (int) (horizon * (0.18f + 0.18f * wave));
            int bot = top + (int) (horizon * (0.34f + 0.12f * Math.sin(u * 0.8 + t)));
            bot = Math.min(bot, horizon);
            float mixc = (float) (0.5 + 0.5 * Math.sin(u * 0.2 + t * 0.3));
            int col = lerp(0x35FFA0, 0xB45CFF, mixc);
            int step = Math.max(2, (bot - top) / 12);
            for (int y = top; y < bot; y += step) {
                float f = 1f - Math.abs((y - top) / (float) Math.max(1, bot - top) - 0.35f) * 1.4f;
                ctx.fill(x, y, x + 3, Math.min(bot, y + step), argb(Math.round(a * Math.max(0, f)), col));
            }
        }
    }

    private static void stars(DrawContext ctx, int w, int horizon, float t, float px, float py, int density, int strength) {
        int n = 40 + density * 3;
        for (int i = 0; i < n; i++) {
            float x = ((hash(i * 3 + 1) * w + px * 0.5f) % w + w) % w;
            float y = (hash(i * 5 + 2) * horizon + py * 0.3f);
            if (y < 0 || y >= horizon - 2) continue;
            float tw = 0.5f + 0.5f * (float) Math.sin(t * 2.2 + i * 1.7);
            int a = Math.round((60 + 195 * tw) * strength / 100f);
            int sz = hash(i * 7 + 3) > 0.92f ? 2 : 1;
            ctx.fill((int) x, (int) y, (int) x + sz, (int) y + sz, argb(a, 0xFFFFFF));
            if (sz == 2) { ctx.fill((int) x - 1, (int) y, (int) x, (int) y + 1, argb(a / 3, 0xFFF1A8)); ctx.fill((int) x + 2, (int) y, (int) x + 3, (int) y + 1, argb(a / 3, 0xFFF1A8)); }
        }
    }

    private static void sun(DrawContext ctx, int w, int horizon, float t, float px, int strength) {
        int cx = (int) (((w * 0.7f - px) % w + w) % w), cy = (int) (horizon * 0.82f);
        int a = Math.round(strength / 100f * 110);
        int maxR = Math.min(110, horizon * 2 / 3);
        for (int r = maxR; r > 6; r -= 2) {                  // gęsta poświata (bloom)
            float k = 1f - r / (float) maxR;
            int rr = r;
            ctx.fill(cx - rr * 2, Math.max(0, cy - rr), cx + rr * 2, Math.min(horizon, cy + rr / 2), argb(Math.round(a * 0.07f), lerp(0xFFE45C, 0xFF5E3A, k)));
        }
        ctx.fill(cx - 16, cy - 8, cx + 16, cy + 8, argb(Math.min(255, a * 2), 0xFFF4B0));
        ctx.fill(cx - 10, cy - 5, cx + 10, cy + 5, argb(255, 0xFFFFFF));
        for (int i = -6; i <= 6; i++) {                      // promienie światła
            if (i == 0) continue;
            int len = 40 + (int) (30 * hash(i + 99)) + (int) (10 * Math.sin(t + i));
            ctx.fill(cx + i * 12 - 1, Math.max(0, cy - len), cx + i * 12 + 1, cy, argb(a / 6, 0xFFE9A0));
        }
    }

    /** Miękka chmura z kilku półprzezroczystych „bąbli” – wygląda jak rozmyta. */
    private static void clouds(DrawContext ctx, int w, int horizon, float t, float px, int strength) {
        int a = Math.round(strength / 100f * 70);
        for (int i = 0; i < 9; i++) {
            float x = ((hash(i * 11 + 4) * (w + 160) + t * (3 + i % 4) * 2 + px * 0.6f) % (w + 160) + (w + 160)) % (w + 160) - 80;
            int y = (int) (horizon * (0.12f + 0.66f * hash(i * 17 + 9)));
            int cw = 44 + (int) (hash(i * 19 + 2) * 56);
            for (int l = 0; l < 6; l++) {                       // od zewnętrznej, bladej warstwy do jasnego rdzenia
                int in = l * 3;
                ctx.fill((int) x + in, y - 3 + l, (int) x + cw - in, y + 8 - l, argb(a, 0xFFFFFF));
                ctx.fill((int) x + 10 + in, y - 8 + l, (int) x + cw - 14 - in, y - 2, argb(a, 0xFFF4FA));
            }
            ctx.fill((int) x + 4, y + 8, (int) x + cw - 4, y + 11, argb(a / 2, 0xE5B8FF));   // różowy spód
        }
    }

    private static void matrix(DrawContext ctx, int w, int horizon, float t, int density, int strength) {
        int cols = Math.max(10, Math.min(120, density + 10));
        for (int i = 0; i < cols; i++) {
            int x = (int) (hash(i * 3 + 1) * w);
            float sp = 25f + hash(i * 5 + 2) * 55f;
            int len = 20 + (int) (hash(i * 7 + 3) * 50);
            float head = (t * sp + hash(i * 9 + 4) * 400) % (horizon + len);
            for (int k = 0; k < len; k += 4) {
                int y = (int) head - k;
                if (y < 0 || y >= horizon) continue;
                int a = Math.round((1f - k / (float) len) * 210 * strength / 100f);
                ctx.fill(x, y, x + 2, y + 3, argb(a, k == 0 ? 0xC8FFC8 : 0x24FF5A));
            }
        }
    }

    private static void rainbow(DrawContext ctx, int w, int horizon, float t, float px, int strength) {
        int[] cols = {0xFF3B3B, 0xFF9A3B, 0xFFE83B, 0x3BFF6A, 0x3BC7FF, 0x6A5CFF, 0xC84DFF};
        int a = Math.round(strength / 100f * 120);
        int band = Math.max(3, horizon / 18);
        for (int x = 0; x < w; x += 4) {
            float wave = (float) Math.sin((x + px) / 55f + t) * band * 1.5f;
            for (int i = 0; i < cols.length; i++) {
                int y = (int) (horizon * 0.25f + wave + i * band);
                ctx.fill(x, y, x + 4, y + band, argb(a, cols[i]));
            }
        }
    }

    /** „Shaderowy” klimat: delikatna korekcja kolorów całego obrazu i winieta. */
    private static void grade(DrawContext ctx, int w, int h, int style, int strength, int vignette) {
        int[] tints = {0xFFE9B0, 0x66FFD0, 0x8F9CFF, 0xFF9A5C, 0xFFB8E8, 0x30FF70, 0xC0A8FF, 0xB48CD8};
        int a = Math.round(strength / 100f * 20);
        ctx.fill(0, 0, w, h, argb(a, tints[Math.max(0, Math.min(tints.length - 1, style))]));
        int v = Math.round(vignette / 100f * 120);
        if (v <= 0) return;
        int layers = 16;
        for (int i = 0; i < layers; i++) {
            int al = Math.round(v * (float) Math.pow(1 - i / (float) layers, 2) / 4f);
            int d = i * Math.max(2, Math.min(w, h) / 40);
            ctx.fill(0, d, w, d + Math.max(2, h / 40), argb(al, 0x000000));
            ctx.fill(0, h - d - Math.max(2, h / 40), w, h - d, argb(al, 0x000000));
            ctx.fill(d, 0, d + Math.max(2, w / 60), h, argb(al, 0x000000));
            ctx.fill(w - d - Math.max(2, w / 60), 0, w - d, h, argb(al, 0x000000));
        }
    }

    /** Burzowe, fioletowe niebo z ciężkimi chmurami i rzadkimi rozbłyskami. */

    // ───────── „shaderowe” efekty: bloom, świecące cząsteczki, promienie, błyskawice, deszcz ─────────
    private static final int[] ACCENT = {0xFFE27A, 0x6CFFD2, 0xA9B4FF, 0xFFB070, 0xFF9AD8, 0x5CFF8A, 0xFFB0F0, 0xD9A8FF};

    /** Wypełniony krąg (linie poziome) – podstawa poświaty. */
    private static void disc(DrawContext ctx, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            if (half > 0) ctx.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
        }
    }

    /** Bloom: kilka nałożonych kręgów o rosnącej jasności ku środkowi + biały rdzeń. */
    private static void glow(DrawContext ctx, int cx, int cy, int r, int rgb, int a) {
        disc(ctx, cx, cy, r, argb(a / 7, rgb));
        disc(ctx, cx, cy, Math.max(1, r * 2 / 3), argb(a / 4, rgb));
        disc(ctx, cx, cy, Math.max(1, r / 3), argb(a / 2, lerp(rgb, 0xFFFFFF, 0.5f)));
        if (r >= 3) disc(ctx, cx, cy, Math.max(1, r / 6), argb(a, 0xFFFFFF));
    }

    /** Dryfujące, migoczące świetliki z bloomem; głębia = paralaksa przy obrocie kamery. */
    private static void orbs(DrawContext ctx, int w, int horizon, float t, float px, float py, int style, int density, int strength, int size) {
        int n = Math.max(4, Math.min(70, density));
        int acc = ACCENT[Math.max(0, Math.min(ACCENT.length - 1, style))];
        int sec = lerp(acc, 0xFFFFFF, 0.55f);
        for (int i = 0; i < n; i++) {
            float depth = 0.3f + hash(i * 5 + 3) * 0.7f;
            float sp = 4f + 10f * hash(i * 9 + 1);
            float x = ((hash(i * 3 + 7) * w + (float) Math.sin(t * 0.4f + i) * 18f + px * depth * 0.6f) % w + w) % w;
            float y = (((hash(i * 13 + 2) * horizon) - t * sp * depth + py * depth * 0.4f) % horizon + horizon) % horizon;
            float tw = 0.55f + 0.45f * (float) Math.sin(t * (1.2f + 2f * hash(i * 17)) + i * 1.7f);
            int r = Math.max(2, Math.round((2 + 5 * depth) * (0.6f + 0.4f * size)));
            int a = Math.round(strength / 100f * 200 * tw * (0.5f + 0.5f * depth));
            glow(ctx, (int) x, (int) y, r, (i % 3 == 0) ? sec : acc, a);
        }
    }

    /** Smugi światła (god rays) ześlizgujące się z góry – delikatne, szerokie pasy. */
    private static void rays(DrawContext ctx, int w, int horizon, float t, float px, int style, int strength) {
        int acc = ACCENT[Math.max(0, Math.min(ACCENT.length - 1, style))];
        for (int i = 0; i < 6; i++) {
            float baseX = hash(i * 23 + 5) * (w + 200) - 100 + (float) Math.sin(t * 0.25f + i * 2f) * 40f - px * 0.4f;
            int wid = 22 + (int) (hash(i * 31 + 1) * 50);
            float slope = 0.45f + 0.25f * hash(i * 41 + 9);
            float pulse = 0.55f + 0.45f * (float) Math.sin(t * 0.5f + i * 1.3f);
            int a = Math.round(strength / 100f * 20 * pulse);
            if (a < 2) continue;
            for (int y = 0; y < horizon; y += 3) {
                float k = 1f - y / (float) horizon;
                int x = (int) (baseX + y * slope);
                ctx.fill(x, y, x + (int) (wid * (0.6f + 0.6f * (1 - k))), y + 3, argb(Math.round(a * (0.35f + 0.65f * k)), acc));
            }
        }
    }

    /** Błyskawica: zygzak z poświatą, pojawia się losowo. */
    private static void lightning(DrawContext ctx, int w, int horizon, float t, float px, int strength) {
        int slot = (int) (t * 0.6f);
        float local = t * 0.6f - slot;
        if (hash(slot * 7 + 3) < 0.45f || local > 0.28f) return;          // nie każdy slot ma błysk, błysk trwa chwilę
        float fade = 1f - local / 0.28f;
        int x = (int) (hash(slot * 11 + 5) * w * 0.8f + w * 0.1f - px * 0.3f);
        int y = 0, step = Math.max(6, horizon / 14);
        int a = Math.round(strength / 100f * 255 * fade);
        int seg = 0;
        while (y < horizon * 0.85f && seg < 30) {
            int nx = x + Math.round((hash(slot * 97 + seg * 13) - 0.5f) * 26);
            int ny = y + step;
            int x0 = Math.min(x, nx) - 1, x1 = Math.max(x, nx) + 2;
            ctx.fill(x0 - 3, y, x1 + 3, ny, argb(a / 6, 0xB69CFF));      // poświata
            ctx.fill(x0, y, x1, ny, argb(a, 0xFFFFFF));                    // rdzeń
            x = nx; y = ny; seg++;
        }
        ctx.fill(0, 0, w, horizon, argb(Math.round(a * 0.18f), 0xD8C8FF));
    }

    /** Ukośny deszcz (smugi) – głębia przez różną długość i jasność. */
    private static void rain(DrawContext ctx, int w, int horizon, float t, int density, int strength) {
        int n = Math.max(20, density * 3);
        for (int i = 0; i < n; i++) {
            float depth = 0.4f + hash(i * 7 + 1) * 0.6f;
            float x = hash(i * 3 + 2) * (w + 60) + t * 40f * depth;
            float y = ((hash(i * 11 + 4) * horizon + t * 220f * depth) % horizon + horizon) % horizon;
            int xi = (int) (x % (w + 60)) - 30;
            int len = (int) (4 + 9 * depth);
            ctx.fill(xi, (int) y, xi + 1, (int) y + len, argb(Math.round(strength / 100f * 70 * depth), 0xC8D4FF));
        }
    }

    /** Spadające gwiazdy. */
    private static void shooting(DrawContext ctx, int w, int horizon, float t, float px, int strength) {
        int slot = (int) (t * 0.5f);
        float local = t * 0.5f - slot;
        if (hash(slot * 5 + 1) < 0.35f || local > 0.5f) return;
        float k = local / 0.5f;
        float sx = hash(slot * 13 + 2) * w * 0.7f - px * 0.2f, sy = hash(slot * 17 + 3) * horizon * 0.4f;
        for (int i = 0; i < 14; i++) {
            float f = k - i * 0.02f;
            if (f < 0) break;
            int x = (int) (sx + f * w * 0.35f), y = (int) (sy + f * horizon * 0.35f);
            glow(ctx, x, y, i == 0 ? 3 : 2, 0xFFFFFF, Math.round(strength / 100f * 220 * (1 - i / 14f)));
        }
    }

    /** Ciepła poświata przy horyzoncie (bloom na linii nieba). */
    private static void horizonGlow(DrawContext ctx, int w, int horizon, int style, int strength, float t) {
        int acc = ACCENT[Math.max(0, Math.min(ACCENT.length - 1, style))];
        float pulse = 0.8f + 0.2f * (float) Math.sin(t * 0.4f);
        for (int i = 0; i < 14; i++) {
            int a = Math.round(strength / 100f * 52 * pulse * (1 - i / 14f) * (1 - i / 14f));
            ctx.fill(0, Math.max(0, horizon - 4 - i * 4), w, Math.max(0, horizon - i * 4), argb(a, acc));
        }
    }

    private static void storm(DrawContext ctx, int w, int horizon, float t, float px, int strength) {
        int a = Math.round(strength / 100f * 120);
        for (int i = 0; i < 12; i++) {
            float x = ((hash(i * 11 + 4) * (w + 220) + t * (2 + i % 3) * 2 + px * 0.5f) % (w + 220) + (w + 220)) % (w + 220) - 110;
            int y = (int) (horizon * (0.05f + 0.7f * hash(i * 17 + 9)));
            int cw = 90 + (int) (hash(i * 19 + 2) * 120);
            for (int l = 0; l < 7; l++) {
                int in = l * 6;
                ctx.fill((int) x + in, y - 6 + l * 2, (int) x + cw - in, y + 14 - l, argb(a / 4, l < 3 ? 0x2B1A3F : 0x4D3566));
            }
            ctx.fill((int) x + 12, y + 14, (int) x + cw - 12, y + 18, argb(a / 3, 0xFF9AD0));   // różowa poświata od spodu
        }
        float flash = (float) Math.max(0, Math.sin(t * 0.9) * Math.sin(t * 2.3 + 1.7) - 0.86) * 6f;
        if (flash > 0) ctx.fill(0, 0, w, horizon, argb(Math.round(flash * a), 0xE8D8FF));
    }
}
