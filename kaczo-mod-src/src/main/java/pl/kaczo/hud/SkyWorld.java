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
 * Niebo Kaczo w świecie gry. Wszystko ma pozycję 3D (kopuła nieba + cząsteczki wokół gracza), jest rzutowane z perspektywą,
 * zasłaniane przez teren i zostaje w miejscu, gdy się ruszasz. Rysowane w NATYWNEJ rozdzielczości ekranu (nie w grubych
 * pikselach interfejsu) gładkimi, obróconymi odcinkami z neonową poświatą – bez pikseli.
 */
final class SkyWorld {
    private SkyWorld() { }

    // ── kamera (współrzędne w pikselach ekranu) ──
    private static double ex, ey, ez, tanHalf, SF = 1;
    private static Vec3d F, R, U;
    private static int W, H;
    private static MinecraftClient MC;
    private static float yawDeg;

    private static void cam(MinecraftClient mc, DrawContext ctx) {
        MC = mc;
        SF = mc.getWindow().getScaleFactor();
        Vec3d e = WorldLyrics.eye(mc);
        ex = e.x; ey = e.y; ez = e.z;
        W = (int) Math.round(ctx.getScaledWindowWidth() * SF); H = (int) Math.round(ctx.getScaledWindowHeight() * SF);
        yawDeg = mc.player.getYaw();
        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(mc.player.getPitch());
        F = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        R = new Vec3d(-Math.cos(yaw), 0, -Math.sin(yaw));
        U = R.crossProduct(F);
        tanHalf = Math.tan(Math.toRadians(Math.max(30, mc.options.getFov().getValue())) / 2);
    }

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
    private static boolean dome(double az, double el, float[] o) {
        double ce = Math.cos(el);
        return proj(ex + Math.sin(az) * ce * DOME, ey + Math.sin(el) * DOME, ez + Math.cos(az) * ce * DOME, o);
    }
    private static float pxPerRad() { return (float) ((H / 2.0) / tanHalf); }
    private static double mcAz() { return Math.toRadians(180 + yawDeg) - Math.PI; }   // azymut kierunku patrzenia na kopule

    // ── zasłanianie terenu ──
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

    // ── gładkie prymitywy (natywna rozdzielczość) ──
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

    /** Odcinek o dowolnym kącie: jeden obrócony prostokąt. */
    private static void seg(DrawContext c, float x0, float y0, float x1, float y1, int th, int color) {
        float dx = x1 - x0, dy = y1 - y0;
        int len = Math.round((float) Math.sqrt(dx * dx + dy * dy));
        if (len < 1 && th < 2) return;
        th = Math.max(1, th);
        Compat.push(c);
        Compat.translate(c, x0, y0);
        Compat.rotate(c, (float) Math.atan2(dy, dx));
        int top = -(th / 2);
        c.fill(-(th / 2), top, len + (th + 1) / 2, top + th, color);
        Compat.pop(c);
    }

    /** Neonowy odcinek: szeroka miękka poświata + jasny rdzeń. */
    private static void neon(DrawContext c, float x0, float y0, float x1, float y1, float w, int rgb, int a, boolean glowPass) {
        int th = Math.max(1, Math.round(w));
        if (glowPass) {
            seg(c, x0, y0, x1, y1, th * 4, argb(a / 14, rgb));
            seg(c, x0, y0, x1, y1, th * 2 + 1, argb(a / 5, rgb));
        }
        seg(c, x0, y0, x1, y1, th, argb(a, lerp(rgb, 0xFFFFFF, 0.55f)));
    }

    private static void ellipse(DrawContext c, int cx, int cy, int rx, int ry, int color) {
        if (rx < 1 || ry < 1) return;
        int step = Math.max(1, ry / 26);
        for (int dy = -ry; dy < ry; dy += step) {
            float k = 1 - (float) (dy * dy) / (ry * ry);
            if (k <= 0) continue;
            int half = (int) (rx * Math.sqrt(k));
            c.fill(cx - half, cy + dy, cx + half, cy + dy + step, color);
        }
    }

