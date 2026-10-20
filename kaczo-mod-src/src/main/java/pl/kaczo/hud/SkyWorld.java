package pl.kaczo.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Niebo Kaczo w świecie gry: wszystko ma pozycję 3D (kopuła nieba + cząsteczki wokół gracza),
 * jest rzutowane z perspektywą, zasłaniane przez teren i zostaje w miejscu, gdy się ruszasz/obracasz.
 */
final class SkyWorld {
    private SkyWorld() { }

    // ── kamera ──
    private static double ex, ey, ez, tanHalf;
    private static Vec3d F, R, U;
    private static int W, H;
    private static MinecraftClient MC;

    private static void cam(MinecraftClient mc, DrawContext ctx) {
        MC = mc;
        Vec3d e = WorldLyrics.eye(mc);
        ex = e.x; ey = e.y; ez = e.z;
        W = ctx.getScaledWindowWidth(); H = ctx.getScaledWindowHeight();
        double yaw = Math.toRadians(mc.player.getYaw()), pitch = Math.toRadians(mc.player.getPitch());
        F = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        R = new Vec3d(-Math.cos(yaw), 0, -Math.sin(yaw));
        U = R.crossProduct(F);
        tanHalf = Math.tan(Math.toRadians(Math.max(30, mc.options.getFov().getValue())) / 2);
    }

    /** Rzutuje punkt świata na ekran: o = {x, y, głębokość}. */
    private static boolean proj(double x, double y, double z, float[] o) {
        double vx = x - ex, vy = y - ey, vz = z - ez;
        double zc = vx * F.x + vy * F.y + vz * F.z;
        if (zc < 0.25) return false;
        double xc = vx * R.x + vy * R.y + vz * R.z, yc = vx * U.x + vy * U.y + vz * U.z;
        o[0] = (float) (W / 2.0 + xc / zc / tanHalf * (H / 2.0));
        o[1] = (float) (H / 2.0 - yc / zc / tanHalf * (H / 2.0));
        o[2] = (float) zc;
        return true;
    }

    private static final double DOME = 90;
    /** Punkt na kopule nieba: azymut/elewacja w radianach. */
    private static boolean dome(double az, double el, float[] o) {
        double ce = Math.cos(el);
        return proj(ex + Math.sin(az) * ce * DOME, ey + Math.sin(el) * DOME, ez + Math.cos(az) * ce * DOME, o);
    }

    private static float pxPerRad() { return (float) ((H / 2.0) / tanHalf); }

    // ── zasłanianie terenu (raycast z cache, budżet na klatkę) ──
    private static final boolean[] blockedC = new boolean[1024];
    private static final long[] blockedT = new long[1024];
    private static int budget;

    private static boolean blocked(int key, double tx, double ty, double tz) {
        key &= 1023;
        long now = System.currentTimeMillis();
        if (now - blockedT[key] > 260 && budget > 0) {
            budget--;
            blockedT[key] = now;
            try {
                HitResult hr = MC.world.raycast(new RaycastContext(new Vec3d(ex, ey, ez), new Vec3d(tx, ty, tz),
                        RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, MC.player));
                blockedC[key] = hr.getType() != HitResult.Type.MISS;
            } catch (Throwable t) { blockedC[key] = false; }
        }
        return blockedC[key];
    }

    private static boolean domeBlocked(double az, double el) {
        int key = 512 + ((int) Math.floor(Math.toDegrees(az) / 12.0) & 31) * 8 + (int) Math.max(0, Math.min(7, Math.toDegrees(el) / 10.0));
        double ce = Math.cos(el);
        return blocked(key, ex + Math.sin(az) * ce * 60, ey + Math.sin(el) * 60, ez + Math.cos(az) * ce * 60);
    }

