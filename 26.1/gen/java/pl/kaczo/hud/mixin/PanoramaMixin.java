package pl.kaczo.hud.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Panorama;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.kaczo.hud.Glass;

/** Na ekranie tytułowym (i tylko tam) pomija standardową panoramę – zamiast niej widać animowane tło moda. */
@Mixin(Panorama.class)
public class PanoramaMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void kaczo$noPanorama(CallbackInfo ci) {
        try {
            if (Glass.titleBgOn() && Minecraft.getInstance().screen instanceof TitleScreen) ci.cancel();
        } catch (Throwable ignored) { }
    }
}