    /** Miękka poświata (bloom): wiele cienkich warstw o malejącym promieniu. */
    static void glow(DrawContext c, int cx, int cy, int r, int rgb, int a) {
        r = Math.min(r, 600);
        int layers = Math.max(4, Math.min(14, r / 3));
        for (int i = 0; i < layers; i++) {
            float f = 1 - i / (float) layers;
            ellipse(c, cx, cy, Math.max(1, Math.round(r * f)), Math.max(1, Math.round(r * f)), argb(Math.max(1, a / (layers + 4)) + (i == layers - 1 ? a / 3 : 0), i > layers * 0.7 ? lerp(rgb, 0xFFFFFF, 0.6f) : rgb));
        }
    }

    // ── model kaczki 3D z neonowych linii ──
    private static final class Poly { int col; boolean closed; double[][] p; }
    private static final List<Poly> DUCK = new ArrayList<>();
    private static final int CYAN = 0x35F0FF, PINK = 0xFF4FD8, YELLOW = 0xFFE14A, LIME = 0x7CFF6B;

    private static Poly poly(int col, boolean closed, double[]... pts) { Poly q = new Poly(); q.col = col; q.closed = closed; q.p = pts; return q; }
    private static double[] pt(double x, double y, double z) { return new double[]{x, y, z}; }
    /** Elipsa w jednej z płaszczyzn: 0 = XZ (stały y), 1 = XY (stały z), 2 = YZ (stały x). */
    private static Poly ring(int col, int plane, double cx, double cy, double cz, double a, double b, int n) {
        double[][] pts = new double[n][];
        for (int i = 0; i < n; i++) {
            double t = Math.PI * 2 * i / n, c = Math.cos(t), s = Math.sin(t);
            pts[i] = switch (plane) { case 0 -> pt(cx + a * c, cy, cz + b * s); case 1 -> pt(cx + a * c, cy + b * s, cz); default -> pt(cx, cy + a * c, cz + b * s); };
        }
        return poly(col, true, pts);
    }
    private static Poly arc(int col, double cx, double cy, double z, double a, double b, double from, double to, int n) {
        double[][] pts = new double[n][];
        for (int i = 0; i < n; i++) { double t = from + (to - from) * i / (n - 1); pts[i] = pt(cx + a * Math.cos(t), cy + b * Math.sin(t), z); }
        return poly(col, false, pts);
    }
    static {
        // korpus
        DUCK.add(ring(CYAN, 0, 0, -0.14, 0, 0.44, 0.27, 14));
        DUCK.add(ring(CYAN, 0, 0, 0.10, 0, 0.50, 0.31, 14));
        DUCK.add(ring(CYAN, 1, 0, 0, 0, 0.52, 0.31, 16));
        DUCK.add(ring(CYAN, 1, 0, 0, 0.2, 0.46, 0.27, 14));
        DUCK.add(ring(CYAN, 1, 0, 0, -0.2, 0.46, 0.27, 14));
        DUCK.add(ring(CYAN, 2, 0.05, 0, 0, 0.31, 0.32, 12));
        // głowa
        DUCK.add(ring(PINK, 1, 0.44, 0.46, 0, 0.19, 0.19, 12));
        DUCK.add(ring(PINK, 0, 0.44, 0.46, 0, 0.19, 0.17, 12));
        DUCK.add(ring(PINK, 2, 0.44, 0.46, 0, 0.19, 0.17, 12));
        // dziób
        DUCK.add(poly(YELLOW, true, pt(0.60, 0.46, 0.08), pt(0.86, 0.40, 0), pt(0.60, 0.46, -0.08)));
        DUCK.add(poly(YELLOW, false, pt(0.60, 0.37, 0), pt(0.86, 0.40, 0)));
        // skrzydła i ogon
        DUCK.add(arc(LIME, -0.04, 0.02, 0.33, 0.30, 0.17, Math.PI * 1.05, Math.PI * 1.95, 8));
        DUCK.add(arc(LIME, -0.04, 0.02, -0.33, 0.30, 0.17, Math.PI * 1.05, Math.PI * 1.95, 8));
        DUCK.add(poly(LIME, false, pt(-0.46, 0.05, 0), pt(-0.80, 0.30, 0), pt(-0.52, 0.20, 0)));
    }

