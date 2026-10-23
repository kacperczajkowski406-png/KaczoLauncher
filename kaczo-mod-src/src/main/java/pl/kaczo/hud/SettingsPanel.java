package pl.kaczo.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.HashSet;
import java.util.Set;

/**
 * Lista ustawień jednego modułu w nowoczesnym układzie (etykieta nad kontrolką): przełączniki „iOS”, suwaki,
 * przyciski segmentowe i rozwijany edytor koloru. Używana w szufladzie menu oraz w oknie po prawym kliknięciu w edytorze HUD.
 */
public class SettingsPanel {
    private static final int PAD = 8, COLOR_EXTRA = 94;

    private final Module m;
    private final Set<String> open = new HashSet<>();
    public int x, y, w, h;
    private double scroll = 0;
    private Setting dragS = null;
    private int dragBar = -1;
    private float alpha = 1f;

    public SettingsPanel(Module m) { this.m = m; }

    public Module module() { return m; }

    public void bounds(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }

    // ───────────── układ ─────────────

    private boolean segmented(Setting s) {
        if (s.opts == null) return false;
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int segW = (w - 2 * PAD) / s.opts.length;
        for (String o : s.opts) if (tr.getWidth(L.t(o)) + 8 > segW) return false;
        return true;
    }

    private int rowH(Setting s) {
        switch (s.type) {
            case HEADER: return 20;
            case BOOL: return 20;
            case INT: return 28;
            case ENUM: return segmented(s) ? 32 : 20;
            default: return 20 + (open.contains(s.key) ? COLOR_EXTRA : 0);
        }
    }

    private int contentH() {
        int t = 4;
        for (Setting s : m.settings) t += rowH(s);
        return t + 4;
    }

    private double maxScroll() { return Math.max(0, contentH() - h); }

    private int rowTop(int idx) {
        int t = y + 4 - (int) scroll;
        for (int i = 0; i < idx; i++) t += rowH(m.settings.get(i));
        return t;
    }

    private int f(int argb) { return Ui.fade(argb, alpha); }

    // ───────────── rysowanie ─────────────

    public void render(DrawContext ctx, int mx, int my, float a) {
        alpha = a;
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
        ctx.enableScissor(x, y, x + w, y + h);
        int ry = y + 4 - (int) scroll;
        boolean inside = Ui.in(mx, my, x, y, w, h);
        for (Setting s : m.settings) {
            int rh = rowH(s);
            if (ry + rh >= y && ry <= y + h) drawRow(ctx, mc, tr, s, ry, rh, mx, my, inside);
            ry += rh;
        }
        ctx.disableScissor();
        double ms = maxScroll();
        if (ms > 0) {
            int bh = Math.max(16, (int) (h * (double) h / (h + ms)));
            int by = y + (int) ((h - bh) * (scroll / ms));
            Ui.rrectD(ctx, x + w - 3, by, 2, bh, 1, f(0x70FFFFFF));
        }
    }

