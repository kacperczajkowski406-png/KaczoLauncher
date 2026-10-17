package pl.kaczo.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Wybór koloru: gotowe kolory, suwaki R/G/B/A oraz tryb tęczowy (RGB). */
public class ColorScreen extends Screen {
    private final Screen parent;
    private final Module m;
    private final Setting s;
    private int[] c = {255, 255, 255, 255};
    private boolean rainbow;
    private int drag = -1, px, py, pw, ph;
    private static final String[] NAMES = {"Red", "Green", "Blue", "Alpha"};
    private static final int[] BAR = {0xFFFF4040, 0xFF40FF40, 0xFF4080FF, 0xFFCCCCCC};

    public ColorScreen(Screen parent, Module m, Setting s) {
        super(Text.literal(s.label));
        this.parent = parent; this.m = m; this.s = s;
        String[] p = m.raw(s.key).split(",");
        try {
            if (p.length >= 4) {
                for (int i = 0; i < 4; i++) c[i] = Math.max(0, Math.min(255, Integer.parseInt(p[i].trim())));
                rainbow = p.length > 4 && p[4].trim().equals("1");
            }
        } catch (Exception ignored) { }
    }

    @Override
    protected void init() {
        pw = Math.min(260, width - 20); ph = 168;
        px = (width - pw) / 2; py = Math.max(8, (height - ph) / 2);
    }

    private void save() { m.set(s.key, c[0] + "," + c[1] + "," + c[2] + "," + c[3] + "," + (rainbow ? 1 : 0)); }

    private int barX() { return px + 70; }
    private int barW() { return pw - 70 - 36; }
    private int barY(int i) { return py + 72 + i * 16; }

    private void setBar(int i, double mx) {
        double f = Math.max(0, Math.min(1, (mx - barX()) / barW()));
        c[i] = (int) Math.round(f * 255);
        save();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, Ui.DIM);
        Ui.glass(ctx, px, py, pw, ph, 8, Ui.PANEL);
        Ui.rrect(ctx, px + 4, py + 3, pw - 8, 16, 5, 0x30FFFFFF);
        ctx.drawText(textRenderer, L.t(s.label), px + 8, py + 6, Ui.WHITE, false);

        // gotowe kolory
        ctx.drawText(textRenderer, L.t("Presets"), px + 8, py + 27, Ui.GRAY, false);
        for (int i = 0; i < Setting.PALETTE.length; i++) {
            int x = px + 8 + i * 20, y = py + 38;
            Ui.rrect(ctx, x, y, 16, 16, 4, 0xFF000000 | Setting.PALETTE[i]);
            if (Ui.in(mx, my, x, y, 16, 16)) ctx.drawBorder(x - 1, y - 1, 18, 18, Ui.WHITE);
        }
        // podgląd
        ctx.drawText(textRenderer, L.t("Preview"), px + pw - 52, py + 27, Ui.GRAY, false);
        Ui.rrect(ctx, px + pw - 52, py + 38, 44, 16, 4, 0xFF2A2F3C);
        Module tmp = m;
        int prev = rainbow ? previewRainbow() : ((c[3] << 24) | (c[0] << 16) | (c[1] << 8) | c[2]);
        Ui.rrect(ctx, px + pw - 52, py + 38, 44, 16, 4, prev);

        // suwaki
        for (int i = 0; i < 4; i++) {
            int y = barY(i);
            ctx.drawText(textRenderer, L.t(NAMES[i]), px + 8, y, Ui.WHITE, false);
            Ui.rrect(ctx, barX(), y + 2, barW(), 4, 2, 0x40FFFFFF);
            Ui.rrect(ctx, barX(), y + 2, Math.max(2, (int) (barW() * c[i] / 255.0)), 4, 2, BAR[i]);
            int hx = barX() + (int) (barW() * c[i] / 255.0);
            Ui.rrect(ctx, hx - 3, y, 6, 8, 3, Ui.WHITE);
            ctx.drawText(textRenderer, String.valueOf(c[i]), barX() + barW() + 6, y, Ui.GRAY, false);
        }
        // tęcza
        boolean h = Ui.in(mx, my, px + 8, py + 138, 100, 18);
        Ui.button(ctx, client, px + 8, py + 138, 100, 18, L.t("Rainbow") + ": " + L.t(rainbow ? "ON" : "OFF"),
                rainbow ? Ui.GREEN : (h ? Ui.BTN_HOVER : Ui.BTN), rainbow ? Ui.WHITE : Ui.GRAY);
        boolean hd = Ui.in(mx, my, px + pw - 68, py + 138, 60, 18);
        Ui.button(ctx, client, px + pw - 68, py + 138, 60, 18, L.t("Done"), hd ? Ui.BTN_HOVER : Ui.BTN, Ui.WHITE);
    }

    private int previewRainbow() {
        int col = java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 4000) / 4000f, 0.85f, 1f);
        return (c[3] << 24) | (col & 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < Setting.PALETTE.length; i++) {
            if (Ui.in(mx, my, px + 8 + i * 20, py + 38, 16, 16)) {
                int p = Setting.PALETTE[i];
                c[0] = (p >> 16) & 255; c[1] = (p >> 8) & 255; c[2] = p & 255; rainbow = false; save();
                return true;
            }
        }
        for (int i = 0; i < 4; i++) {
            if (Ui.in(mx, my, barX() - 4, barY(i) - 3, barW() + 8, 13)) { drag = i; setBar(i, mx); return true; }
        }
        if (Ui.in(mx, my, px + 8, py + 138, 100, 18)) { rainbow = !rainbow; save(); return true; }
        if (Ui.in(mx, my, px + pw - 68, py + 138, 60, 18)) { close(); return true; }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (drag >= 0) { setBar(drag, mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        drag = -1;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void close() { client.setScreen(parent); }
}
