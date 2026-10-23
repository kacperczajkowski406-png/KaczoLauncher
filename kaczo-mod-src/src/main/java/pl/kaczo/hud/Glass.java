package pl.kaczo.hud;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Nowoczesny wygląd lobby gry w samym kodzie (bez texture packa): „szklane” przyciski – także te dodane przez inne mody
 * (np. „Mody”) i ikony języka/ułatwień dostępu – logo na środku ekranu tytułowego i animowane wejście.
 * Działa wyłącznie na ekranie tytułowym; reszta menu zostaje standardowa. Wszystko do wyłączenia w ustawieniach moda.
 */
public final class Glass {
    private Glass() { }

    private static final Identifier LOGO = Identifier.of("kaczohud", "textures/gui/logo.png");
    /** Przyciski namalowane przez nas w ostatniej klatce – tylko im mixin wyłącza oryginalną grafikę. */
    private static final Map<Object, Long> SEEN = new WeakHashMap<>();
    private static long introStart = -1;
    private static boolean introPlayed = false;

    public static boolean buttonsOn() { return Modules.GENERAL.bool("glassbtn"); }
    public static boolean titleBgOn() { return Modules.GENERAL.bool("titlebg"); }
    public static boolean loadScreenOn() { return Modules.GENERAL.bool("loadscreen"); }
    public static boolean logoOn() { return Modules.GENERAL.bool("titlelogo"); }

