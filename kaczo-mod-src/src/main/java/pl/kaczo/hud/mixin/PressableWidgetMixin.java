package pl.kaczo.hud.mixin;

import net.minecraft.client.gui.widget.PressableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.kaczo.hud.Glass;

/** Wyłącza oryginalną grafikę zwykłych przycisków, gdy rysuje je szklana nakładka moda. */
@Mixin(PressableWidget.class)
public class PressableWidgetMixin {
    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true, require = 0)
    private void kaczo$glass(CallbackInfo ci) {
        try { if (Glass.hideVanilla(this)) ci.cancel(); } catch (Throwable ignored) { }
    }
}
