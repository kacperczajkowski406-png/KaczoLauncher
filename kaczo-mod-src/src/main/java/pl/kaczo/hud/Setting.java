package pl.kaczo.hud;

/** Jedno ustawienie modułu: przełącznik, liczba (suwak), kolor albo lista opcji. */
public class Setting {
    public enum Type { BOOL, INT, COLOR, ENUM }
    public static final int[] PALETTE = {0xFFFFFF, 0xFFE14A, 0x35E6FF, 0xFF4FD8, 0xFF8F3A, 0x55FF55, 0xFF5555, 0xAAAAAA};

    public final String key, label;
    public final Type type;
    public final double min, max;
    public final String[] opts;
    public final String def;

    private Setting(String key, String label, Type type, double min, double max, String[] opts, String def) {
        this.key = key; this.label = label; this.type = type; this.min = min; this.max = max; this.opts = opts; this.def = def;
    }

    public static Setting bool(String key, String label, boolean def) { return new Setting(key, label, Type.BOOL, 0, 1, null, String.valueOf(def)); }
    public static Setting num(String key, String label, double min, double max, double def) { return new Setting(key, label, Type.INT, min, max, null, String.valueOf((int) def)); }
    /** Kolor: "R,G,B,A,tęcza" (tęcza 0/1). */
    public static Setting color(String key, String label, String def) { return new Setting(key, label, Type.COLOR, 0, 255, null, def); }
    public static final String YELLOW = "255,225,74,255,0", BLACK_BG = "0,0,0,144,0", WHITE = "255,255,255,255,0";
    public static Setting choice(String key, String label, int defIndex, String... opts) { return new Setting(key, label, Type.ENUM, 0, opts.length - 1, opts, String.valueOf(defIndex)); }
}
