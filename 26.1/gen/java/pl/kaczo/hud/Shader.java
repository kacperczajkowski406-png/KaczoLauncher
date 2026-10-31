package pl.kaczo.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;

import java.io.InputStream;

/**
 * „Shadery” moda: nakładka na cały obraz gry w stylu ciekłego szkła (czarno-białe, błyszczące wstęgi, ciemniejszy obraz, winieta).
 * To nie jest prawdziwy shader GPU – działa na każdej wersji i nie koliduje z Iris/Sodium.
 */
public final class Shader {
    private Shader() { }

    public static final String[] STYLES = {"Liquid glass", "Noir", "Silk"};
    private static Identifier ov;

    private static Identifier overlay(Minecraft mc) {
        if (ov == null) {
            try (InputStream in = Shader.class.getResourceAsStream("/assets/kaczohud/textures/gui/glass_ov.png")) {
                NativeImage ni = NativeImage.read(in);
                Identifier id = Identifier.fromNamespaceAndPath("kaczohud", "glass_ov");
                mc.getTextureManager().register(id, Compat.newTexture(ni));
                ov = id;
            } catch (Throwable t) { return null; }
        }
        return ov;
    }

    public static void render(GuiGraphicsExtractor ctx, Minecraft mc, Module m, float t) {
        int w = ctx.guiWidth(), h = ctx.guiHeight();
        int style = Math.max(0, Math.min(STYLES.length - 1, m.num("style")));
        float k = m.num("intensity") / 100f;
        // przyciemnienie (klimat „czarnego szkła”)
        if (m.bool("grade")) {
            int dark = style == 1 ? 120 : style == 0 ? 60 : 24;
            ctx.fill(0, 0, w, h, ((int) (dark * k) << 24) | 0x000008);
        }
        // winieta
        int vg = m.num("vignette");
        if (vg > 0) {
            int steps = 14;
            for (int i = 0; i < steps; i++) {
                float f = (float) Math.pow(1f - i / (float) steps, 2.2);
                int a = (int) (vg / 100f * 70 * f * k * 1.4f);
                if (a <= 0) continue;
                int dx = (int) (w * 0.5f * (i / (float) steps) * 0.55f), dy = (int) (h * 0.5f * (i / (float) steps) * 0.55f);
                int col = (Math.min(255, a) << 24);
                ctx.fill(dx, dy, w - dx, dy + Math.max(1, h / 90), col);
                ctx.fill(dx, h - dy - Math.max(1, h / 90), w - dx, h - dy, col);
                ctx.fill(dx, dy, dx + Math.max(1, w / 90), h - dy, col);
                ctx.fill(w - dx - Math.max(1, w / 90), dy, w - dx, h - dy, col);
            }
        }
        // szklane wstęgi
        Identifier id = overlay(mc);
        if (id == null) return;
        float ribbon = style == 2 ? 0.75f : style == 1 ? 0.28f : 0.45f;
        float sp = m.num("speed") / 50f;
        float s = Math.max(w / 1280f, h / 720f) * (1.08f + 0.015f * (float) Math.sin(t * 0.25));
        float ox = (w - 1280 * s) / 2f + (float) Math.sin(t * 0.21 * sp) * w * 0.015f;
        float oy = (h - 720 * s) / 2f + (float) Math.cos(t * 0.17 * sp) * h * 0.015f;
        int tint = 0xFFFFFF;
        try { int c = m.colorRaw("tint"); if ((c >>> 24) > 8) tint = c & 0xFFFFFF; } catch (Throwable ignored) { }
        Compat.push(ctx);
        Compat.translate(ctx, ox, oy);
        Compat.scale(ctx, s, s);
        Compat.texA(ctx, id, 1280, 720, tint, Math.min(1f, ribbon * k * 1.6f));
        Compat.pop(ctx);
    }
}
