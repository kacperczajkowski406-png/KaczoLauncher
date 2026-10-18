package pl.kaczo.hud.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.kaczo.hud.Extras;

/** Podczas widoku wolnego ruch myszy zmienia kamerę, a nie kierunek patrzenia gracza. */
@Mixin(Entity.class)
public class EntityLookMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true, require = 0)
    private void kaczo$look(double dx, double dy, CallbackInfo ci) {
        try {
            if (Extras.freelookActive() && Extras.camHookWorks && (Object) this == MinecraftClient.getInstance().player) {
                Extras.look(dx, dy);
                ci.cancel();
            }
        } catch (Throwable ignored) { }
    }
}
