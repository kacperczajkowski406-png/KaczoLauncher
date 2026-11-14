package pl.kaczo.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

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
        String text; Vec3 pos; long born; long died; int anim;
        List<String> rows = new ArrayList<>();
    }

    private static final List<Line> lines = new ArrayList<>();
    private static final Random RNG = new Random();
    private static String lastText = "";
    private static int side = 1;

    // interpolacja pozycji kamery między tickami (gładkie kotwiczenie podczas ruchu)
    private static Vec3 prevEye = Vec3.ZERO, curEye = Vec3.ZERO;
    private static int lastAge = -1;
    private static long tickNs = 0;

    static Vec3 eye(Minecraft mc) {
        Vec3 e = mc.player.getEyePosition();
        long now = System.nanoTime();
        if (mc.player.tickCount != lastAge) { lastAge = mc.player.tickCount; prevEye = curEye == Vec3.ZERO ? e : curEye; curEye = e; tickNs = now; }
        double f = Math.min(1.0, (now - tickNs) / 50_000_000.0);
        return new Vec3(prevEye.x + (curEye.x - prevEye.x) * f, prevEye.y + (curEye.y - prevEye.y) * f, prevEye.z + (curEye.z - prevEye.z) * f);
    }

    private static double ease(double t) { t = Math.max(0, Math.min(1, t)); double c = 1.70158; return 1 + (c + 1) * Math.pow(t - 1, 3) + c * Math.pow(t - 1, 2); }

    private static List<String> wrap(Minecraft mc, String s, int max) {
        List<String> rows = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : s.split(" ")) {
            String t = cur.length() == 0 ? w : cur + " " + w;
            if (mc.font.width(t) > max && cur.length() > 0) { rows.add(cur.toString()); cur = new StringBuilder(w); }
            else { cur = new StringBuilder(t); }
        }
        if (cur.length() > 0) rows.add(cur.toString());
        return rows;
    }

    private static void spawn(Minecraft mc, String text, Vec3 eye) {
        long now = System.currentTimeMillis();
        for (Line l : lines) if (l.died == 0) l.died = now;
        if (text.isEmpty()) return;
        Module mm = Modules.MUSIC;
        double spread = Math.max(0, mm.num("wspread"));
        double yaw = Math.toRadians(mc.player.getYRot() + side * (spread * 0.6 + RNG.nextDouble() * spread * 0.4));
        double pitch = Math.toRadians(mc.player.getXRot() + (RNG.nextInt(22) - 8) * Math.min(1.0, spread / 22.0));
        side = -side;
        double d = Math.max(2, mm.num("wdist")) + RNG.nextDouble() * 1.2;
        Vec3 dir = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Line l = new Line();
        l.text = text; l.pos = eye.add(dir.scale(d)); l.born = now;
        l.rows = wrap(mc, text, 190);
        l.anim = mm.num("wanim");
        lines.add(l);
    }

    public static void render(GuiGraphicsExtractor ctx, Minecraft mc) {
        Module m = Modules.MUSIC;
        if (!m.enabled() || !m.bool("level") || mc.player == null || mc.level == null || mc.screen != null) { lines.clear(); lastText = ""; return; }
        Vec3 eye = eye(mc);
        String now = NowPlaying.title.isEmpty() ? "" : Lyrics.current();
        if (!now.equals(lastText)) { lastText = now; spawn(mc, now, eye); }

        int w = ctx.guiWidth(), h = ctx.guiHeight();
        double yaw = Math.toRadians(mc.player.getYRot()), pitch = Math.toRadians(mc.player.getXRot());
        Vec3 f = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3 r = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 u = r.cross(f);
        double tanHalf = Math.tan(Math.toRadians(Math.max(30, mc.options.fov().get())) / 2);
        int accent = m.color("color");
        long ms = System.currentTimeMillis();

        for (int i = lines.size() - 1; i >= 0; i--) {
            Line l = lines.get(i);
            double age = (ms - l.born) / 1000.0;
            double out = l.died == 0 ? 0 : (ms - l.died) / 1000.0;
            if (out > 0.6) { lines.remove(i); continue; }
            double inT = ease(age / 0.45);
            double alpha = Math.min(1, age / 0.25) * (1 - out / 0.6);
            double rise = out * 0.9 + (1 - inT) * (l.anim == 2 ? -1.1 : -0.25);   // wpływa z dołu, odpływa w górę
            Vec3 v = l.pos.add(0, rise, 0).subtract(eye);
            double zc = v.dot(f);
            if (zc < 0.5) continue;
            double xc = v.dot(r), yc = v.dot(u);
            float sx = (float) (w / 2.0 + xc / zc / tanHalf * (h / 2.0));
            float sy = (float) (h / 2.0 - yc / zc / tanHalf * (h / 2.0));
            double pop = l.anim == 3 ? 0.15 + 0.85 * inT : 0.7 + 0.3 * inT;
            float scale = (float) Math.max(0.4, Math.min(4.0, 5.0 / zc * pop * (1 + out * 0.15) * (m.num("wsize") / 100.0)));
            int a = (int) Math.round(255 * alpha);
            if (a < 8) continue;
            int rows = l.rows.size(), lh = 11;
            // pisanie liter w tempie muzyki (tylko dla aktualnej linijki; odchodzące są pełne)
            int total = l.text.length();
            int shown = l.died == 0 && l.anim == 0 ? (int) Math.ceil(Lyrics.lineProgress() * total) : total;
            int left = Math.max(0, Math.min(total, shown));
            Compat.push(ctx);
            Compat.translate(ctx, sx, sy);
            Compat.scale(ctx, scale, scale);
            int textCol = m.color("wtext") & 0xFFFFFF;
            float glowK = m.num("wglow") / 100f;
            if (m.bool("wplate")) {                                      // ciemna „szklana” płytka – tekst czytelny na każdym tle
                int maxw = 0;
                for (String r0 : l.rows) maxw = Math.max(maxw, mc.font.width(r0));
                float y0 = (float) (-rows / 2.0 * lh) - 4;
                Ui.extra = scale;
                try { Ui.rrectD(ctx, -maxw / 2.0 - 7, y0, maxw + 14, rows * lh + 6, 6, ((int) (a * 0.42) << 24)); } finally { Ui.extra = 1f; }
            }
            for (int k = 0; k < rows; k++) {
                String full = l.rows.get(k);
                int tw = mc.font.width(full);                // układ liczony z pełnego tekstu – nic nie skacze
                int take = Math.min(full.length(), left);
                left -= take;
                String s = full.substring(0, take);
                if (l.died == 0 && l.anim == 0 && left == 0 && take < full.length() + 1 && take > 0 && shown < total && (ms / 300) % 2 == 0) s = s + "_";
                if (s.isEmpty()) continue;
                int x = -tw / 2, y = (int) ((k - rows / 2.0) * lh);
                int ga = (int) (Math.max(0, Math.min(255, a / 4 * (0.2 + glowK * 1.6))));
                int glow = (ga << 24) | (accent & 0xFFFFFF);
                if (l.anim == 4) {                                      // fala: każda litera osobno kołysze się w górę i w dół
                    int cx = x;
                    for (int ci = 0; ci < s.length(); ci++) {
                        String chs = String.valueOf(s.charAt(ci));
                        int dy = (int) Math.round(Math.sin(ms / 260.0 + ci * 0.55) * 2.2);
                        if (ga > 4) { ctx.text(mc.font, chs, cx - 1, y + dy, glow, false); ctx.text(mc.font, chs, cx + 1, y + dy, glow, false); }
                        ctx.text(mc.font, chs, cx, y + dy, (a << 24) | textCol, true);
                        cx += mc.font.width(chs);
                    }
                    continue;
                }
                if (ga > 4) {
                    ctx.text(mc.font, s, x - 1, y, glow, false);
                    ctx.text(mc.font, s, x + 1, y, glow, false);
                    ctx.text(mc.font, s, x, y - 1, glow, false);
                    ctx.text(mc.font, s, x, y + 1, glow, false);
                }
                ctx.text(mc.font, s, x, y, (a << 24) | textCol, true);
            }
            Compat.pop(ctx);
        }
    }
}
