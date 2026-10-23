package pl.kaczo.hud;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Główne menu moda (prawy Shift) w układzie „panel boczny + zawartość + szuflada”:
 * po lewej nawigacja, w środku lista modułów z przełącznikami, po kliknięciu moduł wysuwa z prawej szufladę z ustawieniami.
 */
public class ModMenuScreen extends KScreen {
    private static final String[] TABS = {"All", "Favorites", "New", "HUD", "PvP", "Utility", "Visual"};
    private static final String[] NAV = {"Modules", "Appearance", "Edit HUD", "About"};
    private static int page = 0;
    private static String tab = "All", query = "";
    private static double scroll = 0;

    private int wx, wy, ww, wh, sb, cx0, cw;
    private int listX, listY, listW, listH;
    private boolean searchFocus = false;
    private Module sel = null;
    private SettingsPanel drawer = null;
    private SettingsPanel general = null;
    private final List<Module> shown = new ArrayList<>();
    private final long openedAt = System.currentTimeMillis();
    private float anim = 0f;

    public ModMenuScreen() { super(Text.literal("Kaczo HUD")); }

    @Override
    protected void init() { general = null; }

    // ───────────── układ ─────────────

    private void layout() {
        ww = Math.min(width - 16, 620);
        wh = Math.min(height - 16, 350);
        wx = (width - ww) / 2;
        wy = (height - wh) / 2;
        sb = ww >= 440 ? 116 : 38;
        cx0 = wx + sb;
        cw = ww - sb;
        listX = cx0 + 8;
        listW = cw - 16;
        listY = wy + (page == 0 ? 60 : 44);
        listH = wy + wh - 8 - listY;
    }

    private boolean expanded() { return sb > 60; }

    private int cols() { return listW >= 340 ? 2 : 1; }
    private static final int RH = 34, GAP = 5;

    private void filter() {
        shown.clear();
        String q = query.toLowerCase();
        for (Module m : Modules.ALL) {
            if (tab.equals("Favorites") && !m.fav()) continue;
            if (tab.equals("New") && !m.isNew) continue;
            if (!tab.equals("All") && !tab.equals("Favorites") && !tab.equals("New") && !m.category.equals(tab)) continue;
            if (!q.isEmpty() && !L.t(m.name).toLowerCase().contains(q) && !m.name.toLowerCase().contains(q)) continue;
            shown.add(m);
        }
    }

    private int rowW() { return (listW - GAP * (cols() - 1)) / cols(); }
    private int rowX(int i) { return listX + (i % cols()) * (rowW() + GAP); }
    private int rowY(int i) { return listY + (i / cols()) * (RH + GAP) - (int) scroll; }
    private int maxScroll() { return Math.max(0, ((shown.size() + cols() - 1) / cols()) * (RH + GAP) - GAP - listH); }

    private int drawerW() { return Math.min(214, cw - 20); }

    // ───────────── rysowanie ─────────────

    private int f(int argb) { return Ui.fade(argb, anim); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        Ui.beginFrame();
        anim = Ui.easeOut((System.currentTimeMillis() - openedAt) / 260f);
        layout();
        TextRenderer tr = client.textRenderer;
        ctx.fill(0, 0, width, height, f(0xB0050810));
        int inset = Math.round((1 - anim) * 10);
        Ui.glass(ctx, wx + inset, wy + inset, ww - 2 * inset, wh - 2 * inset, 10, f(0xE00F1523));
        if (anim < 0.35f) return;                     // zawartość pojawia się, gdy okno jest już prawie na miejscu
        drawSidebar(ctx, tr, mx, my);
        if (page == 0) drawModules(ctx, tr, mx, my);
        else if (page == 1) drawSettingsPage(ctx, tr, mx, my);
        else drawAbout(ctx, tr);
        drawDrawer(ctx, tr, mx, my);
    }

