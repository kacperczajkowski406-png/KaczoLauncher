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

    public static final String[] STYLES = {"Duck rain", "Aurora", "Starfield", "Sunset", "Candy", "Matrix", "Rainbow", "Storm"};

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

    public static void render(DrawContext ctx, MinecraftClient mc) {
        Module m = Modules.byId("sky");
        if (!m.enabled() || mc.player == null || mc.world == null) return;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        float pitch = mc.player.getPitch(), yaw = mc.player.getYaw();
        double fov = Math.max(30, mc.options.getFov().getValue());
        double half = Math.tan(Math.toRadians(fov / 2));
        int horizon = (int) Math.round(h / 2.0 + Math.tan(Math.toRadians(pitch)) * (h / 2.0) / half);
        horizon = Math.max(0, Math.min(h, horizon));
        if (horizon <= 4) return;

        int style = m.num("style");
        int strength = m.num("intensity");               // 0..100
        float speed = m.num("speed") / 50f;              // 1.0 = normalnie
        float t = (System.nanoTime() / 1_000_000_000f) * speed;
        int base = Math.round(strength / 100f * 150);    // maks. alfa tła
        boolean follow = m.bool("follow");
        float px = follow ? yaw * 6f : 0f, py = follow ? -pitch * 1.5f : 0f;
        int tint = m.color("tint");
        boolean tinted = (tint >>> 24) > 0 && (tint & 0xFFFFFF) != 0xFFFFFF;

        // tło: dla stylów gradientowych; „fade” zmiękcza dolną krawędź (ukrywa ostrą granicę horyzontu)
        switch (style) {
            case 0 -> vgrad(ctx, w, 0, horizon, 0x6EC6FF, 0xFFF3B0, base);
            case 1 -> vgrad(ctx, w, 0, horizon, 0x050B26, 0x0B3B4A, base);
            case 2 -> vgrad(ctx, w, 0, horizon, 0x02030F, 0x1B1650, base);
            case 3 -> vgrad(ctx, w, 0, horizon, 0x2B1A5E, 0xFF8A3D, base);
            case 4 -> vgrad(ctx, w, 0, horizon, 0xFFB3E6, 0xB3E6FF, base);
            case 5 -> vgrad(ctx, w, 0, horizon, 0x000A00, 0x021A06, base);
            case 7 -> vgrad(ctx, w, 0, horizon, 0x1E1030, 0x8A5A9C, base);
            default -> vgrad(ctx, w, 0, horizon, 0x6A5CFF, 0xFF7AA8, base);
        }
        if (tinted) ctx.fill(0, 0, w, horizon, argb(Math.min(110, (tint >>> 24) * base / 255), tint));

        switch (style) {
            case 1 -> aurora(ctx, w, horizon, t, px, strength);
            case 2 -> stars(ctx, w, horizon, t, px, py, m.num("density"), strength);
            case 3 -> sun(ctx, w, horizon, t, px, strength);
            case 4 -> clouds(ctx, w, horizon, t, px, strength);
            case 5 -> matrix(ctx, w, horizon, t, m.num("density"), strength);
            case 6 -> rainbow(ctx, w, horizon, t, px, strength);
            case 7 -> storm(ctx, w, horizon, t, px, strength);
            default -> { }
        }
        if (style == 0 || m.bool("ducks")) ducks(ctx, w, horizon, t, px, py, m.num("count"), m.num("size"), strength, style == 0);

        if (m.bool("grade")) grade(ctx, w, h, style, strength, m.num("vignette"));

        // miękkie dociemnienie przy horyzoncie – szkło zamiast ostrej linii
        if (m.bool("fade")) {
            for (int i = 0; i < 10; i++) {
                int a = Math.round(base * 0.32f * (1 - i / 10f));
                ctx.fill(0, Math.max(0, horizon - i * 2 - 2), w, Math.max(0, horizon - i * 2), argb(a, 0x000000));
            }
        }
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
