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

    /** Płynnie przesuwające się kolorowe poświaty na ciemnym gradiencie. */
    public static void aurora(DrawContext ctx, int w, int h, float a) {
        double t = (System.currentTimeMillis() - T0) / 1000.0;
        ctx.fillGradient(0, 0, w, h, Ui.fade(0xFF0A1030, a), Ui.fade(0xFF1D0E3C, a));
        double rad = Math.min(w, h) * 0.46;
        int[] rgb = {0x5078FF, 0xFF5AC8, 0x28DCC8, 0xFFBE46};
        double[][] p = {{0.18, 0.22, 7.0}, {0.82, 0.18, 9.0}, {0.72, 0.88, 8.0}, {0.12, 0.85, 10.0}};
        for (int i = 0; i < 4; i++) {
            double cx = w * (p[i][0] + 0.10 * Math.sin(t / p[i][2] * 2 + i * 1.7));
            double cy = h * (p[i][1] + 0.08 * Math.cos(t / p[i][2] * 2.3 + i));
            Ui.blob(ctx, cx, cy, rad, rgb[i], i == 3 ? 7 : 10, a);
        }
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

    private static float ease(float t) { t = Math.max(0, Math.min(1, t)); float u = 1 - t; return 1 - u * u * u; }

    /**
     * Ekran ładowania gry: poświata, pulsujące logo z pierścieniami i szklany pasek postępu.
     * Rysowany NA wierzch standardowego ekranu Mojang (który ma wtedy kolor czerwony), z tą samą logiką znikania.
     */
    public static void splash(DrawContext ctx, float progress, long completeAt) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float f = completeAt > -1L ? (Util.getMeasuringTimeMs() - completeAt) / 1000f : -1f;
        float a = f < 1f ? 1f : 1f - Math.max(0f, Math.min(1f, f - 1f));
        a = Math.min(1f, a * 1.7f);      // znika odrobinę szybciej niż oryginał, żeby nie prześwitywała czerwień
        if (a <= 0f) return;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        aurora(ctx, w, h, a);

        long now = System.currentTimeMillis() - T0;
        float k = ease(now / 700f);
        int size = Math.max(28, Math.min(64, h / 4));
        int cx = w / 2, cy = h / 2 - 12 + (int) Math.round(Math.sin(now / 800.0) * 2);
        for (int r = 0; r < 3; r++) {
            double ph = ((now / 2600.0) + r / 3.0) % 1.0;
            double d = size * (0.7 + ph * 1.0);
            int ra = (int) (80 * (1 - ph) * k * a);
            if (ra > 2) Ui.ring(ctx, cx - d / 2, cy - d / 2, d, d, d / 2, (ra << 24) | 0xFFFFFF);
        }
        int gs = size + 18;
        double pulse = 0.5 + 0.5 * Math.sin(now / 650.0);
        Ui.rrect(ctx, cx - gs / 2, cy - gs / 2, gs, gs, gs / 2, Ui.fade(((int) (20 + 16 * pulse) << 24) | 0xFFE14A, a));
        float s = size / 64f * (0.55f + 0.45f * k);
        Compat.flush(ctx);
        Compat.alpha(a * k);
        Compat.push(ctx);
        Compat.translate(ctx, cx - 32 * s, cy - 32 * s);
        Compat.scale(ctx, s, s);
        Compat.tex(ctx, logoTexture(mc), 64, 64);
        Compat.pop(ctx);
        Compat.flush(ctx);
        Compat.alpha(1f);

        // szklany pasek postępu
        int bw = Math.min(190, w / 2), bh = 6, bx = cx - bw / 2, by = cy + size / 2 + 22;
        Ui.glass(ctx, bx - 2, by - 2, bw + 4, bh + 4, (bh + 4) / 2, Ui.fade(0x2EFFFFFF, a));
        float pr = Math.max(0.04f, Math.min(1f, progress));
        int fw = Math.max(bh, Math.round(bw * pr));
        Ui.rrect(ctx, bx, by, fw, bh, bh / 2, Ui.fade(0xFFFFE14A, a));
        double sh = (now / 900.0) % 1.0;                                  // połysk przesuwający się po wypełnieniu
        int sx = bx + (int) (sh * fw);
        Ui.rrect(ctx, Math.max(bx, sx - 8), by, Math.min(16, bx + fw - Math.max(bx, sx - 8)), bh, bh / 2, Ui.fade(0x70FFFFFF, a));
    }
}
