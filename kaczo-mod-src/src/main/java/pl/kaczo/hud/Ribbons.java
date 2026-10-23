package pl.kaczo.hud;

import net.minecraft.client.gui.DrawContext;

/** Płynne, szklane wstęgi światła (czarno-biały styl „liquid glass”) rysowane w natywnej rozdzielczości ekranu. */
final class Ribbons {
    private Ribbons() { }

    private static void seg(DrawContext c, float x0, float y0, float x1, float y1, int th, int color) {
        float dx = x1 - x0, dy = y1 - y0;
        int len = Math.round((float) Math.sqrt(dx * dx + dy * dy));
        if (len < 1) return;
        Compat.push(c);
        Compat.translate(c, x0, y0);
        Compat.rotate(c, (float) Math.atan2(dy, dx));
        int top = -(th / 2);
        c.fill(-(th / 2), top, len + (th + 1) / 2, top + th, color);
        Compat.pop(c);
    }

    private static int argb(float a, int rgb) { return (Math.max(0, Math.min(255, Math.round(a))) << 24) | (rgb & 0xFFFFFF); }

    /** Rysuje {@code count} wstęg na ekranie o rozmiarze (gw × gh) w pikselach GUI; a = przezroczystość 0..1. */
    static void draw(DrawContext ctx, int gw, int gh, float t, float a, int count) {
        if (a <= 0.01f) return;
        double sf = Ui.sf();
        int W = (int) Math.round(gw * sf), H = (int) Math.round(gh * sf);
        Compat.push(ctx);
        Compat.scale(ctx, (float) (1.0 / sf), (float) (1.0 / sf));
        int step = Math.max(16, W / 64);
        for (int r = 0; r < count; r++) {
            float baseY = H * (0.34f + 0.2f * r), amp = H * (0.10f + 0.035f * r);
            float f1 = 1.6f + 0.55f * r, f2 = 4.4f - 0.8f * r, sp = 0.22f + 0.06f * r;
            float[] px = new float[7], py = new float[7];
            boolean has = false;
            for (int x = -step; x <= W + step; x += step) {
                float xn = x / (float) W;
                float cy = baseY + amp * (float) Math.sin(xn * f1 * Math.PI + t * sp + r * 1.7) + amp * 0.45f * (float) Math.sin(xn * f2 * Math.PI - t * sp * 1.3 + r);
                float spread = H * 0.016f * (1f + 1.6f * (float) Math.sin(xn * 3.1 + t * 0.18 + r * 2.1));      // wstęga skręca się jak jedwab
                for (int k = 0; k < 7; k++) {
                    float y = cy + (k - 3) * spread;
                    if (has) {
                        float al = (k == 0 || k == 6) ? 0.85f : (k == 1 || k == 5) ? 0.45f : 0.22f;
                        seg(ctx, px[k], py[k], x, y, 1, argb(255 * a * al, 0xFFFFFF));
                        if (k == 3) {
                            seg(ctx, px[k], py[k], x, y, Math.max(6, H / 60), argb(14 * a, 0xFFFFFF));
                            seg(ctx, px[k], py[k], x, y, Math.max(3, H / 140), argb(22 * a, 0xFFFFFF));
                        }
                    }
                    px[k] = x; py[k] = y;
                }
                has = true;
            }
        }
        Compat.pop(ctx);
    }
}
