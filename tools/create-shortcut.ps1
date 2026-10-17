# Tworzy skróty „Kaczo Client” na Pulpicie i w menu Start (z ikoną kaczki).
# Uruchamia launcher bez okna konsoli – bezpośrednio przez electron.exe.
$ErrorActionPreference = 'Stop'

$root     = Split-Path -Parent $PSScriptRoot
$electron = Join-Path $root 'node_modules\electron\dist\electron.exe'
$icon     = Join-Path $root 'build\icon.ico'

if (-not (Test-Path $electron)) { throw "Brak electron.exe - najpierw uruchom: npm install" }
if (-not (Test-Path $icon))     { throw "Brak pliku ikony: $icon" }

$shell   = New-Object -ComObject WScript.Shell
$folders = @(
  [Environment]::GetFolderPath('Desktop'),
  [Environment]::GetFolderPath('Programs')   # menu Start
)

foreach ($dir in $folders) {
  if (-not $dir -or -not (Test-Path $dir)) { continue }
  $lnk = Join-Path $dir 'Kaczo Client.lnk'
  $s = $shell.CreateShortcut($lnk)
  $s.TargetPath       = $electron
  $s.Arguments        = '"' + $root + '"'
  $s.WorkingDirectory = $root
  $s.IconLocation     = $icon
  $s.Description      = 'Kaczo Client'
  $s.Save()
  Write-Host "Utworzono skrot: $lnk"
}
