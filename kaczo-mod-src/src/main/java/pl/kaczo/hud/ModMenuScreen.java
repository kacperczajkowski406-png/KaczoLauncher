package pl.kaczo.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Główne menu moda (prawy Shift): kafelki modułów, filtry, wyszukiwarka, ulubione. */
public class ModMenuScreen extends Screen {
    private static final String[] TABS = {"All", "New", "HUD", "PvP", "Utility"};
    private static final int CW = 112, CH = 66, GAP = 6;
    private static String tab = "All", query = "";
    private static boolean onlyFav = false;
    private static double scroll = 0;

    private TextFieldWidget search;
    private int px, py, pw, ph;               // panel
    private int gx, gy, gw, gh;               // obszar siatki
    private final List<Module> shown = new ArrayList<>();

    public ModMenuScreen() { super(Text.literal("Kaczo HUD")); }

    @Override
    protected void init() {
        px = 16; py = 40; pw = width - 32; ph = height - 52;
        int sw = Math.min(130, pw / 3);
        search = new TextFieldWidget(textRenderer, px + pw - sw - 40, py + 8, sw, 16, Text.literal(L.t("Search")));
        search.setPlaceholder(Text.literal(L.t("Search")));
        search.setText(query);
        search.setChangedListener(s -> { query = s; scroll = 0; });
        addSelectableChild(search);
    }

    private void filter() {
        shown.clear();
        for (Module m : Modules.ALL) {
            if (onlyFav && !m.fav()) continue;
            if (tab.equals("New") && !m.isNew) continue;
            if (!tab.equals("All") && !tab.equals("New") && !m.category.equals(tab)) continue;
            if (!query.isEmpty() && !L.t(m.name).toLowerCase().contains(query.toLowerCase()) && !m.name.toLowerCase().contains(query.toLowerCase())) continue;
            shown.add(m);
        }
    }

    private int cols() { return Math.max(1, (gw - GAP) / (CW + GAP)); }

    private int[] cardPos(int i) {
        int cols = cols();
        int usedW = cols * (CW + GAP) - GAP;
        int ox = gx + (gw - usedW) / 2;
        return new int[]{ox + (i % cols) * (CW + GAP), gy + GAP + (i / cols) * (CH + GAP) - (int) scroll};
    }

    private int maxScroll() {
        int rows = (shown.size() + cols() - 1) / cols();
        return Math.max(0, rows * (CH + GAP) + GAP - gh);
    }