    private void drawRow(DrawContext ctx, MinecraftClient mc, TextRenderer tr, Setting s, int ry, int rh, int mx, int my, boolean inside) {
        String key = m.id + "." + s.key;
        boolean hover = inside && Ui.in(mx, my, x, ry, w, rh) && s.type != Setting.Type.HEADER;
        float hv = Ui.tween("h." + key, hover ? 1f : 0f, 18f);
        if (hv > 0.01f && s.type != Setting.Type.HEADER)
            Ui.rrectD(ctx, x + 3, ry + 1, w - 6, Math.min(rh, s.type == Setting.Type.COLOR ? 20 : rh) - 2, 4, f((int) (0x16 * hv) << 24 | 0xFFFFFF));
        String label = L.t(s.label);
        switch (s.type) {
            case HEADER: {
                ctx.drawText(tr, label.toUpperCase(), x + PAD, ry + 9, f(0xFFE6ECFF), false);
                Ui.rrectD(ctx, x + PAD, ry + 18, w - 2 * PAD, 0.6, 0.3, f(0x22FFFFFF));
                break;
            }
            case BOOL: {
                ctx.drawText(tr, tr.trimToWidth(label, w - 2 * PAD - 30), x + PAD, ry + 6, f(0xFFFFFFFF), false);
                float t = Ui.tween("t." + key, m.bool(s.key) ? 1f : 0f);
                Ui.toggle(ctx, x + w - PAD - 24, ry + 4, 24, 12, t, f(0xFF3DBE6A));
                break;
            }
            case INT: {
                String val = m.num(s.key) + s.unit;
                ctx.drawText(tr, tr.trimToWidth(label, w - 2 * PAD - tr.getWidth(val) - 6), x + PAD, ry + 3, f(0xFFFFFFFF), false);
                ctx.drawText(tr, val, x + w - PAD - tr.getWidth(val), ry + 3, f(0xFFF4F7FF), false);
                float fr = (float) ((m.num(s.key) - s.min) / (s.max - s.min));
                Ui.slider(ctx, x + PAD, ry + 16, w - 2 * PAD, Ui.clamp01(Ui.tween("s." + key, fr, 30f)), f(0xFFE6ECFF), dragS == s);
                break;
            }
            case ENUM: {
                int cur = Math.max(0, Math.min(s.opts.length - 1, m.num(s.key)));
                if (segmented(s)) {
                    ctx.drawText(tr, label, x + PAD, ry + 3, f(0xFFFFFFFF), false);
                    int bx = x + PAD, bw = w - 2 * PAD, by = ry + 14;
                    Ui.rrectD(ctx, bx, by, bw, 14, 7, f(0x30FFFFFF));
                    double sw = bw / (double) s.opts.length;
                    double px = Ui.tween("e." + key, cur, 16f);
                    Ui.rrectD(ctx, bx + 1 + px * sw, by + 1, sw - 2, 12, 6, f(0xFFE6ECFF));
                    for (int i = 0; i < s.opts.length; i++) {
                        String o = L.t(s.opts[i]);
                        int tw = tr.getWidth(o);
                        ctx.drawText(tr, o, (int) (bx + sw * i + (sw - tw) / 2), by + 3, f(i == cur ? 0xFF1A1400 : 0xFFD6DCEA), false);
                    }
                } else {
                    String o = L.t(s.opts[cur]);
                    int rw = Math.min(w / 2, tr.getWidth(o) + 16);
                    ctx.drawText(tr, tr.trimToWidth(label, w - 2 * PAD - rw - 6), x + PAD, ry + 6, f(0xFFFFFFFF), false);
                    Ui.rrectD(ctx, x + w - PAD - rw, ry + 3, rw, 14, 7, f(0x38FFFFFF));
                    ctx.drawText(tr, tr.trimToWidth(o, rw - 8), x + w - PAD - rw + 8, ry + 6, f(0xFFF4F7FF), false);
                }
                break;
            }
            default: {
                ctx.drawText(tr, tr.trimToWidth(label, w - 2 * PAD - 40), x + PAD, ry + 6, f(0xFFFFFFFF), false);
                int col = m.color(s.key);
                Ui.rrectD(ctx, x + w - PAD - 32, ry + 3, 32, 14, 7, f(0xFF2A2F3C));
                Ui.rrectD(ctx, x + w - PAD - 31, ry + 4, 30, 12, 6, f(col | 0xFF000000));
                Ui.ringT(ctx, x + w - PAD - 32, ry + 3, 32, 14, 7, 0.5, f(0x66FFFFFF));
                if (open.contains(s.key)) drawColorEditor(ctx, tr, s, ry + 22, mx, my);
                break;
            }
        }
    }

    // ───────────── edytor koloru ─────────────

    private int[] rgba(Setting s) {
        int[] c = {255, 255, 255, 255, 0};
        String[] p = m.raw(s.key).split(",");
        try {
            if (p.length >= 4) {
                for (int i = 0; i < 4; i++) c[i] = Math.max(0, Math.min(255, Integer.parseInt(p[i].trim())));
                c[4] = p.length > 4 && p[4].trim().equals("1") ? 1 : 0;
            } else {
                int pc = Setting.PALETTE[Math.max(0, Math.min(Setting.PALETTE.length - 1, Integer.parseInt(m.raw(s.key).trim())))];
                c[0] = (pc >> 16) & 255; c[1] = (pc >> 8) & 255; c[2] = pc & 255;
            }
        } catch (Exception ignored) { }
        return c;
    }

    private void saveColor(Setting s, int[] c) { m.set(s.key, c[0] + "," + c[1] + "," + c[2] + "," + c[3] + "," + c[4]); }

    private static final String[] CH = {"R", "G", "B", "A"};
    private static final int[] BAR = {0xFFFF5050, 0xFF50E070, 0xFF5090FF, 0xFFD0D0D0};

    private int barX() { return x + PAD + 14; }
    private int barW() { return w - 2 * PAD - 14 - 24; }

