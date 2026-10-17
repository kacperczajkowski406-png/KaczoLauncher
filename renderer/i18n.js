'use strict';

// Słownik interfejsu launchera. Język gry (options.txt) ustawia proces główny na podstawie tego samego wyboru.
const I18N = {
  pl: {
    'nav.play': 'Graj', 'nav.instances': 'Instancje', 'nav.mods': 'Mody', 'nav.accounts': 'Konta', 'nav.settings': 'Ustawienia', 'nav.owner': 'Panel właściciela',
    'c.cancel': 'Anuluj', 'c.save': 'Zapisz', 'c.create': 'Utwórz', 'c.delete': 'Usuń', 'c.edit': 'Edytuj', 'c.folder': 'Folder', 'c.select': 'Wybierz',
    'c.selected': 'Wybrane', 'c.add': 'Dodaj', 'c.search': 'Szukaj', 'c.more': 'Pokaż więcej', 'c.loading': 'Wczytywanie…', 'c.clear': 'Wyczyść', 'c.choose': 'Wybierz…',

    'play.title': 'Graj', 'play.sub': 'Wybierz instancję i konto, a kaczka zajmie się resztą: Javą, plikami gry i modami.',
    'play.emptyTitle': 'Kwa… nie masz jeszcze żadnej instancji', 'play.emptyText': 'Instancja to osobna wersja gry z własnymi modami, światami i ustawieniami.',
    'play.emptyBtn': 'Utwórz pierwszą instancję', 'play.btn': 'GRAJ', 'play.preparing': 'PRZYGOTOWYWANIE…', 'play.stop': 'ZATRZYMAJ GRĘ',
    'play.log': 'Log gry', 'play.last': 'Ostatnio: {date}', 'play.never': 'jeszcze nie grano', 'play.addAccount': 'Dodaj konto…', 'play.start': 'Start…',
    'play.error': 'Błąd: {msg}', 'play.premium': 'premium', 'play.offline': 'offline', 'play.hud': 'Kaczo HUD',
    'play.signedAs': 'Zalogowano jako',

    'inst.title': 'Instancje', 'inst.sub': 'Każda instancja ma własną wersję gry, mody i światy.', 'inst.new': '+ Nowa instancja', 'inst.newTitle': 'Nowa instancja',
    'inst.editTitle': 'Edytuj instancję', 'inst.name': 'Nazwa', 'inst.namePh': 'np. Survival 1.21', 'inst.loader': 'Loader', 'inst.vanilla': 'Vanilla', 'inst.fabric': 'Fabric (mody)',
    'inst.version': 'Wersja Minecrafta', 'inst.versionHint': 'Dostępne są wydania od 1.21 wzwyż.', 'inst.ram': 'Pamięć RAM: ', 'inst.ramDefault': 'domyślnie ({gb})',
    'inst.optimize': 'Pakiet optymalizacji (Sodium, Lithium i inne)', 'inst.optimizeHint': 'Dla Fabric: mody zwiększające FPS instalują się same przy starcie gry.',
    'inst.editNote': 'Zmiana wersji lub loadera nie usuwa światów. Mody pakietu optymalizacji zostaną dobrane do nowej wersji.',
    'inst.deleteTitle': 'Usunąć instancję?', 'inst.deleteText': '„{name}” zniknie z listy launchera.', 'inst.deleteFiles': 'Usuń też pliki (światy, mody, zrzuty ekranu) – nieodwracalne',
    'inst.current': 'Wybrana', 'inst.hudOn': 'Kaczo HUD', 'inst.fabricShort': 'Fabric', 'inst.vanillaShort': 'Vanilla',

    'mods.title': 'Mody', 'mods.instance': 'Instancja: {name} · {mc} · {loader}', 'mods.vanillaBanner': 'Ta instancja jest vanilla – mody z Modrinth wymagają loadera Fabric. ',
    'mods.switch': 'Przełącz na Fabric', 'mods.tabInstalled': 'Zainstalowane', 'mods.tabBrowse': 'Przeglądaj Modrinth', 'mods.addJar': '+ Dodaj plik .jar',
    'mods.openFolder': 'Otwórz folder instancji', 'mods.count': '{n} modów', 'mods.noneTitle': 'Kwa… tu jeszcze pusto', 'mods.noneText': 'Mody optymalizacyjne dodadzą się przy pierwszym starcie gry, a własne znajdziesz w „Przeglądaj Modrinth”.',
    'mods.disabled': 'wyłączony', 'mods.searchPh': 'Szukaj modów na Modrinth ({mc}, Fabric)…', 'mods.install': 'Zainstaluj', 'mods.installing': 'Instaluję',
    'mods.noResults': 'Nic nie znaleziono', 'mods.noResultsText': 'Brak modów pasujących do „{q}” dla {mc}.', 'mods.searching': 'Szukam…',
    'mods.done': 'Zainstalowano: {list}', 'mods.already': 'Ten mod jest już zainstalowany', 'mods.downloads': '{n} pobrań', 'mods.noInstance': 'Brak instancji',
    'mods.noInstanceText': 'Utwórz instancję z loaderem Fabric, aby dodawać mody.', 'mods.createInstance': 'Utwórz instancję',

    'acc.title': 'Konta', 'acc.sub': 'Zaloguj się kontem Microsoft (premium) albo graj offline.',
    'acc.premiumTitle': 'Premium (Microsoft)', 'acc.premiumText': 'Logowanie w oknie Microsoft. Wymaga konta z kupionym Minecraft: Java Edition. Dzięki temu wejdziesz na serwery premium i będziesz mieć swój skin.',
    'acc.login': 'Zaloguj przez Microsoft', 'acc.waiting': 'Czekam na logowanie…', 'acc.offlineTitle': 'Offline (bez konta)',
    'acc.offlineText': 'Działa w grze jednoosobowej, w sieci LAN i na serwerach z wyłączonym online-mode. Serwery premium odrzucą takie konto. Nick właściciela launchera jest zarezerwowany.',
    'acc.nickPh': 'Twój nick (3–16 znaków)', 'acc.saved': 'Zapisane konta', 'acc.noneTitle': 'Brak kont', 'acc.noneText': 'Dodaj konto powyżej, aby móc uruchomić grę.',
    'acc.premium': 'Premium', 'acc.offline': 'Offline', 'acc.owner': 'OWNER', 'acc.signedIn': 'Zalogowano – kwa!', 'acc.offlineAdded': 'Dodano konto offline', 'acc.add': 'Dodaj konto',

    'set.title': 'Ustawienia', 'set.sub': 'Domyślne wartości dla wszystkich instancji.', 'set.language': 'Język', 'set.languageHint': 'Zmienia język launchera i – przy następnym starcie – języka w grze.',
    'set.ram': 'Domyślna pamięć RAM: ', 'set.java': 'Własna Java (opcjonalnie)', 'set.javaPh': 'Automatycznie (launcher pobierze właściwą Javę)', 'set.minimize': 'Minimalizuj launcher po uruchomieniu gry',
    'set.save': 'Zapisz', 'set.saved': 'Zapisano ustawienia', 'set.openData': 'Otwórz folder danych', 'set.dataDir': 'Dane launchera: {dir}',
    'set.defaultsTitle': 'Ustawienia startowe gry', 'set.defaultsText': 'Przy pierwszym uruchomieniu każdej instancji launcher ustawia: narrator wyłączony, skala interfejsu 4, muzyka wyłączona, VSync wyłączony, efekty pola widzenia i trzęsienie od obrażeń wyłączone, FOV 110. Potem możesz to zmienić w grze. Język gry zawsze zgadza się z językiem launchera.',
    'set.hudTitle': 'Kaczo HUD', 'set.hudText': 'Mod pod Prawym Shiftem (ping, edytor HUD, scoreboard). Zbudowany dla wersji: {list}.', 'set.hudNone': 'brak – zbuduj mod według instrukcji w README',

    'upd.title': 'Aktualizacje', 'upd.version': 'Wersja aplikacji: {v}', 'upd.idle': 'Aktualizacje sprawdzane są automatycznie.', 'upd.checking': 'Sprawdzanie aktualizacji…',
    'upd.none': 'Masz najnowszą wersję.', 'upd.downloading': 'Pobieranie wersji {v}… {p}%', 'upd.ready': 'Wersja {v} jest gotowa do instalacji.', 'upd.error': 'Nie udało się sprawdzić aktualizacji ({msg}).',
    'upd.check': 'Sprawdź aktualizacje', 'upd.install': 'Uruchom ponownie i zaktualizuj', 'upd.dev': 'Aktualizacje działają w zainstalowanej aplikacji (nie w trybie developerskim).',
    'upd.banner': 'Zaktualizuj do {v}', 'upd.bannerDl': 'Pobieranie aktualizacji… {p}%',
    'set.theme': 'Wygląd launchera', 'set.themeGlass': 'Nowoczesny (szkło)', 'set.themeMc': 'Minecraft (klockowy)', 'set.themeNeon': 'Neonowy',
    'set.menuModHint': 'Wygląd menu gry (logo, szklane przyciski, animacja wejścia) ustawisz w modzie Kaczo HUD: Prawy Shift → Ustawienia.',
    'set.menuStyle': 'Menu gry', 'set.menuKaczo': 'Neonowe Kaczo', 'set.menuVanilla': 'Standardowy Minecraft', 'set.menuHint': 'Działa na każdej wersji. Zmiana wchodzi przy następnym uruchomieniu gry.',
    'set.discord': 'Pokazuj Kaczo Client w statusie Discorda',
    'own.featTitle': 'Funkcje klienta – po kolei', 'own.featSub': 'Każda funkcja: co robi, gdzie jest zapisana i jak ją zmienić.',
    'own.featMenu': 'Styl menu gry', 'own.featMenuText': 'Wybiera, czy w grze jest neonowe menu (pakiet zasobów KaczoMenu w folderze instancji) czy zwykłe menu Minecrafta. Zmienisz to też w Ustawieniach.',
    'own.featDiscord': 'Status w Discordzie', 'own.featDiscordText': 'Pokazuje nazwę i logo Kaczo Client na Twoim profilu podczas gry. Wymaga ID aplikacji z discord.com/developers (zakładka Rich Presence → Art Assets: wgraj obrazek „logo”). Bez ID nic się nie wyświetla.',
    'own.discordId': 'ID aplikacji Discord', 'own.discordSave': 'Zapisz ID', 'own.discordSaved': 'Zapisano ID aplikacji Discord',
    'own.featOpt': 'Mody optymalizacyjne', 'own.featOptText': 'Fabric API, Sodium, Lithium i inne instalują się same przy starcie gry, jeśli istnieją dla danej wersji. Wyłączysz je per instancja w zakładce Instancje.',
    'own.featOpts': 'Ustawienia startowe gry', 'own.featOptsText': 'Narrator wyłączony, GUI scale 4, muzyka wyłączona – zapisywane do options.txt przy pierwszym wejściu. Przycisk „Przywróć ustawienia startowe” poniżej wymusza je ponownie.',
    'own.title': 'Panel właściciela', 'own.sub': 'Narzędzia do kontrolowania launchera na tym komputerze.', 'own.profile': 'Twój profil',
    'own.diagTitle': 'Diagnostyka', 'own.copyDiag': 'Kopiuj raport', 'own.copied': 'Skopiowano raport diagnostyczny', 'own.openLogs': 'Otwórz folder logów', 'own.devtools': 'Konsola (DevTools)',
    'own.gameTitle': 'Gra', 'own.stopGame': 'Zatrzymaj grę teraz', 'own.resetOptions': 'Przywróć ustawienia startowe gry (wybrana instancja)', 'own.clearJava': 'Wyczyść pobrane Javy',
    'own.javaCleared': 'Wyczyszczono pobrane środowiska Java', 'own.blockedTitle': 'Zablokowane nicki offline', 'own.blockedText': 'Nicki z tej listy nie utworzą konta offline. Twój nick jest zablokowany zawsze.',
    'own.blockedPh': 'Nick do zablokowania', 'own.blockedNone': 'Lista jest pusta', 'own.block': 'Zablokuj', 'own.unblock': 'Odblokuj',
    'own.note': 'Uwaga: to narzędzia lokalne. Launcher nie ma serwera, więc nie steruje innymi graczami ani serwerami Minecrafta.',
    'own.hudStatus': 'Kaczo HUD zbudowany dla: {list}',

    'fatal.title': 'Błąd interfejsu', 'fatal.hint': 'Naciśnij F12, aby otworzyć konsolę, i wyślij mi ten komunikat.', 'fatal.noApi': 'Brak połączenia z programem',
  },
  en: {
    'nav.play': 'Play', 'nav.instances': 'Instances', 'nav.mods': 'Mods', 'nav.accounts': 'Accounts', 'nav.settings': 'Settings', 'nav.owner': 'Owner panel',
    'c.cancel': 'Cancel', 'c.save': 'Save', 'c.create': 'Create', 'c.delete': 'Delete', 'c.edit': 'Edit', 'c.folder': 'Folder', 'c.select': 'Select',
    'c.selected': 'Selected', 'c.add': 'Add', 'c.search': 'Search', 'c.more': 'Show more', 'c.loading': 'Loading…', 'c.clear': 'Clear', 'c.choose': 'Choose…',

    'play.title': 'Play', 'play.sub': 'Pick an instance and an account – the duck takes care of the rest: Java, game files and mods.',
    'play.emptyTitle': 'Quack… you have no instances yet', 'play.emptyText': 'An instance is a separate game version with its own mods, worlds and settings.',
    'play.emptyBtn': 'Create your first instance', 'play.btn': 'PLAY', 'play.preparing': 'PREPARING…', 'play.stop': 'STOP GAME',
    'play.log': 'Game log', 'play.last': 'Last played: {date}', 'play.never': 'never played', 'play.addAccount': 'Add account…', 'play.start': 'Starting…',
    'play.error': 'Error: {msg}', 'play.premium': 'premium', 'play.offline': 'offline', 'play.hud': 'Kaczo HUD',
    'play.signedAs': 'Signed in as',

    'inst.title': 'Instances', 'inst.sub': 'Every instance has its own game version, mods and worlds.', 'inst.new': '+ New instance', 'inst.newTitle': 'New instance',
    'inst.editTitle': 'Edit instance', 'inst.name': 'Name', 'inst.namePh': 'e.g. Survival 1.21', 'inst.loader': 'Loader', 'inst.vanilla': 'Vanilla', 'inst.fabric': 'Fabric (mods)',
    'inst.version': 'Minecraft version', 'inst.versionHint': 'Releases from 1.21 and newer are available.', 'inst.ram': 'RAM: ', 'inst.ramDefault': 'default ({gb})',
    'inst.optimize': 'Optimization pack (Sodium, Lithium and more)', 'inst.optimizeHint': 'For Fabric: FPS-boosting mods install themselves when the game starts.',
    'inst.editNote': 'Changing the version or loader does not delete your worlds. Optimization mods will be matched to the new version.',
    'inst.deleteTitle': 'Delete instance?', 'inst.deleteText': '“{name}” will disappear from the launcher list.', 'inst.deleteFiles': 'Also delete files (worlds, mods, screenshots) – cannot be undone',
    'inst.current': 'Selected', 'inst.hudOn': 'Kaczo HUD', 'inst.fabricShort': 'Fabric', 'inst.vanillaShort': 'Vanilla',

    'mods.title': 'Mods', 'mods.instance': 'Instance: {name} · {mc} · {loader}', 'mods.vanillaBanner': 'This instance is vanilla – Modrinth mods need the Fabric loader. ',
    'mods.switch': 'Switch to Fabric', 'mods.tabInstalled': 'Installed', 'mods.tabBrowse': 'Browse Modrinth', 'mods.addJar': '+ Add .jar file',
    'mods.openFolder': 'Open instance folder', 'mods.count': '{n} mods', 'mods.noneTitle': 'Quack… nothing here yet', 'mods.noneText': 'Optimization mods are added on the first game start; find more under “Browse Modrinth”.',
    'mods.disabled': 'disabled', 'mods.searchPh': 'Search Modrinth mods ({mc}, Fabric)…', 'mods.install': 'Install', 'mods.installing': 'Installing',
    'mods.noResults': 'Nothing found', 'mods.noResultsText': 'No mods matching “{q}” for {mc}.', 'mods.searching': 'Searching…',
    'mods.done': 'Installed: {list}', 'mods.already': 'This mod is already installed', 'mods.downloads': '{n} downloads', 'mods.noInstance': 'No instance',
    'mods.noInstanceText': 'Create an instance with the Fabric loader to add mods.', 'mods.createInstance': 'Create instance',

    'acc.title': 'Accounts', 'acc.sub': 'Sign in with a Microsoft (premium) account or play offline.',
    'acc.premiumTitle': 'Premium (Microsoft)', 'acc.premiumText': 'Sign in through the Microsoft window. Requires an account that owns Minecraft: Java Edition. Lets you join premium servers and use your skin.',
    'acc.login': 'Sign in with Microsoft', 'acc.waiting': 'Waiting for sign-in…', 'acc.offlineTitle': 'Offline (no account)',
    'acc.offlineText': 'Works in singleplayer, on LAN and on servers with online-mode disabled. Premium servers will reject it. The launcher owner’s nickname is reserved.',
    'acc.nickPh': 'Your nickname (3–16 characters)', 'acc.saved': 'Saved accounts', 'acc.noneTitle': 'No accounts', 'acc.noneText': 'Add an account above to be able to start the game.',
    'acc.premium': 'Premium', 'acc.offline': 'Offline', 'acc.owner': 'OWNER', 'acc.signedIn': 'Signed in – quack!', 'acc.offlineAdded': 'Offline account added', 'acc.add': 'Add account',

    'set.title': 'Settings', 'set.sub': 'Defaults for all instances.', 'set.language': 'Language', 'set.languageHint': 'Changes the launcher language and – on the next start – the in-game language.',
    'set.ram': 'Default RAM: ', 'set.java': 'Custom Java (optional)', 'set.javaPh': 'Automatic (the launcher downloads the right Java)', 'set.minimize': 'Minimize the launcher when the game starts',
    'set.save': 'Save', 'set.saved': 'Settings saved', 'set.openData': 'Open data folder', 'set.dataDir': 'Launcher data: {dir}',
    'set.defaultsTitle': 'Game start-up settings', 'set.defaultsText': 'On the first launch of every instance the launcher sets: narrator off, GUI scale 4, music off, VSync off, FOV effects and damage tilt off, FOV 110. You can change them in-game afterwards. The in-game language always matches the launcher language.',
    'set.hudTitle': 'Kaczo HUD', 'set.hudText': 'Mod opened with Right Shift (ping, HUD editor, scoreboard). Built for versions: {list}.', 'set.hudNone': 'none – build the mod following the README',

    'upd.title': 'Updates', 'upd.version': 'App version: {v}', 'upd.idle': 'Updates are checked automatically.', 'upd.checking': 'Checking for updates…',
    'upd.none': 'You have the latest version.', 'upd.downloading': 'Downloading version {v}… {p}%', 'upd.ready': 'Version {v} is ready to install.', 'upd.error': 'Could not check for updates ({msg}).',
    'upd.check': 'Check for updates', 'upd.install': 'Restart and update', 'upd.dev': 'Updates work in the installed app (not in developer mode).',
    'upd.banner': 'Update to {v}', 'upd.bannerDl': 'Downloading update… {p}%',
    'set.theme': 'Launcher look', 'set.themeGlass': 'Modern (glass)', 'set.themeMc': 'Minecraft (blocky)', 'set.themeNeon': 'Neon',
    'set.menuModHint': 'The in-game menu look (logo, glass buttons, intro animation) is set in the Kaczo HUD mod: Right Shift → Settings.',
    'set.menuStyle': 'Game menu', 'set.menuKaczo': 'Kaczo neon', 'set.menuVanilla': 'Standard Minecraft', 'set.menuHint': 'Works on every version. The change applies the next time the game starts.',
    'set.discord': 'Show Kaczo Client in your Discord status',
    'own.featTitle': 'Client features – one by one', 'own.featSub': 'Each feature: what it does, where it is stored and how to change it.',
    'own.featMenu': 'Game menu style', 'own.featMenuText': 'Chooses between the neon in-game menu (the KaczoMenu resource pack in the instance folder) and the regular Minecraft menu. You can also change it in Settings.',
    'own.featDiscord': 'Discord status', 'own.featDiscordText': 'Shows the Kaczo Client name and logo on your profile while playing. Needs an application ID from discord.com/developers (Rich Presence → Art Assets: upload an image named “logo”). Nothing is shown without the ID.',
    'own.discordId': 'Discord application ID', 'own.discordSave': 'Save ID', 'own.discordSaved': 'Discord application ID saved',
    'own.featOpt': 'Optimization mods', 'own.featOptText': 'Fabric API, Sodium, Lithium and others install themselves when the game starts, if they exist for that version. Turn them off per instance on the Instances page.',
    'own.featOpts': 'Game start-up settings', 'own.featOptsText': 'Narrator off, GUI scale 4, music off – written to options.txt on first entry. The “Restore start-up settings” button below forces them again.',
    'own.title': 'Owner panel', 'own.sub': 'Tools for controlling the launcher on this computer.', 'own.profile': 'Your profile',
    'own.diagTitle': 'Diagnostics', 'own.copyDiag': 'Copy report', 'own.copied': 'Diagnostic report copied', 'own.openLogs': 'Open logs folder', 'own.devtools': 'Console (DevTools)',
    'own.gameTitle': 'Game', 'own.stopGame': 'Stop the game now', 'own.resetOptions': 'Restore game start-up settings (selected instance)', 'own.clearJava': 'Clear downloaded Java',
    'own.javaCleared': 'Downloaded Java runtimes cleared', 'own.blockedTitle': 'Blocked offline nicknames', 'own.blockedText': 'Nicknames on this list cannot create an offline account. Your nickname is always blocked.',
    'own.blockedPh': 'Nickname to block', 'own.blockedNone': 'The list is empty', 'own.block': 'Block', 'own.unblock': 'Unblock',
    'own.note': 'Note: these are local tools. The launcher has no server, so it does not control other players or Minecraft servers.',
    'own.hudStatus': 'Kaczo HUD built for: {list}',

    'fatal.title': 'Interface error', 'fatal.hint': 'Press F12 to open the console and send me this message.', 'fatal.noApi': 'No connection to the application',
  },
};

let LANG = 'pl';

function setLang(l) {
  LANG = l === 'en' ? 'en' : 'pl';
  document.documentElement.lang = LANG;
}

function t(key, vars) {
  const table = I18N[LANG] || I18N.pl;
  let s = table[key] !== undefined ? table[key] : (I18N.pl[key] !== undefined ? I18N.pl[key] : key);
  if (vars) for (const [k, v] of Object.entries(vars)) s = s.split('{' + k + '}').join(String(v));
  return s;
}
