package pl.kaczo.hud.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.kaczo.hud.Glass;

/** Na ekranie tytułowym (i tylko tam) pomija standardową panoramę – zamiast niej widać animowane tło moda. */
@Mixin(RotatingCubeMapRenderer.class)
public class PanoramaMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void kaczo$noPanorama(CallbackInfo ci) {
        try {
            if (Glass.titleBgOn() && MinecraftClient.getInstance().currentScreen instanceof TitleScreen) ci.cancel();
        } catch (Throwable ignored) { }
    }
}
