package pl.kaczo.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

/**
 * Menu moda jako panel 3D stojący w świecie (jak okno w goglach VR): nie obraca się do gracza i nie podąża za nim.
 * Patrzysz na niego celownikiem, LPM/PPM klikają, a przytrzymanie klawisza G przesuwa panel razem ze spojrzeniem.
 */
final class SpatialMenu {
    private SpatialMenu() { }

    static final int PW = 560, PH = 340;
    private static ModMenuScreen menu;
    private static double yaw0, pitch0, gdy, gdp;
    private static boolean grabbing, down0, down1;
    private static double lastX = -1, lastY = -1;

    static boolean isOpen() { return menu != null; }

    static boolean enabled() { return Modules.GENERAL.bool("spatial"); }

    static void toggle(MinecraftClient mc) {
        if (menu != null) { close(); return; }
        if (mc.player == null) return;
        menu = new ModMenuScreen();
        menu.embedded = true;
        menu.embed(mc, PW, PH);
        yaw0 = mc.player.getYaw();
        pitch0 = Math.max(-35, Math.min(35, mc.player.getPitch()));
        down0 = down1 = grabbing = false;
    }

    static void close() { menu = null; Config.save(); }

    private static double wrap(double d) { d %= 360; if (d > 180) d -= 360; if (d < -180) d += 360; return d; }

    private static double focal(MinecraftClient mc) {
        double f = 70;
        try { f = mc.options.getFov().getValue(); } catch (Throwable ignored) { }
        return (mc.getWindow().getScaledHeight() / 2.0) / Math.tan(Math.toRadians(Math.max(30, Math.min(110, f))) / 2);
    }

    /** Geometria panelu na ekranie: {środek x, środek y, skala x, skala y, k} albo null, gdy poza polem widzenia. */
    private static double[] geom(MinecraftClient mc) {
        double dyaw = wrap(yaw0 - mc.player.getYaw()), dpit = pitch0 - mc.player.getPitch();
        if (Math.abs(dyaw) > 75 || Math.abs(dpit) > 70) return null;
        double f = focal(mc);
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        double cx = w / 2.0 + Math.tan(Math.toRadians(dyaw)) * f, cy = h / 2.0 + Math.tan(Math.toRadians(dpit)) * f;
        double k = Math.min(0.9 * w / PW, 0.82 * h / PH);
        k = Math.min(k, 1.0);
        return new double[]{cx, cy, Math.max(0.2, Math.cos(Math.toRadians(dyaw))), Math.max(0.2, Math.cos(Math.toRadians(dpit))), k};
    }

    /** Punkt, na który patrzy celownik, we współrzędnych panelu; null gdy obok panelu. */
    private static double[] aim(MinecraftClient mc, double[] g) {
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        double lx = PW / 2.0 + (w / 2.0 - g[0]) / (g[4] * g[2]), ly = PH / 2.0 + (h / 2.0 - g[1]) / (g[4] * g[3]);
        return (lx < 0 || ly < 0 || lx > PW || ly > PH) ? null : new double[]{lx, ly};
    }

    /** Wywoływane na początku każdego ticku: obsługa klików, przeciągania i chwytania. */
    static void tick(MinecraftClient mc) {
        if (menu == null) return;
        if (mc.player == null || mc.currentScreen != null) { close(); return; }
        long win = mc.getWindow().getHandle();
        // chwyt: panel podąża za spojrzeniem, dopóki trzymasz G
        boolean g = KaczoHudClient.grabKey != null && KaczoHudClient.grabKey.isPressed();
        double py = mc.player.getYaw(), pp = mc.player.getPitch();
        if (g && !grabbing) { gdy = wrap(yaw0 - py); gdp = pitch0 - pp; }
        grabbing = g;
        if (g) { yaw0 = py + gdy; pitch0 = Math.max(-80, Math.min(80, pp + gdp)); }
        double[] geo = geom(mc);
        double[] a = geo == null ? null : aim(mc, geo);
        boolean d0 = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean d1 = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (!g && a != null) {
            if (d0 && !down0) { menu.spatialClick(a[0], a[1], 0); lastX = a[0]; lastY = a[1]; }
            if (d1 && !down1) { menu.spatialClick(a[0], a[1], 1); }
            if (d0 && down0) { menu.spatialDrag(a[0], a[1], a[0] - lastX, a[1] - lastY); lastX = a[0]; lastY = a[1]; }
        }
        if (!d0 && down0) menu.spatialRelease(a == null ? -1 : a[0], a == null ? -1 : a[1]);
        down0 = d0; down1 = d1;
        // klik w panel nie może jednocześnie bić/stawiać bloków
        if (a != null || g) {
            try {
                while (mc.options.attackKey.wasPressed()) { }
                while (mc.options.useKey.wasPressed()) { }
                mc.options.attackKey.setPressed(false);
                mc.options.useKey.setPressed(false);
            } catch (Throwable ignored) { }
        }
    }

    /** Rysowanie panelu w świecie (z perspektywą: zwęża się, gdy patrzysz pod kątem). */
    static void render(DrawContext ctx, MinecraftClient mc) {
        if (menu == null || mc.player == null) return;
        double[] g = geom(mc);
        if (g == null) return;
        double[] a = aim(mc, g);
        boolean pi = Glass.inHud, pf = Glass.forceUi;
        Glass.inHud = false; Glass.forceUi = true;
        Compat.push(ctx);
        Compat.translate(ctx, (float) g[0], (float) g[1]);
        Compat.scale(ctx, (float) (g[4] * g[2]), (float) (g[4] * g[3]));
        Compat.translate(ctx, -PW / 2f, -PH / 2f);
        try {
            int mx = a == null ? -1000 : (int) Math.round(a[0]), my = a == null ? -1000 : (int) Math.round(a[1]);
            menu.render(ctx, mx, my, 0f);
            if (a != null) Ui.rrectD(ctx, mx - 2, my - 2, 4, 4, 2, grabbing ? 0xFF6BE08A : 0xCCFFFFFF);   // „palec” – punkt, w który celujesz
        } finally { Compat.pop(ctx); Glass.inHud = pi; Glass.forceUi = pf; }
        if (grabbing) {
            String t = L.t("Moving panel – release G to place it");
            ctx.drawText(mc.textRenderer, t, (mc.getWindow().getScaledWidth() - mc.textRenderer.getWidth(t)) / 2, 8, 0xFFFFFFFF, true);
        }
    }
}
