package pl.kaczo.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Stan wszystkich modułów. Zapisywany we WSPÓLNYM pliku (folder danych Kaczo Client), dzięki czemu ustawienia
 * gracza są takie same we wszystkich instalacjach. Gdy się nie da – w config/kaczohud.json danej instancji.
 */
public class Config {
    public static class State {
        public Boolean enabled;                 // null = domyślnie
        public boolean fav;
        public int x = -1, y = -1;              // -1 = pozycja domyślna
        public Map<String, String> vals = new HashMap<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path LOCAL = FabricLoader.getInstance().getConfigDir().resolve("kaczohud.json");
    public Map<String, State> mods = new HashMap<>();
    public static Config I = new Config();

    public static State st(String id) { return I.mods.computeIfAbsent(id, k -> new State()); }

    /** Wspólny plik: %APPDATA%/KaczoLauncher/hud-settings.json (Windows) albo ~/.kaczo-client/ (inne systemy). */
    private static Path shared() {
        String appdata = System.getenv("APPDATA");
        Path dir = appdata != null && !appdata.isEmpty()
                ? Path.of(appdata, "KaczoLauncher")
                : Path.of(System.getProperty("user.home"), ".kaczo-client");
        return dir.resolve("hud-settings.json");
    }

    private static Config read(Path p) {
        try (Reader r = Files.newBufferedReader(p)) {
            Config c = GSON.fromJson(r, Config.class);
            return c != null && c.mods != null ? c : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static void load() {
        Config c = read(shared());
        if (c == null) c = read(LOCAL); // migracja ze starego, lokalnego pliku
        if (c != null) I = c;
    }

    public static void save() {
        try {
            Path p = shared();
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) { GSON.toJson(I, w); }
        } catch (Exception e) {
            try (Writer w = Files.newBufferedWriter(LOCAL)) { GSON.toJson(I, w); } catch (Exception ignored) { }
        }
    }
}
