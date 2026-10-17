package pl.kaczo.hud;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

/** Licznik kliknięć na sekundę (sprawdza stan przycisków myszy co tick). */
public class Cps {
    private static final ArrayDeque<Long> L = new ArrayDeque<>(), R = new ArrayDeque<>();
    private static boolean ld = false, rd = false;

    public static void tick(MinecraftClient mc) {
        long h = mc.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();
        if (l && !ld) L.add(now);
        if (r && !rd) R.add(now);
        ld = l; rd = r;
        while (!L.isEmpty() && now - L.peekFirst() > 1000) L.pollFirst();
        while (!R.isEmpty() && now - R.peekFirst() > 1000) R.pollFirst();
    }

    public static int left() { return L.size(); }
    public static int right() { return R.size(); }
    public static boolean leftDown() { return ld; }
    public static boolean rightDown() { return rd; }
}
