package pl.kaczo.hud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KaczoHudClient implements ClientModInitializer {
    public static KeyBinding openKey, freelookKey, waypointKey;

    @Override
    public void onInitializeClient() {
        Config.load();
        try { Glass.init(); } catch (Throwable ignored) { }
        openKey = KeyBindingHelper.registerKeyBinding(Compat.newKey("key.kaczohud.open", GLFW.GLFW_KEY_RIGHT_SHIFT));
        freelookKey = KeyBindingHelper.registerKeyBinding(Compat.newKey("key.kaczohud.freelook", GLFW.GLFW_KEY_LEFT_ALT));
        waypointKey = KeyBindingHelper.registerKeyBinding(Compat.newKey("key.kaczohud.waypoint", GLFW.GLFW_KEY_B));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try { Modules.tick(client); } catch (Throwable ignored) { /* moduł nie może wywrócić gry */ }
            try { ShaderCtl.tick(client); } catch (Throwable ignored) { }
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ModMenuScreen());
            }
            while (waypointKey.wasPressed()) {
                if (client.currentScreen != null || client.player == null || !Modules.byId("waypoint").enabled()) continue;
                boolean shift = Compat.keyDown(client, GLFW.GLFW_KEY_LEFT_SHIFT);
                if (shift) Extras.clearWaypoint(client); else Extras.setWaypoint(client);
            }
        });
        HudRenderCallback.EVENT.register((ctx, tick) -> {
            try { Hud.render(ctx); } catch (Throwable ignored) { }
        });
    }
}
