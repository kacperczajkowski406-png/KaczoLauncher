# Kaczo Client

Neonowy launcher Minecraft (Electron) – wiele instancji, Fabric, mody z Modrinth, logowanie
Microsoft (premium) i offline, język PL/EN.

## Uruchomienie (Windows)
1. Zainstaluj Node.js (LTS).
2. Kliknij dwukrotnie `Zainstaluj-i-utworz-skrot.bat` – zainstaluje zależności i utworzy skrót
   „Kaczo Client” na pulpicie i w menu Start.
   Ręcznie: `npm install` i `npm start`.
3. Prawdziwa aplikacja: `Zrob-instalator.bat` buduje i uruchamia instalator `.exe` (skrót na pulpicie + Start, odinstalowanie w Aplikacjach). Alternatywnie `npm run dist`.

## Funkcje
- Tylko wydania 1.21+ (i 26.x); starsze instancje można nadal edytować.
- Przy pierwszym wejściu do gry: narrator wyłączony, GUI scale 4, muzyka wyłączona.
- Automatyczny pakiet optymalizacji (Fabric API, Sodium, Lithium, FerriteCore, ImmediatelyFast,
  Entity Culling, ModernFix, Dynamic FPS) – mody niedostępne dla danej wersji są pomijane.
- Neonowe menu gry (pakiet zasobów `KaczoMenu`): logo, panorama, ciemne przyciski z ikonami
  (Realms przywrócony). W Ustawieniach można przełączyć na standardowe menu Minecrafta.
- Discord Rich Presence „Kaczo Client” – wymaga ID aplikacji z discord.com/developers
  (Rich Presence → Art Assets: obrazek „logo”); wpisz je w Panelu właściciela.
- Zmiana języka launchera zmienia też język gry (od następnego uruchomienia).
- Właściciel: konto Microsoft I_Kaczor_I (rozpoznawane po UUID) ma panel „Owner”;
  konta offline nie mogą używać tego nicku ani nicków podobnych.
- Głowa skina: premium – skin gracza, offline – Steve (wymaga internetu).

## Nie przetestowane na żywo
Logowanie Microsoft, prawdziwy start gry, wczytanie pakietu w prawdziwym Minecrafcie
(w tym położenie ikon na przyciskach), skrót PowerShell i instalator NSIS. Logikę sprawdzono na
atrapach (47 testów integracyjnych, 12 grup jednostkowych, interfejs w Chromium).

## Znane ograniczenia
- Mod Kaczo HUD (prawy Shift) nie jest zbudowany – patrz `kaczo-mod/README.txt`.
- Znajomi/wiadomości (wymagają serwera), radio, celownik i reszta funkcji moda – niezrobione.
- Status właściciela działa tylko lokalnie w launcherze (brak serwera).


Aktualizacja: tablicę wyników można przesuwać (EDIT HUD lub 'Move on screen' w jej ustawieniach), przezroczystość nie wpływa już na pasek HP. Okładka: gdy Windows nie poda miniatury, pobierana jest z wyszukiwarki iTunes. Dziennik: %APPDATA%\KaczoLauncher\nowplaying.log


Wygląd: launcher ma teraz motyw klockowy (ziemia, kamień, deski, zielone przyciski, losowe teksty „splash”). Tekstury: tools/make-mc-textures.py.
Mod: workflow buduje Kaczo HUD dla 1.21.1, 1.21.3 i 1.21.4 (wersja, która się nie skompiluje, jest pomijana). Launcher dobiera plik do wersji instancji.


Aktualizacja: nowoczesny wygląd „liquid glass” (domyślny; do wyboru też neon i klockowy w Ustawieniach), animowane wejście do gry, brak texture packa – logo na ekranie tytułowym, szklane przyciski i animacja są w modzie (Prawy Shift → Settings). Mod: nowe moduły (zdrowie, głód, XP, obrót, chunk, przedmiot w ręce, data, limit FPS, auto-skradanie, błysk obrażeń), grubość celownika, cień/pasek akcentu/margines dla każdego elementu HUD. Mod budowany dla 1.21–1.21.5.

Wersje moda: 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5 oraz (eksperymentalnie, bez płynnego zanikania animacji) 1.21.6–1.21.8. Wersje, które się nie zbudują, są pomijane.

Aktualizacje: aplikacja sama sprawdza GitHub Releases i aktualizuje się w tle (Ustawienia → Aktualizacje). Mod jest częścią instalatora, więc aktualizuje się razem z nią. Repozytorium musi być publiczne. W grze: animowany ekran ładowania zamiast czerwonego ekranu Mojang oraz szklane przyciski, logo i tło tylko na ekranie tytułowym (lobby).

Strona do pobrania: folder `site/` (GitHub Pages). Włącz ją raz: Settings → Pages → Source: GitHub Actions. Adres: https://TWOJ-NICK.github.io/NAZWA-REPO/ – strona sama znajduje repozytorium i pokazuje najnowszą wersję oraz listę zmian z wydań.
