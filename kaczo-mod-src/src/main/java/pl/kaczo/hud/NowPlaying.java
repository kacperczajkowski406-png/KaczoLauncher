package pl.kaczo.hud;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import com.google.gson.JsonArray;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * „Teraz gra”: co sekundę pyta Windows (System Media Transport Controls – używa go m.in. Spotify,
 * YouTube Music, przeglądarki) o tytuł, artystę, okładkę i pozycję utworu. Działa tylko na Windows.
 */
public class NowPlaying {
    public static final int COVER = 128;
    public static volatile String title = "", artist = "";
    public static volatile boolean playing = false;
    public static volatile double positionSec = 0, durationSec = 0;
    private static volatile long stamp = 0;               // kiedy odczytano pozycję
    private static volatile boolean coverDirty = false;
    private static volatile BufferedImage coverImage;
    private static Identifier coverId;
    private static NativeImageBackedTexture coverTex;
    private static boolean started = false;
    private static String lastKey = "", lastCover = "";

    private static final String SCRIPT = String.join("\n",
        "$ErrorActionPreference='Stop'",
        "[Console]::OutputEncoding=[System.Text.Encoding]::UTF8",
        "Add-Type -AssemblyName System.Runtime.WindowsRuntime",
        "$asTask=([System.WindowsRuntimeSystemExtensions].GetMethods()|?{$_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'})[0]",
        "function Await($t,$r){$m=$asTask.MakeGenericMethod($r);$n=$m.Invoke($null,@($t));$n.Wait(-1)|Out-Null;$n.Result}",
        "[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]|Out-Null",
        "[Windows.Storage.Streams.DataReader,Windows.Storage.Streams,ContentType=WindowsRuntime]|Out-Null",
        "$mgr=Await([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])",
        "$dir=$args[0];$log=$args[1];$n=0;$coverFor='';$coverPath=''",
        "while($true){",
        " try{",
        "  $s=$mgr.GetCurrentSession()",
        "  if($s -eq $null){Write-Output '{\"none\":true}'}else{",
        "   $p=Await($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])",
        "   $tl=$s.GetTimelineProperties();$pi=$s.GetPlaybackInfo()",
        "   $key=$p.Artist+'|'+$p.Title",
        "   if($key -ne $coverFor -and $p.Thumbnail -ne $null){try{",
        "    $rs=Await($p.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])",
        "    $st=[System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($rs)",
        "    $n++;$f=Join-Path $dir ('cover'+$n+'.img');$fs=[System.IO.File]::Create($f);$st.CopyTo($fs);$fs.Close();$st.Close()",
        "    $coverFor=$key;$coverPath=$f}catch{[System.IO.File]::AppendAllText($log,($_.Exception.Message+[Environment]::NewLine))}}",
        "   if($key -ne $coverFor){$cp=''}else{$cp=$coverPath}",
        "   $o=@{title=$p.Title;artist=$p.Artist;playing=($pi.PlaybackStatus -eq 4);pos=$tl.Position.TotalSeconds;age=([DateTimeOffset]::UtcNow-$tl.LastUpdatedTime).TotalSeconds;dur=($tl.EndTime-$tl.StartTime).TotalSeconds;cover=$cp}",
        "   Write-Output ($o|ConvertTo-Json -Compress)}",
        " }catch{[System.IO.File]::AppendAllText($log,($_.Exception.Message+[Environment]::NewLine));Write-Output '{\"none\":true}'}",
        " Start-Sleep -Milliseconds 250",
        "}");

    /** Dziennik diagnostyczny: %APPDATA%\\KaczoLauncher\\nowplaying.log (pomaga, gdy okładka się nie pokazuje). */
    static Path logFile() {
        String appdata = System.getenv("APPDATA");
        Path dir = appdata != null ? Path.of(appdata, "KaczoLauncher") : Path.of(System.getProperty("user.home"), ".kaczo-client");
        try { Files.createDirectories(dir); } catch (Exception ignored) { }
        return dir.resolve("nowplaying.log");
    }

