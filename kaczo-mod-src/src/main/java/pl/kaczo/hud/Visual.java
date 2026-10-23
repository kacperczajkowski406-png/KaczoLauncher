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

    /** Tło w stylu „liquid glass”: czerń i płynne, białe, szklane wstęgi. */
    public static void aurora(DrawContext ctx, int w, int h, float a) {
        float t = (System.currentTimeMillis() - T0) / 1000f;
        ctx.fillGradient(0, 0, w, h, Ui.fade(0xFF000000, a), Ui.fade(0xFF090A0E, a));
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
        float k = ease(now / 700f);
        net.minecraft.client.font.TextRenderer tr = mc.textRenderer;
        int cx = w / 2, cy = h / 2;
        // duży, czysty napis jak zegar na ekranie blokady
        String title = "Kaczo Client";
        float ts = Math.max(2f, Math.min(5f, h / 120f));
        int tw = tr.getWidth(title);
        Compat.push(ctx);
        Compat.translate(ctx, cx - tw * ts / 2f, cy - 5 * ts - 6 + (1 - k) * 10);
        Compat.scale(ctx, ts, ts);
        ctx.drawText(tr, title, 0, 0, Ui.fade(0xFFFFFFFF, a * k), false);
        Compat.pop(ctx);
        String sub = Math.round(Math.max(0.04f, Math.min(1f, progress)) * 100) + "%";
        ctx.drawText(tr, sub, cx - tr.getWidth(sub) / 2, cy + (int) (6 * ts) + 2, Ui.fade(0xFFB9BECB, a * k), false);

        // cienki, szklany pasek postępu
        int bw = Math.min(170, w / 2), bh = 3, bx = cx - bw / 2, by = cy + (int) (6 * ts) + 18;
        Ui.glass(ctx, bx - 1, by - 1, bw + 2, bh + 2, (bh + 2) / 2, Ui.fade(0x30FFFFFF, a));
        float pr = Math.max(0.04f, Math.min(1f, progress));
        int fw = Math.max(bh, Math.round(bw * pr));
        Ui.rrect(ctx, bx, by, fw, bh, bh / 2, Ui.fade(0xFFFFFFFF, a));
        double sh = (now / 1100.0) % 1.0;                                  // połysk przesuwający się po wypełnieniu
        int sx = bx + (int) (sh * fw);
        Ui.rrect(ctx, Math.max(bx, sx - 10), by, Math.min(20, bx + fw - Math.max(bx, sx - 10)), bh, bh / 2, Ui.fade(0x90FFFFFF, a));
    }
}