    private void drawSidebar(DrawContext ctx, TextRenderer tr, int mx, int my) {
        Ui.card(ctx, wx + 6, wy + 6, sb - 8, wh - 12, 8, f(0x1AFFFFFF), f(0x1CFFFFFF));
        // marka
        Compat.push(ctx);
        Compat.translate(ctx, wx + 13, wy + 13);
        Compat.scale(ctx, 20f / 64f, 20f / 64f);
        Compat.tex(ctx, Modules.logo(), 64, 64);
        Compat.pop(ctx);
        if (expanded()) {
            ctx.drawText(tr, "KACZO", wx + 38, wy + 13, f(0xFFF4F7FF), true);
            ctx.drawText(tr, "CLIENT", wx + 38, wy + 23, f(0xFFA9B2C6), false);
        }
        int y = wy + 44;
        for (int i = 0; i < NAV.length; i++) {
            int ix = wx + 10, iw = sb - 16;
            boolean hover = Ui.in(mx, my, ix, y, iw, 22);
            boolean on = i == page && i != 2;
            float hv = Ui.tween("nav" + i, hover ? 1f : 0f);
            float sv = Ui.tween("navs" + i, on ? 1f : 0f);
            if (sv > 0.01f) Ui.rrectD(ctx, ix, y, iw, 22, 7, f((int) (0x40 * sv) << 24 | 0xE6ECFF));
            if (hv > 0.01f && !on) Ui.rrectD(ctx, ix, y, iw, 22, 7, f((int) (0x1C * hv) << 24 | 0xFFFFFF));
            if (sv > 0.01f) Ui.rrectD(ctx, ix + 1, y + 6, 2, 10, 1, f((int) (255 * sv) << 24 | 0xE6ECFF));
            int col = on ? 0xFFF4F7FF : (hover ? 0xFFFFFFFF : 0xFFB8C0D4);
            navIcon(ctx, i, ix + 12, y + 11, f(col));
            if (expanded()) ctx.drawText(tr, L.t(NAV[i]), ix + 24, y + 7, f(col), false);
            y += 25;
        }
        if (expanded()) {
            String v = version();
            ctx.drawText(tr, "v" + v, wx + 14, wy + wh - 24, f(0xFF6F7890), false);
            ctx.drawText(tr, L.t("Esc = close"), wx + 14, wy + wh - 15, f(0xFF6F7890), false);
        }
    }

    private static String version() {
        try {
            return FabricLoader.getInstance().getModContainer("kaczohud")
                    .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
        } catch (Throwable t) { return ""; }
    }

