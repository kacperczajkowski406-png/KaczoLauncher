package pl.kaczo.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Moduł klienta: kafelek w menu + (opcjonalnie) element HUD + ustawienia. */
public class Module {
    public interface Drawer { void draw(DrawContext ctx, MinecraftClient mc, int x, int y); }
    public interface Ticker { void tick(MinecraftClient mc); }

    public final String id, name, desc, category, icon;
    public boolean isNew = false, defaultEnabled = false, hud = false;
    public int defX = 6, defY = 6, boxW = 80, boxH = 12;
    public final List<Setting> settings = new ArrayList<>();
    public Function<MinecraftClient, String> text;     // moduły tekstowe
    public Drawer drawer;                              // moduły z własnym rysowaniem
    public Ticker ticker;
    public Function<MinecraftClient, int[]> sizeFn;    // rozmiar dynamiczny {szerokość, wysokość}

    public Module(String id, String name, String desc, String category, String icon) {
        this.id = id; this.name = name; this.desc = desc; this.category = category; this.icon = icon;
    }

    // ── konfiguracja (budowanie) ──
    public Module on() { defaultEnabled = true; return this; }
    public Module fresh() { isNew = true; return this; }
    public Module add(Setting s) { settings.add(s); return this; }

    /** Element HUD: dodaje wspólne ustawienia (rozmiar, tło, kolor). */
    public Module hud(int x, int y) {
        hud = true; defX = x; defY = y;
        add(Setting.num("scale", "Size %", 50, 200, 100));
        add(Setting.bool("bg", "Background", true));
        add(Setting.color("bgc", "Background color", Setting.BLACK_BG));
        add(Setting.color("color", "Text color", Setting.YELLOW));
        add(Setting.bool("shadow", "Text shadow", true));
        add(Setting.bool("border", "Accent bar", false));
        add(Setting.num("pad", "Padding", 0, 6, 2));
        return this;
    }
    public Module text(Function<MinecraftClient, String> f) { text = f; return this; }
    public Module draw(int w, int h, Drawer d) { drawer = d; boxW = w; boxH = h; return this; }
    public Module tick(Ticker t) { ticker = t; return this; }
    public Module size(Function<MinecraftClient, int[]> f) { sizeFn = f; return this; }

    // ── stan ──
    public boolean enabled() {
        Boolean e = Config.st(id).enabled;
        return e == null ? defaultEnabled : e;
    }
    public void setEnabled(boolean v) { Config.st(id).enabled = v; }
    public boolean fav() { return Config.st(id).fav; }
    public void toggleFav() { Config.st(id).fav = !Config.st(id).fav; }

    private Setting find(String key) {
        for (Setting s : settings) if (s.key.equals(key)) return s;
        return null;
    }
    public boolean has(String key) { return find(key) != null; }
    public String raw(String key) {
        String v = Config.st(id).vals.get(key);
        if (v != null) return v;
        Setting s = find(key);
        return s == null ? "" : s.def;
    }
    public void set(String key, String v) { Config.st(id).vals.put(key, v); }
    public boolean bool(String key) { return Boolean.parseBoolean(raw(key)); }
    public int num(String key) { try { return (int) Double.parseDouble(raw(key)); } catch (Exception e) { return 0; } }
    /** Kolor ARGB (z obsługą trybu tęczowego). Stary zapis (numer z palety) też działa. */
    public int color(String key) {
        String r = raw(key);
        int R = 255, G = 255, B = 255, A = 255; boolean rainbow = false;
        try {
            if (r.contains(",")) {
                String[] p = r.split(",");
                R = Integer.parseInt(p[0].trim()); G = Integer.parseInt(p[1].trim()); B = Integer.parseInt(p[2].trim());
                A = Integer.parseInt(p[3].trim()); rainbow = p.length > 4 && p[4].trim().equals("1");
            } else {
                int c = Setting.PALETTE[Math.max(0, Math.min(Setting.PALETTE.length - 1, Integer.parseInt(r.trim())))];
                R = (c >> 16) & 255; G = (c >> 8) & 255; B = c & 255;
            }
        } catch (Exception ignored) { }
        if (rainbow) {
            int c = java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 4000) / 4000f, 0.85f, 1f);
            R = (c >> 16) & 255; G = (c >> 8) & 255; B = c & 255;
        }
        A = Math.max(4, Math.min(255, A));
        return (A << 24) | (R << 16) | (G << 8) | B;
    }

    /** Tło elementu HUD (0, gdy wyłączone). */
    public int bg() { return bool("bg") ? color("bgc") : 0; }

    public int x() { int v = Config.st(id).x; return v < 0 ? defX : v; }
    public int y() { int v = Config.st(id).y; return v < 0 ? defY : v; }
    public void setPos(int x, int y) { Config.st(id).x = x; Config.st(id).y = y; }

    public float scale() { return hud ? num("scale") / 100f : 1f; }

    public int width(MinecraftClient mc) {
        if (sizeFn != null) return sizeFn.apply(mc)[0];
        return text != null ? mc.textRenderer.getWidth(text.apply(mc)) : boxW;
    }
    public int height(MinecraftClient mc) {
        if (sizeFn != null) return sizeFn.apply(mc)[1];
        return text != null ? 10 : boxH;
    }
}
