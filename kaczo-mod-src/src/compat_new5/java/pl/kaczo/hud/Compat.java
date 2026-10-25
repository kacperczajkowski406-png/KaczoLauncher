package pl.kaczo.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.RenderLayer;

/** Różnice API Minecrafta między wersjami (tylko 1.21.5: nowy konstruktor tekstury). */
public final class Compat {
    private Compat() { }

    public static void tex(DrawContext ctx, Identifier id, int w, int h) {
        ctx.drawTexture(RenderLayer::getGuiTextured, id, 0, 0, 0f, 0f, w, h, w, h);
    }
    public static void pixel(NativeImage ni, int x, int y, int argb) { ni.setColorArgb(x, y, argb); }
    public static NativeImageBackedTexture newTexture(NativeImage ni) { return new NativeImageBackedTexture(() -> "kaczohud_cover", ni); }

    public static void push(DrawContext ctx) { ctx.getMatrices().push(); }
    public static void pop(DrawContext ctx) { ctx.getMatrices().pop(); }
    public static void translate(DrawContext ctx, float x, float y) { ctx.getMatrices().translate(x, y, 0f); }
    public static void rotate(DrawContext ctx, float rad) { ctx.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation(rad)); }
    public static void scale(DrawContext ctx, float x, float y) { ctx.getMatrices().scale(x, y, 1f); }
    /** Dorysowuje zebrane elementy (potrzebne, by przezroczystość nie objęła wcześniejszych). */
    public static void flush(DrawContext ctx) { ctx.draw(); }
    public static void alpha(float a) { RenderSystem.setShaderColor(1f, 1f, 1f, a); }

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
    public static net.minecraft.text.Text clockText(String s) {
        return net.minecraft.text.Text.literal(s).setStyle(net.minecraft.text.Style.EMPTY.withFont(Identifier.of("kaczohud", "clock")));
    }
    /** Tekstura z tintem rgb i przezroczystością a (0..1). */
    public static void texA(DrawContext ctx, Identifier id, int w, int h, int rgb, float a) {
        ctx.draw();
        RenderSystem.setShaderColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, a);
        tex(ctx, id, w, h);
        ctx.draw();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
