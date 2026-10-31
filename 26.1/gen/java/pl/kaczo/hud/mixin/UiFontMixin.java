package pl.kaczo.hud.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import pl.kaczo.hud.Glass;

/** Czcionka Inter tylko w menu głównym i w oknach moda – reszta gry zostaje z domyślną czcionką Minecrafta. */
@Mixin(Font.class)
public class UiFontMixin {
    @ModifyVariable(method = "getFontStorage", at = @At("HEAD"), argsOnly = true, require = 0)
    private Identifier kaczo$font(Identifier id) {
        try { if (Glass.interNow() && "default".equals(id.getPath()) && "minecraft".equals(id.getNamespace())) return Glass.UI_FONT; } catch (Throwable ignored) { }
        return id;
    }
}
