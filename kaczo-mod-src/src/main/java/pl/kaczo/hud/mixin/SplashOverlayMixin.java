package pl.kaczo.hud.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.kaczo.hud.Glass;
import pl.kaczo.hud.Visual;

/** Rysuje własny, animowany ekran ładowania na wierzch standardowego (czerwonego) ekranu Mojang. */
@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {
    @Shadow private float progress;
    @Shadow private long reloadCompleteTime;

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void kaczo$splash(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try { if (Glass.loadScreenOn()) Visual.splash(ctx, progress, reloadCompleteTime); } catch (Throwable ignored) { }
    }
}
