package pl.kaczo.hud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Edytor HUD: przeciągaj elementy (z przyciąganiem do środka, krawędzi i innych elementów),
 * kliknij PRAWYM przyciskiem element, aby otworzyć jego ustawienia, a przyciskiem „Elementy” włączaj i wyłączaj moduły.
 */
public class EditScreen extends KScreen {
    private final Screen parent;
    private final long openedAt = System.currentTimeMillis();
    private Module dragging;
    private boolean dragSb = false;
    private int sbStartX, sbStartY, sbMouseX, sbMouseY, offX, offY;
    private boolean snap = true;
    private final List<int[]> guides = new ArrayList<>();   // {0=pionowa/1=pozioma, pozycja}

    private Module popup = null;                 // moduł, którego ustawienia są otwarte
    private SettingsPanel pPanel = null;
    private int pX, pY, pW = 206, pH;
    private boolean listOpen = false;
    private double listScroll = 0;
    private Module flash;
    private float anim = 0f;

    public EditScreen(Screen parent, Module focus) {
        super(Text.literal("Edit HUD"));
        this.parent = parent;
        this.flash = focus;
        if (focus != null) { popup = focus; }
    }

    private int w(Module m) { return Math.round(m.width(client) * m.scale()); }
    private int h(Module m) { return Math.round(m.height(client) * m.scale()); }

    private boolean hit(Module m, double mx, double my) {
        return Ui.in(mx, my, m.x() - 3, m.y() - 3, w(m) + 6, h(m) + 6);
    }

    private int[] sbRect() {
        Module sb = Modules.SCOREBOARD;
        float sc = sb.num("scale") / 100f;
        int w = Math.round(110 * sc), h = Math.round(80 * sc);
        return new int[]{width - w + Hud.sbDx(), height / 2 - h / 2 + Hud.sbDy(), w, h};
    }

    private int f(int argb) { return Ui.fade(argb, anim); }

    // ───────────── pasek narzędzi ─────────────

    private static final String[] TB = {"Elements", "Snap", "Reset all", "Done"};

    private String tbLabel(int i) {
        if (i == 1) return L.t("Snap") + ": " + L.t(snap ? "ON" : "OFF");
        return L.t(TB[i]);
    }

    private int[] tbRect(int i) {
        TextRenderer tr = client.textRenderer;
        int total = 0;
        for (int k = 0; k < TB.length; k++) total += tr.getWidth(tbLabel(k)) + 20 + (k > 0 ? 4 : 0);
        int x = (width - total) / 2;
        for (int k = 0; k < i; k++) x += tr.getWidth(tbLabel(k)) + 24;
        return new int[]{x, 8, tr.getWidth(tbLabel(i)) + 20, 18};
    }

    // ───────────── rysowanie ─────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        Ui.beginFrame();
        anim = Ui.easeOut((System.currentTimeMillis() - openedAt) / 220f);
        TextRenderer tr = client.textRenderer;
        ctx.fill(0, 0, width, height, f(0x70060910));

        if (dragging != null || dragSb) {              // linie środka ekranu podczas przeciągania
            ctx.fill(width / 2, 0, width / 2 + 1, height, f(0x2235E6FF));
            ctx.fill(0, height / 2, width, height / 2 + 1, f(0x2235E6FF));
        }

