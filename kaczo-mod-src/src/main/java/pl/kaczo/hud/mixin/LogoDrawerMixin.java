package pl.kaczo.hud.mixin;

import net.minecraft.client.gui.LogoDrawer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.kaczo.hud.Glass;

/** Zamiast logo Minecrafta na ekranie tytułowym rysujemy logo Kaczo Client (patrz Glass). */
@Mixin(LogoDrawer.class)
public class LogoDrawerMixin {
    @Inject(method = "draw", at = @At("HEAD"), cancellable = true, require = 0)
    private void kaczo$logo(CallbackInfo ci) {
        try { if (Glass.logoOn()) ci.cancel(); } catch (Throwable ignored) { }
    }
}
