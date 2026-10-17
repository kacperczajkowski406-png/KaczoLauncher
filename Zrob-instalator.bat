@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"
title Kaczo Client - tworzenie instalatora

if not exist "%~dp0package.json" (
  echo.
  echo  Uruchomiono plik z samego archiwum ZIP/RAR - brakuje reszty plikow.
  echo  Najpierw wypakuj CALY folder: prawy przycisk na pliku zip ^> Wypakuj wszystko,
  echo  a potem uruchom ten plik z wypakowanego folderu.
  echo.
  pause
  exit /b 1
)

:: electron-builder potrzebuje uprawnien administratora (tworzenie dowiazan symbolicznych) - uruchom sie ponownie jako admin
net session >nul 2>&1
if errorlevel 1 (
  echo  Prosze o uprawnienia administratora ^(wymagane do zbudowania instalatora^)...
  powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
  exit /b
)
set CSC_IDENTITY_AUTO_DISCOVERY=false
:: wyczysc niedokonczone pobranie z poprzedniej proby
rmdir /s /q "%LOCALAPPDATA%\electron-builder\Cache\winCodeSign" >nul 2>nul

echo.
echo  === Kaczo Client: tworzenie prawdziwej aplikacji (instalator .exe) ===
echo.

where node >nul 2>nul
if errorlevel 1 (
  echo  Nie znaleziono Node.js. Zainstaluj wersje LTS z https://nodejs.org i uruchom ten plik ponownie.
  pause
  exit /b 1
)

call npm install
if errorlevel 1 ( echo  npm install nie powiodlo sie. & pause & exit /b 1 )

call npm run dist
if errorlevel 1 ( echo  Budowanie nie powiodlo sie. Przepisz mi blad. & pause & exit /b 1 )

set "SETUP="
for %%F in ("dist\*Setup*.exe") do set "SETUP=%%F"
if not defined SETUP ( echo  Nie znaleziono instalatora w folderze dist. & pause & exit /b 1 )

echo.
echo  Gotowe. Uruchamiam instalator - po instalacji masz "Kaczo Client" na pulpicie i w menu Start.
echo  Potem mozesz usunac ten folder z plikami.
start "" "%SETUP%"
endlocal