        for (Module m : Modules.ALL) {
            if (!m.hud || !m.enabled()) continue;
            Hud.drawModule(ctx, client, m, m.x(), m.y());
            boolean hv = hit(m, mx, my) && popupCovers(mx, my) == false;
            boolean sel = m == dragging || m == popup;
            float t = Ui.tween("eh" + m.id, hv || sel ? 1f : 0f);
            int col = sel ? 0xFFFFC21A : Ui.mix(0x88FFE14A, 0xFFFFFFFF, t);
            if (m == flash) { col = Ui.mix(0xFFFFC21A, 0xFFFFFFFF, (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 140.0))); }
            Ui.ringT(ctx, m.x() - 3, m.y() - 3, w(m) + 6, h(m) + 6, 4, sel ? 1.2 : 0.8, f(col));
            if (hv || sel) {
                String name = L.t(m.name);
                int tw = tr.getWidth(name) + 10;
                int ty = m.y() - 16 < 2 ? m.y() + h(m) + 6 : m.y() - 17;
                Ui.rrectD(ctx, m.x() - 3, ty, tw, 12, 6, f(0xE0141B2C));
                ctx.drawText(tr, name, m.x() + 2, ty + 2, f(0xFFFFE14A), false);
            }
        }
        if (Modules.SCOREBOARD.enabled()) {
            int[] r = sbRect();
            Ui.rrectD(ctx, r[0], r[1], r[2], r[3], 5, f(0x66000000));
            boolean sel = dragSb || popup == Modules.SCOREBOARD;
            Ui.ringT(ctx, r[0], r[1], r[2], r[3], 5, sel ? 1.2 : 0.8, f(sel ? 0xFFFFC21A : 0x88FFE14A));
            Ui.centered(ctx, client, L.t("Scoreboard"), r[0] + r[2] / 2, r[1] + r[3] / 2 - 4, f(0xFFFFFFFF));
        }
        for (int[] g : guides) {
            if (g[0] == 0) ctx.fill(g[1], 0, g[1] + 1, height, f(0xCC35E6FF));
            else ctx.fill(0, g[1], width, g[1] + 1, f(0xCC35E6FF));
        }

        // pasek narzędzi
        int total = 0;
        for (int k = 0; k < TB.length; k++) total += tr.getWidth(tbLabel(k)) + 24;
        Ui.glass(ctx, (width - total) / 2 - 6, 4, total + 8, 26, 13, f(0xD0101624));
        for (int k = 0; k < TB.length; k++) {
            int[] r = tbRect(k);
            boolean hv = Ui.in(mx, my, r[0], r[1] + 2, r[2], r[3]) && !popupCovers(mx, my);
            boolean on = (k == 0 && listOpen) || (k == 1 && snap);
            int fill = k == 3 ? 0xFFFFC21A : (on ? 0x55FFC21A : (hv ? 0x40FFFFFF : 0x22FFFFFF));
            Ui.rrectD(ctx, r[0], r[1] + 2, r[2], r[3], 9, f(fill));
            ctx.drawText(tr, tbLabel(k), r[0] + 10, r[1] + 7, f(k == 3 ? 0xFF1A1400 : 0xFFFFFFFF), false);
        }
        if (popup == null && !listOpen)
            Ui.centered(ctx, client, L.t("Drag elements. Right-click an element to configure it. Hold Shift = no snapping."), width / 2, height - 14, f(0xFFC4CBDD));

        if (listOpen) drawList(ctx, tr, mx, my);
        if (popup != null) drawPopup(ctx, tr, mx, my);
    }

    private boolean popupCovers(double mx, double my) {
        if (popup != null && Ui.in(mx, my, pX, pY, pW, pH)) return true;
        if (listOpen && Ui.in(mx, my, 8, 36, 176, listH())) return true;
        return false;
    }

    // lista modułów do włączania
    private int listH() { return Math.min(height - 48, 280); }

    private List<Module> hudModules() {
        List<Module> out = new ArrayList<>();
        for (Module m : Modules.ALL) if (m.hud) out.add(m);
        out.add(Modules.SCOREBOARD);
        return out;
    }

    private void drawList(DrawContext ctx, TextRenderer tr, int mx, int my) {
        int x = 8, y = 36, w = 176, h = listH();
        Ui.glass(ctx, x, y, w, h, 9, f(0xF0141B2C));
        ctx.drawText(tr, L.t("Elements").toUpperCase(), x + 10, y + 8, f(0xFFFFC21A), false);
        List<Module> list = hudModules();
        int top = y + 22, vh = h - 28;
        double max = Math.max(0, list.size() * 20 - vh);
        listScroll = Math.max(0, Math.min(max, listScroll));
        ctx.enableScissor(x, top, x + w, top + vh);
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int ry = top + i * 20 - (int) listScroll;
            if (ry + 20 < top || ry > top + vh) continue;
            boolean hv = Ui.in(mx, my, x + 4, ry, w - 8, 19);
            if (hv) Ui.rrectD(ctx, x + 4, ry, w - 8, 19, 5, f(0x1CFFFFFF));
            ctx.drawText(tr, tr.trimToWidth(L.t(m.name), w - 56), x + 10, ry + 6, f(m.enabled() ? 0xFFFFFFFF : 0xFF9AA3B8), false);
            Ui.toggle(ctx, x + w - 36, ry + 3, 24, 12, Ui.tween("on" + m.id, m.enabled() ? 1f : 0f), f(0xFF3DBE6A));
        }
        ctx.disableScissor();
    }

    private void placePopup() {
        pH = Math.min(height - 16, 270);
        int ex, ey, ew;
        if (popup == Modules.SCOREBOARD) { int[] r = sbRect(); ex = r[0]; ey = r[1]; ew = r[2]; }
        else { ex = popup.x(); ey = popup.y(); ew = w(popup); }
        pX = ex + ew + 12;
        if (pX + pW > width - 8) pX = ex - pW - 12;
        if (pX < 8) pX = Math.max(8, Math.min(width - pW - 8, ex));
        pY = Math.max(8, Math.min(height - pH - 8, ey - 10));
        if (listOpen && pX < 190) pX = Math.min(width - pW - 8, 192);
    }

    private void drawPopup(DrawContext ctx, TextRenderer tr, int mx, int my) {
        if (pPanel == null || pPanel.module() != popup) pPanel = new SettingsPanel(popup);
        placePopup();
        Module m = popup;
        Ui.glass(ctx, pX, pY, pW, pH, 9, f(0xF2141B2C));
        ctx.drawText(tr, tr.trimToWidth(L.t(m.name), pW - 90), pX + 10, pY + 10, f(0xFFFFFFFF), true);
        Ui.toggle(ctx, pX + pW - 52, pY + 9, 26, 13, Ui.tween("on" + m.id, m.enabled() ? 1f : 0f), f(0xFF3DBE6A));
        boolean xh = Ui.in(mx, my, pX + pW - 22, pY + 8, 14, 14);
        Ui.rrectD(ctx, pX + pW - 22, pY + 8, 14, 14, 7, f(xh ? 0x40FFFFFF : 0x1CFFFFFF));
        ctx.drawText(tr, "x", pX + pW - 17, pY + 11, f(0xFFFFFFFF), false);
        Ui.rrectD(ctx, pX + 10, pY + 27, pW - 20, 0.6, 0.3, f(0x22FFFFFF));
        pPanel.bounds(pX + 2, pY + 30, pW - 4, pH - 30 - 28);
        pPanel.render(ctx, mx, my, anim);
        int by = pY + pH - 24;
        int bw = (pW - 30) / 2;
        button(ctx, tr, pX + 10, by, bw, 16, L.t("Reset position"), mx, my, 0x30FFFFFF, 0xFFFFFFFF);
        button(ctx, tr, pX + 20 + bw, by, bw, 16, L.t("Reset"), mx, my, 0x30FF5C63, 0xFFFF8A90);
    }

    private void button(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, String label, int mx, int my, int fill, int fg) {
        boolean hover = Ui.in(mx, my, x, y, w, h);
        Ui.card(ctx, x, y, w, h, h / 2, f(Ui.mix(fill, fill | 0x30000000, hover ? 1f : 0f)), f(0x26FFFFFF));
        int tw = tr.getWidth(label);
        ctx.drawText(tr, label, x + (w - tw) / 2, y + (h - 8) / 2, f(fg), false);
    }

    // ───────────── mysz ─────────────

    @Override
    protected boolean onClick(double mx, double my, int button) {
        // pasek narzędzi
        for (int k = 0; k < TB.length; k++) {
            int[] r = tbRect(k);
            if (Ui.in(mx, my, r[0], r[1] + 2, r[2], r[3])) {
                switch (k) {
                    case 0 -> listOpen = !listOpen;
                    case 1 -> snap = !snap;
                    case 2 -> resetAll();
                    default -> close();
                }
                return true;
            }
        }
        // okno ustawień
        if (popup != null && Ui.in(mx, my, pX, pY, pW, pH)) {
            if (Ui.in(mx, my, pX + pW - 22, pY + 8, 14, 14)) { popup = null; return true; }
            if (Ui.in(mx, my, pX + pW - 52, pY + 9, 26, 13)) { popup.setEnabled(!popup.enabled()); return true; }
            int by = pY + pH - 24, bw = (pW - 30) / 2;
            if (Ui.in(mx, my, pX + 10, by, bw, 16)) {
                if (popup == Modules.SCOREBOARD) Hud.setSb(0, 0); else { Config.st(popup.id).x = -1; Config.st(popup.id).y = -1; }
                return true;
            }
            if (Ui.in(mx, my, pX + 20 + bw, by, bw, 16)) { Config.st(popup.id).vals.clear(); if (popup == Modules.SCOREBOARD) Hud.setSb(0, 0); return true; }
            if (pPanel != null) pPanel.mouseClicked(mx, my, button);
            return true;
        }
        // lista elementów
        if (listOpen && Ui.in(mx, my, 8, 36, 176, listH())) {
            List<Module> list = hudModules();
            int top = 36 + 22;
            for (int i = 0; i < list.size(); i++) {
                int ry = top + i * 20 - (int) listScroll;
                if (Ui.in(mx, my, 12, ry, 168, 19) && my >= top) {
                    Module m = list.get(i);
                    m.setEnabled(!m.enabled());
                    return true;
                }
            }
            return true;
        }
        flash = null;
        // elementy HUD
        if (Modules.SCOREBOARD.enabled()) {
            int[] r = sbRect();
            if (Ui.in(mx, my, r[0], r[1], r[2], r[3])) {
                if (button == 1) { popup = Modules.SCOREBOARD; pPanel = null; return true; }
                dragSb = true; sbStartX = Hud.sbDx(); sbStartY = Hud.sbDy(); sbMouseX = (int) mx; sbMouseY = (int) my;
                return true;
            }
        }
        for (int i = Modules.ALL.size() - 1; i >= 0; i--) {
            Module m = Modules.ALL.get(i);
            if (m.hud && m.enabled() && hit(m, mx, my)) {
                if (button == 1) { popup = m; pPanel = null; return true; }      // prawy przycisk: ustawienia elementu
                dragging = m; offX = (int) mx - m.x(); offY = (int) my - m.y();
                return true;
            }
        }
        if (button == 1 || button == 0) popup = null;
        return true;
    }

    private void resetAll() {
        for (Module m : Modules.ALL) if (m.hud) { Config.st(m.id).x = -1; Config.st(m.id).y = -1; }
        Hud.setSb(0, 0);
    }

    @Override
    protected boolean onDrag(double mx, double my, int button, double dx, double dy) {
        if (popup != null && pPanel != null && pPanel.mouseDragged(mx, my)) return true;
        guides.clear();
        if (dragSb) {
            Hud.setSb(sbStartX + (int) mx - sbMouseX, sbStartY + (int) my - sbMouseY);
            return true;
        }
        if (dragging != null) {
            int nx = (int) mx - offX, ny = (int) my - offY;
            int mw = w(dragging), mh = h(dragging);
            boolean shift = Compat.keyDown(client, GLFW.GLFW_KEY_LEFT_SHIFT);
            if (snap && !shift) {
                List<int[]> xs = new ArrayList<>(), ys = new ArrayList<>();      // {cel}
                xs.add(new int[]{4}); xs.add(new int[]{width / 2}); xs.add(new int[]{width - 4});
                ys.add(new int[]{4}); ys.add(new int[]{height / 2}); ys.add(new int[]{height - 4});
                for (Module o : Modules.ALL) {
                    if (o == dragging || !o.hud || !o.enabled()) continue;
                    xs.add(new int[]{o.x()}); xs.add(new int[]{o.x() + w(o) / 2}); xs.add(new int[]{o.x() + w(o)});
                    ys.add(new int[]{o.y()}); ys.add(new int[]{o.y() + h(o) / 2}); ys.add(new int[]{o.y() + h(o)});
                }
                int bestX = 5, bestY = 5, gx = -1, gy = -1, ax = nx, ay = ny;
                int[] ex = {0, mw / 2, mw}, ey = {0, mh / 2, mh};
                for (int[] t : xs) for (int e : ex) { int d = t[0] - (nx + e); if (Math.abs(d) < Math.abs(bestX)) { bestX = d; ax = nx + d; gx = t[0]; } }
                for (int[] t : ys) for (int e : ey) { int d = t[0] - (ny + e); if (Math.abs(d) < Math.abs(bestY)) { bestY = d; ay = ny + d; gy = t[0]; } }
                if (Math.abs(bestX) <= 4) { nx = ax; guides.add(new int[]{0, gx}); }
                if (Math.abs(bestY) <= 4) { ny = ay; guides.add(new int[]{1, gy}); }
            }
            dragging.setPos(Math.max(0, Math.min(width - 8, nx)), Math.max(0, Math.min(height - 8, ny)));
            return true;
        }
        return false;
    }

    @Override
    protected boolean onRelease(double mx, double my, int button) {
        boolean was = dragging != null || dragSb;
        dragging = null; dragSb = false; guides.clear();
        if (pPanel != null && pPanel.mouseReleased()) was = true;
        if (was) Config.save();
        return was;
    }

    @Override
    protected boolean onScroll(double mx, double my, double v) {
        if (popup != null && pPanel != null && pPanel.scrolled(mx, my, v)) return true;
        if (listOpen && Ui.in(mx, my, 8, 36, 176, listH())) { listScroll -= v * 20; return true; }
        return false;
    }

    @Override
    protected boolean onKey(int key, int scan, int mods) {
        if (key == 256) {                                   // Esc: najpierw zamknij okna
            if (popup != null) { popup = null; return true; }
            if (listOpen) { listOpen = false; return true; }
        }
        return false;
    }

    @Override
    public void close() {
        Config.save();
        client.setScreen(parent);
    }
}
