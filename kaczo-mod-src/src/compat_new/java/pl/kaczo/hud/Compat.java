package pl.kaczo.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.RenderLayer;

/** Różnice API Minecrafta między wersjami (1.21.2 – 1.21.5). */
public final class Compat {
    private Compat() { }

    public static void tex(DrawContext ctx, Identifier id, int w, int h) {
        ctx.drawTexture(RenderLayer::getGuiTextured, id, 0, 0, 0f, 0f, w, h, w, h);
    }
    public static void pixel(NativeImage ni, int x, int y, int argb) { ni.setColorArgb(x, y, argb); }
    public static NativeImageBackedTexture newTexture(NativeImage ni) { return new NativeImageBackedTexture(ni); }

    public static void push(DrawContext ctx) { ctx.getMatrices().push(); }
    public static void pop(DrawContext ctx) { ctx.getMatrices().pop(); }
    public static void translate(DrawContext ctx, float x, float y) { ctx.getMatrices().translate(x, y, 0f); }
    public static void scale(DrawContext ctx, float x, float y) { ctx.getMatrices().scale(x, y, 1f); }
    /** Dorysowuje zebrane elementy (potrzebne, by przezroczystość nie objęła wcześniejszych). */
    public static void flush(DrawContext ctx) { ctx.draw(); }
    public static void alpha(float a) { RenderSystem.setShaderColor(1f, 1f, 1f, a); }

}
