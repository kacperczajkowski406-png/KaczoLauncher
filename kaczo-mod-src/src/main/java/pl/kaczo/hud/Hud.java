package pl.kaczo.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Rysowanie elementów HUD oraz własnego celownika. */
public class Hud {
    /** Ostatni DrawContext używany do rysowania HUD (potrzebny mixinowi scoreboardu). */
    public static volatile DrawContext lastContext;

    private static int sv(String key) {
        try { return Integer.parseInt(Config.st("scoreboard").vals.getOrDefault(key, "0")); } catch (Exception e) { return 0; }
    }
    /** Przesunięcie scoreboardu (ustawiane w edytorze HUD). */
    public static int sbDx() { return sv("dx"); }
    public static int sbDy() { return sv("dy"); }
    public static void setSb(int dx, int dy) {
        Config.st("scoreboard").vals.put("dx", String.valueOf(dx));
        Config.st("scoreboard").vals.put("dy", String.valueOf(dy));
    }

    public static void drawModule(DrawContext ctx, MinecraftClient mc, Module m, int x, int y) {
        float s = m.scale();
        Compat.push(ctx);
        Compat.scale(ctx, s, s);
        int px = Math.round(x / s), py = Math.round(y / s);
        if (m.text != null) {
            String t = m.text.apply(mc);
            int w = mc.textRenderer.getWidth(t);
            int pad = m.has("pad") ? m.num("pad") : 2;
            if (m.bg() != 0) Ui.box(ctx, px - pad - 1, py - pad - 1, w + 2 * pad + 2, 9 + 2 * pad + 2, m.bg());
            if (m.has("border") && m.bool("border")) Ui.rrect(ctx, px - pad - 1, py - pad - 1, 2, 9 + 2 * pad + 2, 1, m.color("color"));
            if (m.has("shadow") && !m.bool("shadow")) ctx.drawText(mc.textRenderer, t, px, py, m.color("color"), false);
            else ctx.drawTextWithShadow(mc.textRenderer, t, px, py, m.color("color"));
        } else if (m.drawer != null) {
            m.drawer.draw(ctx, mc, px, py);
        }
        Compat.pop(ctx);
    }

    public static void render(DrawContext ctx) {
        lastContext = ctx;
        Compat.alpha(1f); // na wszelki wypadek
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        boolean anim = Modules.GENERAL.bool("hudanim");
        long since = Modules.sinceJoin();
        int idx = 0;
        for (Module m : Modules.ALL) {
            if (!m.hud || !m.enabled()) continue;
            float k = 1f;
            if (anim && since < 1800) k = ease((since - idx * 70L) / 450f);
            idx++;
            if (k <= 0f) continue;
            if (k < 1f) {
                Compat.flush(ctx);
                Compat.alpha(k);
                drawModule(ctx, mc, m, m.x() - Math.round((1 - k) * 14), m.y());
                Compat.flush(ctx);
                Compat.alpha(1f);
            } else drawModule(ctx, mc, m, m.x(), m.y());
        }
        welcome(ctx, mc, since);
        if (Modules.CROSSHAIR.enabled()) crosshair(ctx);
        damageFlash(ctx);
    }

    private static float ease(float t) { t = Math.max(0, Math.min(1, t)); float u = 1 - t; return 1 - u * u * u; }

    /** Szklana plakietka „Kaczo Client” zjeżdżająca z góry po wejściu do świata. */
    private static void welcome(DrawContext ctx, MinecraftClient mc, long since) {
        if (!Modules.GENERAL.bool("intro") || since > 4200) return;
        float in = ease(since / 500f), out = since > 3500 ? 1 - ease((since - 3500) / 600f) : 1f;
        float k = Math.min(in, out);
        if (k <= 0) return;
        String a = "KACZO CLIENT", b = L.t("Right Shift = menu");
        boolean hint = Modules.GENERAL.bool("hint");
        int w = 16 + Math.max(mc.textRenderer.getWidth(a), hint ? mc.textRenderer.getWidth(b) : 0) + 22, h = hint ? 26 : 20;
        int x = (ctx.getScaledWindowWidth() - w) / 2, y = 6 - Math.round((1 - k) * (h + 8));
        Ui.glass(ctx, x, y, w, h, Math.min(8, h / 2), 0xB0121826);
        Compat.push(ctx);
        Compat.translate(ctx, x + 6, y + (h - 14) / 2f);
        Compat.scale(ctx, 14 / 64f, 14 / 64f);
        Compat.tex(ctx, Modules.logo(), 64, 64);
        Compat.pop(ctx);
        ctx.drawTextWithShadow(mc.textRenderer, a, x + 24, y + (hint ? 5 : 6), 0xFFFFE14A);
        if (hint) ctx.drawText(mc.textRenderer, b, x + 24, y + 15, 0xFFA9B2C6, false);
    }

    private static void damageFlash(DrawContext ctx) {
        Module m = Modules.byId("hitflash");
        float f = Modules.flash();
        if (!m.enabled() || f <= 0) return;
        int base = m.color("color"), w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        int a = (int) (f * m.num("strength") / 100f * 150) & 255;
        for (int i = 0; i < 6; i++) {            // pasma coraz słabsze ku środkowi
            int c = ((a * (6 - i) / 6) << 24) | (base & 0xFFFFFF);
            ctx.fill(0, i * 4, w, i * 4 + 4, c); ctx.fill(0, h - i * 4 - 4, w, h - i * 4, c);
            ctx.fill(i * 4, 0, i * 4 + 4, h, c); ctx.fill(w - i * 4 - 4, 0, w - i * 4, h, c);
        }
    }

    /** Czy celownik jest teraz na graczu (albo na dowolnym moba, zależnie od ustawienia). */
    private static boolean onTarget(Module m) {
        var t = MinecraftClient.getInstance().crosshairTarget;
        if (!(t instanceof net.minecraft.util.hit.EntityHitResult ehr)) return false;
        var e = ehr.getEntity();
        return m.num("react") == 0 ? e instanceof net.minecraft.entity.player.PlayerEntity : e instanceof net.minecraft.entity.LivingEntity;
    }

    private static void crosshair(DrawContext ctx) {
        Module m = Modules.CROSSHAIR;
        int cx = ctx.getScaledWindowWidth() / 2, cy = ctx.getScaledWindowHeight() / 2;
        int col = m.color("color"), size = m.num("size"), gap = m.num("gap");
        if (m.bool("indicator") && onTarget(m)) col = m.color("tcolor");
        switch (m.num("style")) {
            case 0 -> ctx.fill(cx - 1, cy - 1, cx + 1, cy + 1, col);
            case 1 -> {
                int t = Math.max(1, m.num("thick")), o = t / 2;
                ctx.fill(cx - gap - size, cy - o, cx - gap, cy - o + t, col);
                ctx.fill(cx + gap + 1, cy - o, cx + gap + 1 + size, cy - o + t, col);
                ctx.fill(cx - o, cy - gap - size, cx - o + t, cy - gap, col);
                ctx.fill(cx - o, cy + gap + 1, cx - o + t, cy + gap + 1 + size, col);
            }
            case 2 -> {
                for (int a = 0; a < 360; a += 15) {
                    int x = cx + (int) Math.round(Math.cos(Math.toRadians(a)) * size);
                    int y = cy + (int) Math.round(Math.sin(Math.toRadians(a)) * size);
                    ctx.fill(x, y, x + 1, y + 1, col);
                }
            }
            default -> ctx.drawBorder(cx - size, cy - size, size * 2 + 1, size * 2 + 1, col);
        }
    }
}
