package pl.kaczo.hud;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;

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

    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("kaczohud", "textures/gui/logo.png");
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

    private static boolean autoDone;
    /** Tylko do testów w CI (zmienna KACZO_AUTOWORLD): sam tworzy świat, żeby można było obejrzeć niebo. */
    private static void autoWorld(Minecraft client, Screen screen) {
        if (autoDone || System.getenv("KACZO_AUTOWORLD") == null) return;
        try {
            if (screen instanceof TitleScreen) {
                for (java.lang.reflect.Method m : Class.forName("net.minecraft.client.gui.screens.worldselection.CreateWorldScreen").getMethods())
                    if (m.getName().equals("show") && m.getParameterCount() == 2) {
                        Object arg = m.getParameterTypes()[1] == Runnable.class ? (Runnable) () -> client.setScreen(screen) : screen;
                        m.invoke(null, client, arg); return;
                    }
            } else if (screen.getClass().getSimpleName().contains("CreateWorld")) {
                autoDone = true;
                new Thread(() -> {
                    try { Thread.sleep(4000); } catch (Throwable ignored) { }
                    client.execute(() -> {
                        for (AbstractWidget b : Screens.getWidgets(client.screen))
                            if (b instanceof Button bw && bw.getMessage().getContents() instanceof TranslatableContents t && t.getKey().equals("selectWorld.create")) bw.onPress();
                    });
                }).start();
            }
        } catch (Throwable ignored) { }
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            autoWorld(client, screen);
            if (screen instanceof TitleScreen && !introPlayed && Modules.GENERAL.bool("intro")) {
                introStart = System.currentTimeMillis();
                introPlayed = true;
            }
            if (screen instanceof TitleScreen) {
                long now = System.currentTimeMillis();
                try {
                    for (AbstractWidget b : Screens.getWidgets(screen)) if (pillKind(b) != 0 || glassKind(b) >= 2) SEEN.put(b, now);   // bez mignięcia oryginału w 1. klatce
                } catch (Throwable ignored) { }
                ScreenEvents.beforeExtract(screen).register((s, ctx, mx, my, delta) -> {
                    try { if (titleBgOn()) Visual.aurora(ctx, s.width, s.height, 1f); } catch (Throwable ignored) { }
                });
            }
            ScreenEvents.afterExtract(screen).register((s, ctx, mx, my, delta) -> {
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
    private static int glassKind(AbstractWidget b) {
        if (!(b instanceof Button) || !b.visible) return 0;
        Component msg = b.getMessage();
        if (!msg.getString().isEmpty() && b.getWidth() >= 30) return 1;
        try {
            if (msg.getContents() instanceof TranslatableContents t) {
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

    private static void paint(Minecraft mc, Screen screen, GuiGraphicsExtractor ctx, int mx, int my) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        if (!(screen instanceof TitleScreen)) return;          // szkło tylko w lobby gry – reszta menu zostaje standardowa
        float p = intro();
        if (!buttonsOn()) return;
        lock(mc, screen, ctx, mx, my, p, dt);
    }

    // ───────────── ekran blokady: data, zegar i szklana pigułka z ikonami (jak na wzorze) ─────────────

    /** Rodzaj ikony dla znanych przycisków menu głównego (0 = nie ruszamy). Kolejność = kolejność w pigułce. */
    private static int pillKind(AbstractWidget b) {
        if (!(b instanceof Button) || !b.visible) return 0;
        try {
            if (b.getMessage().getContents() instanceof TranslatableContents t) {
                String k = t.getKey();
                if (k.equals("menu.singleplayer")) return 1;
                if (k.equals("menu.multiplayer")) return 2;
                if (k.equals("menu.online")) return 3;
                if (k.contains("modmenu")) return 4;
                if (k.equals("menu.options")) return 5;
                if (k.equals("menu.quit")) return 6;
            }
        } catch (Throwable ignored) { }
        return 0;
    }

    private static final int CELL = 26;

    private static List<AbstractWidget> pillButtons(Screen screen) {
        List<AbstractWidget> out = new java.util.ArrayList<>();
        for (AbstractWidget b : Screens.getWidgets(screen)) if (pillKind(b) != 0) out.add(b);
        out.sort(java.util.Comparator.comparingInt(Glass::pillKind));
        return out;
    }

    private static void lock(Minecraft mc, Screen screen, GuiGraphicsExtractor ctx, int mx, int my, float p, float dt) {
        long now = System.currentTimeMillis();
        int w = screen.width, h = screen.height;
        float k = ease((p - 0.1f) / 0.6f);
        // data i zegar
        java.time.LocalDateTime dtm = java.time.LocalDateTime.now();
        java.util.Locale loc = L.pl() ? java.util.Locale.forLanguageTag("pl") : java.util.Locale.ENGLISH;
        String date = dtm.format(java.time.format.DateTimeFormatter.ofPattern(L.pl() ? "EEEE, d MMMM" : "EEEE, MMMM d", loc));
        date = date.substring(0, 1).toUpperCase(loc) + date.substring(1);
        String clock = dtm.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        Font tr = mc.font;
        float rise = (1 - k) * 8;
        Compat.flush(ctx);
        float ds = Math.max(1f, h / 270f);
        int dw = tr.width(date);
        Compat.push(ctx);
        Compat.translate(ctx, w / 2f - dw * ds / 2f, h * 0.105f - 4f * ds + rise);
        Compat.scale(ctx, ds, ds);
        ctx.text(tr, date, 0, 0, fade(0xFFE8ECF4, k), false);
        Compat.pop(ctx);
        float cs = Math.max(1.2f, h * 0.125f / 24f);
        Component ct = Compat.clockText(clock);
        int cw = tr.width(ct);
        Compat.push(ctx);
        Compat.translate(ctx, w / 2f - cw * cs / 2f, h * 0.215f + 4.5f * cs + rise);
        Compat.scale(ctx, cs, cs);
        ctx.text(tr, ct, 0, 0, fade(0xFFFFFFFF, k), false);
        Compat.pop(ctx);
        for (AbstractWidget b : Screens.getWidgets(screen)) {
            int kd = glassKind(b);
            if (kd < 2) continue;
            SEEN.put(b, now);
            boolean hv = b.active && Ui.in(mx, my, b.getX(), b.getY(), b.getWidth(), b.getHeight());
            Ui.glass(ctx, b.getX(), b.getY(), b.getWidth(), b.getHeight(), b.getHeight() / 2, fade(hv ? 0x50FFFFFF : 0x30FFFFFF, k));
            icon(ctx, kd, b.getX() + b.getWidth() / 2.0, b.getY() + b.getHeight() / 2.0, Math.min(b.getWidth(), b.getHeight()), fade(0xFFFFFFFF, k));
        }
        // pigułka
        List<AbstractWidget> bs = pillButtons(screen);
        if (bs.isEmpty()) return;
        int n = bs.size(), pw = n * CELL + 10, ph = 26;
        int px0 = (w - pw) / 2, cy = Math.round(h * 0.52f + (1 - k) * 8);
        Ui.glass(ctx, px0, cy - ph / 2, pw, ph, ph / 2, fade(0x30FFFFFF, k));
        String hoverLabel = null; int hoverX = 0;
        int i = 0;
        for (AbstractWidget b : bs) {
            SEEN.put(b, now);
            b.setX(px0 + 5 + i * CELL);
            b.setY(cy - 10);
            b.setWidth(CELL);
            int kind = pillKind(b);
            double cx = b.getX() + CELL / 2.0;
            boolean hover = b.active && Ui.in(mx, my, b.getX(), cy - 13, CELL, 26);
            float t = HOVER.getOrDefault(b, 0f);
            t = Math.max(0f, Math.min(1f, t + (hover ? 1 : -1) * dt * 8f));
            HOVER.put(b, t);
            float te = t * t * (3 - 2 * t);
            if (te > 0.01f) Ui.rrectD(ctx, cx - 11, cy - 11, 22, 22, 11, fade(0x38FFFFFF, te * k));
            int col = kind == 6 ? 0xFFFF5A68 : 0xFFFFFFFF;
            col = Ui.mix(Ui.fade(col, 0.85f) , col, te);
            iconPill(ctx, kind, cx, cy, fade(col, k));
            if (hover) { hoverLabel = b.getMessage().getString(); hoverX = (int) cx; }
            i++;
        }
        if (hoverLabel != null) {
            int lw = tr.width(hoverLabel);
            ctx.text(tr, hoverLabel, hoverX - lw / 2, cy + ph / 2 + 6, 0xE0F4F7FF, false);
        }
    }

    private static void dot(GuiGraphicsExtractor ctx, double cx, double cy, double r, int col) { Ui.rrectD(ctx, cx - r, cy - r, 2 * r, 2 * r, r, col); }

    private static void person(GuiGraphicsExtractor ctx, double cx, double cy, double s, int col) {
        dot(ctx, cx, cy - s * 0.42, s * 0.34, col);
        Ui.rrectD(ctx, cx - s * 0.6, cy + s * 0.06, s * 1.2, s * 0.62, s * 0.3, col);
    }

    private static void iconPill(GuiGraphicsExtractor ctx, int kind, double cx, double cy, int col) {
        double s = 7;
        switch (kind) {
            case 1 -> person(ctx, cx, cy, s, col);
            case 2 -> { person(ctx, cx - 3.2, cy, s * 0.82, col); person(ctx, cx + 3.2, cy, s * 0.82, col); }
            case 3 -> icon(ctx, 2, cx, cy, 24, col);
            case 4 -> {
                Ui.rrectD(ctx, cx - s * 0.95, cy - s * 0.65, s * 1.9, s * 1.3, s * 0.22, col);
                Ui.rrectD(ctx, cx - s * 0.78, cy - s * 0.5, s * 1.56, s * 1.0, s * 0.14, (0xB0 << 24) | 0x0A0C12);
                dot(ctx, cx - s * 0.42, cy - s * 0.12, s * 0.2, col);
                Ui.rrectD(ctx, cx - s * 0.62, cy + s * 0.12, s * 0.4, s * 0.2, s * 0.08, col);
                Ui.rrectD(ctx, cx + s * 0.02, cy - s * 0.22, s * 0.6, s * 0.1, s * 0.05, col);
                Ui.rrectD(ctx, cx + s * 0.02, cy + s * 0.02, s * 0.6, s * 0.1, s * 0.05, col);
            }
            case 5 -> {
                double R = s * 0.62, th = s * 0.34;
                for (int j = 0; j < 8; j++) { double a = j * Math.PI / 4; dot(ctx, cx + Math.cos(a) * R * 1.12, cy + Math.sin(a) * R * 1.12, s * 0.2, col); }
                Ui.ringT(ctx, cx - R, cy - R, 2 * R, 2 * R, R, th, col);
            }
            case 6 -> {
                double R = s * 0.82, th = s * 0.2;
                for (int j = 0; j <= 44; j++) {
                    double a = Math.toRadians(40 + j * (280.0 / 44));
                    dot(ctx, cx + Math.sin(a) * R, cy - Math.cos(a) * R, th / 2, col);
                }
                Ui.rrectD(ctx, cx - th / 2, cy - R - s * 0.2, th, s * 0.85, th / 2, col);
            }
            default -> { }
        }
    }

    private static void smallLogo(GuiGraphicsExtractor ctx, Screen screen, float k) {
        int size = 22;
        Compat.flush(ctx);
        Compat.alpha(0.8f * k);
        Compat.push(ctx);
        Compat.translate(ctx, screen.width / 2f - size / 2f, screen.height * 0.66f);
        Compat.scale(ctx, size / 64f, size / 64f);
        Compat.tex(ctx, LOGO, 64, 64);
        Compat.pop(ctx);
        Compat.flush(ctx);
        Compat.alpha(1f);
    }

    /** Ikony narysowane z gładkich kształtów (kula ziemska i człowieczek ułatwień dostępu). */
    private static void icon(GuiGraphicsExtractor ctx, int kind, double cx, double cy, int size, int col) {
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

    private static void logo(Minecraft mc, Screen screen, GuiGraphicsExtractor ctx, float p) {
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