    private final long openedAt = System.currentTimeMillis();

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float p = Math.min(1f, (System.currentTimeMillis() - openedAt) / 240f);
        p = 1 - (1 - p) * (1 - p) * (1 - p);
        Compat.push(ctx);
        Compat.translate(ctx, width / 2f, height / 2f + (1 - p) * 10);
        Compat.scale(ctx, 0.94f + 0.06f * p, 0.94f + 0.06f * p);
        Compat.translate(ctx, -width / 2f, -height / 2f);
        renderMenu(ctx, mx, my, delta, p);
        Compat.pop(ctx);
    }

    private void renderMenu(DrawContext ctx, int mx, int my, float delta, float anim) {
        ctx.fill(-width, -height, width * 2, height * 2, (Math.round(((Ui.DIM >>> 24) & 255) * anim) << 24) | (Ui.DIM & 0xFFFFFF));
        gx = px + 4; gy = py + 34; gw = pw - 8; gh = ph - 38;
        filter();
        scroll = Math.max(0, Math.min(maxScroll(), scroll));

        // pasek górny
        Ui.button(ctx, client, 16, 10, 90, 24, L.t("MOD MENU"), Ui.ORANGE, 0xFF1A1400);
        Ui.button(ctx, client, 110, 10, 70, 24, L.t("SETTINGS"), Ui.in(mx, my, 110, 10, 70, 24) ? Ui.BTN_HOVER : Ui.BTN, Ui.WHITE);
        Ui.button(ctx, client, 184, 10, 80, 24, L.t("EDIT HUD"), Ui.in(mx, my, 184, 10, 80, 24) ? Ui.BTN_HOVER : Ui.BTN, Ui.WHITE);
        Ui.button(ctx, client, width - 40, 10, 24, 24, "X", Ui.in(mx, my, width - 40, 10, 24, 24) ? 0x99FF5C63 : Ui.BTN, Ui.WHITE);

        // panel
        Ui.glass(ctx, px, py, pw, ph, 8, Ui.PANEL);
        int x = px + 8;
        for (String t : TABS) {
            int w = textRenderer.getWidth(L.t(t)) + 20;
            Ui.button(ctx, client, x, py + 8, w, 16, L.t(t), t.equals(tab) ? Ui.ORANGE : Ui.BTN, t.equals(tab) ? 0xFF1A1400 : Ui.GRAY);
            x += w + 4;
        }
        search.render(ctx, mx, my, delta);
        int hx = px + pw - 28;
        Ui.button(ctx, client, hx, py + 8, 20, 16, "♥", onlyFav ? Ui.BTN_HOVER : Ui.BTN, onlyFav ? Ui.RED : Ui.GRAY);

        // siatka (przycinana do obszaru)
        ctx.enableScissor(gx, gy, gx + gw, gy + gh);
        for (int i = 0; i < shown.size(); i++) {
            int[] p = cardPos(i);
            if (p[1] + CH < gy || p[1] > gy + gh) continue;
            drawCard(ctx, shown.get(i), p[0], p[1], mx, my);
        }
        ctx.disableScissor();

        if (shown.isEmpty()) Ui.centered(ctx, client, L.t("No modules found"), gx + gw / 2, gy + 30, Ui.GRAY);

        // pasek przewijania
        int ms = maxScroll();
        if (ms > 0) {
            int bh = Math.max(20, gh * gh / (gh + ms));
            int by = gy + (int) ((gh - bh) * (scroll / ms));
            Ui.rrect(ctx, gx + gw - 3, by, 3, bh, 1, 0x80FFFFFF);
        }
        Ui.centered(ctx, client, L.t("Right Shift = open/close  |  Esc = close"), width / 2, height - 10, Ui.GRAY);
    }

    private void drawCard(DrawContext ctx, Module m, int x, int y, int mx, int my) {
        boolean on = m.enabled();
        Ui.glass(ctx, x, y, CW, CH, 6, Ui.in(mx, my, x, y, CW, CH) ? 0x66FFFFFF : (on ? Ui.CARD : Ui.CARD_OFF));
        if (on) Ui.rrect(ctx, x + 10, y + 1, CW - 20, 2, 1, Ui.ORANGE);
        ctx.drawText(textRenderer, textRenderer.trimToWidth(L.t(m.name), CW - 22), x + 6, y + 5, on ? Ui.WHITE : Ui.GRAY, false);
        // serduszko
        boolean hh = Ui.in(mx, my, x + CW - 16, y + 3, 12, 12);
        ctx.drawText(textRenderer, "♥", x + CW - 14, y + 5, m.fav() ? Ui.RED : (hh ? Ui.WHITE : 0xFF3A4252), false);
        // „ikona” – duży skrót modułu
        float s = m.icon.length() > 4 ? 1.2f : 2f;
        Compat.push(ctx);
        Compat.scale(ctx, s, s);
        int iw = textRenderer.getWidth(m.icon);
        ctx.drawText(textRenderer, m.icon, Math.round((x + CW / 2f) / s - iw / 2f), Math.round((y + 25) / s), on ? Ui.YELLOW : 0xFF59627A, false);
        Compat.pop(ctx);
        // dół: ustawienia + włącz/wyłącz
        int by = y + CH - 20;
        boolean gh2 = Ui.in(mx, my, x + 6, by, 16, 14);
        Ui.button(ctx, client, x + 6, by, 16, 14, "⚙", gh2 ? Ui.BTN_HOVER : Ui.BTN, Ui.WHITE);
        boolean th = Ui.in(mx, my, x + 26, by, CW - 32, 14);
        Ui.button(ctx, client, x + 26, by, CW - 32, 14, L.t(on ? "Enabled" : "Disabled"),
                on ? (th ? Ui.GREEN_HOVER : Ui.GREEN) : (th ? Ui.BTN_HOVER : Ui.BTN), on ? Ui.WHITE : Ui.GRAY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (search.mouseClicked(mx, my, button)) { setFocused(search); return true; }
        setFocused(null);
        if (Ui.in(mx, my, width - 40, 10, 24, 24)) { close(); return true; }
        if (Ui.in(mx, my, 110, 10, 70, 24)) { client.setScreen(new ModSettingsScreen(this, Modules.GENERAL)); return true; }
        if (Ui.in(mx, my, 184, 10, 80, 24)) { client.setScreen(new EditScreen(this)); return true; }
        if (Ui.in(mx, my, px + pw - 28, py + 8, 20, 16)) { onlyFav = !onlyFav; scroll = 0; return true; }
        int x = px + 8;
        for (String t : TABS) {
            int w = textRenderer.getWidth(L.t(t)) + 20;
            if (Ui.in(mx, my, x, py + 8, w, 16)) { tab = t; scroll = 0; return true; }
            x += w + 4;
        }
        if (Ui.in(mx, my, gx, gy, gw, gh)) {
            for (int i = 0; i < shown.size(); i++) {
                int[] p = cardPos(i);
                Module m = shown.get(i);
                if (Ui.in(mx, my, p[0] + CW - 16, p[1] + 3, 12, 12)) { m.toggleFav(); return true; }
                int by = p[1] + CH - 20;
                if (Ui.in(mx, my, p[0] + 6, by, 16, 14)) { client.setScreen(new ModSettingsScreen(this, m)); return true; }
                if (Ui.in(mx, my, p[0] + 26, by, CW - 32, 14)) { m.setEnabled(!m.enabled()); return true; }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - v * 20));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == KaczoHudClient.openKey.getDefaultKey().getCode() && !search.isFocused()) { close(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void close() {
        Config.save();
        client.setScreen(null);
    }
}
