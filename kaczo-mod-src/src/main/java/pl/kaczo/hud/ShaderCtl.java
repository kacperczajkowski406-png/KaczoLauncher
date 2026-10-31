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
    private static void extractPack(MinecraftClient mc) throws IOException {
        Path dst = FabricLoader.getInstance().getGameDir().resolve("shaderpacks").resolve(PACK);
        Path src = FabricLoader.getInstance().getModContainer("kaczohud").orElseThrow()
                .findPath("assets/kaczohud/shaderpack/" + PACK).orElseThrow(() -> new IOException("brak paczki w modzie"));
        try (Stream<Path> w = Files.walk(src)) {
            for (Path p : (Iterable<Path>) w::iterator) {
                Path t = dst.resolve(src.relativize(p).toString());
                if (Files.isDirectory(p)) Files.createDirectories(t);
                else { Files.createDirectories(t.getParent()); Files.copy(p, t, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
            }
        }
        int style = Math.max(0, Math.min(2, Modules.byId("shaders").num("style")));
        Files.writeString(dst.getParent().resolve(PACK + ".txt"), "STYLE=" + style + "\n");
        appliedStyle = style;
    }

    /** Włącza/wyłącza shadery. Zwraca false, gdy nie da się ich włączyć (np. brak Iris). */
    public static boolean apply(boolean on) {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            if (!irisPresent()) {
                if (on) toast(mc, "Brak Iris – uruchom grę przez Kaczo Client (instaluje go sam)");
                return false;
            }
            if (on) extractPack(mc);
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            Object cfg = iris.getMethod("getIrisConfig").invoke(null);
            cfg.getClass().getMethod("setShaderPackName", String.class).invoke(cfg, PACK);
            try { cfg.getClass().getMethod("save").invoke(cfg); } catch (Throwable ignored) { }
            Object api = Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("getInstance").invoke(null);
            Object ac = api.getClass().getMethod("getConfig").invoke(api);
            Class.forName("net.irisshaders.iris.api.v0.IrisApiConfig").getMethod("setShadersEnabledAndApply", boolean.class).invoke(ac, on);
            return true;
        } catch (Throwable t) {
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
            int style = Math.max(0, Math.min(2, m.num("style")));
            if (style != appliedStyle && appliedStyle != -1) apply(true);
        }
    }
}
