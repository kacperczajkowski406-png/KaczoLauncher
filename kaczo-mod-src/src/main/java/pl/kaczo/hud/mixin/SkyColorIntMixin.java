package pl.kaczo.hud.mixin;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.kaczo.hud.Sky;

/** Prawdziwe niebo (1.21.2+: kolor jako int ARGB). Nie wymagany – na innych wersjach cicho się pomija. */
@Mixin(ClientWorld.class)
public class SkyColorIntMixin {
    @Inject(method = "getSkyColor(Lnet/minecraft/util/math/Vec3d;F)I", at = @At("RETURN"), cancellable = true, require = 0)
    private void kaczo$sky(CallbackInfoReturnable<Integer> cir) {
        try {
            int c = cir.getReturnValue();
            int o = Sky.worldColor(c & 0xFFFFFF);
            if ((o & 0xFFFFFF) != (c & 0xFFFFFF)) cir.setReturnValue((c & 0xFF000000) | (o & 0xFFFFFF));
        } catch (Throwable ignored) { }
    }
}
