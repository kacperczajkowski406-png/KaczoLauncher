package pl.kaczo.hud;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Teksty z lrclib.net (bezpłatne, bez klucza) – linijki zsynchronizowane z czasem utworu. */
public class Lyrics {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final Pattern LINE = Pattern.compile("^\\[(\\d+):(\\d+(?:\\.\\d+)?)](.*)$");
    private static volatile List<double[]> times = new ArrayList<>();
    private static volatile List<String> lines = new ArrayList<>();
    private static volatile List<String> plain = new ArrayList<>();

    public static void fetch(String artist, String title, double duration) {
        times = new ArrayList<>();
        lines = new ArrayList<>();
        plain = new ArrayList<>();
        Thread t = new Thread(() -> {
            try {
                String url = "https://lrclib.net/api/get?artist_name=" + enc(artist) + "&track_name=" + enc(title)
                        + (duration > 0 ? "&duration=" + Math.round(duration) : "");
                HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", "KaczoClient/1.0").timeout(Duration.ofSeconds(8)).build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (r.statusCode() != 200) return;
                JsonObject o = new Gson().fromJson(r.body(), JsonObject.class);
                if (o == null) return;
                if ((!o.has("syncedLyrics") || o.get("syncedLyrics").isJsonNull()) && o.has("plainLyrics") && !o.get("plainLyrics").isJsonNull()) {
                    List<String> pl = new ArrayList<>();
                    for (String raw : o.get("plainLyrics").getAsString().split("\n")) pl.add(raw.trim());
                    if (title.equals(NowPlaying.title)) plain = pl;
                    return;
                }
                if (!o.has("syncedLyrics") || o.get("syncedLyrics").isJsonNull()) return;
                List<double[]> ts = new ArrayList<>();
                List<String> ls = new ArrayList<>();
                for (String raw : o.get("syncedLyrics").getAsString().split("\n")) {
                    Matcher m = LINE.matcher(raw.trim());
                    if (!m.matches()) continue;
                    ts.add(new double[]{Integer.parseInt(m.group(1)) * 60 + Double.parseDouble(m.group(2))});
                    ls.add(m.group(3).trim());
                }
                // nie nadpisuj, jeśli w międzyczasie zmienił się utwór
                if (title.equals(NowPlaying.title)) { times = ts; lines = ls; }
            } catch (Exception ignored) { }
        }, "kaczo-lyrics");
        t.setDaemon(true);
        t.start();
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    /** Pokazuj linijkę odrobinę wcześniej, żeby tekst był zgrany z dźwiękiem mimo opóźnień odczytu/renderu. */
    private static final double LEAD = 0.3;

    public static boolean has() { return !lines.isEmpty() || !plain.isEmpty(); }

    /**
     * Okno linijek wokół aktualnej: zwraca pary {tekst, "1" gdy to linijka aktualna}.
     * Przy tekście bez synchronizacji przewija go proporcjonalnie do postępu utworu.
     */
    public static List<String[]> window(int count) {
        List<String[]> out = new ArrayList<>();
        List<String> ls = lines, pl = plain;
        boolean synced = !ls.isEmpty();
        List<String> src = synced ? ls : pl;
        if (src.isEmpty() || count < 1) return out;
        int cur;
        if (synced) {
            double pos = NowPlaying.position() + LEAD;
            cur = -1;
            List<double[]> ts = times;
            for (int i = 0; i < ts.size() && i < ls.size(); i++) if (ts.get(i)[0] <= pos) cur = i; else break;
        } else {
            double d = NowPlaying.durationSec;
            cur = d > 0 ? (int) Math.min(src.size() - 1, Math.max(0, NowPlaying.position() / d * src.size())) : 0;
        }
        int before = (count - 1) / 3, after = count - 1 - before;
        int from = Math.max(0, cur - before);
        int to = Math.min(src.size(), Math.max(from + count, cur + after + 1));
        from = Math.max(0, Math.min(from, to - count));
        for (int i = from; i < to && out.size() < count; i++) out.add(new String[]{src.get(i), i == cur && synced ? "1" : "0"});
        return out;
    }

    /** Linijka tekstu dla aktualnej pozycji ("" gdy brak tekstu). */
    public static String current() {
        List<double[]> ts = times;
        List<String> ls = lines;
        if (ts.isEmpty()) return "";
        double pos = NowPlaying.position() + LEAD;
        int idx = -1;
        for (int i = 0; i < ts.size() && i < ls.size(); i++) if (ts.get(i)[0] <= pos) idx = i; else break;
        return idx < 0 ? "" : ls.get(idx);
    }
}
