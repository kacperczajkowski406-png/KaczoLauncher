package pl.kaczo.hud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Studio tekstu piosenki (Music Display): zakładki, gotowe style, czcionki, animacje i podgląd na żywo. */
public class LyricsScreen extends KScreen {
    private static final String[] TABS = {"Styles", "Look", "Animation", "Layout", "General"};
    private static final String[] FONTS = {"Global", "Inter", "Poppins", "Lora", "Mono", "Minecraft"};
    private static final String[] ANIMS = {"Typewriter", "Fade", "Rise", "Pop", "Wave"};
    private static final String[][] COLORS = {
            {"White", "255,255,255,255,0"}, {"Ice", "170,215,255,255,0"}, {"Cyan", "53,230,255,255,0"}, {"Pink", "255,120,230,255,0"},
            {"Amber", "255,190,80,255,0"}, {"Orange", "255,140,70,255,0"}, {"Lime", "140,255,120,255,0"}, {"Lavender", "190,170,255,255,0"},
            {"Rainbow", "255,255,255,255,1"}};

    /** Gotowy styl: nazwa, czcionka (0 = globalna), kolor, blask, animacja, płytka. */
    private record Preset(String name, int font, String color, int glow, int anim, boolean plate) { }
    private static final Preset[] PRESETS = {
            new Preset("Clean", 1, "255,255,255,255,0", 0, 1, false),
            new Preset("Neon", 2, "255,120,230,255,0", 100, 3, false),
            new Preset("Music", 5, "255,255,255,255,0", 25, 0, true),
            new Preset("Rainbow Wave", 2, "255,255,255,255,1", 40, 4, false),
            new Preset("Karaoke", 1, "255,190,80,255,0", 50, 0, true),
            new Preset("Sunset", 2, "255,140,70,255,0", 70, 2, false),
            new Preset("Retro", 4, "53,230,255,255,0", 60, 0, true),
            new Preset("Elegant", 3, "255,255,255,255,0", 55, 1, false),
            new Preset("Dreamy", 3, "190,170,255,255,0", 90, 1, false),
    };
    private static int tab = 0;
    static void openTab(int t) { tab = Math.max(0, Math.min(4, t)); }

    private final Screen parent;
    private final long opened = System.currentTimeMillis();
    private int px, py, pw, ph, sbw = 92, cx, cy, cw, chh, prevH = 38;
    private float S = 1f;
    private int vw, vh;
    private double scroll = 0;
    private String dragKey = null;
    private int dragX, dragW, dragMin, dragMax;

    public LyricsScreen(Screen parent) { super(Text.literal("Music Display")); this.parent = parent; }

    @Override public void close() { client.setScreen(parent); }

    private void layout() {
        S = (float) Math.min(1.0, 1.5 / Ui.baseSf());
        vw = Math.round(width / S); vh = Math.round(height / S);
        pw = Math.min(Math.round(vw * 0.88f), 470);
        ph = Math.min(Math.round(vh * 0.92f), 330);
        px = (vw - pw) / 2; py = (vh - ph) / 2;
        cx = px + sbw + 6; cy = py + 30; cw = pw - sbw - 12;
        chh = ph - 30 - prevH - 34;
    }

    private Module m() { return Modules.MUSIC; }

    private int fontIdx() { return m().num("font"); }

    private static int fid(int idx) { return idx <= 0 ? -1 : idx - 1; }

