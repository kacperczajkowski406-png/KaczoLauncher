!include "FileFunc.nsh"
!insertmacro GetParent

; Przed instalacją: usuń stare skróty (np. ręcznie robione) i stare wersje, żeby nic się nie dublowało.
!macro customInit
  Delete "$DESKTOP\Kaczo Client.lnk"
  Delete "$SMPROGRAMS\Kaczo Client.lnk"
  Delete "$DESKTOP\Kaczo Launcher.lnk"
  Delete "$DESKTOP\KaczoLauncher.lnk"
!macroend

; Po instalacji: jeśli instalator leży na pulpicie, usuń go (z opóźnieniem, bo sam siebie nie skasuje).
!macro customInstall
  ${GetParent} "$EXEPATH" $R0
  StrCmp "$R0" "$DESKTOP" 0 +2
    Exec 'cmd.exe /c ping -n 4 127.0.0.1 >nul & del /f /q "$EXEPATH"'
!macroend
