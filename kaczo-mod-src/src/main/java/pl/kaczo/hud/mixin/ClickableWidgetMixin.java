package pl.kaczo.hud.mixin;

import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.kaczo.hud.Glass;

/**
 * Wyłącza oryginalną grafikę przycisków, które na ekranie tytułowym maluje szklana nakładka moda
 * (także przyciski z innych modów, np. „Mody”, oraz ikony języka i ułatwień dostępu).
 */
@Mixin(ClickableWidget.class)
public class ClickableWidgetMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void kaczo$glass(CallbackInfo ci) {
        try { if (Glass.hideVanilla(this)) ci.cancel(); } catch (Throwable ignored) { }
    }
}
