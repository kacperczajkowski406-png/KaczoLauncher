package pl.kaczo.hud.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.kaczo.hud.Modules;
import pl.kaczo.hud.Sb;

import net.minecraft.client.gui.hud.InGameHud;

/**
 * Chowa standardowy celownik (gdy włączony Custom Crosshair) oraz standardową tablicę wyników – tę rysuje teraz
 * własna klasa Sb (z przezroczystym tłem i przesuwaniem). Wszystkie wstrzyknięcia mają require=0.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void kaczo$crosshair(CallbackInfo ci) {
        if (Modules.CROSSHAIR != null && Modules.CROSSHAIR.enabled()) ci.cancel();
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true, require = 0)
    private void kaczo$sb(CallbackInfo ci) {
        if (Modules.SCOREBOARD == null) return;
        Sb.hookSeen = System.currentTimeMillis();
        if (Sb.hooked() || !Modules.SCOREBOARD.enabled()) ci.cancel();
    }
}
