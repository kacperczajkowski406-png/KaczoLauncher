package pl.kaczo.hud;

import net.minecraft.client.Minecraft;

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
            {"Rainbow", "Tęcza"}, {"Storm", "Burza"}, {"Liquid glass", "Płynne szkło"}, {"Star trails", "Smugi gwiazd"}, {"Red", "Czerwony"}, {"Green", "Zielony"}, {"Blue", "Niebieski"}, {"Alpha", "Przezroczystość"},
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
            {"Favorites", "Ulubione"}, {"Visual", "Wizualne"}, {"Modules", "Moduły"}, {"Appearance", "Wygląd"}, {"Edit HUD", "Edytor HUD"}, {"About", "O modzie"},
            {"Esc = close", "Esc = zamknij"}, {"modules", "modułów"}, {"enabled", "włączonych"}, {"NEW", "NOWE"},
            {"Look and behaviour of the whole client", "Wygląd i zachowanie całego klienta"},
            {"Right Shift – open this menu anywhere in the game.", "Prawy Shift – otwiera to menu w dowolnym miejscu gry."},
            {"Edit HUD: drag elements, right-click an element to configure it.", "Edytor HUD: przeciągaj elementy, a prawym przyciskiem otwierasz ich ustawienia."},
            {"Glass buttons and the animated background are drawn only on the title screen.", "Szklane przyciski i animowane tło są tylko w lobby (ekran tytułowy)."},
            {"Music: \"Kaczo Theme\" is an original track made for this client – free to use.", "Muzyka: „Kaczo Theme” to oryginalny utwór stworzony dla tego klienta – bez praw autorskich."},
            {"Settings are shared between all your Kaczo Client instances.", "Ustawienia są wspólne dla wszystkich Twoich instancji Kaczo Client."},
            {"Elements", "Elementy"}, {"Snap", "Przyciąganie"}, {"Reset all", "Resetuj wszystko"}, {"Reset position", "Resetuj pozycję"},
            {"Drag elements. Right-click an element to configure it. Hold Shift = no snapping.", "Przeciągaj elementy. Prawy przycisk = ustawienia elementu. Shift = bez przyciągania."},
            {"Title screen", "Ekran tytułowy"}, {"Glass buttons on title screen", "Szklane przyciski w lobby"}, {"Music", "Muzyka"},
            {"Kaczo music", "Muzyka Kaczo"}, {"Off", "Wyłączona"}, {"Menu", "W menu"}, {"Menu + game", "W menu i w grze"}, {"Music volume", "Głośność muzyki"},
            {"Reach Display", "Zasięg ataku"}, {"Distance of your last hit.", "Odległość ostatniego trafienia."},
            {"Combo Display", "Combo"}, {"Consecutive hits in a row.", "Trafienia z rzędu."}, {"Combo", "Combo"}, {"best", "rekord"},
            {"Mouse Strokes", "Ruch myszy"}, {"Shows your mouse movement.", "Pokazuje ruch Twojej myszy."},
            {"PvP Items", "Przedmioty PvP"}, {"Totems, crystals, apples and pearls in your inventory.", "Totemy, kryształy, jabłka i perły w ekwipunku."},
            {"Totem", "Totem"}, {"End crystal", "Kryształ Endu"}, {"Golden apple", "Złote jabłko"}, {"Enchanted apple", "Zaczarowane jabłko"},
            {"XP bottle", "Butelka XP"}, {"Ender pearl", "Perła Endu"},
            {"Inventory", "Ekwipunek"}, {"Your inventory on the screen.", "Twój ekwipunek na ekranie."},
            {"Death Info", "Miejsce śmierci"}, {"Coordinates of your last death.", "Współrzędne ostatniej śmierci."}, {"Death", "Śmierć"}, {"No death yet", "Jeszcze nie zginąłeś"},
            {"Waypoint", "Znacznik"}, {"Distance and direction to a saved point (B = set, Shift+B = clear).", "Odległość i kierunek do zapisanego punktu (B = ustaw, Shift+B = usuń)."},
            {"Waypoint set", "Znacznik ustawiony"}, {"Waypoint cleared", "Znacznik usunięty"}, {"No waypoint (press B)", "Brak znacznika (naciśnij B)"},
            {"Perspective (Freelook)", "Widok wolny (Freelook)"}, {"Hold Left Alt to look around without turning.", "Przytrzymaj lewy Alt, by rozglądać się bez obracania postaci."},
            {"Kaczo Sky", "Niebo Kaczo"}, {"Decorative sky overlay: duck rain, aurora, stars, sunset and more.", "Ozdobne niebo: kaczki, zorza, gwiazdy, zachód słońca i więcej."}, {"Duck rain", "Deszcz kaczek"}, {"Aurora", "Zorza"}, {"Starfield", "Gwiazdy"}, {"Sunset", "Zachód słońca"}, {"Candy", "Cukierkowe"}, {"Matrix", "Matrix"}, {"Intensity", "Intensywność"}, {"Speed", "Prędkość"}, {"Ducks", "Kaczki"}, {"Ducks on any style", "Kaczki w każdym stylu"}, {"Duck count", "Liczba kaczek"}, {"Duck size", "Rozmiar kaczek"}, {"Look", "Wygląd"}, {"Density", "Gęstość"}, {"Follow camera", "Podążaj za kamerą"}, {"Soft horizon", "Miękki horyzont"}, {"Shader look", "Klimat shaderów"}, {"Color grading", "Korekcja kolorów"}, {"Vignette", "Winieta"}, {"Tint", "Odcień"}, {"HUD Color", "Kolor HUD"}, {"One color for the whole HUD: all elements and their accents switch to it.", "Jeden kolor dla całego HUD: wszystkie elementy i ich akcenty zmieniają się na niego."}, {"Theme color", "Kolor motywu"}, {"Shader effects", "Efekty shaderowe"}, {"Glowing particles (bloom)", "Świecące cząsteczki (bloom)"}, {"Light rays", "Promienie światła"}, {"Horizon bloom", "Poświata horyzontu"}, {"Replace real sky color", "Podmień prawdziwy kolor nieba"}, {"Lyrics in the world (anchored)", "Tekst w świecie (zakotwiczony)"}, {"Storm", "Burza"}, {"Rainbow", "Tęcza"}, 
            {"No Fire Overlay","Bez ognia na ekranie"},{"Hides the fire on your screen when you are burning.","Ukrywa ogień na ekranie, gdy się palisz."},{"Clear Water","Czysta woda"},{"Removes the blue overlay when you are underwater.","Usuwa niebieską nakładkę pod wodą."},{"Background opacity %","Przezroczystość tła %"},{"Show numbers","Pokaż liczby"},{"Accent line","Kreska pod tytułem"},
            {"Hitboxes", "Hitboxy"}, {"Shows entity hitboxes (like F3 + B).", "Pokazuje hitboxy (jak F3 + B)."},
            {"No Hurt Camera", "Bez drgania po obrażeniach"}, {"No camera shake when you take damage.", "Kamera nie drży, gdy dostajesz obrażenia."},
            {"No View Bobbing", "Bez bujania widoku"}, {"Turns off the swaying of the view while walking.", "Wyłącza bujanie widoku podczas chodzenia."},
            {"Hide Boss Bar", "Ukryj pasek bossa"}, {"Hides boss health bars at the top of the screen.", "Ukrywa paski życia bossów u góry ekranu."},
            {"FOV Changer", "Zmiana FOV"}, {"Sets your field of view and the sprint/speed FOV effect.", "Ustawia pole widzenia i efekt FOV przy sprincie."},
            {"Field of view", "Pole widzenia"}, {"Speed FOV effect", "Efekt FOV przy szybkości"},
        };
        for (String[] e : p) PL.put(e[0], e[1]);
    }

    public static boolean pl() {
        try {
            String lang = Minecraft.getInstance().options.languageCode;
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
