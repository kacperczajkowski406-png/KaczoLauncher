package pl.kaczo.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.RenderPipelines;

/** Różnice API Minecrafta między wersjami (1.21.6 – 1.21.8 (eksperymentalne)). */
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
    public static void scale(DrawContext ctx, float x, float y) { ctx.getMatrices().scale(x, y); }
    public static void flush(DrawContext ctx) { }
    public static void alpha(float a) { }

    // --- różnice dodane dla wspólnego kodu (klawisze, skróty, hitboxy) ---
    public static boolean keyDown(net.minecraft.client.MinecraftClient mc, int code) {
        return net.minecraft.client.util.InputUtil.isKeyPressed(mc.getWindow().getHandle(), code);
    }
    public static net.minecraft.client.option.KeyBinding newKey(String id, int code) {
        return new net.minecraft.client.option.KeyBinding(id, net.minecraft.client.util.InputUtil.Type.KEYSYM, code, "key.categories.kaczohud");
    }
    public static void hitboxes(net.minecraft.client.MinecraftClient mc, boolean on) {
        mc.getEntityRenderDispatcher().setRenderHitboxes(on);
    }
    public static void border(net.minecraft.client.gui.DrawContext ctx, int x, int y, int w, int h, int col) { ctx.drawBorder(x, y, w, h, col); }
}
