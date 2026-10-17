package pl.kaczo.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Wspólne kolory i elementy w stylu „liquid glass”: zaokrąglone, półprzezroczyste, z jasną krawędzią. */
public class Ui {
    public static final int DIM = 0x99050810, PANEL = 0xD0121826, CARD = 0x55FFFFFF, CARD_OFF = 0x26FFFFFF,
            BTN = 0x38FFFFFF, BTN_HOVER = 0x62FFFFFF, ORANGE = 0xFFFFC21A, GREEN = 0xE03DBE6A, GREEN_HOVER = 0xF050D47C,
            WHITE = 0xFFFFFFFF, GRAY = 0xFFA9B2C6, RED = 0xFFFF5C63, YELLOW = 0xFFFFE14A;

    public static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ───────────── rysowanie w pikselach ekranu (gładkie rogi niezależnie od skali GUI) ─────────────

    /** Skala GUI (ile pikseli ekranu przypada na jeden „piksel” Minecrafta). */
    public static double sf() {
        try { return Math.max(1.0, MinecraftClient.getInstance().getWindow().getScaleFactor()); } catch (Throwable t) { return 1.0; }
    }

    private static void begin(DrawContext ctx, double s) { Compat.push(ctx); Compat.scale(ctx, (float) (1.0 / s), (float) (1.0 / s)); }
    private static void end(DrawContext ctx) { Compat.pop(ctx); }
    private static int px(double v, double s) { return (int) Math.round(v * s); }

    /** Mnoży przezroczystość koloru ARGB przez a (0..1). */
    public static int fade(int argb, float a) {
        int al = Math.round(((argb >>> 24) & 255) * Math.max(0f, Math.min(1f, a)));
        return (al << 24) | (argb & 0xFFFFFF);
    }

    /** Miękka poświata: kilka nakładających się, półprzezroczystych kół (tanie, bez wygładzania – przy małej alfie niewidoczne schodki). */
    public static void blob(DrawContext ctx, double cx, double cy, double rad, int rgb, int alphaPerLayer, float fade) {
        int col = ((int) (alphaPerLayer * fade) << 24) | (rgb & 0xFFFFFF);
        if ((col >>> 24) < 1) return;
        for (int j = 0; j < 5; j++) {
            double r = rad * (1 - j * 0.17);
            int ir = (int) r;
            for (int dy = -ir; dy < ir; dy++) {
                int half = (int) Math.sqrt(Math.max(0, r * r - (dy + 0.5) * (dy + 0.5)));
                ctx.fill((int) cx - half, (int) cy + dy, (int) cx + half, (int) cy + dy + 1, col);
            }
        }
    }

    private static int cov(int color, double c) {
        int al = (int) Math.round(((color >>> 24) & 255) * Math.max(0, Math.min(1, c)));
        return (al << 24) | (color & 0xFFFFFF);
    }

    /** Wypełniony zaokrąglony prostokąt w pikselach ekranu, z wygładzonymi (półprzezroczystymi) krawędziami łuków. */
    private static void pr(DrawContext ctx, int X, int Y, int W, int H, int R, int color) {
        if (W <= 0 || H <= 0) return;
        R = Math.max(0, Math.min(R, Math.min(W, H) / 2));
        if (R == 0) { ctx.fill(X, Y, X + W, Y + H, color); return; }
        for (int i = 0; i < R; i++) {
            double dy = R - i - 0.5, inset = R - Math.sqrt(Math.max(0, (double) R * R - dy * dy));
            int c = (int) Math.ceil(inset), f = (int) Math.floor(inset);
            if (c > f) {
                int col = cov(color, c - inset);
                ctx.fill(X + f, Y + i, X + f + 1, Y + i + 1, col);
                ctx.fill(X + W - f - 1, Y + i, X + W - f, Y + i + 1, col);
                ctx.fill(X + f, Y + H - i - 1, X + f + 1, Y + H - i, col);
                ctx.fill(X + W - f - 1, Y + H - i - 1, X + W - f, Y + H - i, col);
            }
            ctx.fill(X + c, Y + i, X + W - c, Y + i + 1, color);
            ctx.fill(X + c, Y + H - i - 1, X + W - c, Y + H - i, color);
        }
        ctx.fill(X, Y + R, X + W, Y + H - R, color);
    }

