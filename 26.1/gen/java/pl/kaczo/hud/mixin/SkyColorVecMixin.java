package pl.kaczo.hud.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.kaczo.hud.Sky;

/** Prawdziwe niebo (1.21–1.21.1: kolor jako Vec3). Nie wymagany – na innych wersjach cicho się pomija. */
@Mixin(ClientLevel.class)
public class SkyColorVecMixin {
    @Inject(method = "getSkyColor(Lnet/minecraft/util/math/Vec3;F)Lnet/minecraft/util/math/Vec3;", at = @At("RETURN"), cancellable = true, require = 0)
    private void kaczo$sky(CallbackInfoReturnable<Vec3> cir) {
        try {
            Vec3 v = cir.getReturnValue();
            int rgb = ((int) (v.x * 255) << 16) | ((int) (v.y * 255) << 8) | (int) (v.z * 255);
            int o = Sky.worldColor(rgb);
            if (o != rgb) cir.setReturnValue(new Vec3(((o >> 16) & 255) / 255.0, ((o >> 8) & 255) / 255.0, (o & 255) / 255.0));
        } catch (Throwable ignored) { }
    }
}
