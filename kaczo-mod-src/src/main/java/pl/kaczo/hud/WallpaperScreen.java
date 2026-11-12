package pl.kaczo.hud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Panel tapet menu głównego: siatka miniatur (live i statyczne), wyszukiwarka, kategorie. Najechanie = podgląd w tle. */
public class WallpaperScreen extends KScreen {
    private static final String[] TABS = {"All", "Live", "Static", "Dark", "Nature", "Space", "Colors"};
    private static String tab = "All", query = "";
    private static double scroll = 0;

    private final Screen parent;
    private final long openedAt = System.currentTimeMillis();
    private boolean focus = false;
    private final List<Wallpapers.Wp> shown = new ArrayList<>();
    private int px, py, pw, ph, gx, gy, gw, gh, cols, cw, ch;
    private static final int GAP = 8;

    public WallpaperScreen(Screen parent) { super(Text.literal("Wallpapers")); this.parent = parent; }

    @Override public void close() { client.setScreen(parent); }

    private void layout() {
        pw = Math.min(width - 20, 560);
        ph = Math.min(height - 20, 340);
        px = (width - pw) / 2; py = (height - ph) / 2;
        gx = px + 14; gy = py + 64; gw = pw - 28; gh = ph - 64 - 12;
        cols = Math.max(2, Math.min(4, (gw + GAP) / 118));
        cw = (gw - GAP * (cols - 1)) / cols;
        ch = cw * 9 / 16;
    }

    private void filter() {
        shown.clear();
        String q = query.toLowerCase();
        for (Wallpapers.Wp w : Wallpapers.ALL) {
            if (tab.equals("Live") && !w.live()) continue;
            if (tab.equals("Static") && w.live()) continue;
            if (!tab.equals("All") && !tab.equals("Live") && !tab.equals("Static") && !w.cat().equals(tab)) continue;
            if (!q.isEmpty() && !w.name().toLowerCase().contains(q)) continue;
            shown.add(w);
        }
    }

