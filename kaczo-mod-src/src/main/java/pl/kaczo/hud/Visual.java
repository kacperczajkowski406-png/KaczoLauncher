package pl.kaczo.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.io.InputStream;

/** Animowane tło („zorza”) ekranu tytułowego oraz własny ekran ładowania gry zamiast czerwonego ekranu Mojang. */
public final class Visual {
    private Visual() { }

    private static Identifier splashLogo;
    private static final long T0 = System.currentTimeMillis();

    private static Identifier bgTex;
    private static Identifier bgTexture(MinecraftClient mc) {
        if (bgTex == null) {
            try (InputStream in = Visual.class.getResourceAsStream("/assets/kaczohud/textures/gui/glass_bg.png")) {
                NativeImage ni = NativeImage.read(in);
                Identifier id = Identifier.of("kaczohud", "glass_bg");
                mc.getTextureManager().registerTexture(id, Compat.newTexture(ni));
                bgTex = id;
            } catch (Throwable t) { return null; }
        }
        return bgTex;
    }

    /** Tło w stylu „liquid glass”: czerń i gładkie, srebrne wstęgi jedwabiu (gotowa tekstura, leciutko pływająca). */
    public static void aurora(DrawContext ctx, int w, int h, float a) {
        float t = (System.currentTimeMillis() - T0) / 1000f;
        ctx.fillGradient(0, 0, w, h, Ui.fade(0xFF000000, a), Ui.fade(0xFF000000, a));
        try { if (Wallpapers.full(ctx, w, h, a)) return; } catch (Throwable ignored) { }
        Ribbons.draw(ctx, w, h, t, a * 0.9f, 3);
    }

    private static Identifier logoTexture(MinecraftClient mc) {
        if (splashLogo == null) {
            // podczas pierwszego ładowania zasobów tekstury moda mogą jeszcze nie być gotowe – wczytujemy logo sami z pliku .jar
            try (InputStream in = Visual.class.getResourceAsStream("/assets/kaczohud/textures/gui/logo.png")) {
                NativeImage ni = NativeImage.read(in);
                Identifier id = Identifier.of("kaczohud", "splash_logo");
                mc.getTextureManager().registerTexture(id, Compat.newTexture(ni));
                splashLogo = id;
            } catch (Throwable t) {
                splashLogo = Modules.logo();
            }
        }
        return splashLogo;
    }

    private static Identifier titleTex;
    private static Identifier titleTexture(MinecraftClient mc) {
        if (titleTex == null) {
            try (InputStream in = Visual.class.getResourceAsStream("/assets/kaczohud/textures/gui/title.png")) {
                NativeImage ni = NativeImage.read(in);
                Identifier id = Identifier.of("kaczohud", "splash_title");
                mc.getTextureManager().registerTexture(id, Compat.newTexture(ni));
                titleTex = id;
            } catch (Throwable t) { titleTex = logoTexture(mc); }
        }
        return titleTex;
    }

    private static float ease(float t) { t = Math.max(0, Math.min(1, t)); float u = 1 - t; return 1 - u * u * u; }

    private static Object curOverlay;
    private static long overlayT0;

    /**
     * Ekran ładowania gry: poświata, pulsujące logo z pierścieniami i szklany pasek postępu.
     * Rysowany na wierzch ekranu Mojang (jego czerwone tło jest wyłączone w SplashOverlayMixin), z tą samą logiką znikania.
     */
    public static void splash(Object inst, DrawContext ctx, float progress, long completeAt) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long nowMs = System.currentTimeMillis();
        if (inst != curOverlay) { curOverlay = inst; overlayT0 = nowMs; }
        float f = completeAt > -1L ? (Util.getMeasuringTimeMs() - completeAt) / 1000f : -1f;
        float a = f < 1f ? 1f : 1f - Math.max(0f, Math.min(1f, f - 1f));
        a = Math.min(1f, a * 1.5f);      // znika odrobinę szybciej niż oryginał, żeby nie prześwitywało logo Mojang
        a *= Math.min(1f, (nowMs - overlayT0) / 380f);   // płynne wejście
        if (a <= 0f) return;
        if (completeAt > -1L) progress = 1f;
        // najpierw dorysuj wszystko, co było wcześniej (ekran pod spodem) – inaczej jego tekst wyskakuje NA WIERZCH naszego tła
        Compat.flush(ctx);
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        aurora(ctx, w, h, a);