    // ── cząsteczki ──
    private static final class Pt { double x, y, z; float vx, vy, vz, ph, sz; int kind, col, id; }
    private static final List<Pt> pts = new ArrayList<>();
    private static final Random RNG = new Random();
    private static int pstyle = -9, pcount = -1, pglow = -1, pDens = -1;
    private static boolean pDucks;
    private static long lastNs;
    private static final int DUCK_K = 0, GLOW_K = 1, RAIN_K = 2, GLYPH_K = 3, PETAL_K = 4;
    private static final String GL = "01ｱｲｳｴｵｶｷｸ";
    private static final int[] ACCENT = {0xFFE27A, 0x6CFFD2, 0xA9B4FF, 0xFFB070, 0xFF9AD8, 0x5CFF8A, 0xFFB0F0, 0xD9A8FF};

    private static Pt spawn(int kind, boolean fresh) {
        Pt p = new Pt();
        p.kind = kind; p.id = RNG.nextInt(100000); p.ph = RNG.nextFloat() * 6.28f; p.sz = 0.75f + RNG.nextFloat() * 0.5f;
        double ang = RNG.nextDouble() * Math.PI * 2;
        double rad = (kind == RAIN_K ? 1 : 3) + RNG.nextDouble() * (kind == RAIN_K ? 13 : (kind == GLOW_K ? 14 : 22));
        p.x = ex + Math.cos(ang) * rad; p.z = ez + Math.sin(ang) * rad;
        switch (kind) {
            case DUCK_K -> { p.y = ey + (fresh ? 14 * RNG.nextDouble() - 3 : 12 + 4 * RNG.nextDouble()); p.vy = -(0.7f + RNG.nextFloat() * 0.9f); p.vx = (RNG.nextFloat() - .5f) * .8f; p.vz = (RNG.nextFloat() - .5f) * .8f; }
            case GLOW_K -> { p.y = ey - 2 + RNG.nextDouble() * 7; p.vy = 0.1f + RNG.nextFloat() * 0.25f; p.vx = (RNG.nextFloat() - .5f) * .5f; p.vz = (RNG.nextFloat() - .5f) * .5f; }
            case RAIN_K -> { p.y = ey + (fresh ? 16 * RNG.nextDouble() - 4 : 14 + 4 * RNG.nextDouble()); p.vy = -(15f + RNG.nextFloat() * 6f); p.vx = -2.2f; }
            case GLYPH_K -> { p.y = ey + (fresh ? 14 * RNG.nextDouble() - 3 : 12 + 4 * RNG.nextDouble()); p.vy = -(2.5f + RNG.nextFloat() * 3f); }
            default -> { p.y = ey + (fresh ? 14 * RNG.nextDouble() - 3 : 12 + 4 * RNG.nextDouble()); p.vy = -(0.7f + RNG.nextFloat() * 0.8f); p.vx = (RNG.nextFloat() - .5f) * .8f; p.col = new int[]{0xFF8AD8, 0x8AD8FF, 0xFFF08A, 0xB88AFF}[RNG.nextInt(4)]; }
        }
        return p;
    }

