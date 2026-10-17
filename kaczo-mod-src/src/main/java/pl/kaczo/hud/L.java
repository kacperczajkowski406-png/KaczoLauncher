package pl.kaczo.hud;

import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.Map;

/** Proste tłumaczenia: gdy język gry to polski (ustawiany przez launcher), napisy moda są po polsku. */
public class L {
    private static final Map<String, String> PL = new HashMap<>();

    static {
        String[][] p = {
            {"All", "Wszystkie"}, {"New", "Nowe"}, {"HUD", "HUD"}, {"PvP", "PvP"}, {"Utility", "Narzędzia"},
            {"MOD MENU", "MENU MODÓW"}, {"SETTINGS", "USTAWIENIA"}, {"EDIT HUD", "EDYCJA HUD"}, {"Search", "Szukaj"},
            {"Enabled", "Włączony"}, {"Disabled", "Wyłączony"}, {"No modules found", "Brak modułów"},
            {"Right Shift = open/close  |  Esc = close", "Prawy Shift = otwórz/zamknij  |  Esc = zamknij"},
            {"Back", "Wstecz"}, {"Reset", "Resetuj"}, {"Move on screen", "Przesuń na ekranie"}, {"Done", "Gotowe"},
            {"Drag the elements with the mouse. Esc = back.", "Przeciągaj elementy myszką. Esc = wróć."},
            {"Settings", "Ustawienia"}, {"ON", "WŁ"}, {"OFF", "WYŁ"},
            {"Size %", "Rozmiar %"}, {"Background", "Tło"}, {"Background color", "Kolor tła"}, {"Text color", "Kolor tekstu"},
            {"Color", "Kolor"}, {"Style", "Styl"}, {"Size", "Rozmiar"}, {"Gap", "Odstęp"}, {"Width", "Szerokość"},
            {"Rainbow", "Tęcza"}, {"Red", "Czerwony"}, {"Green", "Zielony"}, {"Blue", "Niebieski"}, {"Alpha", "Przezroczystość"},
            {"Presets", "Gotowe kolory"}, {"Preview", "Podgląd"},
            {"FPS", "FPS"}, {"Frames per second.", "Klatki na sekundę."},
            {"Ping", "Ping"}, {"Latency to the server.", "Opóźnienie do serwera."},
            {"Coordinates", "Koordynaty"}, {"Your XYZ position.", "Twoja pozycja XYZ."},
            {"Direction", "Kierunek"}, {"Where you are facing.", "W którą stronę patrzysz."},
            {"Clock", "Zegar"}, {"Real time.", "Prawdziwa godzina."},
            {"Day Counter", "Licznik dni"}, {"World day number.", "Numer dnia w świecie."},
            {"Speed Meter", "Prędkość"}, {"Blocks per second.", "Bloki na sekundę."},
            {"System Resources", "Zasoby systemu"}, {"Memory used by the game.", "Pamięć używana przez grę."},
            {"Server Address", "Adres serwera"}, {"Address of the current server.", "Adres aktualnego serwera."},
            {"CPS", "CPS"}, {"Clicks per second.", "Kliknięcia na sekundę."},
            {"Keystrokes", "Klawisze"}, {"Shows WASD, space and mouse buttons.", "Pokazuje WASD, spację i przyciski myszy."},
            {"Armor Status", "Pancerz"}, {"Your armor and durability.", "Twój pancerz i wytrzymałość."},
            {"Now Playing", "Teraz gra"}, {"Cover, title and lyrics of your music (Windows).", "Okładka, tytuł i tekst Twojej muzyki (Windows)."},
            {"Show lyrics", "Pokaż tekst"}, {"Lyrics lines", "Linijki tekstu"}, {"Show cover", "Pokaż okładkę"}, {"Hide when nothing plays", "Ukryj, gdy nic nie gra"},
            {"Custom Crosshair", "Własny celownik"}, {"Your own crosshair.", "Twój własny celownik."},
            {"Dot", "Kropka"}, {"Cross", "Krzyż"}, {"Circle", "Okrąg"}, {"Square", "Kwadrat"},
            {"Indicator", "Wskaźnik"}, {"Target color", "Kolor na celu"}, {"React on", "Reaguj na"}, {"Players", "Graczy"}, {"All mobs", "Wszystkie moby"},
            {"Zoom", "Przybliżenie"}, {"Hold C to zoom in.", "Przytrzymaj C, aby przybliżyć."}, {"Zoom FOV", "FOV przybliżenia"},
            {"Fullbright", "Pełna jasność"}, {"See in the dark (night vision effect).", "Widzisz w ciemności (noktowizor)."},
            {"Toggle Sprint", "Auto-sprint"}, {"Always sprint while moving forward.", "Zawsze biegnij, gdy idziesz do przodu."},
            {"Scoreboard", "Tablica wyników"}, {"Sidebar scoreboard: hide, scale, make transparent.", "Tablica z boku: ukryj, skaluj, przezroczystość."},
            {"Opacity %", "Przezroczystość %"}, {"Biome", "Biom"}, {"Name of the biome you are in.", "Nazwa biomu, w którym jesteś."},
            {"Light Level", "Poziom światła"}, {"Light level at your position.", "Poziom światła w Twoim miejscu."},
            {"World Time", "Czas w świecie"}, {"Minecraft time of day.", "Godzina w Minecrafcie."},
            {"Player Count", "Liczba graczy"}, {"Players online on the server.", "Gracze online na serwerze."},
            {"Session Timer", "Czas sesji"}, {"How long you have been playing.", "Jak długo grasz."},
            {"Potion Effects", "Efekty mikstur"}, {"Active effects and time left.", "Aktywne efekty i pozostały czas."},
            {"Item Counter", "Licznik przedmiotów"}, {"Count of the item in your hand.", "Ile masz przedmiotu z ręki."},
            {"Nick Badge", "Nick z logo"}, {"Your nickname with the Kaczo Client logo.", "Twój nick z logo Kaczo Client."},
            {"Show nick", "Pokaż nick"}, {"Show logo", "Pokaż logo"},
            {"Nothing playing", "Nic nie gra"}, {"Day", "Dzień"}, {"Singleplayer", "Gra jednoosobowa"},
            {"Health", "Zdrowie"}, {"Your hearts as a number.", "Twoje serca jako liczba."}, {"food", "jedzenia"},
            {"Hunger", "Głód"}, {"Food level.", "Poziom najedzenia."}, {"Experience", "Doświadczenie"}, {"Your XP level.", "Twój poziom XP."}, {"Level", "Poziom"},
            {"Rotation", "Obrót"}, {"Yaw and pitch.", "Obrót w poziomie i pionie."}, {"Chunk", "Chunk"}, {"Chunk coordinates.", "Współrzędne chunka."},
            {"Held Item", "Przedmiot w ręce"}, {"Name and durability of the item in hand.", "Nazwa i wytrzymałość przedmiotu w ręce."},
            {"Date", "Data"}, {"Today's date.", "Dzisiejsza data."}, {"FPS Limit", "Limit FPS"}, {"Shows the FPS limit from video settings.", "Limit FPS z ustawień wideo."}, {"Unlimited", "Bez limitu"},
            {"Toggle Sneak", "Auto-skradanie"}, {"Press the sneak key once to stay sneaking.", "Naciśnij raz klawisz skradania, by się skradać."},
            {"Damage Flash", "Błysk obrażeń"}, {"Red flash on the screen edges when you take damage.", "Czerwony błysk na brzegach ekranu po otrzymaniu obrażeń."}, {"Strength %", "Siła %"},
            {"Text shadow", "Cień tekstu"}, {"Accent bar", "Pasek akcentu"}, {"Padding", "Margines"}, {"Thickness", "Grubość"},
            {"HUD style", "Styl HUD"}, {"Glass", "Szkło"}, {"Flat", "Płaski"}, {"Corner radius", "Zaokrąglenie rogów"}, {"Glass border", "Szklana krawędź"},
            {"Glass buttons in game menus", "Szklane przyciski w menu gry"}, {"Logo on title screen", "Logo na ekranie tytułowym"},
            {"Intro animation", "Animacja wejścia"}, {"Title logo size %", "Rozmiar logo %"}, {"Show key hint", "Pokaż podpowiedź klawisza"},
            {"Kaczo Badge", "Plakietka Kaczo"}, {"Glass badge with the Kaczo Client logo.", "Szklana plakietka z logo Kaczo Client."},
            {"Right Shift = menu", "Prawy Shift = menu"}, {"Animate HUD on join", "Animuj HUD po wejściu"},
            {"Animated title background", "Animowane tło ekranu tytułowego"}, {"Custom loading screen", "Własny ekran ładowania"},
            {"Lines", "Linijki"}, {"Min width", "Min. szerokość"},
        };
        for (String[] e : p) PL.put(e[0], e[1]);
    }

    public static boolean pl() {
        try {
            String lang = MinecraftClient.getInstance().options.language;
            return lang != null && lang.toLowerCase().startsWith("pl");
        } catch (Throwable t) {
            return false;
        }
    }

    public static String t(String en) {
        if (!pl()) return en;
        String v = PL.get(en);
        return v == null ? en : v;
    }
}
