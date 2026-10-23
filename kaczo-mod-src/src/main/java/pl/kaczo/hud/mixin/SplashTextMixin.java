package pl.kaczo.hud.mixin;

import net.minecraft.client.gui.screen.SplashTextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.kaczo.hud.Glass;

/** Żółty „splash” Minecrafta nie pasuje do stylu ekranu blokady – nie rysujemy go, gdy działa szklane menu. */
@Mixin(SplashTextRenderer.class)
public class SplashTextMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void kaczo$noSplash(CallbackInfo ci) {
        try { if (Glass.buttonsOn()) ci.cancel(); } catch (Throwable ignored) { }
    }
}