    /** Cienka (1 piksel ekranu) obwódka zaokrąglonego prostokąta. */
    private static void pring(DrawContext ctx, int X, int Y, int W, int H, int R, int top, int bottom) {
        if (W <= 2 || H <= 2) return;
        R = Math.max(0, Math.min(R, Math.min(W, H) / 2));
        if (R == 0) {
            ctx.fill(X, Y, X + W, Y + 1, top); ctx.fill(X, Y + H - 1, X + W, Y + H, bottom);
            ctx.fill(X, Y + 1, X + 1, Y + H - 1, top); ctx.fill(X + W - 1, Y + 1, X + W, Y + H - 1, bottom);
            return;
        }
        int prev = R;
        for (int i = 0; i < R; i++) {
            double dy = R - i - 0.5, inset = R - Math.sqrt(Math.max(0, (double) R * R - dy * dy));
            int cur = (int) Math.round(inset);
            if (i == 0) {
                ctx.fill(X + cur, Y, X + W - cur, Y + 1, top);
                ctx.fill(X + cur, Y + H - 1, X + W - cur, Y + H, bottom);
            } else {
                int e = Math.max(cur + 1, prev);
                ctx.fill(X + cur, Y + i, X + e, Y + i + 1, top);
                ctx.fill(X + W - e, Y + i, X + W - cur, Y + i + 1, bottom);
                ctx.fill(X + cur, Y + H - i - 1, X + e, Y + H - i, top);
                ctx.fill(X + W - e, Y + H - i - 1, X + W - cur, Y + H - i, bottom);
            }
            prev = cur;
        }
        ctx.fill(X, Y + R, X + 1, Y + H - R, top);
        ctx.fill(X + W - 1, Y + R, X + W, Y + H - R, bottom);
    }

    /** Wypełniony prostokąt z zaokrąglonymi rogami (wymiary i promień w pikselach GUI, rysowane gładko). */
    public static void rrect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        double s = sf();
        begin(ctx, s);
        pr(ctx, px(x, s), px(y, s), px(w, s), px(h, s), px(r, s), color);
        end(ctx);
    }

    /** Sama obwódka (1 piksel ekranu), np. do pierścieni animacji. */
    public static void ring(DrawContext ctx, double x, double y, double w, double h, double r, int color) {
        double s = sf();
        begin(ctx, s);
        pring(ctx, px(x, s), px(y, s), px(w, s), px(h, s), px(r, s), color, color);
        end(ctx);
    }

    private static void physGlass(DrawContext ctx, double s, int x, int y, int w, int h, int r, int fill, boolean border) {
        int X = px(x, s), Y = px(y, s), W = px(w, s), H = px(h, s), R = px(r, s);
        begin(ctx, s);
        pr(ctx, X, Y + (int) Math.round(s * 0.6), W, H, R, 0x22000000);                 // miękki cień
        pr(ctx, X, Y, W, H, R, fill);                                                    // tafla
        if (H > 10) pr(ctx, X + 2, Y + 2, W - 4, Math.max(2, H / 2 - 2), Math.max(0, R - 2), 0x14FFFFFF); // połysk górnej połowy
        if (border) pring(ctx, X, Y, W, H, R, 0x58FFFFFF, 0x26FFFFFF);                   // krawędź: jasna u góry, ciemniejsza u dołu
        end(ctx);
    }

    /** „Szkło”: półprzezroczysta tafla z cieniem, połyskiem i cienką krawędzią. */
    public static void glass(DrawContext ctx, int x, int y, int w, int h, int r, int fill) {
        physGlass(ctx, sf(), x, y, w, h, r, fill, true);
    }

    /** Tło elementów HUD zależne od globalnego stylu (Glass / Flat). */
    public static void box(DrawContext ctx, int x, int y, int w, int h, int color) {
        if (color == 0) return;
        Module g = Modules.GENERAL;
        if (g.num("style") == 1) { ctx.fill(x, y, x + w, y + h, color); return; }
        physGlass(ctx, sf(), x, y, w, h, g.num("radius"), color, g.bool("glassborder"));
    }

    public static void button(DrawContext ctx, MinecraftClient mc, int x, int y, int w, int h, String label, int bg, int fg) {
        boolean solid = (bg >>> 24) >= 0xF0;
        if (solid) { rrect(ctx, x, y, w, h, 4, bg); }
        else glass(ctx, x, y, w, h, 4, bg);
        int tw = mc.textRenderer.getWidth(label);
        ctx.drawText(mc.textRenderer, label, x + (w - tw) / 2, y + (h - 8) / 2, fg, false);
    }

    public static void centered(DrawContext ctx, MinecraftClient mc, String s, int cx, int y, int color) {
        ctx.drawText(mc.textRenderer, s, cx - mc.textRenderer.getWidth(s) / 2, y, color, false);
    }
}