    /** Używane przez mixin: czy oryginalny przycisk ma nie rysować swojej grafiki. */
    public static boolean hideVanilla(Object widget) {
        if (!buttonsOn()) return false;
        Long t = SEEN.get(widget);
        return t != null && System.currentTimeMillis() - t < 400;
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof TitleScreen && !introPlayed && Modules.GENERAL.bool("intro")) {
                introStart = System.currentTimeMillis();
                introPlayed = true;
            }
            if (screen instanceof TitleScreen) {
                long now = System.currentTimeMillis();
                try {
                    for (ClickableWidget b : Screens.getButtons(screen)) if (glassKind(b) != 0) SEEN.put(b, now);   // bez mignięcia oryginału w 1. klatce
                } catch (Throwable ignored) { }
                ScreenEvents.beforeRender(screen).register((s, ctx, mx, my, delta) -> {
                    try { if (titleBgOn()) Visual.aurora(ctx, s.width, s.height, 1f); } catch (Throwable ignored) { }
                });
            }
            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> {
                try { paint(client, s, ctx, mx, my); } catch (Throwable ignored) { /* wygląd nie może wywrócić gry */ }
            });
        });
    }

    private static float intro() {
        if (introStart < 0) return 1f;
        float p = (System.currentTimeMillis() - introStart) / 1300f;
        if (p >= 1f) { introStart = -1; return 1f; }
        return p;
    }

    private static float ease(float t) { t = Math.max(0, Math.min(1, t)); float u = 1 - t; return 1 - u * u * u; }

    private static int fade(int argb, float a) {
        int al = Math.round(((argb >>> 24) & 255) * Math.max(0, Math.min(1, a)));
        return (al << 24) | (argb & 0xFFFFFF);
    }

    /** 0 = nie ruszamy, 1 = przycisk z napisem, 2 = ikona języka, 3 = ikona ułatwień dostępu. */
    private static int glassKind(ClickableWidget b) {
        if (!(b instanceof ButtonWidget) || !b.visible) return 0;
        Text msg = b.getMessage();
        if (!msg.getString().isEmpty() && b.getWidth() >= 30) return 1;
        try {
            if (msg.getContent() instanceof TranslatableTextContent t) {
                String k = t.getKey();
                if (k.contains("language")) return 2;
                if (k.contains("accessibility")) return 3;
            }
        } catch (Throwable ignored) { }
        return 0;
    }

    /** Płynne „najechanie” – postęp 0..1 osobno dla każdego przycisku. */
    private static final Map<Object, Float> HOVER = new WeakHashMap<>();
    private static long lastFrame = System.currentTimeMillis();

    private static void paint(MinecraftClient mc, Screen screen, DrawContext ctx, int mx, int my) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        if (!(screen instanceof TitleScreen)) return;          // szkło tylko w lobby gry – reszta menu zostaje standardowa
        float p = intro();
        if (logoOn()) logo(mc, screen, ctx, p);
        if (!buttonsOn()) return;
        List<ClickableWidget> btns = Screens.getButtons(screen);
        int i = 0;
        for (ClickableWidget b : btns) {
            int kind = glassKind(b);
            if (kind == 0) continue;
            SEEN.put(b, now);
            float k = ease((p - 0.25f - i * 0.07f) / 0.5f);
            i++;
            if (k <= 0) continue;
            int w = b.getWidth(), h = b.getHeight();
            boolean hover = b.active && Ui.in(mx, my, b.getX(), b.getY(), w, h);
            float t = HOVER.getOrDefault(b, 0f);
            t += (hover ? 1 : -1) * dt * 7f;
            t = Math.max(0f, Math.min(1f, t));
            HOVER.put(b, t);
            float te = t * t * (3 - 2 * t);
            int y = b.getY() + Math.round((1 - k) * 10) - Math.round(te);   // przy najechaniu lekko unosi się
            int grow = Math.round(te * 1.5f);
            int alpha = !b.active ? 0x1E : 0x3A + Math.round(te * 0x2C);
            int fill = (alpha << 24) | (hover || te > 0.5f ? 0xFFF4D0 : 0xFFFFFF);
            int rad = kind == 1 ? h / 2 : h / 2;
            Ui.glass(ctx, b.getX() - grow, y - grow, w + 2 * grow, h + 2 * grow, rad, fade(fill, k));
            int tc = b.active ? (te > 0.5f ? 0xFFFFF2B0 : 0xFFFFFFFF) : 0xFF9AA3B5;
            if (kind == 1) {
                Text msg = b.getMessage();
                int tw = mc.textRenderer.getWidth(msg);
                ctx.drawTextWithShadow(mc.textRenderer, msg, b.getX() + (w - tw) / 2, y + (h - 8) / 2, fade(tc, k));
            } else {
                icon(ctx, kind, b.getX() + w / 2.0, y + h / 2.0, Math.min(w, h), fade(tc, k));
            }
        }
    }

    /** Ikony narysowane z gładkich kształtów (kula ziemska i człowieczek ułatwień dostępu). */
    private static void icon(DrawContext ctx, int kind, double cx, double cy, int size, int col) {
        double r = size * 0.33, th = Math.max(0.7, size * 0.055);
        Ui.ringT(ctx, cx - r, cy - r, 2 * r, 2 * r, r, th, col);
        if (kind == 2) {
            Ui.ringT(ctx, cx - r * 0.42, cy - r, r * 0.84, 2 * r, r * 0.42, th, col);          // południk
            Ui.rrectD(ctx, cx - r, cy - th / 2, 2 * r, th, th / 2, col);                        // równik
            Ui.rrectD(ctx, cx - r * 0.86, cy - r * 0.5 - th / 2, r * 1.72, th, th / 2, col);    // równoleżniki
            Ui.rrectD(ctx, cx - r * 0.86, cy + r * 0.5 - th / 2, r * 1.72, th, th / 2, col);
        } else {
            double hd = r * 0.34;
            Ui.rrectD(ctx, cx - hd, cy - r * 0.62 - hd, 2 * hd, 2 * hd, hd, col);                       // głowa
            Ui.rrectD(ctx, cx - r * 0.62, cy - r * 0.2 - th / 2, r * 1.24, th * 1.2, th / 2, col);     // ręce
            Ui.rrectD(ctx, cx - th / 2, cy - r * 0.2, th * 1.2, r * 0.62, th / 2, col);                 // tułów
            Ui.rrectD(ctx, cx - r * 0.34, cy + r * 0.3, th * 1.1, r * 0.55, th / 2, col);               // nogi
            Ui.rrectD(ctx, cx + r * 0.34 - th, cy + r * 0.3, th * 1.1, r * 0.55, th / 2, col);
        }
    }

    private static void logo(MinecraftClient mc, Screen screen, DrawContext ctx, float p) {
        long now = System.currentTimeMillis();
        int size = Math.max(24, Math.round(64 * Modules.GENERAL.num("logosize") / 100f * 1.1f));
        float k = ease(p / 0.6f);
        double bob = p >= 1f ? Math.sin(now / 900.0) * 1.6 : 0;           // delikatne unoszenie po animacji wejścia
        int cx = screen.width / 2;
        int cy = Math.round(screen.height / 4f - 8 - (1 - k) * 24 + (float) bob);   // wyżej niż wcześniej
        // pierścienie rozchodzące się od logo – jak w launcherze
        for (int r = 0; r < 3; r++) {
            double ph = ((now / 2600.0) + r / 3.0) % 1.0;
            double d = size * (0.62 + ph * 0.95);
            int a = (int) (70 * (1 - ph) * k);
            if (a > 2) Ui.ring(ctx, cx - d / 2, cy - d / 2, d, d, d / 2, (a << 24) | 0xFFFFFF);
        }
        double pulse = 0.5 + 0.5 * Math.sin(now / 700.0);
        int ga = (int) ((16 + 14 * pulse) * k);
        int gs = size + 16;
        Ui.rrect(ctx, cx - gs / 2, cy - gs / 2, gs, gs, gs / 2, (ga << 24) | 0xF4F7FF);   // poświata
        float s = size / 64f * (0.4f + 0.6f * k);
        float px = cx - 32 * s, py = cy - 32 * s;
        Compat.push(ctx);
        Compat.translate(ctx, px, py);
        Compat.scale(ctx, s, s);
        Compat.tex(ctx, LOGO, 64, 64);
        Compat.pop(ctx);
    }
}