    private static void rebuild(int style, boolean ducksAny, boolean glowOn, int count, int density) {
        pts.clear();
        if (style == 0 || ducksAny) for (int i = 0; i < Math.max(1, Math.min(40, count)); i++) pts.add(spawn(DUCK_K, true));
        if (style == 7) for (int i = 0; i < Math.max(30, Math.min(200, density * 3)); i++) pts.add(spawn(RAIN_K, true));
        if (style == 5) for (int i = 0; i < Math.max(20, Math.min(140, density * 2)); i++) pts.add(spawn(GLYPH_K, true));
        if (style == 4) for (int i = 0; i < Math.max(20, Math.min(100, density * 2)); i++) pts.add(spawn(PETAL_K, true));
        if (glowOn) for (int i = 0; i < Math.max(4, Math.min(60, density)); i++) pts.add(spawn(GLOW_K, true));
    }

    private static final class Draw { Pt p; float x, y, z; }
    private static final List<Draw> draws = new ArrayList<>();

    private static void duck3d(DrawContext ctx, Draw d, float t, int a, int size, boolean neonPalette) {
        Pt p = d.p;
        float k = pxPerRad();
        float scale = (0.5f + 0.2f * size) * p.sz;                       // długość kaczki w blokach
        float pxBlock = k / d.z;
        if (scale * pxBlock < 10) { ctx.fill((int) d.x - 1, (int) d.y - 1, (int) d.x + 2, (int) d.y + 2, argb(a, CYAN)); return; }
        double yaw = p.ph + t * 0.7 * (p.id % 2 == 0 ? 1 : -1);
        double roll = Math.sin(t * 1.3 + p.ph) * 0.28, cr = Math.cos(roll), sr = Math.sin(roll), cy = Math.cos(yaw), sy = Math.sin(yaw);
        float w = Math.max(1f, Math.min(3.6f, scale * pxBlock * 0.035f));
        boolean near = d.z < 20;
        float[] o0 = new float[3], o1 = new float[3];
        for (Poly q : DUCK) {
            int n = q.p.length;
            float[][] sp = new float[n][];
            for (int i = 0; i < n; i++) {
                double mx = q.p[i][0] * scale, my = q.p[i][1] * scale, mz = q.p[i][2] * scale;
                double rx = mx * cr - my * sr, ry = mx * sr + my * cr;           // przechył (roll)
                double wx = rx * cy + mz * sy, wz = -rx * sy + mz * cy;          // obrót wokół osi Y
                float[] o = new float[3];
                if (proj(p.x + wx, p.y + ry, p.z + wz, o)) sp[i] = o;
            }
            int segs = q.closed ? n : n - 1;
            int col = neonPalette ? q.col : YELLOW;
            for (int i = 0; i < segs; i++) {
                float[] A = sp[i], B = sp[(i + 1) % n];
                if (A == null || B == null) continue;
                float depth = ((A[2] + B[2]) / 2f - d.z) / Math.max(0.3f, scale);   // dalsza część modelu = ciemniejsza (głębia 3D)
                float shade = Math.max(0.3f, Math.min(1f, 0.8f - depth * 0.55f));
                neon(ctx, A[0], A[1], B[0], B[1], w, col, Math.round(a * shade), near);
            }
        }
        // oko
        double ex2 = 0.55 * scale, ey2 = 0.52 * scale, ez2 = 0.1 * scale;
        for (int s = -1; s <= 1; s += 2) {
            double rx = ex2 * cr - ey2 * sr, ry = ex2 * sr + ey2 * cr, zz = s * ez2;
            float[] o = new float[3];
            if (proj(p.x + rx * cy + zz * sy, p.y + ry, p.z - rx * sy + zz * cy, o)) ctx.fill((int) o[0] - 1, (int) o[1] - 1, (int) o[0] + 2, (int) o[1] + 2, argb(a, 0xFFFFFF));
        }
    }

