package pl.kaczo.hud.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.kaczo.hud.Glass;
import pl.kaczo.hud.Visual;

/** Własny, animowany ekran ładowania zamiast czerwonego ekranu Mojang. */
@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {
    @Shadow private float progress;
    @Shadow private long reloadCompleteTime;

    /** Rysujemy na wierzch całego ekranu Mojang (po dorysowaniu tego, co było pod spodem). */
    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void kaczo$splash(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try { if (Glass.loadScreenOn()) Visual.splash(this, ctx, progress, reloadCompleteTime); } catch (Throwable ignored) { }
    }

    /**
     * Czerwone tło Mojang to wypełnienie kolorem z funkcji withAlpha – robimy je całkiem przezroczystym.
     * Dzięki temu czerwień nie miesza się z naszym tłem ani z ekranem widocznym pod spodem (np. przy przeładowaniu zasobów).
     */
    @Inject(method = "withAlpha", at = @At("RETURN"), cancellable = true, require = 0)
    private static void kaczo$noRed(int color, int alpha, CallbackInfoReturnable<Integer> cir) {
        try { if (Glass.loadScreenOn()) cir.setReturnValue(0); } catch (Throwable ignored) { }
    }
}
