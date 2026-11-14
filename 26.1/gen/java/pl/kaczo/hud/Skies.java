package pl.kaczo.hud;

import java.util.List;

/**
 * Nieba do shaderów – osobna lista, niezależna od tapet menu.
 * Rodzaj „atlas”: animowane niebo z klatek (siatka 4×4 w jednym PNG, shader płynnie przechodzi między klatkami).
 * Rodzaj „tapeta”: gotowe obrazy z warstwą ruchu (jak w tapetach), tu użyte jako niebo.
 */
public final class Skies {
    private Skies() { }

    public record Sky(String id, String name, String atlas, int frames, int fps10, String wallpaperId) {
        boolean isAtlas() { return atlas != null; }
    }

    public static final List<Sky> ALL = List.of(
            new Sky("kaczo", "Kaczo (default)", null, 0, 0, null),
            new Sky("night", "Night Clouds", "night", 16, 3, null),
            new Sky("waves", "Wave Lines", "anim2", 16, 18, null),
            new Sky("flow", "Flow Lines", "anim3", 16, 16, null),
            new Sky("storm", "Storm Lightning", "rain", 11, 149, null),
            new Sky("star", "Shooting Star", "stars", 12, 100, null),
            new Sky("aurora", "Northern Lights", null, 0, 0, "aurora"),
            new Sky("nebula", "Nebula", null, 0, 0, "nebula"),
            new Sky("galaxy", "Galaxy", null, 0, 0, "galaxy"),
            new Sky("sakura", "Sakura Dusk", null, 0, 0, "sakura"),
            new Sky("synth", "Synthwave", null, 0, 0, "synthwave"));

    public static String[] names() { return ALL.stream().map(Sky::name).toArray(String[]::new); }

    /** Wybrane niebo; w trybie „Samo niebo” domyślne niebo Kaczo zastępujemy pierwszym animowanym. */
    public static Sky current(boolean skyOnly) {
        int i = Math.max(0, Math.min(ALL.size() - 1, Modules.byId("shaders").num("sky")));
        if (i == 0 && skyOnly) i = 1;
        return ALL.get(i);
    }
}
