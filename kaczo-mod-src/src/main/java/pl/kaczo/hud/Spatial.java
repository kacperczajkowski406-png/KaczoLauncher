package pl.kaczo.hud;

import net.minecraft.client.MinecraftClient;

/**
 * „Spatial HUD” (jak okna w goglach VR): każdy element HUD wisi w świecie jako pływające okno zakotwiczone
 * kierunkiem (yaw/pitch). Rozglądasz się – okna zostają na miejscu; w edytorze HUD możesz je „chwycić” kursorem
 * i przesunąć w przestrzeni. Pozycje na ekranie wynikają z rzutowania kierunku na obraz.
 */
final class Spatial {
    private Spatial() { }

    static boolean on() { return Modules.GENERAL.bool("spatial"); }

    private static double fovRad(MinecraftClient mc) {
        double f = 70;
        try { f = mc.options.getFov().getValue(); } catch (Throwable ignored) { }
        return Math.toRadians(Math.max(30, Math.min(110, f)));
    }

    private static double focal(MinecraftClient mc) { return (mc.getWindow().getScaledHeight() / 2.0) / Math.tan(fovRad(mc) / 2); }

    private static double wrap(double d) { d %= 360; if (d > 180) d -= 360; if (d < -180) d += 360; return d; }

    private static double dbl(Module m, String k, double def) {
        try { return Double.parseDouble(Config.st(m.id).vals.getOrDefault(k, "")); } catch (Exception e) { return def; }
    }

    /** Wczytuje pozę okna; przy pierwszym użyciu wylicza ją z dotychczasowej pozycji na ekranie. */
    private static double[] pose(MinecraftClient mc, Module m) {
        var vals = Config.st(m.id).vals;
        if (!vals.containsKey("syaw")) {
            int px = Config.st(m.id).x, py = Config.st(m.id).y;
            if (px < 0) px = m.defX;
            if (py < 0) py = m.defY;
            int w = Math.round(m.width(mc) * m.scale()), h = Math.round(m.height(mc) * m.scale());
            toPose(mc, m, px + w / 2, py + h / 2);
        }
        return new double[]{dbl(m, "syaw", 0), dbl(m, "spitch", 0)};
    }

    private static void toPose(MinecraftClient mc, Module m, int cx, int cy) {
        if (mc.player == null) return;
        double f = focal(mc);
        double dyaw = Math.toDegrees(Math.atan((cx - mc.getWindow().getScaledWidth() / 2.0) / f));
        double dpit = Math.toDegrees(Math.atan((cy - mc.getWindow().getScaledHeight() / 2.0) / f));
        var vals = Config.st(m.id).vals;
        vals.put("syaw", String.valueOf(wrap(mc.player.getYaw() + dyaw)));
        vals.put("spitch", String.valueOf(Math.max(-85, Math.min(85, mc.player.getPitch() + dpit))));
    }

    /** Kąty okna względem kierunku patrzenia: {dyaw, dpitch} w stopniach. */
    static double[] angles(Module m) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return new double[]{0, 0};
        double[] p = pose(mc, m);
        return new double[]{wrap(p[0] - mc.player.getYaw()), p[1] - mc.player.getPitch()};
    }

    /** Lewy górny róg okna na ekranie (współrzędne GUI); poza polem widzenia: duże ujemne. */
    static int[] topLeft(Module m) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getWindow() == null) return new int[]{0, 0};
        double[] a = angles(m);
        int w = Math.round(m.width(mc) * m.scale()), h = Math.round(m.height(mc) * m.scale());
        if (Math.abs(a[0]) > 80 || Math.abs(a[1]) > 80) return new int[]{-10000, -10000};
        double f = focal(mc);
        double cx = mc.getWindow().getScaledWidth() / 2.0 + Math.tan(Math.toRadians(a[0])) * f;
        double cy = mc.getWindow().getScaledHeight() / 2.0 + Math.tan(Math.toRadians(a[1])) * f;
        return new int[]{(int) Math.round(cx - w / 2.0), (int) Math.round(cy - h / 2.0)};
    }

    /** Przeciąganie „ręką”: nowa pozycja lewego górnego rogu na ekranie → nowa poza w świecie. */
    static void drag(Module m, int x, int y) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = Math.round(m.width(mc) * m.scale()), h = Math.round(m.height(mc) * m.scale());
        toPose(mc, m, x + w / 2, y + h / 2);
    }
}