    private int cellX(int i) { return gx + (i % cols) * (cw + GAP); }
    private int cellY(int i) { return gy + (i / cols) * (ch + 22 + GAP) - (int) scroll; }
    private int maxScroll() { return Math.max(0, ((shown.size() + cols - 1) / cols) * (ch + 22 + GAP) - GAP - gh); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        Ui.beginFrame();
        layout(); filter();
        float anim = Ui.easeOut((System.currentTimeMillis() - openedAt) / 260f);
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
        TextRenderer tr = client.textRenderer;
        // tło: tapeta pod kursorem (podgląd) albo wybrana
        Wallpapers.Wp hov = null;
        for (int i = 0; i < shown.size(); i++)
            if (Ui.in(mx, my, cellX(i), cellY(i), cw, ch) && my >= gy && my <= gy + gh) hov = shown.get(i);
        Wallpapers.Wp cur = Wallpapers.current();
        ctx.fill(0, 0, width, height, 0xFF000000);
        try { Wallpapers.draw(ctx, hov != null ? hov : cur, 0, 0, width, height, 1f, 1f); } catch (Throwable ignored) { }
        Ui.glass(ctx, px, py, pw, ph, 12, Ui.fade(0xB4000000, anim));
        if (anim < 0.3f) return;
        // nagłówek
        Compat.push(ctx);
        Compat.translate(ctx, px + 16, py + 12);
        Compat.scale(ctx, 1.4f, 1.4f);
        ctx.drawText(tr, L.t("Wallpapers"), 0, 0, 0xFFFFFFFF, true);
        Compat.pop(ctx);
        long live = Wallpapers.ALL.stream().filter(Wallpapers.Wp::live).count();
        ctx.drawText(tr, Wallpapers.ALL.size() + " " + L.t("wallpapers") + "  ·  " + live + " live  ·  " + L.t("click to apply, hover to preview"), px + 16, py + 28, 0xFF8A8A8A, false);
        // wyszukiwarka
        int sw = Math.min(140, pw / 3), sx = px + pw - sw - 14, sy = py + 14;
        boolean sh = Ui.in(mx, my, sx, sy, sw, 16);
        Ui.card(ctx, sx, sy, sw, 16, 8, focus ? 0x30FFFFFF : (sh ? 0x26FFFFFF : 0x18FFFFFF), focus ? 0x88FFFFFF : 0x22FFFFFF);
        String st = tr.trimToWidth(query.isEmpty() && !focus ? L.t("Search") : query, sw - 18);
        ctx.drawText(tr, st, sx + 9, sy + 4, query.isEmpty() ? 0xFF808080 : 0xFFFFFFFF, false);
        if (focus && (System.currentTimeMillis() / 500) % 2 == 0) ctx.fill(sx + 9 + tr.getWidth(st), sy + 3, sx + 10 + tr.getWidth(st), sy + 13, 0xFFFFFFFF);
        // zakładki
        int tx = px + 16, ty = py + 42;
        int total = 0;
        for (String t : TABS) total += tr.getWidth(L.t(t)) + 16;
        Ui.glass(ctx, tx - 3, ty - 3, total + 4, 20, 10, 0x26FFFFFF);
        int x = tx;
        for (String t : TABS) {
            int w = tr.getWidth(L.t(t)) + 14;
            boolean on = t.equals(tab), h = Ui.in(mx, my, x, ty, w, 14);
            float sv = Ui.tween("wt" + t, on ? 1f : (h ? 0.4f : 0f));
            if (sv > 0.01f) Ui.rrectD(ctx, x, ty, w, 14, 7, (int) (0x50 * sv) << 24 | 0xFFFFFF);
            ctx.drawText(tr, L.t(t), x + 7, ty + 3, on || h ? 0xFFFFFFFF : 0xFFC4C4C4, false);
            x += w + 2;
        }
        // siatka
        ctx.enableScissor(gx - 4, gy - 2, gx + gw + 4, gy + gh);
        for (int i = 0; i < shown.size(); i++) {
            Wallpapers.Wp w = shown.get(i);
            int cx = cellX(i), cy = cellY(i);
            if (cy + ch + 20 < gy || cy > gy + gh) continue;
            boolean hv = Ui.in(mx, my, cx, cy, cw, ch) && my >= gy && my <= gy + gh, sel = w == cur;
            float hz = Ui.tween("wc" + w.id(), hv ? 1f : 0f);
            ctx.enableScissor(cx, Math.max(cy, gy), cx + cw, Math.min(cy + ch, gy + gh));
            Wallpapers.draw(ctx, w, cx, cy, cw, ch, 1f, hv ? 1f : 0f);
            ctx.disableScissor();
            Ui.ringT(ctx, cx - 1, cy - 1, cw + 2, ch + 2, 7, sel ? 2 : 1, sel ? 0xFFFFFFFF : (int) (0x30 + 0x60 * hz) << 24 | 0xFFFFFF);
            if (w.live()) {
                Ui.rrectD(ctx, cx + 5, cy + 5, 28, 11, 5.5, 0xC0000000);
                Ui.rrectD(ctx, cx + 9, cy + 9, 4, 4, 2, 0xFFFF4D5A);
                Compat.push(ctx); Compat.scale(ctx, 0.65f, 0.65f);
                ctx.drawText(tr, "LIVE", Math.round((cx + 15) / 0.65f), Math.round((cy + 8) / 0.65f), 0xFFFFFFFF, false);
                Compat.pop(ctx);
            }
            if (sel) {
                Ui.rrectD(ctx, cx + cw - 17, cy + 5, 12, 12, 6, 0xFFFFFFFF);
                ctx.drawText(tr, "✓", cx + cw - 14, cy + 7, 0xFF000000, false);
            }
            ctx.drawText(tr, tr.trimToWidth(w.name(), cw), cx + 2, cy + ch + 5, hv || sel ? 0xFFFFFFFF : 0xFFC0C0C0, false);
            ctx.drawText(tr, w.cat() + " · " + (w.live() ? "live" : "static"), cx + 2, cy + ch + 14, 0xFF707070, false);
        }
        ctx.disableScissor();
        if (shown.isEmpty()) ctx.drawText(tr, L.t("No wallpapers found"), gx + 4, gy + 10, 0xFF8A8A8A, false);
    }

    @Override
    protected boolean onClick(double mx, double my, int b) {
        layout(); filter();
        focus = false;
        if (!Ui.in(mx, my, px, py, pw, ph)) { close(); return true; }
        int sw = Math.min(140, pw / 3);
        if (Ui.in(mx, my, px + pw - sw - 14, py + 14, sw, 16)) { focus = true; return true; }
        TextRenderer tr = client.textRenderer;
        int x = px + 16;
        for (String t : TABS) {
            int w = tr.getWidth(L.t(t)) + 14;
            if (Ui.in(mx, my, x, py + 42, w, 14)) { tab = t; scroll = 0; return true; }
            x += w + 2;
        }
        if (my >= gy && my <= gy + gh)
            for (int i = 0; i < shown.size(); i++)
                if (Ui.in(mx, my, cellX(i), cellY(i), cw, ch)) { Wallpapers.select(shown.get(i)); return true; }
        return true;
    }

    @Override protected boolean onScroll(double mx, double my, double v) { scroll = Math.max(0, Math.min(maxScroll(), scroll - v * 24)); return true; }

    @Override
    protected boolean onChar(int cp) {
        if (!focus || cp < 32 || cp == 127) return false;
        if (query.length() < 24) { query += new String(Character.toChars(cp)); scroll = 0; }
        return true;
    }

    @Override
    protected boolean onKey(int key, int scan, int mods) {
        if (focus) {
            if (key == 259 && !query.isEmpty()) { query = query.substring(0, query.length() - 1); return true; }
            if (key == 257 || key == 335 || key == 256) { focus = false; return true; }
            return true;
        }
        return false;
    }

    @Override public boolean shouldPause() { return false; }
}
