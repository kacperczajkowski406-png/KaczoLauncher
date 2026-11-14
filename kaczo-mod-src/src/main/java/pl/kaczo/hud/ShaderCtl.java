package pl.kaczo.hud;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/** Włączanie i wyłączanie shaderów Kaczo jednym kliknięciem (przez Iris, bez wchodzenia w menu shaderów). */
public final class ShaderCtl {
    private ShaderCtl() { }

    public static final String PACK = "KaczoShaders";
    private static int appliedStyle = -1;
    private static boolean synced;

    public static boolean irisPresent() {
        try { Class.forName("net.irisshaders.iris.api.v0.IrisApi"); return true; } catch (Throwable t) { return false; }
    }

    /** Rozpakowuje paczkę shaderów z moda do folderu shaderpacks (zawsze świeża wersja). */
    public static boolean skyOnly() { return Modules.byId("shaders").num("mode") == 1; }
    public static String packName() { return skyOnly() ? "KaczoSky" : PACK; }

    private static void extractPack(MinecraftClient mc) throws IOException {
        boolean only = skyOnly();
        Path dst = FabricLoader.getInstance().getGameDir().resolve("shaderpacks").resolve(packName());
        Path src = FabricLoader.getInstance().getModContainer("kaczohud").orElseThrow()
                .findPath("assets/kaczohud/shaderpack/" + PACK).orElseThrow(() -> new IOException("brak paczki w modzie"));
        try (Stream<Path> w = Files.walk(src)) {
            for (Path p : (Iterable<Path>) w::iterator) {
                String rel = src.relativize(p).toString().replace('\\', '/');
                // „Samo niebo”: tylko program nieba + biblioteki – reszta gry zostaje waniliowa (bez cieni, świateł i post-processingu)
                if (only && !Files.isDirectory(p) && !(rel.startsWith("shaders/lib/") || rel.startsWith("shaders/gbuffers_skybasic."))) continue;
                Path t = dst.resolve(src.relativize(p).toString());
                if (Files.isDirectory(p)) Files.createDirectories(t);
                else { Files.createDirectories(t.getParent()); Files.copy(p, t, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
            }
        }
        int style = Math.max(0, Math.min(2, Modules.byId("shaders").num("style")));
        Skies.Sky sk = Skies.current(only);
        boolean wall = !sk.id().equals("kaczo");
        String opts = "STYLE=" + style + "\n";
        Path td = dst.resolve("shaders").resolve("textures");
        Files.createDirectories(td);
        String W = "/assets/kaczohud/textures/wallpaper/";
        if (!wall) {
            copyRes(W + "none.png", td.resolve("kwall.png"));
            copyRes(W + "none.png", td.resolve("kwall_l.png"));
        } else if (sk.atlas() != null) {
            // animowane niebo z klatek: jeden atlas 4×4, shader miesza kolejne klatki
            copyRes("/assets/kaczohud/textures/sky/" + sk.atlas() + ".png", td.resolve("kwall.png"));
            copyRes(W + "none.png", td.resolve("kwall_l.png"));
            opts += "WALL=1\nWATLAS=1\nWFRAMES=" + sk.frames() + "\nWFPS=" + sk.fps10() + "\n";
        } else {
            Wallpapers.Wp wp = Wallpapers.ALL.stream().filter(x -> x.id().equals(sk.wallpaperId())).findFirst().orElse(Wallpapers.ALL.get(0));
            copyRes(wp.base().startsWith("/") ? wp.base() : W + wp.base() + ".png", td.resolve("kwall.png"));
            copyRes(W + (wp.layer() != null ? wp.layer() : "none") + ".png", td.resolve("kwall_l.png"));
            int sp = wp.layer() == null ? 0 : (wp.id().equals("aurora") ? 47 : wp.id().equals("nebula") ? 26 : wp.id().equals("sakura") ? 73 : 0);
            opts += "WALL=1\nWMODE=" + (wp.layer() == null ? 0 : wp.mode()) + "\nWSPEED=" + sp + "\n";
        }
        if (only) {
            opts += "SKYONLY=1\n";
            Files.writeString(dst.resolve("shaders").resolve("shaders.properties"), "customTexture.kWall=textures/kwall.png\ncustomTexture.kWallL=textures/kwall_l.png\n");
        }
        Files.writeString(dst.getParent().resolve(packName() + ".txt"), opts);
        appliedStyle = stateKey();
    }

    private static void copyRes(String res, Path to) throws IOException {
        try (java.io.InputStream in = ShaderCtl.class.getResourceAsStream(res)) {
            if (in == null) throw new IOException("brak " + res);
            Files.copy(in, to, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static int stateKey() {
        Module m = Modules.byId("shaders");
        int style = Math.max(0, Math.min(2, m.num("style"))) + (m.num("mode") == 1 ? 5_000_000 : 0);
        return style + 10 + m.num("sky") * 10 + (m.num("mode") == 1 ? 1000 : 0);
    }

    /** Włącza/wyłącza shadery. Zwraca false, gdy nie da się ich włączyć (np. brak Iris). */
    public static boolean apply(boolean on) {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            if (!irisPresent()) {
                if (on) toast(mc, "Brak Iris – uruchom grę przez Kaczo Client (instaluje go sam)");
                return false;
            }
            System.out.println("[KaczoShaders] apply on=" + on);
            if (on) extractPack(mc);
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            Object cfg = iris.getMethod("getIrisConfig").invoke(null);
            cfg.getClass().getMethod("setShaderPackName", String.class).invoke(cfg, packName());
            try { cfg.getClass().getMethod("save").invoke(cfg); } catch (Throwable ignored) { }
            Object api = Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("getInstance").invoke(null);
            Object ac = api.getClass().getMethod("getConfig").invoke(api);
            Class.forName("net.irisshaders.iris.api.v0.IrisApiConfig").getMethod("setShadersEnabledAndApply", boolean.class).invoke(ac, on);
            return true;
        } catch (Throwable t) {
            System.out.println("[KaczoShaders] apply failed: " + t); t.printStackTrace(System.out);
            toast(mc, "Nie udało się przełączyć shaderów");
            return false;
        }
    }

    private static void toast(MinecraftClient mc, String msg) {
        try { if (mc.inGameHud != null) mc.inGameHud.setOverlayMessage(Text.literal(msg), false); } catch (Throwable ignored) { }
    }

    /** Wołane co tick: po starcie gry odtwarza zapisany stan, a przy zmianie wyglądu przeładowuje shadery. */
    public static void tick(MinecraftClient mc) {
        Module m = Modules.byId("shaders");
        if (m == null || !irisPresent()) return;
        if (!synced) {
            synced = true;
            if (m.enabled()) apply(true);
            return;
        }
        if (m.enabled()) {
            if (stateKey() != appliedStyle && appliedStyle != -1) apply(true);
        }
    }
}