    static void log(String msg) {
        try {
            Files.writeString(logFile(), java.time.LocalTime.now() + " " + msg + "\n",
                    StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) { }
    }

    public static void start() {
        if (started || !System.getProperty("os.name", "").toLowerCase().contains("win")) return;
        started = true;
        Thread t = new Thread(NowPlaying::run, "kaczo-nowplaying");
        t.setDaemon(true);
        t.start();
    }

    private static void run() {
        try {
            Path dir = Files.createTempDirectory("kaczohud");
            File ps = dir.resolve("np.ps1").toFile();
            
            Files.writeString(ps.toPath(), "﻿" + SCRIPT, StandardCharsets.UTF_8);
            Process p = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", ps.getAbsolutePath(), dir.toAbsolutePath().toString(), logFile().toString()).redirectErrorStream(true).start();
            Runtime.getRuntime().addShutdownHook(new Thread(p::destroyForcibly));
            Gson gson = new Gson();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (!line.startsWith("{")) continue;
                    JsonObject o = gson.fromJson(line, JsonObject.class);
                    if (o.has("none")) { title = ""; artist = ""; playing = false; lastKey = ""; continue; }
                    String ti = o.get("title").getAsString(), ar = o.get("artist").getAsString();
                    playing = o.get("playing").getAsBoolean();
                    double age = o.has("age") && !o.get("age").isJsonNull() ? o.get("age").getAsDouble() : 0;
                    // pozycja zgłaszana przez odtwarzacz jest z chwili LastUpdatedTime – dodajemy upływ czasu, żeby nie było opóźnienia
                    positionSec = o.get("pos").getAsDouble() + (playing ? Math.max(0, Math.min(age, 10)) : 0);
                    durationSec = o.get("dur").getAsDouble();
                    stamp = System.currentTimeMillis();
                    String key = ar + "|" + ti;
                    if (!key.equals(lastKey)) {
                        lastKey = key;
                        title = ti; artist = ar;
                        Lyrics.fetch(ar, ti, durationSec);
                        coverImage = null; lastCover = ""; coverDirty = true;
                        log("utwór: " + ar + " - " + ti);
                        fallbackCover(key, ar, ti);
                    }
                    String cp = o.has("cover") && !o.get("cover").isJsonNull() ? o.get("cover").getAsString() : "";
                    if (!cp.isEmpty() && !cp.equals(lastCover)) {
                        lastCover = cp;
                        try {
                            BufferedImage raw = ImageIO.read(new File(cp));
                            coverImage = scale(raw);
                            log("okładka " + cp + " -> " + (raw == null ? "NIEOBSŁUGIWANY FORMAT" : raw.getWidth() + "x" + raw.getHeight()));
                        } catch (Exception e) { coverImage = null; log("okładka błąd: " + e); }
                        coverDirty = true;
                    }
                }
            }
        } catch (Exception e) { log("start błąd: " + e); }
    }

    /**
     * Zapasowa okładka: gdy Windows nie poda miniatury w ciągu kilku sekund, szukamy okładki w publicznym
     * wyszukiwaniu iTunes (bez klucza). Używane tylko gdy nie ma okładki z odtwarzacza.
     */
    private static void fallbackCover(String key, String artist, String title) {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(2500);
                if (!key.equals(lastKey) || coverImage != null) return;
                HttpClient http = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
                String q = URLEncoder.encode(artist + " " + title, StandardCharsets.UTF_8);
                HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create("https://itunes.apple.com/search?entity=song&limit=1&term=" + q))
                        .timeout(java.time.Duration.ofSeconds(8)).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                JsonObject o = new Gson().fromJson(r.body(), JsonObject.class);
                JsonArray res = o == null ? null : o.getAsJsonArray("results");
                if (res == null || res.size() == 0) { log("okładka zapasowa: brak wyników dla " + artist + " - " + title); return; }
                String url = res.get(0).getAsJsonObject().get("artworkUrl100").getAsString().replace("100x100bb", "300x300bb");
                HttpResponse<byte[]> img = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(java.time.Duration.ofSeconds(10)).build(),
                        HttpResponse.BodyHandlers.ofByteArray());
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(img.body()));
                if (bi == null || !key.equals(lastKey) || coverImage != null) return;
                coverImage = scale(bi);
                coverDirty = true;
                log("okładka zapasowa (iTunes) załadowana: " + url);
            } catch (Exception e) {
                log("okładka zapasowa błąd: " + e);
            }
        }, "kaczo-cover");
        t.setDaemon(true);
        t.start();
    }

    private static BufferedImage scale(BufferedImage src) {
        if (src == null) return null;
        BufferedImage out = new BufferedImage(COVER, COVER, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, COVER, COVER, null);
        g.dispose();
        return out;
    }

    /** Aktualna pozycja w sekundach (z uwzględnieniem czasu od ostatniego odczytu). */
    public static double position() {
        return positionSec + (playing ? (System.currentTimeMillis() - stamp) / 1000.0 : 0);
    }

    /** Wywoływane z wątku renderowania: ładuje okładkę do tekstury. Zwraca null, gdy brak okładki. */
    public static Identifier cover() {
        if (coverDirty) {
            coverDirty = false;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (coverId != null) { mc.getTextureManager().destroyTexture(coverId); coverId = null; coverTex = null; }
            BufferedImage img = coverImage;
            if (img != null) {
                NativeImage ni = new NativeImage(COVER, COVER, false);
                for (int y = 0; y < COVER; y++)
                    for (int x = 0; x < COVER; x++) Compat.pixel(ni, x, y, img.getRGB(x, y));
                coverTex = Compat.newTexture(ni);
                coverId = Identifier.of("kaczohud", "cover");
                mc.getTextureManager().registerTexture(coverId, coverTex);
            }
        }
        return coverId;
    }
}
