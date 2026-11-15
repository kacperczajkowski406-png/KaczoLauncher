package pl.kaczo.hud.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import pl.kaczo.hud.Extras;

/** Widok wolny (Perspective): kamera obraca się niezależnie od gracza. */
@Mixin(Camera.class)
public class CameraMixin {
    @Inject(method = "update", at = @At("HEAD"), require = 0)
    private void kaczo$start(CallbackInfo ci) { Extras.camStart(); }

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"), require = 0)
    private void kaczo$rot(Args args) {
        try {
            if (Extras.freelookActive() && Extras.camFirst()) {
                args.set(0, Extras.flYaw);
                args.set(1, Extras.flPitch);
                Extras.camHookWorks = true;
            }
        } catch (Throwable ignored) { }
    }
}
