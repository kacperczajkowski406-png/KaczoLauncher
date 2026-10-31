package pl.kaczo.hud.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.kaczo.hud.Modules;

/** Moduły „Bez drgania kamery po obrażeniach” i „Bez bujania widoku”. */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true, require = 0)
    private void kaczo$noHurt(CallbackInfo ci) {
        try { if (Modules.byId("nohurtcam").enabled()) ci.cancel(); } catch (Throwable ignored) { }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true, require = 0)
    private void kaczo$noBob(CallbackInfo ci) {
        try { if (Modules.byId("nobob").enabled()) ci.cancel(); } catch (Throwable ignored) { }
    }
}
