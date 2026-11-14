package pl.kaczo.hud;

import net.minecraft.client.gui.DrawContext;

/** Efekt „3D GUI”: panele delikatnie obracają się w stronę kursora (jak okna w goglach VR). */
final class Tilt3D {
    private Tilt3D() { }
    private static float ty, tx;
    private static long last = System.nanoTime();
    private static int depth = 0;

    static boolean on() { return Modules.GENERAL.bool("gui3d"); }

    /** Wywołaj raz na klatkę przed rysowaniem panelu; zawsze sparuj z end(). */
    static void begin(DrawContext ctx, double mx, double my, double cx, double cy, double w, double h) {
        depth = 0;
        if (!on()) return;
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f); last = now;
        float tgY = (float) Math.max(-1, Math.min(1, (mx - cx) / (w / 2))) * 0.13f;
        float tgX = (float) Math.max(-1, Math.min(1, (my - cy) / (h / 2))) * 0.09f;
        float k = 1 - (float) Math.exp(-9 * dt);
        ty += (tgY - ty) * k; tx += (tgX - tx) * k;
        Compat.push(ctx);
        Compat.translate(ctx, (float) cx, (float) cy);
        Compat.scale(ctx, (float) Math.cos(ty), (float) Math.cos(tx));
        Compat.translate(ctx, (float) -cx, (float) -cy);
        depth = 1;
    }

    static void end(DrawContext ctx) { if (depth == 1) { Compat.pop(ctx); depth = 0; } }

    /** Przesunięcie „głębi” – elementy bliżej widza ruszają się trochę bardziej (paralaksa). */
    static float parallaxX(float layer) { return on() ? -ty * 40f * layer : 0f; }
    static float parallaxY(float layer) { return on() ? tx * 30f * layer : 0f; }
}
