package pl.kaczo.hud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class KaczoHudClient implements ClientModInitializer {
    public static KeyMapping openKey, freelookKey, waypointKey;

    @Override
    public void onInitializeClient() {
        Config.load();
        try { Glass.init(); } catch (Throwable ignored) { }
        openKey = KeyMappingHelper.registerKeyMapping(Compat.newKey("key.kaczohud.open", GLFW.GLFW_KEY_RIGHT_SHIFT));
        freelookKey = KeyMappingHelper.registerKeyMapping(Compat.newKey("key.kaczohud.freelook", GLFW.GLFW_KEY_LEFT_ALT));
        waypointKey = KeyMappingHelper.registerKeyMapping(Compat.newKey("key.kaczohud.waypoint", GLFW.GLFW_KEY_B));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try { Modules.tick(client); } catch (Throwable ignored) { /* moduł nie może wywrócić gry */ }
            try { ShaderCtl.tick(client); } catch (Throwable ignored) { }
            while (openKey.consumeClick()) {
                if (client.screen == null) client.setScreen(new ModMenuScreen());
            }
            while (waypointKey.consumeClick()) {
                if (client.screen != null || client.player == null || !Modules.byId("waypoint").enabled()) continue;
                boolean shift = Compat.keyDown(client, GLFW.GLFW_KEY_LEFT_SHIFT);
                if (shift) Extras.clearWaypoint(client); else Extras.setWaypoint(client);
            }
        });
        HudElementRegistry.addLast(net.minecraft.resources.Identifier.fromNamespaceAndPath("kaczohud", "hud"), (ctx, tick) -> {
            try { Hud.render(ctx); } catch (Throwable ignored) { }
        });
    }
}
