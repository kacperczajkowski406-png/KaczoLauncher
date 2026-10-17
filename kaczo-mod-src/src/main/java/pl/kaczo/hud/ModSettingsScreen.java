package pl.kaczo.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Ustawienia jednego modułu: przełączniki, suwaki, kolory, listy opcji. */
public class ModSettingsScreen extends Screen {
    private final Screen parent;
    private final Module m;
    private int px, py, pw, ph;
    private Setting dragging = null;
    private int ROW = 22;

    public ModSettingsScreen(Screen parent, Module m) {
        super(Text.literal(m.name));
        this.parent = parent;
        this.m = m;
    }

    @Override
    protected void init() {
        pw = Math.min(280, width - 20);
        int n = Math.max(1, m.settings.size());
        ROW = Math.max(17, Math.min(22, (height - 20 - 70) / n));   // przy dużej skali GUI zagęszczamy wiersze
        ph = Math.min(height - 20, 70 + n * ROW);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    private int rowY(int i) { return py + 34 + i * ROW; }

    private String valueText(Setting s) {
        switch (s.type) {
            case BOOL: return L.t(m.bool(s.key) ? "ON" : "OFF");
            case INT: return String.valueOf(m.num(s.key));
            case COLOR: return "";
            default: return L.t(s.opts[Math.max(0, Math.min(s.opts.length - 1, m.num(s.key)))]);
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, Ui.DIM);
        Ui.glass(ctx, px, py, pw, ph, 8, Ui.PANEL);
        Ui.rrect(ctx, px + 4, py + 4, pw - 8, 18, 5, 0x30FFFFFF);
        ctx.drawText(textRenderer, L.t(m.name), px + 8, py + 7, Ui.WHITE, false);
        
        if (!m.desc.isEmpty()) ctx.drawText(textRenderer, textRenderer.trimToWidth(L.t(m.desc), pw - 16), px + 8, py + 26, Ui.GRAY, false);

        for (int i = 0; i < m.settings.size(); i++) {
            Setting s = m.settings.get(i);
            int y = rowY(i) + 8;
            ctx.drawText(textRenderer, L.t(s.label), px + 8, y + 4, Ui.WHITE, false);
            int cx = px + pw - 120, cw = 112;
            if (s.type == Setting.Type.INT) {
                Ui.rrect(ctx, cx, y + 6, cw - 30, 4, 2, 0x40FFFFFF);
                double f = (m.num(s.key) - s.min) / (s.max - s.min);
                Ui.rrect(ctx, cx, y + 6, Math.max(2, (int) ((cw - 30) * f)), 4, 2, Ui.ORANGE);
                Ui.rrect(ctx, cx + (int) ((cw - 30) * f) - 3, y + 4, 6, 8, 3, Ui.WHITE);
                ctx.drawText(textRenderer, valueText(s), cx + cw - 26, y + 4, Ui.GRAY, false);
            } else if (s.type == Setting.Type.COLOR) {
                boolean ch = Ui.in(mx, my, cx, y, cw, 16);
                Ui.button(ctx, client, cx, y, cw, 16, "", ch ? Ui.BTN_HOVER : Ui.BTN, Ui.WHITE);
                                Ui.rrect(ctx, cx + 4, y + 3, cw - 8, 10, 3, m.color(s.key));
            } else {
                boolean on = s.type == Setting.Type.BOOL && m.bool(s.key);
                boolean h = Ui.in(mx, my, cx, y, cw, 16);
                Ui.button(ctx, client, cx, y, cw, 16, valueText(s), on ? Ui.GREEN : (h ? Ui.BTN_HOVER : Ui.BTN), on ? Ui.WHITE : Ui.GRAY);
            }
        }

        int by = py + ph - 24;
        Ui.button(ctx, client, px + 8, by, 60, 18, L.t("Back"), Ui.in(mx, my, px + 8, by, 60, 18) ? Ui.BTN_HOVER : Ui.BTN, Ui.WHITE);
        if (m.hud || m == Modules.SCOREBOARD) Ui.button(ctx, client, px + 72, by, 90, 18, L.t("Move on screen"), Ui.in(mx, my, px + 72, by, 90, 18) ? Ui.BTN_HOVER : Ui.BTN, Ui.WHITE);
        Ui.button(ctx, client, px + pw - 68, by, 60, 18, L.t("Reset"), Ui.in(mx, my, px + pw - 68, by, 60, 18) ? Ui.BTN_HOVER : Ui.BTN, Ui.RED);
    }

    private void setFromMouse(Setting s, double mx) {
        int cx = px + pw - 120, w = 112 - 30;
        double f = Math.max(0, Math.min(1, (mx - cx) / w));
        m.set(s.key, String.valueOf((int) Math.round(s.min + f * (s.max - s.min))));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int by = py + ph - 24;
        if (Ui.in(mx, my, px + 8, by, 60, 18)) { close(); return true; }
        if ((m.hud || m == Modules.SCOREBOARD) && Ui.in(mx, my, px + 72, by, 90, 18)) { client.setScreen(new EditScreen(this)); return true; }
        if (Ui.in(mx, my, px + pw - 68, by, 60, 18)) { Config.st(m.id).vals.clear(); Config.st(m.id).x = -1; Config.st(m.id).y = -1; return true; }
        for (int i = 0; i < m.settings.size(); i++) {
            Setting s = m.settings.get(i);
            int y = rowY(i) + 8, cx = px + pw - 120;
            if (!Ui.in(mx, my, cx, y, 112, 16)) continue;
            switch (s.type) {
                case BOOL -> m.set(s.key, String.valueOf(!m.bool(s.key)));
                case INT -> { dragging = s; setFromMouse(s, mx); }
                case COLOR -> client.setScreen(new ColorScreen(this, m, s));
                case ENUM -> {
                    int n = (int) s.max + 1;
                    m.set(s.key, String.valueOf((m.num(s.key) + 1) % n));
                }
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) { setFromMouse(dragging, mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void close() {
        Config.save();
        client.setScreen(parent);
    }
}
