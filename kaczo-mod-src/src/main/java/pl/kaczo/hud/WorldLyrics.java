package pl.kaczo.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tekst piosenki „w świecie”: każda nowa linijka pojawia się w miejscu, w które akurat patrzysz,
 * i zostaje tam zakotwiczona (nie przesuwa się z kamerą). Gdy przychodzi następna linijka, poprzednia
 * odpływa w górę i znika. Liczone w pełni na ekranie (rzutowanie 3D→2D), więc działa na każdej wersji.
 */
public class WorldLyrics {
    private static final class Line {
        String text; Vec3d pos; long born; long died;
        List<String> rows = new ArrayList<>();
    }

    private static final List<Line> lines = new ArrayList<>();
    private static final Random RNG = new Random();
    private static String lastText = "";
    private static int side = 1;

    // interpolacja pozycji kamery między tickami (gładkie kotwiczenie podczas ruchu)
    private static Vec3d prevEye = Vec3d.ZERO, curEye = Vec3d.ZERO;
    private static int lastAge = -1;
    private static long tickNs = 0;

    private static Vec3d eye(MinecraftClient mc) {
        Vec3d e = mc.player.getEyePos();
        long now = System.nanoTime();
        if (mc.player.age != lastAge) { lastAge = mc.player.age; prevEye = curEye == Vec3d.ZERO ? e : curEye; curEye = e; tickNs = now; }
        double f = Math.min(1.0, (now - tickNs) / 50_000_000.0);
        return new Vec3d(prevEye.x + (curEye.x - prevEye.x) * f, prevEye.y + (curEye.y - prevEye.y) * f, prevEye.z + (curEye.z - prevEye.z) * f);
    }

    private static double ease(double t) { t = Math.max(0, Math.min(1, t)); double c = 1.70158; return 1 + (c + 1) * Math.pow(t - 1, 3) + c * Math.pow(t - 1, 2); }

    private static List<String> wrap(MinecraftClient mc, String s, int max) {
        List<String> rows = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : s.split(" ")) {
            String t = cur.length() == 0 ? w : cur + " " + w;
            if (mc.textRenderer.getWidth(t) > max && cur.length() > 0) { rows.add(cur.toString()); cur = new StringBuilder(w); }
            else { cur = new StringBuilder(t); }
        }
        if (cur.length() > 0) rows.add(cur.toString());
        return rows;
    }

    private static void spawn(MinecraftClient mc, String text, Vec3d eye) {
        long now = System.currentTimeMillis();
        for (Line l : lines) if (l.died == 0) l.died = now;
        if (text.isEmpty()) return;
        double yaw = Math.toRadians(mc.player.getYaw() + side * (14 + RNG.nextInt(18)));
        double pitch = Math.toRadians(mc.player.getPitch() + (RNG.nextInt(22) - 8));
        side = -side;
        double d = 5.5 + RNG.nextDouble() * 2.0;
        Vec3d dir = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Line l = new Line();
        l.text = text; l.pos = eye.add(dir.multiply(d)); l.born = now;
        l.rows = wrap(mc, text, 190);
        lines.add(l);
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        Module m = Modules.MUSIC;
        if (!m.enabled() || !m.bool("world") || mc.player == null || mc.world == null || mc.currentScreen != null) { lines.clear(); lastText = ""; return; }
        Vec3d eye = eye(mc);
        String now = NowPlaying.title.isEmpty() ? "" : Lyrics.current();
        if (!now.equals(lastText)) { lastText = now; spawn(mc, now, eye); }

        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        double yaw = Math.toRadians(mc.player.getYaw()), pitch = Math.toRadians(mc.player.getPitch());
        Vec3d f = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3d r = new Vec3d(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3d u = r.crossProduct(f);
        double tanHalf = Math.tan(Math.toRadians(Math.max(30, mc.options.getFov().getValue())) / 2);
        int accent = m.color("color");
        long ms = System.currentTimeMillis();

        for (int i = lines.size() - 1; i >= 0; i--) {
            Line l = lines.get(i);
            double age = (ms - l.born) / 1000.0;
            double out = l.died == 0 ? 0 : (ms - l.died) / 1000.0;
            if (out > 0.6) { lines.remove(i); continue; }
            double inT = ease(age / 0.45);
            double alpha = Math.min(1, age / 0.25) * (1 - out / 0.6);
            double rise = out * 0.9 + (1 - inT) * -0.25;               // wpływa z dołu, odpływa w górę
            Vec3d v = l.pos.add(0, rise, 0).subtract(eye);
            double zc = v.dotProduct(f);
            if (zc < 0.5) continue;
            double xc = v.dotProduct(r), yc = v.dotProduct(u);
            float sx = (float) (w / 2.0 + xc / zc / tanHalf * (h / 2.0));
            float sy = (float) (h / 2.0 - yc / zc / tanHalf * (h / 2.0));
            float scale = (float) Math.max(0.55, Math.min(2.6, 5.0 / zc * (0.7 + 0.3 * inT) * (1 + out * 0.15)));
            int a = (int) Math.round(255 * alpha);
            if (a < 8) continue;
            int rows = l.rows.size(), lh = 11;
            Compat.push(ctx);
            Compat.translate(ctx, sx, sy);
            Compat.scale(ctx, scale, scale);
            for (int k = 0; k < rows; k++) {
                String s = l.rows.get(k);
                int tw = mc.textRenderer.getWidth(s);
                int x = -tw / 2, y = (int) ((k - rows / 2.0) * lh);
                int glow = (Math.max(8, a / 4) << 24) | (accent & 0xFFFFFF);
                ctx.drawText(mc.textRenderer, s, x - 1, y, glow, false);
                ctx.drawText(mc.textRenderer, s, x + 1, y, glow, false);
                ctx.drawText(mc.textRenderer, s, x, y - 1, glow, false);
                ctx.drawText(mc.textRenderer, s, x, y + 1, glow, false);
                ctx.drawText(mc.textRenderer, s, x, y, (a << 24) | 0xFFFFFF, true);
            }
            Compat.pop(ctx);
        }
    }
}
