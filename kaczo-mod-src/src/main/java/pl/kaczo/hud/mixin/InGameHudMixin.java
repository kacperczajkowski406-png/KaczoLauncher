package pl.kaczo.hud.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.kaczo.hud.Compat;
import pl.kaczo.hud.Hud;
import pl.kaczo.hud.Module;
import pl.kaczo.hud.Modules;

/**
 * Chowa standardowy celownik (gdy włączony Custom Crosshair) oraz steruje scoreboardem:
 * wyłączony = ukryty, włączony może mieć własny rozmiar, położenie i przezroczystość.
 * Wszystkie wstrzyknięcia mają require=0 – gdy nazwa metody się nie zgadza, po prostu nie działają.
 * (Handlery nie wymieniają argumentów metody, więc nie zależą od jej dokładnego podpisu.)
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    // metoda renderScoreboardSidebar może mieć kilka przeciążeń wywołujących się nawzajem – liczymy zagnieżdżenie
    private int kaczo$depth = 0;
    private boolean kaczo$alpha = false;

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void kaczo$crosshair(CallbackInfo ci) {
        if (Modules.CROSSHAIR != null && Modules.CROSSHAIR.enabled()) ci.cancel();
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true, require = 0)
    private void kaczo$sbHead(CallbackInfo ci) {
        Module m = Modules.SCOREBOARD;
        if (m == null) return;
        if (!m.enabled()) { ci.cancel(); return; }
        DrawContext ctx = Hud.lastContext;           // ten sam obiekt DrawContext jest używany co klatkę
        if (ctx == null) return;
        if (kaczo$depth++ > 0) return;               // zagnieżdżone wywołanie – ustawienia już zastosowane
        MinecraftClient mc = MinecraftClient.getInstance();
        float s = m.num("scale") / 100f;
        float a = m.num("opacity") / 100f;
        float w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        Compat.flush(ctx);                                  // dorysuj wszystko, co było wcześniej (żeby nie dostało przezroczystości)
        Compat.push(ctx);
        Compat.translate(ctx, Hud.sbDx(), Hud.sbDy());
        Compat.translate(ctx, w, h / 2f);
        Compat.scale(ctx, s, s);
        Compat.translate(ctx, -w, -h / 2f);
        if (a < 1f) { Compat.alpha(a); kaczo$alpha = true; }
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("RETURN"), require = 0)
    private void kaczo$sbTail(CallbackInfo ci) {
        if (kaczo$depth <= 0) return;
        if (--kaczo$depth > 0) return;
        DrawContext ctx = Hud.lastContext;
        if (ctx == null) return;
        if (kaczo$alpha) {
            Compat.flush(ctx);                              // narysuj scoreboard z przezroczystością ZANIM ją zdejmiemy
            Compat.alpha(1f);
            kaczo$alpha = false;
        }
        Compat.pop(ctx);
    }
}
