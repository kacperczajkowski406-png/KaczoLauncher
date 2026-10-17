package pl.kaczo.hud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KaczoHudClient implements ClientModInitializer {
    public static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        Config.load();
        try { Glass.init(); } catch (Throwable ignored) { }
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.kaczohud.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.kaczohud"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try { Modules.tick(client); } catch (Throwable ignored) { /* moduł nie może wywrócić gry */ }
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ModMenuScreen());
            }
        });
        HudRenderCallback.EVENT.register((ctx, tick) -> {
            try { Hud.render(ctx); } catch (Throwable ignored) { }
        });
    }
}