        long now = System.currentTimeMillis() - T0;
        float tt = now / 1000f;
        float k = ease(now / 900f);
        float done = completeAt > -1L ? Math.max(0f, Math.min(1f, f)) : 0f;     // 0..1 po zakończeniu ładowania
        splashP += (progress - splashP) * 0.06f;
        float pr = Math.max(0.03f, Math.min(1f, splashP));
        int cx = w / 2, cy = Math.round(h * 0.44f);
        float R = Math.min(w, h) * 0.15f;
        float open = 1f + done * 0.9f;                                         // przy końcu wszystko „rozkwita” na zewnątrz

        // ciemna winieta, żeby animacja była czytelna na każdej tapecie
        ctx.fillGradient(0, 0, w, h, Ui.fade(0x70000000, a), Ui.fade(0xB0000000, a));
        // miękka poświata za logo (kilka przezroczystych kół)
        float pulse = 0.85f + 0.15f * (float) Math.sin(tt * 2.1);
        for (int i = 7; i >= 1; i--) {
            double rr = R * (0.7 + i * 0.28) * open * pulse;
            Ui.rrectD(ctx, cx - rr, cy - rr, rr * 2, rr * 2, rr, Ui.fade(0x10FFFFFF | 0, a * k * (1f - done)));
        }
        // promienie światła obracające się za logo
        for (int i = 0; i < 14; i++) {
            float ang = tt * 0.12f + i * (float) (Math.PI * 2 / 14);
            int al = (int) (26 * a * k * (1f - done) * (0.6f + 0.4f * Math.sin(tt * 1.3 + i)));
            if (al < 2) continue;
            Compat.push(ctx);
            Compat.translate(ctx, cx, cy);
            Compat.rotate(ctx, ang);
            ctx.fillGradient(-1, Math.round(R * 0.9f), 1, Math.round(R * 4.4f * open), al << 24 | 0xFFFFFF, 0);
            Compat.pop(ctx);
        }
        // unoszące się drobinki światła
        for (int i = 0; i < 46; i++) {
            double sd = i * 7.31;
            double sx = (Math.sin(sd * 12.9898) * 43758.5453) % 1; sx = Math.abs(sx);
            double sp = 0.03 + (i % 7) * 0.012;
            double yy = ((sx * 3.7 + tt * sp + i * 0.137) % 1.0);
            double px = sx * w + Math.sin(tt * 0.7 + i) * 14;
            double py = h - yy * (h + 20);
            double tw = 0.5 + 0.5 * Math.sin(tt * 2.2 + i * 1.9);
            double ds = 1.2 + (i % 4) * 0.7;
            int al = (int) (150 * tw * a * k * (1f - done));
            if (al > 3) Ui.rrectD(ctx, px, py, ds, ds, ds / 2, al << 24 | (i % 5 == 0 ? 0xBFD8FF : 0xFFFFFF));
        }
        // fale pierścieni rozchodzące się od logo
        for (int i = 0; i < 4; i++) {
            float ph = ((tt / 2.8f) + i / 4f) % 1f;
            double rr = R * (1.05 + ph * 2.2) * open;
            int al = (int) (110 * Math.pow(1 - ph, 1.6) * a * k);
            if (al > 2) Ui.ringT(ctx, cx - rr, cy - rr, rr * 2, rr * 2, rr, 0.8, al << 24 | 0xFFFFFF);
        }
        // pierścień postępu z kropek (z „kometą” na końcu)
        int N = 84;
        double pr0 = R * 1.18 * open;
        for (int i = 0; i < N; i++) {
            double ang = -Math.PI / 2 + i * 2 * Math.PI / N;
            float fi = i / (float) N;
            boolean lit = fi <= pr;
            float head = Math.max(0f, 1f - Math.abs(fi - pr) * 14f);
            double ds = lit ? 2.3 + head * 2.6 : 1.5;
            int al = lit ? (int) (150 + 105 * head) : 55;
            double px0 = cx + Math.cos(ang) * pr0, py0 = cy + Math.sin(ang) * pr0;
            Ui.rrectD(ctx, px0 - ds / 2, py0 - ds / 2, ds, ds, ds / 2, Ui.fade(al << 24 | 0xFFFFFF, a * k));
        }
        // satelity krążące po orbitach (każdy z ogonem)
        for (int o = 0; o < 3; o++) {
            double orr = R * (1.55 + o * 0.38) * open;
            double sp = (o % 2 == 0 ? 1 : -1) * (0.9 - o * 0.2);
            double tilt = 0.32 + o * 0.12;
            for (int d = 0; d < 2; d++) for (int tail = 0; tail < 9; tail++) {
                double ang = tt * sp + d * Math.PI + o * 1.7 - tail * 0.07 * Math.signum(sp);
                double x = cx + Math.cos(ang) * orr, y = cy + Math.sin(ang) * orr * tilt;
                double ds = 4.2 - tail * 0.4;
                int al = (int) (230 * (1 - tail / 9f));
                Ui.rrectD(ctx, x - ds / 2, y - ds / 2, ds, ds, ds / 2, Ui.fade(al << 24 | (o == 1 ? 0xBFD8FF : 0xFFFFFF), a * k));
            }
        }
        // logo kaczki: wjeżdża z lekkim „sprężynowaniem” i delikatnie się unosi
        float spring = (float) (1 - Math.exp(-5 * (now / 1000f)) * Math.cos(9 * (now / 1000f)));
        float ls = R * 1.5f / 64f * spring * (1f + done * 0.25f);
        Compat.flush(ctx);
        Compat.alpha(a * k * (1f - done * 0.8f));
        Compat.push(ctx);
        Compat.translate(ctx, cx - 32 * ls, cy - 32 * ls + (float) Math.sin(tt * 1.6) * 2.5f);
        Compat.scale(ctx, ls, ls);
        Compat.tex(ctx, logoTexture(mc), 64, 64);
        Compat.pop(ctx);
        Compat.flush(ctx);
        Compat.alpha(1f);
        // napis KACZO pod spodem (gotowa tekstura – bez czcionki)
        float ts = Math.max(0.4f, Math.min(0.8f, h / 800f));
        float tx = ts * 0.5f;
        Compat.flush(ctx);
        Compat.alpha(a * ease((now - 500) / 900f) * (1f - done * 0.6f));
        Compat.push(ctx);
        Compat.translate(ctx, cx - 1024 * tx / 2f, cy + R * 2.2f * open - 128 * tx / 2f + (1 - ease((now - 500) / 900f)) * 12);
        Compat.scale(ctx, tx, tx);
        Compat.tex(ctx, titleTexture(mc), 1024, 256);
        Compat.pop(ctx);
        Compat.flush(ctx);
        Compat.alpha(1f);
        // szklany pasek ładowania z procentami
        float ba = a * ease((now - 700) / 800f) * (1f - done);
        if (ba > 0.01f) {
            int bw = Math.round(Math.min(w * 0.34f, 280f)), bx = cx - bw / 2, by = Math.round(cy + R * 2.2f * open + 128 * tx / 2f + 12);
            Ui.rrectD(ctx, bx, by, bw, 5, 2.5, Ui.fade(0x40FFFFFF, ba));
            Ui.rrectD(ctx, bx, by, Math.max(5, bw * pr), 5, 2.5, Ui.fade(0xFFFFFFFF, ba));
            Ui.rrectD(ctx, bx + Math.max(5, bw * pr) - 7, by - 3, 9, 11, 4.5, Ui.fade(0x66FFFFFF, ba));
            String pt = Math.round(pr * 100) + "%";
            ctx.drawText(mc.textRenderer, pt, cx - mc.textRenderer.getWidth(pt) / 2, by + 11, Ui.fade(0xFFC8D0E0, ba), false);
        }
        // błyski gwiazdek wokół logo
        for (int i = 0; i < 6; i++) {
            double ph = (tt * 0.55 + i * 0.37) % 1.0;
            double ang = i * 1.9 + 0.6, rr = R * (0.8 + (i % 3) * 0.28);
            double sx = cx + Math.cos(ang) * rr, sy = cy + Math.sin(ang) * rr * 0.9;
            double sz = Math.sin(ph * Math.PI) * (3 + (i % 3) * 1.5);
            int al = (int) (230 * Math.sin(ph * Math.PI) * a * k * (1f - done));
            if (al < 6) continue;
            Ui.rrectD(ctx, sx - sz, sy - 0.6, sz * 2, 1.2, 0.6, al << 24 | 0xFFFFFF);
            Ui.rrectD(ctx, sx - 0.6, sy - sz, 1.2, sz * 2, 0.6, al << 24 | 0xFFFFFF);
        }
        // finałowy błysk przy wejściu do menu
        if (done > 0f && done < 0.5f) ctx.fill(0, 0, w, h, Ui.fade(0xFFFFFFFF, (1f - done * 2f) * 0.28f * a));
    }

    private static float splashP;
}
