package pl.kaczo.hud;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** Różnice API Minecrafta 26.x (nazwy Mojang, rysowanie „extract”). Ręcznie pisany odpowiednik Compat z wariantów yarn. */
public final class Compat {
    private Compat() { }

    public static void tex(GuiGraphicsExtractor ctx, Identifier id, int w, int h) {
        ctx.blit(RenderPipelines.GUI_TEXTURED, id, 0, 0, 0f, 0f, w, h, w, h);
    }
    public static void pixel(NativeImage ni, int x, int y, int argb) { ni.setPixel(x, y, argb); }
    public static DynamicTexture newTexture(NativeImage ni) { return new DynamicTexture(() -> "kaczohud_cover", ni); }

    public static void push(GuiGraphicsExtractor ctx) { ctx.pose().pushMatrix(); }
    public static void pop(GuiGraphicsExtractor ctx) { ctx.pose().popMatrix(); }
    public static void translate(GuiGraphicsExtractor ctx, float x, float y) { ctx.pose().translate(x, y); }
    public static void scale(GuiGraphicsExtractor ctx, float x, float y) { ctx.pose().scale(x, y); }
    public static void flush(GuiGraphicsExtractor ctx) { }
    public static void alpha(float a) { }

    public static boolean keyDown(Minecraft mc, int code) { return InputConstants.isKeyDown(mc.getWindow(), code); }
    private static KeyMapping.Category cat;
    public static KeyMapping newKey(String id, int code) {
        if (cat == null) cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("kaczohud", "main"));
        return new KeyMapping(id, code, cat);
    }
    public static void hitboxes(Minecraft mc, boolean on) { }
    public static void border(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int col) { ctx.outline(x, y, w, h, col); }
}
