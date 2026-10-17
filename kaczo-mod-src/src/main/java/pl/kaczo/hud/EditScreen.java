package pl.kaczo.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Edytor HUD: przeciągaj włączone elementy myszką. */
public class EditScreen extends Screen {
    private final Screen parent;
    private Module dragging;
    private boolean dragSb = false;
    private int sbStartX, sbStartY, sbMouseX, sbMouseY;
    private int offX, offY;

    public EditScreen(Screen parent) {
        super(Text.literal("Edit HUD"));
        this.parent = parent;
    }

    private int w(Module m) { return Math.round(m.width(client) * m.scale()); }
    private int h(Module m) { return Math.round(m.height(client) * m.scale()); }

    private boolean hit(Module m, double mx, double my) {
        return Ui.in(mx, my, m.x() - 2, m.y() - 2, w(m) + 4, h(m) + 4);
    }

    private int[] sbRect() {
        Module sb = Modules.SCOREBOARD;
        float sc = sb.num("scale") / 100f;
        int w = Math.round(110 * sc), h = Math.round(80 * sc);
        return new int[]{width - w + Hud.sbDx(), height / 2 - h / 2 + Hud.sbDy(), w, h};
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (Modules.SCOREBOARD.enabled()) {
            int[] r = sbRect();
            if (Ui.in(mx, my, r[0], r[1], r[2], r[3])) {
                dragSb = true; sbStartX = Hud.sbDx(); sbStartY = Hud.sbDy(); sbMouseX = (int) mx; sbMouseY = (int) my;
                return true;
            }
        }
        for (int i = Modules.ALL.size() - 1; i >= 0; i--) {
            Module m = Modules.ALL.get(i);
            if (m.hud && m.enabled() && hit(m, mx, my)) {
                dragging = m; offX = (int) mx - m.x(); offY = (int) my - m.y();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragSb) {
            Hud.setSb(sbStartX + (int) mx - sbMouseX, sbStartY + (int) my - sbMouseY);
            return true;
        }
        if (dragging != null) {
            dragging.setPos(Math.max(0, Math.min(width - 8, (int) mx - offX)), Math.max(0, Math.min(height - 8, (int) my - offY)));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null; dragSb = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x80000000);
        for (Module m : Modules.ALL) {
            if (!m.hud || !m.enabled()) continue;
            Hud.drawModule(ctx, client, m, m.x(), m.y());
            ctx.drawBorder(m.x() - 2, m.y() - 2, w(m) + 4, h(m) + 4, m == dragging ? Ui.ORANGE : Ui.YELLOW);
        }
        if (Modules.SCOREBOARD.enabled()) {
            int[] r = sbRect();
            ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], 0x70000000);
            ctx.drawBorder(r[0], r[1], r[2], r[3], dragSb ? Ui.ORANGE : Ui.YELLOW);
            Ui.centered(ctx, client, L.t("Scoreboard"), r[0] + r[2] / 2, r[1] + r[3] / 2 - 4, Ui.WHITE);
        }
        Ui.centered(ctx, client, L.t("Drag the elements with the mouse. Esc = back."), width / 2, height - 14, Ui.WHITE);
    }

    @Override
    public void close() {
        Config.save();
        client.setScreen(parent);
    }
}
