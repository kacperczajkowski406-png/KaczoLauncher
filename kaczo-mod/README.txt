Kaczo HUD – miejsce na zbudowane jary moda (Fabric).

Launcher szuka tu plików  kaczo-hud-<wersja MC>.jar  (np. kaczo-hud-1.21.4.jar) i przy starcie gry
kopiuje pasujący do mods/kaczo-hud.jar. Bez jara dla danej wersji mod jest pomijany.

Kod moda jest w folderze kaczo-mod-src (Fabric, Minecraft 1.21.4). Jest NIEPRZETESTOWANY –
nie został jeszcze skompilowany. Zbudujesz go przez GitHub Actions (.github/workflows/build.yml)
albo lokalnie: w kaczo-mod-src uruchom  gradle build  (Java 21), a jar z build/libs skopiuj tutaj
jako kaczo-hud-1.21.4.jar.
Dla innych wersji MC trzeba zmienić gradle.properties i prawdopodobnie poprawić kod.

Menu moda (prawy Shift, w grze): kafelki modułów z przełącznikiem Enabled/Disabled, ustawieniami (zębatka),
ulubionymi (serduszko), filtrami All/New/HUD/PvP/Utility i wyszukiwarką. Moduły: FPS, Ping, Coordinates,
Direction, Clock, Day Counter, Speed Meter, System Resources, Server Address, CPS, Keystrokes, Armor Status,
Now Playing (okładka, tytuł, tekst – Windows), Custom Crosshair, Zoom (klawisz C), Fullbright, Toggle Sprint,
Scoreboard (wyłącz = ukryj). EDIT HUD: przeciąganie elementów myszką. Wszystko zapisuje się w config/kaczohud.json.
Mixiny (celownik, scoreboard) mają require=0: gdy nazwa metody się nie zgadza, po prostu nie działają, ale gra się nie wywala.

Nowości: kolory (RGB + tęcza) dla każdej opcji, żółty HUD domyślnie, „Teraz gra” z okładką i jedną linijką tekstu
(karta płynnie poszerza się w bok, gdy linijka jest długa), Biome, Light Level, World Time, Player Count, Session Timer, Potion Effects,
Item Counter, Nick Badge (nick + logo Kaczo Client, można ukryć nick/logo), wskaźnik celownika (czerwony na graczu),
scoreboard z rozmiarem i przezroczystością, język polski gdy launcher ma polski.
Ustawienia są wspólne dla wszystkich instalacji: %APPDATA%\KaczoLauncher\hud-settings.json.
Logo obok nicka INNYCH graczy wymaga serwera, który wie, kto używa Kaczo Client – tego nie ma.


Aktualizacja: tablicę wyników można przesuwać (EDIT HUD lub 'Move on screen' w jej ustawieniach), przezroczystość nie wpływa już na pasek HP. Okładka: gdy Windows nie poda miniatury, pobierana jest z wyszukiwarki iTunes. Dziennik: %APPDATA%\KaczoLauncher\nowplaying.log


Aktualizacja: nowoczesny wygląd „liquid glass” (domyślny; do wyboru też neon i klockowy w Ustawieniach), animowane wejście do gry, brak texture packa – logo na ekranie tytułowym, szklane przyciski i animacja są w modzie (Prawy Shift → Settings). Mod: nowe moduły (zdrowie, głód, XP, obrót, chunk, przedmiot w ręce, data, limit FPS, auto-skradanie, błysk obrażeń), grubość celownika, cień/pasek akcentu/margines dla każdego elementu HUD. Mod budowany dla 1.21–1.21.5.

Wersje moda: 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5 oraz (eksperymentalnie, bez płynnego zanikania animacji) 1.21.6–1.21.8. Wersje, które się nie zbudują, są pomijane.

Aktualizacje: aplikacja sama sprawdza GitHub Releases i aktualizuje się w tle (Ustawienia → Aktualizacje). Mod jest częścią instalatora, więc aktualizuje się razem z nią. Repozytorium musi być publiczne. W grze: animowany ekran ładowania zamiast czerwonego ekranu Mojang oraz szklane przyciski, logo i tło tylko na ekranie tytułowym (lobby).