    /** Małe ikony narysowane z prostokątów – bez tekstur. */
    private void navIcon(DrawContext ctx, int kind, int cx, int cy, int col) {
        switch (kind) {
            case 0 -> {
                for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) Ui.rrectD(ctx, cx - 5.5 + a * 6, cy - 5.5 + b * 6, 5, 5, 1.6, col);
            }
            case 1 -> {
                for (int i = 0; i < 3; i++) {
                    Ui.rrectD(ctx, cx - 6, cy - 5 + i * 4.2, 12, 1.1, 0.55, col);
                    Ui.rrectD(ctx, cx - 5 + (i * 4) % 9, cy - 6.1 + i * 4.2, 3, 3, 1.5, col);
                }
            }
            case 2 -> {
                Ui.ringT(ctx, cx - 6, cy - 5, 12, 10, 2.5, 1, col);
                Ui.rrectD(ctx, cx - 3.5, cy - 2.5, 4.5, 4, 1, col);
            }
            default -> {
                Ui.ringT(ctx, cx - 5.5, cy - 5.5, 11, 11, 5.5, 1, col);
                Ui.rrectD(ctx, cx - 0.6, cy - 0.5, 1.2, 4, 0.6, col);
                Ui.rrectD(ctx, cx - 0.6, cy - 3.4, 1.2, 1.2, 0.6, col);
            }
        }
    }

    private void header(DrawContext ctx, TextRenderer tr, String title, String sub) {
        Compat.push(ctx);
        Compat.translate(ctx, cx0 + 12, wy + 11);
        Compat.scale(ctx, 1.3f, 1.3f);
        ctx.drawText(tr, title, 0, 0, f(0xFFFFFFFF), true);
        Compat.pop(ctx);
        ctx.drawText(tr, sub, cx0 + 12, wy + 25, f(0xFF8B94AA), false);
    }

    private int searchW() { return Math.min(150, cw / 3); }

    private void drawSearch(DrawContext ctx, TextRenderer tr, int mx, int my) {
        int sw = searchW(), sx = wx + ww - sw - 12, sy = wy + 12;
        boolean hover = Ui.in(mx, my, sx, sy, sw, 16);
        Ui.card(ctx, sx, sy, sw, 16, 8, f(searchFocus ? 0x30FFFFFF : (hover ? 0x26FFFFFF : 0x18FFFFFF)), f(searchFocus ? 0x88E6ECFF : 0x22FFFFFF));
        String shownText = query.isEmpty() && !searchFocus ? L.t("Search") : query;
        int col = query.isEmpty() ? 0xFF7F88A0 : 0xFFFFFFFF;
        String t = tr.trimToWidth(shownText, sw - 18);
        ctx.drawText(tr, t, sx + 9, sy + 4, f(col), false);
        if (searchFocus && (System.currentTimeMillis() / 500) % 2 == 0)
            ctx.fill(sx + 9 + tr.getWidth(t), sy + 3, sx + 10 + tr.getWidth(t), sy + 13, f(0xFFF4F7FF));
    }

    private void drawModules(DrawContext ctx, TextRenderer tr, int mx, int my) {
        filter();
        int on = 0;
        for (Module m : Modules.ALL) if (m.enabled()) on++;
        header(ctx, tr, L.t("Modules"), Modules.ALL.size() + " " + L.t("modules") + "  ·  " + on + " " + L.t("enabled"));
        drawSearch(ctx, tr, mx, my);
        // zakładki
        int x = cx0 + 12, y = wy + 40;
        for (String t : TABS) {
            int w = tr.getWidth(L.t(t)) + 14;
            if (x + w > wx + ww - 10) break;
            boolean sel = t.equals(tab), hover = Ui.in(mx, my, x, y, w, 14);
            float sv = Ui.tween("tab" + t, sel ? 1f : 0f);
            int fill = Ui.mix(hover ? 0x2CFFFFFF : 0x16FFFFFF, 0xFFE6ECFF, sv);
            Ui.rrectD(ctx, x, y, w, 14, 7, f(fill));
            ctx.drawText(tr, L.t(t), x + 7, y + 3, f(sel ? 0xFF1A1400 : (hover ? 0xFFFFFFFF : 0xFFB8C0D4)), false);
            x += w + 4;
        }
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
        ctx.enableScissor(listX - 2, listY - 1, listX + listW + 2, listY + listH + 1);
        boolean drawerOpen = sel != null;
        for (int i = 0; i < shown.size(); i++) {
            int ry = rowY(i);
            if (ry + RH < listY || ry > listY + listH) continue;
            drawRow(ctx, tr, shown.get(i), rowX(i), ry, rowW(), drawerOpen ? -1 : mx, drawerOpen ? -1 : my);
        }
        ctx.disableScissor();
        if (shown.isEmpty()) Ui.centered(ctx, client, L.t("No modules found"), listX + listW / 2, listY + 30, f(0xFF8B94AA));
        int ms = maxScroll();
        if (ms > 0) {
            int bh = Math.max(18, listH * listH / (listH + ms));
            Ui.rrectD(ctx, listX + listW + 3, listY + (listH - bh) * (scroll / ms), 2, bh, 1, f(0x70FFFFFF));
        }
    }

    private static int tint(String cat) {
        switch (cat) {
            case "PvP": return 0xFF5A4D;
            case "Utility": return 0x3DBE6A;
            case "Visual": return 0xB070FF;
            default: return 0x4C8DFF;
        }
    }

    private void drawRow(DrawContext ctx, TextRenderer tr, Module m, int x, int y, int w, int mx, int my) {
        boolean on = m.enabled();
        boolean hover = Ui.in(mx, my, x, y, w, RH);
        float hv = Ui.tween("row" + m.id, hover ? 1f : 0f, 18f);
        float ov = Ui.tween("on" + m.id, on ? 1f : 0f);
        int fill = Ui.mix(0x1CFFFFFF, 0x34FFFFFF, hv);
        fill = Ui.mix(fill, 0x30E6ECFF, ov * 0.55f);
        Ui.card(ctx, x, y, w, RH, 8, f(fill), f(Ui.mix(0x1AFFFFFF, 0x55E6ECFF, ov)));
        // ikona
        int tc = tint(m.category);
        Ui.rrectD(ctx, x + 6, y + 6, 22, 22, 7, f(Ui.mix(0x38000000 | tc, 0xA0000000 | tc, ov)));
        float sc = Math.min(1f, 18f / Math.max(1, tr.getWidth(m.icon)));
        sc = Math.max(0.55f, sc);
        Compat.push(ctx);
        Compat.scale(ctx, sc, sc);
        ctx.drawText(tr, m.icon, Math.round((x + 17) / sc - tr.getWidth(m.icon) / 2f), Math.round((y + 17) / sc - 4), f(0xFFFFFFFF), false);
        Compat.pop(ctx);
        // opisy
        int textW = w - 40 - 58;
        String name = tr.trimToWidth(L.t(m.name), textW - (m.isNew ? 24 : 0));
        ctx.drawText(tr, name, x + 34, y + 7, f(on ? 0xFFFFFFFF : 0xFFD0D6E6), false);
        if (m.isNew) {
            int nx = x + 34 + tr.getWidth(name) + 4;
            Ui.rrectD(ctx, nx, y + 7, 18, 9, 4.5, f(0xFFE6ECFF));
            Compat.push(ctx);
            Compat.scale(ctx, 0.65f, 0.65f);
            ctx.drawText(tr, L.t("NEW"), Math.round((nx + 9 - tr.getWidth(L.t("NEW")) * 0.325f) / 0.65f), Math.round((y + 8.6f) / 0.65f), f(0xFF1A1400), false);
            Compat.pop(ctx);
        }
        ctx.drawText(tr, tr.trimToWidth(L.t(m.desc), textW), x + 34, y + 19, f(0xFF8B94AA), false);
        // serce + przełącznik
        boolean hh = Ui.in(mx, my, x + w - 52, y + 10, 14, 14);
        ctx.drawText(tr, "♥", x + w - 49, y + 13, f(m.fav() ? 0xFFFF5C63 : (hh ? 0xFFFFFFFF : 0xFF3F4760)), false);
        Ui.toggle(ctx, x + w - 34, y + 11, 26, 13, ov, f(0xFF3DBE6A));
    }

    private void drawSettingsPage(DrawContext ctx, TextRenderer tr, int mx, int my) {
        header(ctx, tr, L.t("Appearance"), L.t("Look and behaviour of the whole client"));
        if (general == null) general = new SettingsPanel(Modules.GENERAL);
        general.bounds(cx0 + 8, wy + 42, cw - 16, wh - 50);
        general.render(ctx, sel == null ? mx : -1, sel == null ? my : -1, anim);
    }

    private void drawAbout(DrawContext ctx, TextRenderer tr) {
        header(ctx, tr, L.t("About"), "Kaczo HUD v" + version());
        String[] lines = {
                L.t("Right Shift – open this menu anywhere in the game."),
                L.t("Edit HUD: drag elements, right-click an element to configure it."),
                L.t("Glass buttons and the animated background are drawn only on the title screen."),
                L.t("Music: \"Kaczo Theme\" is an original track made for this client – free to use."),
                L.t("Settings are shared between all your Kaczo Client instances."),
        };
        int y = wy + 50;
        for (String s : lines) {
            for (OrderedText ot : tr.wrapLines(Text.literal(s), cw - 36)) {
                ctx.drawText(tr, ot, cx0 + 14, y, f(0xFFC4CBDD), false);
                y += 11;
            }
            y += 5;
        }
    }

    private void drawDrawer(DrawContext ctx, TextRenderer tr, int mx, int my) {
        float d = Ui.tween("drawer", sel != null ? 1f : 0f, 15f);
        if (d <= 0.01f) { if (sel == null) drawer = null; return; }
        int dw = drawerW();
        int dx = wx + ww - 6 - Math.round(dw * d), dy = wy + 6, dh = wh - 12;
        // przyciemnienie zawartości
        ctx.fill(cx0, wy + 6, wx + ww - 6, wy + wh - 6, f((int) (0x70 * d) << 24 | 0x050810));
        if (sel == null && drawer == null) return;
        Module m = sel != null ? sel : drawer.module();
        Ui.glass(ctx, dx, dy, dw, dh, 9, f(0xF0141B2C));
        // nagłówek
        int tc = tint(m.category);
        Ui.rrectD(ctx, dx + 8, dy + 8, 18, 18, 6, f(0xA0000000 | tc));
        float sc = Math.max(0.5f, Math.min(1f, 14f / Math.max(1, tr.getWidth(m.icon))));
        Compat.push(ctx);
        Compat.scale(ctx, sc, sc);
        ctx.drawText(tr, m.icon, Math.round((dx + 17) / sc - tr.getWidth(m.icon) / 2f), Math.round((dy + 17) / sc - 4), f(0xFFFFFFFF), false);
        Compat.pop(ctx);
        ctx.drawText(tr, tr.trimToWidth(L.t(m.name), dw - 90), dx + 32, dy + 10, f(0xFFFFFFFF), true);
        boolean xh = Ui.in(mx, my, dx + dw - 22, dy + 8, 14, 14);
        Ui.rrectD(ctx, dx + dw - 22, dy + 8, 14, 14, 7, f(xh ? 0x40FFFFFF : 0x1CFFFFFF));
        ctx.drawText(tr, "x", dx + dw - 17, dy + 11, f(0xFFFFFFFF), false);
        Ui.toggle(ctx, dx + dw - 52, dy + 9, 26, 13, Ui.tween("on" + m.id, m.enabled() ? 1f : 0f), f(0xFF3DBE6A));
        int ty = dy + 30;
        int lines = 0;
        for (OrderedText ot : tr.wrapLines(Text.literal(L.t(m.desc)), dw - 20)) {
            if (lines++ >= 2) break;
            ctx.drawText(tr, ot, dx + 10, ty, f(0xFF8B94AA), false);
            ty += 10;
        }
        Ui.rrectD(ctx, dx + 10, ty + 2, dw - 20, 0.6, 0.3, f(0x22FFFFFF));
        // ustawienia
        int by = dy + dh - 26;
        if (drawer == null || drawer.module() != m) drawer = new SettingsPanel(m);
        drawer.bounds(dx + 2, ty + 5, dw - 4, by - ty - 8);
        drawer.render(ctx, mx, my, anim);
        // przyciski
        boolean mv = m.hud || m == Modules.SCOREBOARD;
        int bw = mv ? (dw - 30) / 2 : dw - 20;
        if (mv) button(ctx, tr, dx + 10, by + 3, bw, 16, L.t("Move on screen"), mx, my, 0x30FFFFFF, 0xFFFFFFFF);
        button(ctx, tr, mv ? dx + 20 + bw : dx + 10, by + 3, bw, 16, L.t("Reset"), mx, my, 0x30FF5C63, 0xFFFF8A90);
    }

    private void button(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, String label, int mx, int my, int fill, int fg) {
        boolean hover = Ui.in(mx, my, x, y, w, h);
        Ui.card(ctx, x, y, w, h, h / 2, f(Ui.mix(fill, fill | 0x30000000, hover ? 1f : 0f)), f(0x26FFFFFF));
        int tw = tr.getWidth(label);
        ctx.drawText(tr, label, x + (w - tw) / 2, y + (h - 8) / 2, f(fg), false);
    }

    // ───────────── mysz i klawiatura ─────────────

    @Override
    protected boolean onClick(double mx, double my, int button) {
        layout();
        // szuflada
        if (sel != null) {
            int dw = drawerW(), dx = wx + ww - 6 - dw, dy = wy + 6, dh = wh - 12;
            if (Ui.in(mx, my, dx, dy, dw, dh)) {
                if (Ui.in(mx, my, dx + dw - 22, dy + 8, 14, 14)) { sel = null; return true; }
                if (Ui.in(mx, my, dx + dw - 52, dy + 9, 26, 13)) { sel.setEnabled(!sel.enabled()); return true; }
                int by = dy + dh - 26;
                boolean mv = sel.hud || sel == Modules.SCOREBOARD;
                int bw = mv ? (dw - 30) / 2 : dw - 20;
                if (my >= by + 3 && my < by + 19) {
                    if (mv && Ui.in(mx, my, dx + 10, by + 3, bw, 16)) { client.setScreen(new EditScreen(this, sel)); return true; }
                    if (Ui.in(mx, my, mv ? dx + 20 + bw : dx + 10, by + 3, bw, 16)) {
                        Config.st(sel.id).vals.clear(); Config.st(sel.id).x = -1; Config.st(sel.id).y = -1;
                        if (sel == Modules.SCOREBOARD) Hud.setSb(0, 0);
                        return true;
                    }
                }
                if (drawer != null) drawer.mouseClicked(mx, my, button);
                return true;
            }
            if (Ui.in(mx, my, wx, wy, ww, wh)) { sel = null; return true; }   // kliknięcie obok = zamknij szufladę
        }
        searchFocus = false;
        if (!Ui.in(mx, my, wx, wy, ww, wh)) { close(); return true; }
        // nawigacja
        int y = wy + 44;
        for (int i = 0; i < NAV.length; i++) {
            if (Ui.in(mx, my, wx + 10, y, sb - 16, 22)) {
                if (i == 2) client.setScreen(new EditScreen(this, null));
                else { page = i; sel = null; scroll = 0; }
                return true;
            }
            y += 25;
        }
        if (page == 0) {
            int sw = searchW();
            if (Ui.in(mx, my, wx + ww - sw - 12, wy + 12, sw, 16)) { searchFocus = true; return true; }
            int x = cx0 + 12, ty = wy + 40;
            for (String t : TABS) {
                int w = client.textRenderer.getWidth(L.t(t)) + 14;
                if (x + w > wx + ww - 10) break;
                if (Ui.in(mx, my, x, ty, w, 14)) { tab = t; scroll = 0; return true; }
                x += w + 4;
            }
            if (Ui.in(mx, my, listX, listY, listW, listH)) {
                for (int i = 0; i < shown.size(); i++) {
                    int rx = rowX(i), ry = rowY(i), rw = rowW();
                    if (!Ui.in(mx, my, rx, ry, rw, RH)) continue;
                    Module m = shown.get(i);
                    if (Ui.in(mx, my, rx + rw - 52, ry + 10, 14, 14)) { m.toggleFav(); return true; }
                    if (Ui.in(mx, my, rx + rw - 36, ry + 9, 30, 16)) { m.setEnabled(!m.enabled()); return true; }
                    sel = m; drawer = null;
                    return true;
                }
            }
        } else if (page == 1 && general != null) {
            general.mouseClicked(mx, my, button);
        }
        return true;
    }

    @Override
    protected boolean onDrag(double mx, double my, int button, double dx, double dy) {
        if (drawer != null && sel != null && drawer.mouseDragged(mx, my)) return true;
        return page == 1 && general != null && general.mouseDragged(mx, my);
    }

    @Override
    protected boolean onRelease(double mx, double my, int button) {
        boolean r = false;
        if (drawer != null) r |= drawer.mouseReleased();
        if (general != null) r |= general.mouseReleased();
        return r;
    }

    @Override
    protected boolean onScroll(double mx, double my, double v) {
        if (sel != null && drawer != null) return drawer.scrolled(mx, my, v) || true;
        if (page == 1 && general != null && general.scrolled(mx, my, v)) return true;
        if (page == 0) scroll = Math.max(0, Math.min(maxScroll(), scroll - v * 22));
        return true;
    }

    @Override
    protected boolean onChar(int cp) {
        if (!searchFocus || cp < 32 || cp == 127) return false;
        if (query.length() < 24) { query += new String(Character.toChars(cp)); scroll = 0; }
        return true;
    }

    @Override
    protected boolean onKey(int key, int scan, int mods) {
        if (searchFocus) {
            if (key == 259) { if (!query.isEmpty()) query = query.substring(0, query.length() - 1); scroll = 0; return true; }   // Backspace
            if (key == 257 || key == 335) { searchFocus = false; return true; }                                                     // Enter
            if (key == 256) { searchFocus = false; return true; }                                                                    // Esc
            return true;
        }
        if (key == 256 && sel != null) { sel = null; return true; }
        if (key == KaczoHudClient.openKey.getDefaultKey().getCode()) { close(); return true; }
        return false;
    }

    @Override
    public void close() {
        Config.save();
        client.setScreen(null);
    }
}