    /** Rysuje przykładowy napis w danym stylu (czcionka, kolor, blask, animacja). */
    private void sample(DrawContext ctx, TextRenderer tr, String text, int midX, int y, int font, int color, int glow, int anim, long ms, float alpha) {
        boolean pi = Glass.inHud; int pf = Glass.hudFont;
        Glass.inHud = true; Glass.hudFont = fid(font);
        try {
            double t = (ms % 3200) / 3200.0;
            int a = (int) (255 * alpha);
            int dy = 0;
            if (anim == 1) a = (int) (a * (0.35 + 0.65 * (0.5 + 0.5 * Math.sin(ms / 420.0))));
            if (anim == 2) dy = (int) Math.round((1 - Util2.ease(Math.min(1, t * 3))) * 5);
            String s = text;
            if (anim == 0) s = text.substring(0, (int) Math.ceil(Math.min(1, t * 1.5) * text.length()));
            int tw = tr.getWidth(text);
            int x = midX - tw / 2;
            int ga = (int) Math.max(0, Math.min(255, a / 4 * (0.2 + glow / 100f * 1.6)));
            int rgb = color & 0xFFFFFF;
            if (anim == 4) {
                int xx = x;
                for (int i = 0; i < text.length(); i++) {
                    String ch = String.valueOf(text.charAt(i));
                    int d = (int) Math.round(Math.sin(ms / 260.0 + i * 0.55) * 2.2);
                    if (ga > 4) { ctx.drawText(tr, ch, xx - 1, y + d, (ga << 24) | rgb, false); ctx.drawText(tr, ch, xx + 1, y + d, (ga << 24) | rgb, false); }
                    int c = rgbOf(color, i, ms);
                    ctx.drawText(tr, ch, xx, y + d, (a << 24) | c, true);
                    xx += tr.getWidth(ch);
                }
                return;
            }
            if (ga > 4) {
                ctx.drawText(tr, s, x - 1, y + dy, (ga << 24) | rgb, false); ctx.drawText(tr, s, x + 1, y + dy, (ga << 24) | rgb, false);
                ctx.drawText(tr, s, x, y + dy - 1, (ga << 24) | rgb, false); ctx.drawText(tr, s, x, y + dy + 1, (ga << 24) | rgb, false);
            }
            ctx.drawText(tr, s, x, y + dy, (a << 24) | (color & 0xFFFFFF), true);
        } finally { Glass.inHud = pi; Glass.hudFont = pf; }
    }

    private static int rgbOf(int color, int i, long ms) { return color & 0xFFFFFF; }

    private static final class Util2 { static double ease(double t) { t = Math.max(0, Math.min(1, t)); return 1 - Math.pow(1 - t, 3); } }

    private static int colorOf(String raw, long ms) {
        try {
            String[] p = raw.split(",");
            if (p.length > 4 && p[4].trim().equals("1")) return java.awt.Color.HSBtoRGB((ms % 4000) / 4000f, 0.85f, 1f) & 0xFFFFFF;
            return (Integer.parseInt(p[0].trim()) << 16) | (Integer.parseInt(p[1].trim()) << 8) | Integer.parseInt(p[2].trim());
        } catch (Exception e) { return 0xFFFFFF; }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        Ui.beginFrame();
        layout();
        mx = Math.round(mx / S); my = Math.round(my / S);
        float anim = Ui.easeOut((System.currentTimeMillis() - opened) / 240f);
        ctx.fill(0, 0, width, height, Ui.fade(0x66000000, anim));
        TextRenderer tr = client.textRenderer;
        Compat.push(ctx);
        Compat.scale(ctx, S, S);
        Ui.extra = S;
        try { panel(ctx, tr, mx, my, anim); } finally { Ui.extra = 1f; Compat.pop(ctx); }
    }

    private void panel(DrawContext ctx, TextRenderer tr, int mx, int my, float anim) {
        long ms = System.currentTimeMillis();
        Ui.glass(ctx, px, py, pw, ph, 14, Ui.fade(0xC4080C16, anim));
        if (anim < 0.3f) return;
        ctx.drawText(tr, "♪ " + L.t("Music Display"), px + 12, py + 10, 0xFFFFFFFF, true);
        boolean xh = Ui.in(mx, my, px + pw - 22, py + 8, 14, 14);
        Ui.card(ctx, px + pw - 22, py + 8, 14, 14, 7, xh ? 0x40FFFFFF : 0x1CFFFFFF, 0x44FFFFFF);
        ctx.drawText(tr, "x", px + pw - 17, py + 11, 0xFFFFFFFF, false);
        // zakładki
        for (int i = 0; i < TABS.length; i++) {
            int ty = py + 30 + i * 22;
            boolean on = i == tab, h = Ui.in(mx, my, px + 6, ty, sbw - 4, 19);
            Ui.card(ctx, px + 6, ty, sbw - 4, 19, 9, on ? 0x38FFFFFF : (h ? 0x20FFFFFF : 0x0CFFFFFF), on ? 0xAAFFFFFF : 0x22FFFFFF);
            ctx.drawText(tr, tr.trimToWidth(L.t(TABS[i]), sbw - 20), px + 14, ty + 6, on ? 0xFFFFFFFF : 0xFFB8BFCC, false);
        }
        // zawartość
        ctx.enableScissor(Math.round(cx * S), Math.round(cy * S), Math.round((cx + cw) * S), Math.round((cy + chh) * S));
        try { content(ctx, tr, mx, my, ms); } finally { ctx.disableScissor(); }
        // podgląd
        int pY = py + ph - 34 - prevH;
        Ui.card(ctx, cx, pY, cw, prevH, 8, 0x40000000, 0x33FFFFFF);
        ctx.drawText(tr, L.t("PREVIEW"), cx + 6, pY + 4, 0xFF6F7788, false);
        int col = colorOf(m().raw("wtext"), ms);
        sample(ctx, tr, "Under a midnight blue sky", cx + cw / 2, pY + prevH / 2 - 2, fontIdx(), col, m().num("wglow"), m().num("wanim"), ms, 1f);
        // stopka
        int fy = py + ph - 24;
        button(ctx, tr, px + 10, fy, bw(L.t("Reset Tab")), 16, L.t("Reset Tab"), mx, my, false);
        button(ctx, tr, px + 16 + bw(L.t("Reset Tab")), fy, bw(L.t("Move on screen")), 16, L.t("Move on screen"), mx, my, false);
        button(ctx, tr, px + pw - 10 - bw(L.t("Done")), fy, bw(L.t("Done")), 16, L.t("Done"), mx, my, true);
    }

