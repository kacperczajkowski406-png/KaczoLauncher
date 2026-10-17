@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"
title Kaczo Client - instalacja

if not exist "%~dp0package.json" (
  echo.
  echo  Uruchomiono plik z samego archiwum ZIP/RAR - brakuje reszty plikow.
  echo  Najpierw wypakuj CALY folder: prawy przycisk na pliku zip ^> Wypakuj wszystko,
  echo  a potem uruchom ten plik z wypakowanego folderu.
  echo.
  pause
  exit /b 1
)

echo.
echo  === Kaczo Client: instalacja i skrot na pulpicie ===
echo.

where node >nul 2>nul
if errorlevel 1 (
  echo  Nie znaleziono Node.js. Zainstaluj wersje LTS z https://nodejs.org i uruchom ten plik ponownie.
  echo.
  pause
  exit /b 1
)

if not exist "node_modules\electron\dist\electron.exe" (
  echo  Instalowanie bibliotek ^(pierwszy raz, chwile potrwa^)...
  call npm install
  if errorlevel 1 (
    echo.
    echo  npm install nie powiodlo sie. Przepisz mi blad, a go naprawie.
    pause
    exit /b 1
  )
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\create-shortcut.ps1"
if errorlevel 1 (
  echo.
  echo  Nie udalo sie utworzyc skrotu.
  pause
  exit /b 1
)

echo.
echo  Gotowe! Na pulpicie i w menu Start masz teraz "Kaczo Client".
echo  Mozesz go przypiac do paska zadan: prawy przycisk na skrocie ^> Przypnij do paska zadan.
echo.
set /p RUN=Uruchomic launcher teraz? (T/N):
if /i "%RUN%"=="T" start "" "node_modules\electron\dist\electron.exe" "%~dp0"
endlocal
