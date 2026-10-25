package pl.kaczo.hud;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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

    public static final Identifier UI_FONT = Identifier.of("kaczohud", "ui");
    /** Czy aktualnie rysujemy menu główne albo okno moda (tylko tam używamy czcionki Inter). */
    public static boolean interNow() {
        Screen s = MinecraftClient.getInstance().currentScreen;
        return s instanceof TitleScreen || s instanceof KScreen;
    }

    private static boolean autoDone;
    /** Tylko do testów w CI (zmienna KACZO_AUTOWORLD): sam tworzy świat, żeby można było obejrzeć niebo. */
    private static void autoWorld(MinecraftClient client, Screen screen) {
        if (!autoDone && screen instanceof TitleScreen && System.getenv("KACZO_OPENMENU") != null) {
            autoDone = true;
            new Thread(() -> {
                try { Thread.sleep(25000); } catch (Throwable ignored) { }
                client.execute(() -> client.setScreen(new ModMenuScreen()));
            }).start();
            return;
        }
        if (autoDone || System.getenv("KACZO_AUTOWORLD") == null) return;
        try {
            if (screen instanceof TitleScreen) {
                for (java.lang.reflect.Method m : Class.forName("net.minecraft.client.gui.screen.world.CreateWorldScreen").getMethods())
                    if (m.getName().equals("show") && m.getParameterCount() == 2) {
                        Object arg = m.getParameterTypes()[1] == Runnable.class ? (Runnable) () -> client.setScreen(screen) : screen;
                        m.invoke(null, client, arg); return;
                    }
            } else if (screen.getClass().getSimpleName().contains("CreateWorld")) {
                autoDone = true;
                new Thread(() -> {
                    try { Thread.sleep(4000); } catch (Throwable ignored) { }
                    client.execute(() -> {
                        for (ClickableWidget b : Screens.getButtons(client.currentScreen))
                            if (b instanceof ButtonWidget bw && bw.getMessage().getContent() instanceof TranslatableTextContent t && t.getKey().equals("selectWorld.create")) bw.onPress();
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
            if (screen instanceof TitleScreen && buttonsOn()) {
                try {
                    kaczoBtn = ButtonWidget.builder(Text.literal("Kaczo HUD"), bt -> client.setScreen(new ModMenuScreen())).dimensions(0, 0, CELL, 20).build();
                    Screens.getButtons(screen).add(kaczoBtn);
                } catch (Throwable ignored) { kaczoBtn = null; }
            }
            if (screen instanceof TitleScreen) {
                long now = System.currentTimeMillis();
                try {
                    for (ClickableWidget b : Screens.getButtons(screen)) if (pillKind(b) != 0 || glassKind(b) >= 2) SEEN.put(b, now);   // bez mignięcia oryginału w 1. klatce
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
        if (!buttonsOn()) return;
        lock(mc, screen, ctx, mx, my, p, dt);
    }

    // ───────────── ekran blokady: data, zegar i szklana pigułka z ikonami (jak na wzorze) ─────────────

    /** Rodzaj ikony dla znanych przycisków menu głównego (0 = nie ruszamy). Kolejność = kolejność w pigułce. */
    private static ClickableWidget kaczoBtn;

    private static int pillKind(ClickableWidget b) {
        if (!(b instanceof ButtonWidget) || !b.visible) return 0;
        if (b == kaczoBtn) return 4;
        try {
            if (b.getMessage().getContent() instanceof TranslatableTextContent t) {
                String k = t.getKey();
                if (k.equals("menu.singleplayer")) return 1;
                if (k.equals("menu.multiplayer")) return 2;
                if (k.equals("menu.options")) return 3;
                if (k.equals("menu.quit")) return 5;
                if (k.equals("menu.online") || k.contains("modmenu")) b.visible = false;     // nie ma ich we wzorze – zostaje 5 ikon
            }
        } catch (Throwable ignored) { }
        return 0;
    }

    private static final int CELL = 26;

    private static List<ClickableWidget> pillButtons(Screen screen) {
        List<ClickableWidget> out = new java.util.ArrayList<>();
        for (ClickableWidget b : Screens.getButtons(screen)) if (pillKind(b) != 0) out.add(b);
        out.sort(java.util.Comparator.comparingInt(Glass::pillKind));
        return out;
    }

    private static void lock(MinecraftClient mc, Screen screen, DrawContext ctx, int mx, int my, float p, float dt) {
        long now = System.currentTimeMillis();
        int w = screen.width, h = screen.height;
        float k = ease((p - 0.1f) / 0.6f);
        // data i zegar
        java.time.LocalDateTime dtm = java.time.LocalDateTime.now();
        java.util.Locale loc = L.pl() ? java.util.Locale.forLanguageTag("pl") : java.util.Locale.ENGLISH;
        String date = dtm.format(java.time.format.DateTimeFormatter.ofPattern(L.pl() ? "EEEE, d MMMM" : "EEEE, MMMM d", loc));
        date = date.substring(0, 1).toUpperCase(loc) + date.substring(1);
        String clock = dtm.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        TextRenderer tr = mc.textRenderer;
        float rise = (1 - k) * 8;
        Compat.flush(ctx);
        float ds = Math.max(1f, h / 270f);
        int dw = tr.getWidth(date);
        Compat.push(ctx);
        Compat.translate(ctx, w / 2f - dw * ds / 2f, h * 0.105f - 4f * ds + rise);
        Compat.scale(ctx, ds, ds);
        ctx.drawText(tr, date, 0, 0, fade(0xFFE8ECF4, k), false);
        Compat.pop(ctx);
        float cs = Math.max(1.2f, h * 0.125f / 24f);
        Text ct = Compat.clockText(clock);
        int cw = tr.getWidth(ct);
        Compat.push(ctx);
        Compat.translate(ctx, w / 2f - cw * cs / 2f, h * 0.215f + 4.5f * cs + rise);
        Compat.scale(ctx, cs, cs);
        ctx.drawText(tr, ct, 0, 0, fade(0xFFFFFFFF, k), false);
        Compat.pop(ctx);
        for (ClickableWidget b : Screens.getButtons(screen)) {
            int kd = glassKind(b);
            if (kd < 2) continue;
            SEEN.put(b, now);
            boolean hv = b.active && Ui.in(mx, my, b.getX(), b.getY(), b.getWidth(), b.getHeight());
            Ui.glass(ctx, b.getX(), b.getY(), b.getWidth(), b.getHeight(), b.getHeight() / 2, fade(hv ? 0x50FFFFFF : 0x30FFFFFF, k));
            icon(ctx, kd, b.getX() + b.getWidth() / 2.0, b.getY() + b.getHeight() / 2.0, Math.min(b.getWidth(), b.getHeight()), fade(0xFFFFFFFF, k));
        }
        // pigułka
        List<ClickableWidget> bs = pillButtons(screen);
        if (bs.isEmpty()) return;
        int n = bs.size(), pw = n * CELL + 10, ph = 26;
        int px0 = (w - pw) / 2, cy = Math.round(h * 0.52f + (1 - k) * 8);
        Ui.glass(ctx, px0, cy - ph / 2, pw, ph, ph / 2, fade(0x26FFFFFF, k));
        String hoverLabel = null; int hoverX = 0;
        int i = 0;
        for (ClickableWidget b : bs) {
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
            int col = kind == 5 ? 0xFFFF5A68 : 0xFFFFFFFF;
            col = Ui.mix(Ui.fade(col, 0.85f) , col, te);
            iconPill(ctx, kind, cx, cy, fade(col, k));
            if (hover) { hoverLabel = b.getMessage().getString(); hoverX = (int) cx; }
            i++;
        }
        if (hoverLabel != null) {
            int lw = tr.getWidth(hoverLabel);
            ctx.drawText(tr, hoverLabel, hoverX - lw / 2, cy + ph / 2 + 6, 0xE0F4F7FF, false);
        }
    }

    private static void dot(DrawContext ctx, double cx, double cy, double r, int col) { Ui.rrectD(ctx, cx - r, cy - r, 2 * r, 2 * r, r, col); }

    private static void person(DrawContext ctx, double cx, double cy, double s, int col) {
        dot(ctx, cx, cy - s * 0.42, s * 0.34, col);
        Ui.rrectD(ctx, cx - s * 0.6, cy + s * 0.06, s * 1.2, s * 0.62, s * 0.3, col);
    }

    private static void iconPill(DrawContext ctx, int kind, double cx, double cy, int col) {
        double s = 7;
        switch (kind) {
            case 1 -> person(ctx, cx, cy, s, col);
            case 2 -> { person(ctx, cx - 3.4, cy, s * 0.8, col); person(ctx, cx + 3.4, cy, s * 0.8, col); }
            case 3 -> {                                                        // zębatka: pierścień + 8 zębów
                double R = s * 0.58, th = s * 0.36;
                for (int j = 0; j < 8; j++) {
                    Compat.push(ctx);
                    Compat.translate(ctx, (float) cx, (float) cy);
                    Compat.rotate(ctx, (float) (j * Math.PI / 4));
                    Ui.rrectD(ctx, -s * 0.17, -s * 1.0, s * 0.34, s * 0.5, s * 0.08, col);
                    Compat.pop(ctx);
                }
                Ui.ringT(ctx, cx - R - th * 0.35, cy - R - th * 0.35, 2 * (R + th * 0.35), 2 * (R + th * 0.35), R + th * 0.35, th, col);
            }
            case 4 -> {                                                        // karta ID
                double w = s * 2.1, h = s * 1.5;
                Ui.ringT(ctx, cx - w / 2, cy - h / 2, w, h, s * 0.3, s * 0.17, col);
                dot(ctx, cx - w * 0.26, cy - h * 0.14, s * 0.2, col);
                Ui.rrectD(ctx, cx - w * 0.38, cy + h * 0.06, w * 0.24, h * 0.2, s * 0.1, col);
                Ui.rrectD(ctx, cx + w * 0.02, cy - h * 0.2, w * 0.3, s * 0.15, s * 0.07, col);
                Ui.rrectD(ctx, cx + w * 0.02, cy + h * 0.05, w * 0.3, s * 0.15, s * 0.07, col);
            }
            case 5 -> {
                double R = s * 0.8, th = s * 0.2;
                for (int j = 0; j <= 44; j++) {
                    double a = Math.toRadians(40 + j * (280.0 / 44));
                    dot(ctx, cx + Math.sin(a) * R, cy - Math.cos(a) * R, th / 2, col);
                }
                Ui.rrectD(ctx, cx - th / 2, cy - R - s * 0.2, th, s * 0.85, th / 2, col);
            }
            default -> { }
        }
    }

    private static void smallLogo(DrawContext ctx, Screen screen, float k) {
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