    private int bw(String l) { return client.textRenderer.getWidth(l) + 20; }

    private void button(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, String label, int mx, int my, boolean accent) {
        boolean hv = Ui.in(mx, my, x, y, w, h);
        Ui.card(ctx, x, y, w, h, h / 2, accent ? (hv ? 0x7040C060 : 0x5040C060) : (hv ? 0x3CFFFFFF : 0x1CFFFFFF), accent ? 0xCC6BE08A : (hv ? 0xCCFFFFFF : 0x88FFFFFF));
        ctx.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + (h - 8) / 2, 0xFFFFFFFF, false);
    }

    // ───────── zawartość zakładek ─────────
    private int rowY;

    private void content(DrawContext ctx, TextRenderer tr, int mx, int my, long ms) {
        rowY = cy - (int) scroll;
        Module m = m();
        switch (tab) {
            case 0 -> presets(ctx, tr, mx, my, ms);
            case 1 -> {
                chips(ctx, tr, mx, my, "font", "Font", FONTS);
                colors(ctx, tr, mx, my);
                slider(ctx, tr, mx, my, "wglow", "Glow strength", 0, 100);
                toggle(ctx, tr, mx, my, "wplate", "Dark plate behind text");
            }
            case 2 -> chips(ctx, tr, mx, my, "wanim", "Text animation", ANIMS);
            case 3 -> {
                slider(ctx, tr, mx, my, "wdist", "Text distance (blocks)", 2, 16);
                slider(ctx, tr, mx, my, "wsize", "Text size (%)", 50, 250);
                slider(ctx, tr, mx, my, "wspread", "Side offset (degrees)", 0, 45);
            }
            default -> {
                toggle(ctx, tr, mx, my, "world", "Lyrics in the world (anchored)");
                toggle(ctx, tr, mx, my, "lyrics", "Show lyrics");
                toggle(ctx, tr, mx, my, "cover", "Show cover");
                toggle(ctx, tr, mx, my, "hide", "Hide when nothing plays");
                slider(ctx, tr, mx, my, "width", "Min width", 100, 320);
            }
        }
        totalH = rowY - (cy - (int) scroll);
    }

    private int totalH;

    private int presetBoxH() { return 24; }

    private void presets(DrawContext ctx, TextRenderer tr, int mx, int my, long ms) {
        Module m = m();
        for (Preset p : PRESETS) {
            int y = rowY;
            boolean hv = Ui.in(mx, my, cx, y, cw, presetBoxH()) && my >= cy && my < cy + chh;
            boolean on = p.font == m.num("font") && p.color.equals(m.raw("wtext")) && p.glow == m.num("wglow") && p.anim == m.num("wanim");
            Ui.card(ctx, cx, y, cw, presetBoxH(), 6, hv ? 0x30FFFFFF : 0x18FFFFFF, on ? 0xFF6BE08A : 0x33FFFFFF);
            sample(ctx, tr, p.name, cx + cw / 2, y + 8, p.font, colorOf(p.color, ms), p.glow, p.anim == 0 ? 1 : p.anim, ms, 1f);
            rowY += presetBoxH() + 4;
        }
    }

    private void label(DrawContext ctx, TextRenderer tr, String s) { ctx.drawText(tr, L.t(s), cx + 4, rowY + 2, 0xFFC8CCD6, false); }

    private void chips(DrawContext ctx, TextRenderer tr, int mx, int my, String key, String label, String[] opts) {
        label(ctx, tr, label); rowY += 14;
        int cur = m().num(key), x = cx + 2;
        for (int i = 0; i < opts.length; i++) {
            String o = L.t(opts[i]);
            int w = tr.getWidth(o) + 14;
            if (x + w > cx + cw) { x = cx + 2; rowY += 20; }
            boolean on = i == cur, hv = Ui.in(mx, my, x, rowY, w, 16);
            Ui.card(ctx, x, rowY, w, 16, 8, on ? 0x38FFFFFF : (hv ? 0x26FFFFFF : 0x12FFFFFF), on ? 0xFFFFFFFF : 0x33FFFFFF);
            ctx.drawText(tr, o, x + 7, rowY + 4, on ? 0xFFFFFFFF : 0xFFB8BFCC, false);
            x += w + 4;
        }
        rowY += 24;
    }

    private void colors(DrawContext ctx, TextRenderer tr, int mx, int my) {
        label(ctx, tr, "Lyrics text color"); rowY += 14;
        String cur = m().raw("wtext");
        for (int i = 0; i < COLORS.length; i++) {
            int x = cx + 4 + i * 20;
            int c = colorOf(COLORS[i][1], System.currentTimeMillis()) | 0xFF000000;
            boolean on = COLORS[i][1].equals(cur), hv = Ui.in(mx, my, x, rowY, 16, 16);
            Ui.rrectD(ctx, x, rowY, 16, 16, 8, c);
            Ui.ringT(ctx, x - 1, rowY - 1, 18, 18, 9, 1.0, on ? 0xFFFFFFFF : (hv ? 0xAAFFFFFF : 0x44FFFFFF));
        }
        rowY += 26;
    }

    private void toggle(DrawContext ctx, TextRenderer tr, int mx, int my, String key, String label) {
        label(ctx, tr, label);
        Ui.toggle(ctx, cx + cw - 34, rowY, 26, 13, Ui.tween("ls" + key, m().bool(key) ? 1f : 0f), 0xFF3DBE6A);
        rowY += 20;
    }

    private void slider(DrawContext ctx, TextRenderer tr, int mx, int my, String key, String label, int min, int max) {
        int v = m().num(key);
        label(ctx, tr, label);
        String vs = String.valueOf(v);
        ctx.drawText(tr, vs, cx + cw - 4 - tr.getWidth(vs), rowY + 2, 0xFFFFFFFF, false);
        int sx = cx + 4, sw = cw - 8, sy = rowY + 16;
        Ui.rrectD(ctx, sx, sy, sw, 3, 1.5, 0x33FFFFFF);
        float t = (v - min) / (float) (max - min);
        Ui.rrectD(ctx, sx, sy, Math.max(3, sw * t), 3, 1.5, 0xFFFFFFFF);
        Ui.rrectD(ctx, sx + sw * t - 4, sy - 2.5, 8, 8, 4, 0xFFFFFFFF);
        rowY += 26;
    }

    // ───────── obsługa myszy ─────────
    /** Przechodzi te same rzędy co rysowanie, ale reaguje na kliknięcie. */
    @Override
    protected boolean onClick(double mxd, double myd, int button) {
        layout();
        int mx = (int) (mxd / S), my = (int) (myd / S);
        if (!Ui.in(mx, my, px, py, pw, ph)) { close(); return true; }
        if (Ui.in(mx, my, px + pw - 22, py + 8, 14, 14)) { close(); return true; }
        for (int i = 0; i < TABS.length; i++) if (Ui.in(mx, my, px + 6, py + 30 + i * 22, sbw - 4, 19)) { tab = i; scroll = 0; return true; }
        int fy = py + ph - 24;
        if (Ui.in(mx, my, px + pw - 10 - bw(L.t("Done")), fy, bw(L.t("Done")), 16)) { close(); return true; }
        if (Ui.in(mx, my, px + 16 + bw(L.t("Reset Tab")), fy, bw(L.t("Move on screen")), 16)) { client.setScreen(new EditScreen(this, m())); return true; }
        if (Ui.in(mx, my, px + 10, fy, bw(L.t("Reset Tab")), 16)) { resetTab(); return true; }
        if (!Ui.in(mx, my, cx, cy, cw, chh)) return true;
        Module m = m();
        int y = cy - (int) scroll;
        switch (tab) {
            case 0 -> {
                for (Preset p : PRESETS) {
                    if (Ui.in(mx, my, cx, y, cw, presetBoxH())) { m.set("font", String.valueOf(p.font)); m.set("wtext", p.color); m.set("wglow", String.valueOf(p.glow)); m.set("wanim", String.valueOf(p.anim)); m.set("wplate", String.valueOf(p.plate)); Config.save(); return true; }
                    y += presetBoxH() + 4;
                }
            }
            case 1 -> {
                y = clickChips(mx, my, y, "font", FONTS);
                // kolory
                y += 14;
                for (int i = 0; i < COLORS.length; i++) if (Ui.in(mx, my, cx + 4 + i * 20, y, 16, 16)) { m.set("wtext", COLORS[i][1]); Config.save(); return true; }
                y += 26;
                y = clickSlider(mx, my, y, "wglow", 0, 100); if (dragKey != null) return true;
                if (Ui.in(mx, my, cx, y, cw, 20)) { m.set("wplate", String.valueOf(!m.bool("wplate"))); Config.save(); }
            }
            case 2 -> clickChips(mx, my, y, "wanim", ANIMS);
            case 3 -> {
                y = clickSlider(mx, my, y, "wdist", 2, 16); if (dragKey != null) return true;
                y = clickSlider(mx, my, y, "wsize", 50, 250); if (dragKey != null) return true;
                clickSlider(mx, my, y, "wspread", 0, 45);
            }
            default -> {
                String[] keys = {"world", "lyrics", "cover", "hide"};
                for (String k : keys) {
                    if (Ui.in(mx, my, cx, y, cw, 20)) { m.set(k, String.valueOf(!m.bool(k))); Config.save(); return true; }
                    y += 20;
                }
                clickSlider(mx, my, y, "width", 100, 320);
            }
        }
        return true;
    }

    private int clickChips(int mx, int my, int y, String key, String[] opts) {
        y += 14;
        int x = cx + 2;
        for (int i = 0; i < opts.length; i++) {
            int w = client.textRenderer.getWidth(L.t(opts[i])) + 14;
            if (x + w > cx + cw) { x = cx + 2; y += 20; }
            if (Ui.in(mx, my, x, y, w, 16)) { m().set(key, String.valueOf(i)); Config.save(); }
            x += w + 4;
        }
        return y + 24;
    }

    private int clickSlider(int mx, int my, int y, String key, int min, int max) {
        int sx = cx + 4, sw = cw - 8;
        if (Ui.in(mx, my, sx - 4, y + 10, sw + 8, 16)) { dragKey = key; dragX = sx; dragW = sw; dragMin = min; dragMax = max; applyDrag(mx); }
        return y + 26;
    }

    private void applyDrag(double mx) {
        if (dragKey == null) return;
        double t = Math.max(0, Math.min(1, (mx - dragX) / dragW));
        m().set(dragKey, String.valueOf((int) Math.round(dragMin + t * (dragMax - dragMin))));
    }

    @Override protected boolean onDrag(double mx, double my, int button, double dx, double dy) { if (dragKey == null) return false; applyDrag(mx / S); return true; }
    @Override protected boolean onRelease(double mx, double my, int button) { if (dragKey == null) return false; dragKey = null; Config.save(); return true; }
    @Override protected boolean onScroll(double mx, double my, double v) { scroll = Math.max(0, Math.min(Math.max(0, totalH - chh), scroll - v * 18)); return true; }

    private void resetTab() {
        Module m = m();
        String[][] defs = {
                {"font", "0", "wtext", Setting.WHITE, "wglow", "60", "wanim", "0", "wplate", "true"},
                {"font", "0", "wtext", Setting.WHITE, "wglow", "60", "wplate", "true"},
                {"wanim", "0"},
                {"wdist", "6", "wsize", "100", "wspread", "22"},
                {"world", "true", "lyrics", "true", "cover", "true", "hide", "true", "width", "130"}};
        String[] d = defs[tab];
        for (int i = 0; i + 1 < d.length; i += 2) m.set(d[i], d[i + 1]);
        Config.save();
    }
}