    private static void particles(DrawContext ctx, MinecraftClient mc, float t, float dt, int style, int strength, int size, int density) {
        float k = pxPerRad();
        draws.clear();
        float[] o = new float[3];
        int acc = ACCENT[Math.max(0, Math.min(7, style))];
        for (Pt p : pts) {
            p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt;
            if (p.kind == DUCK_K || p.kind == PETAL_K) { p.x += Math.sin(t * 0.8 + p.ph) * 0.6 * dt; p.z += Math.cos(t * 0.7 + p.ph) * 0.5 * dt; }
            double dx = p.x - ex, dz = p.z - ez;
            boolean out = dx * dx + dz * dz > 26 * 26 || p.y < ey - 4 - (p.kind == GLOW_K ? 1 : 0) || p.y > ey + 22;
            if (out) { Pt n = spawn(p.kind, false); p.x = n.x; p.y = n.y; p.z = n.z; p.vx = n.vx; p.vy = n.vy; p.vz = n.vz; p.id = n.id; continue; }
            if (!proj(p.x, p.y, p.z, o)) continue;
            if (o[0] < -80 || o[0] > W + 80 || o[1] < -80 || o[1] > H + 80) continue;
            boolean hid;
            if (p.kind == RAIN_K || p.kind == GLYPH_K) {
                int top = mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(p.x), (int) Math.floor(p.z));
                hid = p.y < top - 0.5;
            } else hid = blocked(p.id, p.x, p.y, p.z);
            if (hid) continue;
            Draw d = new Draw(); d.p = p; d.x = o[0]; d.y = o[1]; d.z = o[2];
            draws.add(d);
        }
        draws.sort(Comparator.comparingDouble((Draw d) -> -d.z));
        for (Draw d : draws) {
            Pt p = d.p;
            float z = d.z;
            float fade = Math.max(0, Math.min(1, (30 - z) / 14f)) * Math.min(1, z / 1.5f);
            int a = Math.round(255 * strength / 100f * fade);
            if (a < 6) continue;
            float pxBlock = k / z;
            switch (p.kind) {
                case DUCK_K -> duck3d(ctx, d, t, a, size, true);
                case GLOW_K -> {
                    float tw = 0.55f + 0.45f * (float) Math.sin(t * (1.3 + p.sz) + p.ph);
                    int r = Math.max(3, Math.min(120, Math.round(0.14f * p.sz * pxBlock * (0.6f + 0.4f * size))));
                    glow(ctx, (int) d.x, (int) d.y, r, (p.id % 3 == 0) ? lerp(acc, 0xFFFFFF, 0.55f) : acc, Math.round(a * tw));
                }
                case RAIN_K -> {
                    float len = 0.6f * pxBlock;
                    seg(ctx, d.x, d.y, d.x + len * 0.12f, d.y + len, 1, argb(Math.round(a * 0.55f), 0xC8D4FF));
                }
                case GLYPH_K -> {
                    float s = 0.5f * pxBlock / 9f;
                    if (s < 0.3f) break;
                    String ch = String.valueOf(GL.charAt((p.id + (int) (t * 6)) % GL.length()));
                    Compat.push(ctx);
                    Compat.translate(ctx, d.x, d.y);
                    Compat.scale(ctx, s, s);
                    ctx.drawText(mc.textRenderer, ch, 0, 0, argb(a, 0x35FF70), false);
                    Compat.pop(ctx);
                }
                default -> {
                    int sz = Math.max(2, Math.round(0.16f * p.sz * pxBlock));
                    Compat.push(ctx);
                    Compat.translate(ctx, d.x, d.y);
                    Compat.rotate(ctx, t * 1.5f + p.ph);
                    ctx.fill(-sz, -sz / 2, sz, sz - sz / 2, argb(a, p.col));
                    Compat.pop(ctx);
                }
            }
        }
    }

    // ── efekty na kopule nieba ──
    private static void aurora(DrawContext ctx, float t, int strength) {
        int a = Math.round(strength / 100f * 120);
        double az0 = mcAz();
        float[] b0 = new float[3], t0 = new float[3], b1 = new float[3];
        for (int i = -50; i <= 50; i++) {
            double az = az0 + Math.toRadians(i * 1.4);
            double wave = Math.sin(az * 3 + t * 0.6) * 0.5 + Math.sin(az * 1.3 - t * 0.4) * 0.5;
            double elB = Math.toRadians(16 + 6 * wave), elT = Math.toRadians(46 + 14 * Math.sin(az * 2 + t * 0.3));
            if (domeBlocked(az, elB)) continue;
            if (!dome(az, elB, b0) || !dome(az, elT, t0) || !dome(az + Math.toRadians(1.5), elB, b1)) continue;
            int col = lerp(0x35FFA0, 0xB45CFF, (float) (0.5 + 0.5 * Math.sin(az * 2 + t * 0.3)));
            int x0 = Math.round(b0[0]), x1 = Math.round(b1[0]) + 1, yb = Math.round(b0[1]), yt = Math.round(t0[1]);
            int bands = 14;
            for (int j = 0; j < bands; j++) {
                float f0 = j / (float) bands, f1 = (j + 1) / (float) bands;
                int ya = Math.round(yb + (yt - yb) * f0), yb2 = Math.round(yb + (yt - yb) * f1);
                float al = (float) Math.pow(Math.sin(Math.PI * (f0 + f1) / 2), 1.4);
                ctx.fill(x0, Math.min(ya, yb2), x1, Math.max(ya, yb2) + 1, argb(Math.round(a * al * 0.5f), col));
            }
        }
    }

    private static void stars(DrawContext ctx, float t, int strength, int density, float[] o) {
        int n = Math.max(40, Math.min(500, density * 7));
        for (int i = 0; i < n; i++) {
            double az = hash(i * 3 + 1) * Math.PI * 2, el = Math.toRadians(4 + hash(i * 7 + 2) * 80);
            if (!dome(az, el, o)) continue;
            if (o[0] < -4 || o[0] > W + 4 || o[1] < -4 || o[1] > H + 4) continue;
            if (domeBlocked(az, el)) continue;
            float tw = 0.5f + 0.5f * (float) Math.sin(t * (1 + 2 * hash(i)) + i);
            int a = Math.round(strength / 100f * 255 * (0.35f + 0.65f * tw));
            boolean big = hash(i * 11) > 0.9f;
            int sz = big ? 2 : 1;
            ctx.fill((int) o[0], (int) o[1], (int) o[0] + sz, (int) o[1] + sz, argb(a, 0xFFFFFF));
            if (hash(i * 11) > 0.96f) glow(ctx, (int) o[0], (int) o[1], 12, 0xBFD0FF, a);
        }
        int slot = (int) (t * 0.5f);
        float local = t * 0.5f - slot;
        if (hash(slot * 5 + 1) > 0.35f && local < 0.5f) {
            float kf = local / 0.5f;
            double az = hash(slot * 13 + 2) * Math.PI * 2, el = Math.toRadians(35 + hash(slot * 17 + 3) * 35);
            float[] pv = new float[3]; boolean has = false;
            for (int i = 0; i < 16; i++) {
                float f = kf - i * 0.018f;
                if (f < 0) break;
                if (!dome(az + f * 0.7, el - f * 0.5, o)) continue;
                if (has) neon(ctx, pv[0], pv[1], o[0], o[1], 2 - i / 10f, 0xFFFFFF, Math.round(strength / 100f * 230 * (1 - i / 16f)), i < 6);
                pv[0] = o[0]; pv[1] = o[1]; has = true;
            }
        }
    }

    private static void cloud(DrawContext ctx, double az, double el, double angW, int a, int dark, int light, int under, float[] o) {
        if (!dome(az, el, o) || domeBlocked(az, el)) return;
        float w = (float) (angW * pxPerRad());
        if (w < 12) return;
        int cx = Math.round(o[0]), cy = Math.round(o[1]);
        for (int i = 0; i < 9; i++) {
            float u = (i - 4) / 4f;
            int rx = Math.round(w * (0.2f - 0.06f * Math.abs(u)) * (0.8f + 0.4f * hash(i * 7 + (int) (az * 10))));
            int ry = Math.max(2, Math.round(rx * 0.55f));
            int px = cx + Math.round(u * w * 0.42f), py = cy - Math.round((1 - Math.abs(u)) * w * 0.05f);
            ellipse(ctx, px, py + ry / 3, rx, ry, argb(a / 5, dark));
            ellipse(ctx, px, py - ry / 4, Math.round(rx * 0.8f), Math.round(ry * 0.75f), argb(a / 6, light));
        }
        ellipse(ctx, cx, cy + Math.round(w * 0.05f), Math.round(w * 0.35f), Math.max(2, Math.round(w * 0.03f)), argb(a / 5, under));
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
        glow(ctx, cx, cy, Math.max(20, r * 2), 0xFFB070, Math.round(strength / 100f * 230));
        if (rays) {
            for (int i = 0; i < 14; i++) {
                double ang = i * Math.PI / 7 + t * 0.05;
                float len = r * (3f + 2f * hash(i * 7 + 1));
                float pulse = 0.5f + 0.5f * (float) Math.sin(t * 0.7 + i);
                int al = Math.round(strength / 100f * 40 * pulse);
                seg(ctx, cx + (float) Math.cos(ang) * r, cy + (float) Math.sin(ang) * r, cx + (float) Math.cos(ang) * len, cy + (float) Math.sin(ang) * len, Math.max(2, r / 6), argb(al, 0xFFD090));
            }
        }
    }

    private static void rainbow(DrawContext ctx, int strength, float[] o) {
        int[] cols = {0xFF4040, 0xFF9A30, 0xFFE840, 0x50E850, 0x40B0FF, 0x5060FF, 0xB050FF};
        double az0 = mcAz() + Math.toRadians(150);
        int a = Math.round(strength / 100f * 130);
        int wpx = Math.max(2, Math.round(0.012f * pxPerRad()));
        for (int b = 0; b < 7; b++) {
            float px = 0, py = 0; boolean has = false;
            for (int th = 0; th <= 180; th += 3) {
                double rad = Math.toRadians(th);
                double az = az0 + Math.toRadians(44) * Math.cos(rad), el = Math.toRadians(3 + (40 - b * 1.7) * Math.sin(rad));
                if (el < Math.toRadians(1.5) || !dome(az, el, o) || domeBlocked(az, el)) { has = false; continue; }
                if (has) seg(ctx, px, py, o[0], o[1], wpx, argb(a, cols[b]));
                px = o[0]; py = o[1]; has = true;
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
        for (int seg = 0; seg < 22; seg++) {
            az += (hash(slot * 97 + seg * 13) - 0.5) * 0.05;
            el -= Math.toRadians(2.1);
            if (el < Math.toRadians(3) || !dome(az, el, o)) break;
            if (has) neon(ctx, px, py, o[0], o[1], 2.5f, 0xB69CFF, a, true);
            if (has && seg % 6 == 3) {                                      // odnoga
                float bx = o[0] + (hash(slot * 31 + seg) - 0.5f) * 90f, by = o[1] + 50f;
                neon(ctx, o[0], o[1], bx, by, 1.5f, 0xB69CFF, a * 2 / 3, true);
            }
            px = o[0]; py = o[1]; has = true;
        }
        ctx.fill(0, 0, W, H, argb(Math.round(a * 0.12f), 0xD8C8FF));
    }

    private static void horizonBloom(DrawContext ctx, int style, int strength, float t, float[] o) {
        int acc = ACCENT[Math.max(0, Math.min(7, style))];
        double az0 = mcAz();
        float[] t0 = new float[3], n1 = new float[3];
        for (int i = -45; i <= 45; i++) {
            double az = az0 + Math.toRadians(i * 1.6);
            if (!dome(az, Math.toRadians(1), o) || !dome(az, Math.toRadians(10), t0) || !dome(az + Math.toRadians(1.7), Math.toRadians(1), n1)) continue;
            for (int j = 0; j < 12; j++) {
                int ya = Math.round(o[1] + (t0[1] - o[1]) * j / 12f), yb = Math.round(o[1] + (t0[1] - o[1]) * (j + 1) / 12f);
                float f = 1 - j / 12f;
                ctx.fill(Math.round(o[0]), Math.min(ya, yb), Math.round(n1[0]) + 1, Math.max(ya, yb) + 1, argb(Math.round(strength / 100f * 36 * f * f), acc));
            }
        }
    }

    private static void grade(DrawContext ctx, int style, int strength, int vignette) {
        int[] tints = {0xFFE9B0, 0x66FFD0, 0x8F9CFF, 0xFF9A5C, 0xFFB8E8, 0x30FF70, 0xC0A8FF, 0xB48CD8};
        ctx.fill(0, 0, W, H, argb(Math.round(strength / 100f * 18), tints[Math.max(0, Math.min(7, style))]));
        int v = Math.round(vignette / 100f * 130);
        if (v <= 0) return;
        int layers = 40, th = Math.max(2, Math.min(W, H) / 70);
        for (int i = 0; i < layers; i++) {
            int al = Math.round(v * (float) Math.pow(1 - i / (float) layers, 2.4f) / 5f);
            int d = i * th;
            ctx.fill(0, d, W, d + th, argb(al, 0));
            ctx.fill(0, H - d - th, W, H - d, argb(al, 0));
            ctx.fill(d, 0, d + th, H, argb(al, 0));
            ctx.fill(W - d - th, 0, W - d, H, argb(al, 0));
        }
    }

    static void render(DrawContext ctx, MinecraftClient mc, Module m, float t) {
        cam(mc, ctx);
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

        Compat.push(ctx);
        Compat.scale(ctx, (float) (1.0 / SF), (float) (1.0 / SF));        // od teraz 1 jednostka = 1 prawdziwy piksel ekranu
        try {
            if (m.bool("bloom")) horizonBloom(ctx, style, strength, t, o);
            switch (style) {
                case 1 -> aurora(ctx, t, strength);
                case 2 -> stars(ctx, t, strength, density, o);
                case 3 -> { clouds(ctx, t, strength, style, o); sun(ctx, t, strength, m.bool("rays"), o); }
                case 4 -> clouds(ctx, t, strength, style, o);
                case 6 -> { clouds(ctx, t, strength, style, o); rainbow(ctx, strength, o); }
                case 7 -> { clouds(ctx, t, strength, style, o); lightning(ctx, t, strength, o); }
                default -> { if (style == 0) clouds(ctx, t, strength / 2, 99, o); }
            }
            if (m.bool("rays") && style != 3) {
                int acc = ACCENT[Math.max(0, Math.min(7, style))];
                float[] b = new float[3];
                for (int i = 0; i < 5; i++) {
                    double az = hash(i * 23 + 5) * Math.PI * 2 + Math.sin(t * 0.2 + i) * 0.05;
                    if (!dome(az, Math.toRadians(40), o) || !dome(az + 0.08, Math.toRadians(2), b) || domeBlocked(az, Math.toRadians(5))) continue;
                    float pulse = 0.5f + 0.5f * (float) Math.sin(t * 0.5 + i);
                    seg(ctx, o[0], o[1], b[0], b[1], Math.max(4, W / 90), argb(Math.round(strength / 100f * 16 * pulse), acc));
                }
            }
            particles(ctx, mc, t, dt, style, strength, m.num("size"), density);
            if (m.bool("grade")) grade(ctx, style, strength, m.num("vignette"));
        } finally {
            Compat.pop(ctx);
        }
    }
}
