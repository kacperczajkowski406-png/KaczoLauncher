package pl.kaczo.hud.mixin;

import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.kaczo.hud.Modules;

/** Moduły „Bez ognia na ekranie” i „Czysta woda” (bez nakładki pod wodą). */
@Mixin(ScreenEffectRenderer.class)
public class OverlayMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void kaczo$fire(CallbackInfo ci) {
        try { if (Modules.byId("nofire").enabled()) ci.cancel(); } catch (Throwable ignored) { }
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void kaczo$water(CallbackInfo ci) {
        try { if (Modules.byId("nowater").enabled()) ci.cancel(); } catch (Throwable ignored) { }
    }
}
