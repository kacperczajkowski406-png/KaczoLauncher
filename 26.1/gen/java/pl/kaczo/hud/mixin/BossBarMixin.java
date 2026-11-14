package pl.kaczo.hud.mixin;

import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.kaczo.hud.Modules;

/** Moduł „Ukryj pasek bossa”. */
@Mixin(BossHealthOverlay.class)
public class BossBarMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void kaczo$hide(CallbackInfo ci) {
        try { if (Modules.byId("hidebossbar").enabled()) ci.cancel(); } catch (Throwable ignored) { }
    }
}
