package pl.kaczo.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.RenderPipelines;

/** Różnice API Minecrafta między wersjami (1.21.9 – 1.21.11 (eksperymentalne)). */
public final class Compat {
    private Compat() { }

    public static void tex(DrawContext ctx, Identifier id, int w, int h) {
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, id, 0, 0, 0f, 0f, w, h, w, h);
    }
    public static void pixel(NativeImage ni, int x, int y, int argb) { ni.setColorArgb(x, y, argb); }
    public static NativeImageBackedTexture newTexture(NativeImage ni) { return new NativeImageBackedTexture(() -> "kaczohud_cover", ni); }

    // od 1.21.6 macierze GUI są 2D, a przezroczystość/flush załatwia sam renderer – animacje bez płynnego zanikania
    public static void push(DrawContext ctx) { ctx.getMatrices().pushMatrix(); }
    public static void pop(DrawContext ctx) { ctx.getMatrices().popMatrix(); }
    public static void translate(DrawContext ctx, float x, float y) { ctx.getMatrices().translate(x, y); }
    public static void rotate(DrawContext ctx, float rad) { ctx.getMatrices().rotate(rad); }
    public static void scale(DrawContext ctx, float x, float y) { ctx.getMatrices().scale(x, y); }
    public static void flush(DrawContext ctx) { }
    public static void alpha(float a) { }

    // --- różnice dodane dla wspólnego kodu (klawisze, skróty, hitboxy) ---
    public static boolean keyDown(net.minecraft.client.MinecraftClient mc, int code) {
        return net.minecraft.client.util.InputUtil.isKeyPressed(mc.getWindow(), code);
    }
    private static net.minecraft.client.option.KeyBinding.Category cat;
    /** Kategorię można zarejestrować tylko raz – wszystkie klawisze moda dzielą jedną. */
    public static net.minecraft.client.option.KeyBinding newKey(String id, int code) {
        if (cat == null) cat = net.minecraft.client.option.KeyBinding.Category.create(net.minecraft.util.Identifier.of("kaczohud", "main"));
        return new net.minecraft.client.option.KeyBinding(id, code, cat);
    }
    /** Od 1.21.9 hitboxy rysuje debugowy renderer – ten moduł jest tu bez efektu. */
    public static void hitboxes(net.minecraft.client.MinecraftClient mc, boolean on) { }
    public static void border(net.minecraft.client.gui.DrawContext ctx, int x, int y, int w, int h, int col) { ctx.drawStrokedRectangle(x, y, w, h, col); }
    public static net.minecraft.text.Text clockText(String s) {
        return net.minecraft.text.Text.literal(s).formatted(net.minecraft.util.Formatting.BOLD);
    }
    /** Tekstura z tintem rgb i przezroczystością a (0..1). */
    public static void texA(DrawContext ctx, Identifier id, int w, int h, int rgb, float a) {
        int al = Math.max(0, Math.min(255, Math.round(a * 255)));
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, id, 0, 0, 0f, 0f, w, h, w, h, (al << 24) | (rgb & 0xFFFFFF));
    }
}