    // ── pomocnicze rysowanie ──
    private static float hash(int n) {
        n = (n << 13) ^ n;
        return ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 2147483647f;
    }
    private static int argb(int a, int rgb) { return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF); }
    private static int lerp(int c1, int c2, float t) {
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static void disc(DrawContext c, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            if (half > 0) c.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
        }
    }
    static void glow(DrawContext c, int cx, int cy, int r, int rgb, int a) {
        r = Math.min(r, 90);
        disc(c, cx, cy, r, argb(a / 7, rgb));
        disc(c, cx, cy, Math.max(1, r * 2 / 3), argb(a / 4, rgb));
        disc(c, cx, cy, Math.max(1, r / 3), argb(a / 2, lerp(rgb, 0xFFFFFF, 0.5f)));
        if (r >= 3) disc(c, cx, cy, Math.max(1, r / 6), argb(a, 0xFFFFFF));
    }
    private static void line(DrawContext c, float x0, float y0, float x1, float y1, int th, int color) {
        float dx = x1 - x0, dy = y1 - y0;
        int n = Math.max(1, (int) (Math.max(Math.abs(dx), Math.abs(dy)) / Math.max(1, th)));
        n = Math.min(n, 220);
        for (int i = 0; i <= n; i++) {
            int x = Math.round(x0 + dx * i / n), y = Math.round(y0 + dy * i / n);
            c.fill(x - th / 2, y - th / 2, x - th / 2 + th, y - th / 2 + th, color);
        }
    }

    // ── cząsteczki w świecie ──
    private static final class Pt { double x, y, z; float vx, vy, vz, ph, sz; int kind, col, id; }
    private static final List<Pt> pts = new ArrayList<>();
    private static final Random RNG = new Random();
    private static int pstyle = -9, pcount = -1, pglow = -1;
    private static long lastNs;

    private static final int DUCK_K = 0, GLOW_K = 1, RAIN_K = 2, GLYPH_K = 3, PETAL_K = 4;

    private static Pt spawn(int kind, boolean fresh) {
        Pt p = new Pt();
        p.kind = kind; p.id = RNG.nextInt(100000); p.ph = RNG.nextFloat() * 6.28f; p.sz = 0.7f + RNG.nextFloat() * 0.6f;
        double ang = RNG.nextDouble() * Math.PI * 2;
        double rad = (kind == RAIN_K ? 1 : 3) + RNG.nextDouble() * (kind == RAIN_K ? 13 : (kind == GLOW_K ? 14 : 22));
        p.x = ex + Math.cos(ang) * rad; p.z = ez + Math.sin(ang) * rad;
        switch (kind) {
            case DUCK_K -> { p.y = ey + (fresh ? 14 * RNG.nextDouble() - 3 : 12 + 4 * RNG.nextDouble()); p.vy = -(0.9f + RNG.nextFloat() * 1.1f); p.vx = (RNG.nextFloat() - .5f) * .6f; }
            case GLOW_K -> { p.y = ey - 2 + RNG.nextDouble() * 7; p.vy = 0.1f + RNG.nextFloat() * 0.25f; p.vx = (RNG.nextFloat() - .5f) * .5f; p.vz = (RNG.nextFloat() - .5f) * .5f; }
            case RAIN_K -> { p.y = ey + (fresh ? 16 * RNG.nextDouble() - 4 : 14 + 4 * RNG.nextDouble()); p.vy = -(15f + RNG.nextFloat() * 6f); p.vx = -2.2f; }
            case GLYPH_K -> { p.y = ey + (fresh ? 14 * RNG.nextDouble() - 3 : 12 + 4 * RNG.nextDouble()); p.vy = -(2.5f + RNG.nextFloat() * 3f); }
            default -> { p.y = ey + (fresh ? 14 * RNG.nextDouble() - 3 : 12 + 4 * RNG.nextDouble()); p.vy = -(0.7f + RNG.nextFloat() * 0.8f); p.vx = (RNG.nextFloat() - .5f) * .8f; p.col = new int[]{0xFF8AD8, 0x8AD8FF, 0xFFF08A, 0xB88AFF}[RNG.nextInt(4)]; }
        }
        return p;
    }

    private static void rebuild(int style, boolean ducksAny, boolean glowOn, int count, int density) {
        pts.clear();
        if (style == 0 || ducksAny) for (int i = 0; i < Math.max(1, Math.min(60, count)); i++) pts.add(spawn(DUCK_K, true));
        if (style == 7) for (int i = 0; i < Math.max(30, Math.min(220, density * 3)); i++) pts.add(spawn(RAIN_K, true));
        if (style == 5) for (int i = 0; i < Math.max(20, Math.min(160, density * 2)); i++) pts.add(spawn(GLYPH_K, true));
        if (style == 4) for (int i = 0; i < Math.max(20, Math.min(120, density * 2)); i++) pts.add(spawn(PETAL_K, true));
        if (glowOn) for (int i = 0; i < Math.max(4, Math.min(70, density)); i++) pts.add(spawn(GLOW_K, true));
    }

    private static final String[] DUCK = {"..yyy....", ".yywyy...", ".yyeyyoo.", ".yyyyoo..", "..yyy....", ".yyyyyyy.", "yyyyyyyyy", ".yyyyyyy."};
    private static final String GL = "01ｱｲｳｴｵｶｷｸ";

    private static final int[] ACCENT = {0xFFE27A, 0x6CFFD2, 0xA9B4FF, 0xFFB070, 0xFF9AD8, 0x5CFF8A, 0xFFB0F0, 0xD9A8FF};

    private static final class Draw { Pt p; float x, y, z; boolean hid; }
    private static final List<Draw> draws = new ArrayList<>();

    private static void particles(DrawContext ctx, MinecraftClient mc, float t, float dt, int style, int strength, int size, int density) {
        float k = pxPerRad();
        draws.clear();
        float[] o = new float[3];
        int acc = ACCENT[Math.max(0, Math.min(7, style))];
        for (Pt p : pts) {
            p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt;
            if (p.kind == DUCK_K || p.kind == PETAL_K) p.x += Math.sin(t * 0.8 + p.ph) * 0.6 * dt;
            double dx = p.x - ex, dz = p.z - ez;
            boolean out = dx * dx + dz * dz > 26 * 26 || p.y < ey - 4 - (p.kind == GLOW_K ? 1 : 0) || p.y > ey + 22;
            if (out) { Pt n = spawn(p.kind, false); p.x = n.x; p.y = n.y; p.z = n.z; p.vx = n.vx; p.vy = n.vy; p.vz = n.vz; p.id = n.id; continue; }
            if (!proj(p.x, p.y, p.z, o)) continue;
            if (o[0] < -60 || o[0] > W + 60 || o[1] < -60 || o[1] > H + 60) continue;
            boolean hid;
            if (p.kind == RAIN_K || p.kind == GLYPH_K) {
                int top = mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(p.x), (int) Math.floor(p.z));
                hid = p.y < top - 0.5;
            } else {
                hid = blocked(p.id, p.x, p.y, p.z);
            }
            if (hid) continue;
            Draw d = new Draw(); d.p = p; d.x = o[0]; d.y = o[1]; d.z = o[2];
            draws.add(d);
        }
        draws.sort(Comparator.comparingDouble((Draw d) -> -d.z));
        for (Draw d : draws) {
            Pt p = d.p;
            float z = d.z;
            float fade = Math.max(0, Math.min(1, (28 - z) / 12f)) * Math.min(1, z / 1.5f);
            int a = Math.round(255 * strength / 100f * fade);
            if (a < 6) continue;
            float pxBlock = k / z;                                   // ile pikseli ma blok w tej odległości
            switch (p.kind) {
                case DUCK_K -> {
                    float s = (0.5f + 0.22f * size) * p.sz / 9f * pxBlock;
                    if (s * 8 < 1.5f) { ctx.fill((int) d.x, (int) d.y, (int) d.x + 2, (int) d.y + 2, argb(a, 0xFFD92E)); break; }
                    boolean flip = ((p.id >> 3) & 1) == 0;
                    Compat.push(ctx);
                    Compat.translate(ctx, d.x - 4.5f * s, d.y - 4f * s);
                    Compat.scale(ctx, s, s);
                    for (int ry = 0; ry < 8; ry++) for (int rx = 0; rx < 9; rx++) {
                        char c = DUCK[ry].charAt(flip ? 8 - rx : rx);
                        if (c == '.') continue;
                        int col = switch (c) { case 'o' -> 0xFF8F1F; case 'e' -> 0x1A1A1A; case 'w' -> 0xFFF6A8; default -> 0xFFD92E; };
                        ctx.fill(rx, ry, rx + 1, ry + 1, argb(a, col));
                    }
                    Compat.pop(ctx);
                }
                case GLOW_K -> {
                    float tw = 0.55f + 0.45f * (float) Math.sin(t * (1.3 + p.sz) + p.ph);
                    int r = Math.max(2, Math.min(40, Math.round(0.12f * p.sz * pxBlock * (0.6f + 0.4f * size))));
                    glow(ctx, (int) d.x, (int) d.y, r, (p.id % 3 == 0) ? lerp(acc, 0xFFFFFF, 0.55f) : acc, Math.round(a * tw));
                }
                case RAIN_K -> {
                    float len = 0.55f * pxBlock;
                    line(ctx, d.x, d.y, d.x + len * 0.12f, d.y + len, 1, argb(Math.round(a * 0.5f), 0xC8D4FF));
                }
                case GLYPH_K -> {
                    float s = 0.45f * pxBlock / 8f;
                    if (s < 0.25f) break;
                    String ch = String.valueOf(GL.charAt((p.id + (int) (t * 6)) % GL.length()));
                    Compat.push(ctx);
                    Compat.translate(ctx, d.x, d.y);
                    Compat.scale(ctx, s, s);
                    ctx.drawText(mc.textRenderer, ch, 0, 0, argb(a, 0x35FF70), false);
                    Compat.pop(ctx);
                }
                default -> {
                    float s = 0.18f * p.sz * pxBlock;
                    int sz = Math.max(2, Math.round(s));
                    ctx.fill((int) d.x, (int) d.y, (int) d.x + sz, (int) d.y + sz, argb(a, p.col));
                }
            }
        }
    }

    // ── efekty na kopule nieba ──
    private static void aurora(DrawContext ctx, float t, int strength, float[] o) {
        int a = Math.round(strength / 100f * 120);
        double az0 = Math.toRadians(mcYaw());
        for (int i = -34; i <= 34; i++) {
            double az = az0 + Math.toRadians(i * 3.0);
            double wave = Math.sin(az * 3 + t * 0.6) * 0.5 + Math.sin(az * 1.3 - t * 0.4) * 0.5;
            double elB = Math.toRadians(16 + 6 * wave), elT = Math.toRadians(46 + 14 * Math.sin(az * 2 + t * 0.3));
            if (domeBlocked(az, elB)) continue;
            float[] b0 = new float[3], t0 = new float[3], b1 = new float[3];
            if (!dome(az, elB, b0) || !dome(az, elT, t0) || !dome(az + Math.toRadians(3.2), elB, b1)) continue;
            int col = lerp(0x35FFA0, 0xB45CFF, (float) (0.5 + 0.5 * Math.sin(az * 2 + t * 0.3)));
            int x0 = Math.round(b0[0]), x1 = Math.round(b1[0]) + 1, yb = Math.round(b0[1]), yt = Math.round(t0[1]);
            int bands = 7;
            for (int j = 0; j < bands; j++) {
                float f0 = j / (float) bands, f1 = (j + 1) / (float) bands;
                int ya = Math.round(yb + (yt - yb) * f0), yb2 = Math.round(yb + (yt - yb) * f1);
                float al = (float) Math.sin(Math.PI * (f0 + f1) / 2);
                ctx.fill(x0, Math.min(ya, yb2), x1, Math.max(ya, yb2) + 1, argb(Math.round(a * al * 0.55f), col));
            }
        }
    }

    private static float mcYawVal;
    private static double mcYaw() { return mcYawVal + 180; }

    private static void stars(DrawContext ctx, float t, int strength, int density, float[] o) {
        int n = Math.max(40, Math.min(400, density * 6));
        for (int i = 0; i < n; i++) {
            double az = hash(i * 3 + 1) * Math.PI * 2, el = Math.toRadians(4 + hash(i * 7 + 2) * 80);
            if (!dome(az, el, o)) continue;
            if (o[0] < -4 || o[0] > W + 4 || o[1] < -4 || o[1] > H + 4) continue;
            if (domeBlocked(az, el)) continue;
            float tw = 0.5f + 0.5f * (float) Math.sin(t * (1 + 2 * hash(i)) + i);
            int a = Math.round(strength / 100f * 255 * (0.35f + 0.65f * tw));
            int sz = hash(i * 11) > 0.93f ? 2 : 1;
            ctx.fill((int) o[0], (int) o[1], (int) o[0] + sz, (int) o[1] + sz, argb(a, 0xFFFFFF));
            if (hash(i * 11) > 0.97f) glow(ctx, (int) o[0], (int) o[1], 5, 0xBFD0FF, a);
        }
        // spadająca gwiazda
        int slot = (int) (t * 0.5f);
        float local = t * 0.5f - slot;
        if (hash(slot * 5 + 1) > 0.35f && local < 0.5f) {
            float kf = local / 0.5f;
            double az = hash(slot * 13 + 2) * Math.PI * 2, el = Math.toRadians(35 + hash(slot * 17 + 3) * 35);
            for (int i = 0; i < 14; i++) {
                float f = kf - i * 0.02f;
                if (f < 0) break;
                if (dome(az + f * 0.7, el - f * 0.5, o)) glow(ctx, (int) o[0], (int) o[1], i == 0 ? 3 : 2, 0xFFFFFF, Math.round(strength / 100f * 220 * (1 - i / 14f)));
            }
        }
    }

    private static void cloud(DrawContext ctx, double az, double el, double angW, int a, int dark, int light, int under, float[] o) {
        if (!dome(az, el, o)) return;
        if (domeBlocked(az, el)) return;
        float w = (float) (angW * pxPerRad()), h = w * 0.22f;
        if (w < 6) return;
        int cx = Math.round(o[0]), cy = Math.round(o[1]);
        for (int l = 0; l < 7; l++) {
            float f = 1 - l / 7f * 0.55f;
            int hw = Math.round(w * 0.5f * f), hh = Math.max(1, Math.round(h * (0.5f + l * 0.1f)));
            ctx.fill(cx - hw, cy - hh + l, cx + hw, cy + hh - l / 2, argb(a / 4, l < 3 ? dark : light));
        }
        ctx.fill(cx - Math.round(w * 0.3f), cy + Math.round(h * 0.4f), cx + Math.round(w * 0.3f), cy + Math.round(h * 0.55f), argb(a / 3, under));
    }

    private static void clouds(DrawContext ctx, float t, int strength, int style, float[] o) {
        int a = Math.round(strength / 100f * 150);
        int dark = style == 7 ? 0x2B1A3F : style == 4 ? 0xE8B8FF : style == 3 ? 0xB05A3A : 0x8A97B8;
        int light = style == 7 ? 0x4D3566 : style == 4 ? 0xFFD0F0 : style == 3 ? 0xFFB070 : 0xE8F0FF;
        int under = style == 7 ? 0xFF9AD0 : style == 4 ? 0xB8E8FF : style == 3 ? 0xFFE0A0 : 0xFFFFFF;
        int n = style == 7 ? 16 : 11;
        for (int i = 0; i < n; i++) {
            double az = hash(i * 11 + 4) * Math.PI * 2 + t * 0.006 * (1 + i % 3);
            double el = Math.toRadians(8 + hash(i * 17 + 9) * (style == 7 ? 40 : 34));
            cloud(ctx, az, el, Math.toRadians(14 + hash(i * 19 + 2) * 22), a, dark, light, under, o);
        }
    }

    private static void sun(DrawContext ctx, float t, int strength, boolean rays, float[] o) {
        double az = Math.toRadians(25), el = Math.toRadians(11 + 2 * Math.sin(t * 0.1));
        if (!dome(az, el, o) || domeBlocked(az, el)) return;
        int cx = Math.round(o[0]), cy = Math.round(o[1]);
        int r = Math.round(0.11f * pxPerRad());
        glow(ctx, cx, cy, Math.max(8, r), 0xFFB070, Math.round(strength / 100f * 230));
        if (rays) {
            for (int i = 0; i < 12; i++) {
                double ang = i * Math.PI / 6 + t * 0.05;
                float len = r * (2.4f + 1.6f * hash(i * 7 + 1));
                float pulse = 0.5f + 0.5f * (float) Math.sin(t * 0.7 + i);
                line(ctx, cx + (float) Math.cos(ang) * r * 0.9f, cy + (float) Math.sin(ang) * r * 0.9f,
                        cx + (float) Math.cos(ang) * len, cy + (float) Math.sin(ang) * len, 3, argb(Math.round(strength / 100f * 26 * pulse), 0xFFD090));
            }
        }
    }

    private static void rainbow(DrawContext ctx, int strength, float[] o) {
        int[] cols = {0xFF4040, 0xFF9A30, 0xFFE840, 0x50E850, 0x40B0FF, 0x5060FF, 0xB050FF};
        double az0 = Math.toRadians(mcYaw() + 150);
        int a = Math.round(strength / 100f * 120);
        for (int b = 0; b < 7; b++) {
            for (int th = 0; th <= 180; th += 3) {
                double rad = Math.toRadians(th);
                double az = az0 + Math.toRadians(44) * Math.cos(rad), el = Math.toRadians(3 + (40 - b * 1.7) * Math.sin(rad));
                if (el < Math.toRadians(1.5) || !dome(az, el, o)) continue;
                if (domeBlocked(az, el)) continue;
                int sz = Math.max(2, Math.round(0.014f * pxPerRad() * 1.6f));
                ctx.fill((int) o[0], (int) o[1], (int) o[0] + sz + 2, (int) o[1] + sz, argb(a, cols[b]));
            }
        }
    }

    private static void lightning(DrawContext ctx, float t, int strength, float[] o) {
        int slot = (int) (t * 0.6f);
        float local = t * 0.6f - slot;
        if (hash(slot * 7 + 3) < 0.45f || local > 0.28f) return;
        float fade = 1f - local / 0.28f;
        int a = Math.round(strength / 100f * 255 * fade);
        double az = hash(slot * 11 + 5) * Math.PI * 2, el = Math.toRadians(58);
        float px = 0, py = 0; boolean has = false;
        for (int seg = 0; seg < 18; seg++) {
            az += (hash(slot * 97 + seg * 13) - 0.5) * 0.05;
            el -= Math.toRadians(2.6);
            if (el < Math.toRadians(3) || !dome(az, el, o)) break;
            if (has) {
                line(ctx, px, py, o[0], o[1], 7, argb(a / 6, 0xB69CFF));
                line(ctx, px, py, o[0], o[1], 2, argb(a, 0xFFFFFF));
            }
            px = o[0]; py = o[1]; has = true;
        }
        ctx.fill(0, 0, W, H, argb(Math.round(a * 0.12f), 0xD8C8FF));
    }

    private static void horizonBloom(DrawContext ctx, int style, int strength, float t, float[] o) {
        int acc = ACCENT[Math.max(0, Math.min(7, style))];
        double az0 = Math.toRadians(mcYaw());
        for (int i = -40; i <= 40; i++) {
            double az = az0 + Math.toRadians(i * 2.0);
            if (!dome(az, Math.toRadians(1), o)) continue;
            float[] t0 = new float[3];
            if (!dome(az, Math.toRadians(9), t0)) continue;
            float[] n1 = new float[3];
            if (!dome(az + Math.toRadians(2.1), Math.toRadians(1), n1)) continue;
            for (int j = 0; j < 5; j++) {
                int ya = Math.round(o[1] + (t0[1] - o[1]) * j / 5f), yb = Math.round(o[1] + (t0[1] - o[1]) * (j + 1) / 5f);
                ctx.fill(Math.round(o[0]), Math.min(ya, yb), Math.round(n1[0]) + 1, Math.max(ya, yb) + 1, argb(Math.round(strength / 100f * 34 * (1 - j / 5f)), acc));
            }
        }
    }

    static void render(DrawContext ctx, MinecraftClient mc, Module m, float t) {
        cam(mc, ctx);
        mcYawVal = mc.player.getYaw();
        budget = 18;
        float[] o = new float[3];
        int style = m.num("style"), strength = m.num("intensity"), density = m.num("density");
        long now = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
        dt *= m.num("speed") / 50f;

        boolean ducksAny = style == 0 || m.bool("ducks"), glowOn = m.bool("glow");
        int count = m.num("count");
        if (style != pstyle || count != pcount || (glowOn ? 1 : 0) != pglow || ducksAny != pDucks || density != pDens) {
            pstyle = style; pcount = count; pglow = glowOn ? 1 : 0; pDucks = ducksAny; pDens = density;
            rebuild(style, ducksAny, glowOn, count, density);
        }

        if (m.bool("bloom")) horizonBloom(ctx, style, strength, t, o);
        switch (style) {
            case 1 -> aurora(ctx, t, strength, o);
            case 2 -> stars(ctx, t, strength, density, o);
            case 3 -> { clouds(ctx, t, strength, style, o); sun(ctx, t, strength, m.bool("rays"), o); }
            case 4 -> clouds(ctx, t, strength, style, o);
            case 6 -> { clouds(ctx, t, strength, style, o); rainbow(ctx, strength, o); }
            case 7 -> { clouds(ctx, t, strength, style, o); lightning(ctx, t, strength, o); }
            default -> { if (style == 0) clouds(ctx, t, strength / 2, 99, o); }
        }
        if (m.bool("rays") && style != 3) {
            // promienie światła z nieba (smugi na kopule)
            int acc = ACCENT[Math.max(0, Math.min(7, style))];
            for (int i = 0; i < 5; i++) {
                double az = hash(i * 23 + 5) * Math.PI * 2 + Math.sin(t * 0.2 + i) * 0.05;
                if (!dome(az, Math.toRadians(40), o)) continue;
                float[] b = new float[3];
                if (!dome(az + 0.08, Math.toRadians(2), b) || domeBlocked(az, Math.toRadians(5))) continue;
                line(ctx, o[0], o[1], b[0], b[1], 6, argb(Math.round(strength / 100f * 14 * (0.5f + 0.5f * (float) Math.sin(t * 0.5 + i))), acc));
            }
        }
        particles(ctx, mc, t, dt, style, strength, m.num("size"), density);
    }

    private static boolean pDucks; private static int pDens = -1;
}