    private void drawColorEditor(DrawContext ctx, TextRenderer tr, Setting s, int top, int mx, int my) {
        int[] c = rgba(s);
        for (int i = 0; i < Setting.PALETTE.length; i++) {
            int sx = x + PAD + i * 18;
            Ui.rrectD(ctx, sx, top, 14, 14, 4, f(0xFF000000 | Setting.PALETTE[i]));
            if (Ui.in(mx, my, sx, top, 14, 14)) Ui.ringT(ctx, sx - 1, top - 1, 16, 16, 5, 0.7, f(0xFFFFFFFF));
        }
        for (int i = 0; i < 4; i++) {
            int by = top + 20 + i * 13;
            ctx.drawText(tr, CH[i], x + PAD, by, f(0xFFA9B2C6), false);
            Ui.rrectD(ctx, barX(), by + 3, barW(), 3, 1.5, f(0x40FFFFFF));
            int fw = Math.max(2, (int) (barW() * c[i] / 255.0));
            Ui.rrectD(ctx, barX(), by + 3, fw, 3, 1.5, f(BAR[i]));
            Ui.rrectD(ctx, barX() + fw - 3.5, by + 1.5, 7, 7, 3.5, f(0xFFFFFFFF));
            ctx.drawText(tr, String.valueOf(c[i]), barX() + barW() + 5, by, f(0xFFA9B2C6), false);
        }
        int ry = top + 20 + 4 * 13 + 2;
        ctx.drawText(tr, L.t("Rainbow"), x + PAD, ry + 2, f(0xFFFFFFFF), false);
        Ui.toggle(ctx, x + w - PAD - 24, ry, 24, 12, Ui.tween("t." + m.id + "." + s.key + ".rb", c[4] == 1 ? 1f : 0f), f(0xFF3DBE6A));
    }

    // ───────────── mysz ─────────────

    private void setNum(Setting s, double mx) {
        int bx = x + PAD, bw = w - 2 * PAD;
        double fr = Math.max(0, Math.min(1, (mx - bx) / bw));
        m.set(s.key, String.valueOf((int) Math.round(s.min + fr * (s.max - s.min))));
    }

    private void setBar(Setting s, int i, double mx) {
        int[] c = rgba(s);
        c[i] = (int) Math.round(Math.max(0, Math.min(1, (mx - barX()) / barW())) * 255);
        saveColor(s, c);
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!Ui.in(mx, my, x, y, w, h)) return false;
        for (int i = 0; i < m.settings.size(); i++) {
            Setting s = m.settings.get(i);
            int ry = rowTop(i), rh = rowH(s);
            if (my < ry || my >= ry + rh) continue;
            switch (s.type) {
                case BOOL:
                    m.set(s.key, String.valueOf(!m.bool(s.key)));
                    return true;
                case INT:
                    if (my >= ry + 10) { dragS = s; setNum(s, mx); }
                    return true;
                case ENUM: {
                    int n = s.opts.length, cur = m.num(s.key);
                    if (segmented(s)) {
                        if (my >= ry + 12) {
                            int idx = (int) ((mx - (x + PAD)) / ((w - 2 * PAD) / (double) n));
                            m.set(s.key, String.valueOf(Math.max(0, Math.min(n - 1, idx))));
                        }
                    } else {
                        m.set(s.key, String.valueOf(button == 1 ? (cur + n - 1) % n : (cur + 1) % n));
                    }
                    return true;
                }
                case COLOR: {
                    if (my < ry + 20) {
                        if (open.contains(s.key)) open.remove(s.key); else open.add(s.key);
                        return true;
                    }
                    int top = ry + 22;
                    int[] c = rgba(s);
                    if (my >= top && my < top + 14) {
                        int idx = (int) ((mx - (x + PAD)) / 18);
                        if (idx >= 0 && idx < Setting.PALETTE.length && mx - (x + PAD) - idx * 18 < 14) {
                            int p = Setting.PALETTE[idx];
                            c[0] = (p >> 16) & 255; c[1] = (p >> 8) & 255; c[2] = p & 255; c[4] = 0;
                            saveColor(s, c);
                        }
                        return true;
                    }
                    for (int b = 0; b < 4; b++) {
                        int by = top + 20 + b * 13;
                        if (my >= by - 1 && my < by + 11) { dragS = s; dragBar = b; setBar(s, b, mx); return true; }
                    }
                    int rb = top + 20 + 4 * 13 + 2;
                    if (my >= rb - 2 && my < rb + 14) { c[4] = c[4] == 1 ? 0 : 1; saveColor(s, c); return true; }
                    return true;
                }
                default:
                    return true;
            }
        }
        return true;
    }

    public boolean mouseDragged(double mx, double my) {
        if (dragS == null) return false;
        if (dragS.type == Setting.Type.INT) setNum(dragS, mx);
        else if (dragBar >= 0) setBar(dragS, dragBar, mx);
        return true;
    }

    public boolean mouseReleased() {
        boolean was = dragS != null;
        dragS = null; dragBar = -1;
        return was;
    }

    public boolean scrolled(double mx, double my, double v) {
        if (!Ui.in(mx, my, x, y, w, h)) return false;
        scroll = Math.max(0, Math.min(maxScroll(), scroll - v * 18));
        return true;
    }
}
