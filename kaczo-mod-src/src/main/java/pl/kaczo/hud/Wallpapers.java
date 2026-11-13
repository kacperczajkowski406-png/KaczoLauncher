package pl.kaczo.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tapety menu głównego: statyczne (gotowa tekstura) i „live” (tekstura + ruchoma warstwa, dryf kamery, pulsowanie).
 * Wszystko to zwykłe tekstury rysowane przez Compat, więc działa identycznie na każdej wersji Minecrafta.
 */
public final class Wallpapers {
    private Wallpapers() { }

    /** layer: 0 brak, 1 przewijana poziomo, 2 pulsująca. */
    public record Wp(String id, String name, String cat, boolean live, String base, int bw, int bh, String layer, int mode, float speed) { }

    public static final List<Wp> ALL = List.of(
            new Wp("silk", "Black Silk", "Dark", true, "/assets/kaczohud/textures/gui/glass_bg.png", 1920, 1080, null, 0, 0),
            new Wp("aurora", "Northern Lights", "Nature", true, "aurora", 960, 540, "aurora_l", 1, 9f),
            new Wp("nebula", "Nebula", "Space", true, "nebula", 960, 540, "nebula_l", 1, 5f),
            new Wp("synthwave", "Synthwave", "Colors", true, "synthwave", 960, 540, "synthwave_l", 2, 0f),
            new Wp("sakura", "Sakura Dusk", "Nature", true, "sakura", 960, 540, "sakura_l", 1, 14f),
            new Wp("galaxy", "Galaxy", "Space", true, "galaxy", 960, 540, "galaxy_l", 2, 0f),
            new Wp("ocean", "Ocean Sunset", "Nature", false, "ocean", 960, 540, null, 0, 0),
            new Wp("mountains", "Dusk Peaks", "Nature", false, "mountains", 960, 540, null, 0, 0),
            new Wp("citynight", "City Night", "Dark", false, "citynight", 960, 540, null, 0, 0),
            new Wp("moonlake", "Moon Lake", "Nature", false, "moonlake", 960, 540, null, 0, 0),
            new Wp("forest", "Misty Forest", "Nature", false, "forest", 960, 540, null, 0, 0),
            new Wp("ember", "Ember", "Colors", false, "ember", 960, 540, null, 0, 0),
            new Wp("ice", "Glacier", "Colors", false, "ice", 960, 540, null, 0, 0),
            new Wp("graphite", "Graphite", "Dark", false, "graphite", 960, 540, null, 0, 0),
            new Wp("duck", "Kaczo Gold", "Colors", false, "duck", 960, 540, null, 0, 0),
            new Wp("violet", "Violet", "Colors", false, "violet", 960, 540, null, 0, 0));

    private static final long T0 = System.currentTimeMillis();
    private static final Map<String, Identifier> TEX = new HashMap<>();
    private static final java.util.Set<String> BAD = new java.util.HashSet<>();

    public static Wp current() {
        String id = Config.st("wallpaper").vals.get("id");
        for (Wp w : ALL) if (w.id().equals(id)) return w;
        return ALL.get(0);
    }

    public static boolean isFav(Wp w) {
        String f = Config.st("wallpaper").vals.get("fav");
        return f != null && ("," + f + ",").contains("," + w.id() + ",");
    }

    public static void toggleFav(Wp w) {
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        String f = Config.st("wallpaper").vals.get("fav");
        if (f != null && !f.isEmpty()) set.addAll(java.util.Arrays.asList(f.split(",")));
        if (!set.remove(w.id())) set.add(w.id());
        Config.st("wallpaper").vals.put("fav", String.join(",", set));
        Config.save();
    }

    public static void select(Wp w) {
        Config.st("wallpaper").vals.put("id", w.id());
        Config.save();
    }

    private static Identifier tex(String res) {
        Identifier id = TEX.get(res);
        if (id != null || BAD.contains(res)) return id;
        String path = res.startsWith("/") ? res : "/assets/kaczohud/textures/wallpaper/" + res + ".png";
        try (InputStream in = Wallpapers.class.getResourceAsStream(path)) {
            NativeImage ni = NativeImage.read(in);
            id = Identifier.of("kaczohud", "wp_" + res.replaceAll("[^a-z0-9_]", "_").replaceAll("^_+", ""));
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, Compat.newTexture(ni));
            TEX.put(res, id);
            return id;
        } catch (Throwable t) { BAD.add(res); return null; }
    }

    public static boolean ready(Wp w) { return tex(w.base()) != null; }

    /** Rysuje tapetę „na okładkę” w prostokącie (x,y,w,h); t – czas w sekundach; motion – siła dryfu. */
    public static void draw(DrawContext ctx, Wp wp, float x, float y, float w, float h, float a, float motion) {
        float t = (System.currentTimeMillis() - T0) / 1000f;
        Identifier id = tex(wp.base());
        if (id == null) return;
        float s = Math.max(w / wp.bw(), h / wp.bh());
        float z = 1f + (wp.live() ? motion * (0.07f + 0.015f * (float) Math.sin(t * 0.25)) : 0f);
        float ds = s * z;
        float ox = x + (w - wp.bw() * ds) / 2f + (wp.live() ? (float) Math.sin(t * 0.21) * w * 0.012f * motion : 0f);
        float oy = y + (h - wp.bh() * ds) / 2f + (wp.live() ? (float) Math.cos(t * 0.17) * h * 0.012f * motion : 0f);
        Compat.push(ctx);
        Compat.translate(ctx, ox, oy);
        Compat.scale(ctx, ds, ds);
        Compat.texA(ctx, id, wp.bw(), wp.bh(), 0xFFFFFF, a);
        Compat.pop(ctx);
        if (wp.layer() == null || motion <= 0f) return;
        Identifier lid = tex(wp.layer());
        if (lid == null) return;
        float pulse = wp.mode() == 2 ? 0.55f + 0.45f * (float) Math.sin(t * 1.3) : 0.8f + 0.2f * (float) Math.sin(t * 0.7);
        float la = a * pulse;
        float tw = wp.bw() * ds;
        float off = wp.mode() == 1 ? (t * wp.speed() * ds) % tw : 0f;
        for (int k = 0; k < (wp.mode() == 1 ? 2 : 1); k++) {
            Compat.push(ctx);
            Compat.translate(ctx, ox - off + k * tw, oy);
            Compat.scale(ctx, ds, ds);
            Compat.texA(ctx, lid, wp.bw(), wp.bh(), 0xFFFFFF, la);
            Compat.pop(ctx);
        }
    }

    /** Pełny ekran (menu główne, tło moda, ekran ładowania). Zwraca false, gdy tapety nie da się narysować. */
    public static boolean full(DrawContext ctx, int w, int h, float a) {
        Wp wp = current();
        if (!ready(wp)) return false;
        draw(ctx, wp, 0, 0, w, h, a, 1f);
        return true;
    }
}
