package pl.kaczo.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.io.InputStream;

/** Animowane tło („zorza”) ekranu tytułowego oraz własny ekran ładowania gry zamiast czerwonego ekranu Mojang. */
public final class Visual {
    private Visual() { }

    private static Identifier splashLogo;
    private static final long T0 = System.currentTimeMillis();

    private static Identifier bgTex;
    private static Identifier bgTexture(Minecraft mc) {
        if (bgTex == null) {
            try (InputStream in = Visual.class.getResourceAsStream("/assets/kaczohud/textures/gui/glass_bg.png")) {
                NativeImage ni = NativeImage.read(in);
                Identifier id = Identifier.fromNamespaceAndPath("kaczohud", "glass_bg");
                mc.getTextureManager().register(id, Compat.newTexture(ni));
                bgTex = id;
            } catch (Throwable t) { return null; }
        }
        return bgTex;
    }

    /** Tło w stylu „liquid glass”: czerń i gładkie, srebrne wstęgi jedwabiu (gotowa tekstura, leciutko pływająca). */
    public static void aurora(GuiGraphicsExtractor ctx, int w, int h, float a) {
        float t = (System.currentTimeMillis() - T0) / 1000f;
        ctx.fillGradient(0, 0, w, h, Ui.fade(0xFF000000, a), Ui.fade(0xFF000000, a));
        Identifier id = bgTexture(Minecraft.getInstance());
        if (id == null) { Ribbons.draw(ctx, w, h, t, a * 0.9f, 3); return; }
        float s = Math.max(w / 1920f, h / 1080f) * (1.07f + 0.015f * (float) Math.sin(t * 0.25));
        float ox = (w - 1920 * s) / 2f + (float) Math.sin(t * 0.21) * w * 0.012f;
        float oy = (h - 1080 * s) / 2f + (float) Math.cos(t * 0.17) * h * 0.012f;
        Compat.flush(ctx);
        Compat.alpha(a);
        Compat.push(ctx);
        Compat.translate(ctx, ox, oy);
        Compat.scale(ctx, s, s);
        Compat.tex(ctx, id, 1920, 1080);
        Compat.pop(ctx);
        Compat.flush(ctx);
        Compat.alpha(1f);
    }

    private static Identifier logoTexture(Minecraft mc) {
        if (splashLogo == null) {
            // podczas pierwszego ładowania zasobów tekstury moda mogą jeszcze nie być gotowe – wczytujemy logo sami z pliku .jar
            try (InputStream in = Visual.class.getResourceAsStream("/assets/kaczohud/textures/gui/logo.png")) {
                NativeImage ni = NativeImage.read(in);
                Identifier id = Identifier.fromNamespaceAndPath("kaczohud", "splash_logo");
                mc.getTextureManager().register(id, Compat.newTexture(ni));
                splashLogo = id;
            } catch (Throwable t) {
                splashLogo = Modules.logo();
            }
        }
        return splashLogo;
    }

    private static Identifier titleTex;
    private static Identifier titleTexture(Minecraft mc) {
        if (titleTex == null) {
            try (InputStream in = Visual.class.getResourceAsStream("/assets/kaczohud/textures/gui/title.png")) {
                NativeImage ni = NativeImage.read(in);
                Identifier id = Identifier.fromNamespaceAndPath("kaczohud", "splash_title");
                mc.getTextureManager().register(id, Compat.newTexture(ni));
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
    public static void splash(Object inst, GuiGraphicsExtractor ctx, float progress, long completeAt) {
        Minecraft mc = Minecraft.getInstance();
        long nowMs = System.currentTimeMillis();
        if (inst != curOverlay) { curOverlay = inst; overlayT0 = nowMs; }
        float f = completeAt > -1L ? (Util.getMillis() - completeAt) / 1000f : -1f;
        float a = f < 1f ? 1f : 1f - Math.max(0f, Math.min(1f, f - 1f));
        a = Math.min(1f, a * 1.5f);      // znika odrobinę szybciej niż oryginał, żeby nie prześwitywało logo Mojang
        a *= Math.min(1f, (nowMs - overlayT0) / 380f);   // płynne wejście
        if (a <= 0f) return;
        if (completeAt > -1L) progress = 1f;
        // najpierw dorysuj wszystko, co było wcześniej (ekran pod spodem) – inaczej jego tekst wyskakuje NA WIERZCH naszego tła
        Compat.flush(ctx);
        int w = ctx.guiWidth(), h = ctx.guiHeight();
        aurora(ctx, w, h, a);

        long now = System.currentTimeMillis() - T0;
        float k = ease(now / 700f);
        int cx = w / 2, cy = h / 2;
        // duży napis jak zegar na ekranie blokady – jako gotowa tekstura (bez czcionki i shaderów tekstu, których podczas ładowania jeszcze nie ma)
        float ts = Math.max(0.5f, Math.min(1.1f, h / 560f));
        float tw2 = 1024 * ts * 0.5f, th2 = 256 * ts * 0.5f;
        Compat.flush(ctx);
        Compat.alpha(a * k);
        Compat.push(ctx);
        Compat.translate(ctx, cx - tw2 / 2f, cy - th2 / 2f - 6 + (1 - k) * 10);
        Compat.scale(ctx, ts * 0.5f, ts * 0.5f);
        Compat.tex(ctx, titleTexture(mc), 1024, 256);
        Compat.pop(ctx);
        Compat.flush(ctx);
        Compat.alpha(1f);

        // cienki, szklany pasek postępu
        int bw = Math.min(170, w / 2), bh = 3, bx = cx - bw / 2, by = cy + (int) (th2 / 2) + 14;
        Ui.glass(ctx, bx - 1, by - 1, bw + 2, bh + 2, (bh + 2) / 2, Ui.fade(0x30FFFFFF, a));
        float pr = Math.max(0.04f, Math.min(1f, progress));
        int fw = Math.max(bh, Math.round(bw * pr));
        Ui.rrect(ctx, bx, by, fw, bh, bh / 2, Ui.fade(0xFFFFFFFF, a));
        double sh = (now / 1100.0) % 1.0;                                  // połysk przesuwający się po wypełnieniu
        int sx = bx + (int) (sh * fw);
        Ui.rrect(ctx, Math.max(bx, sx - 10), by, Math.min(20, bx + fw - Math.max(bx, sx - 10)), bh, bh / 2, Ui.fade(0x90FFFFFF, a));
    }
}
