// Przechwytuje "teraz gra" z Windows (Spotify, YouTube Music, przeglądarka itd.) przez SMTC.
// Zwraca tytuł, wykonawcę, okładkę (data URL) i postęp. Tekstów piosenek nie pobieramy (prawa autorskie).
const { spawn } = require('child_process');

const PS = `
$ErrorActionPreference='SilentlyContinue'
[Console]::OutputEncoding=[Text.Encoding]::UTF8
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$null=[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]
$null=[Windows.Storage.Streams.DataReader,Windows.Storage.Streams,ContentType=WindowsRuntime]
$at=([System.WindowsRuntimeSystemExtensions].GetMethods()|?{$_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation\`1'})[0]
function Aw($op,$t){$m=$at.MakeGenericMethod($t);$k=$m.Invoke($null,@($op));$k.Wait(4000)|Out-Null;$k.Result}
$mgr=Aw ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
$lastKey='';$cover=''
while($true){
  try{
    $s=$mgr.GetCurrentSession()
    if($s){
      $p=Aw ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
      $pb=$s.GetPlaybackInfo();$tl=$s.GetTimelineProperties()
      $key=$p.Artist+'|'+$p.Title
      if($key -ne $lastKey){
        $lastKey=$key;$cover=''
        if($p.Thumbnail){
          $st=Aw ($p.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
          if($st -and $st.Size -gt 0 -and $st.Size -lt 3000000){
            $rd=New-Object Windows.Storage.Streams.DataReader($st)
            $n=Aw ($rd.LoadAsync([uint32]$st.Size)) ([uint32])
            $b=New-Object byte[] $st.Size;$rd.ReadBytes($b)
            $cover='data:'+$st.ContentType+';base64,'+[Convert]::ToBase64String($b)
          }
        }
      }
      $pos=$tl.Position.TotalSeconds
      if($pb.PlaybackStatus -eq 4){$pos=$pos+((Get-Date)-$tl.LastUpdatedTime.LocalDateTime).TotalSeconds}
      $o=@{title=$p.Title;artist=$p.Artist;album=$p.AlbumTitle;app=$s.SourceAppUserModelId;playing=($pb.PlaybackStatus -eq 4);pos=[math]::Round($pos,1);dur=[math]::Round($tl.EndTime.TotalSeconds,1);cover=$cover}
      Write-Output ($o|ConvertTo-Json -Compress)
    } else { Write-Output '{"none":true}' }
  }catch{ Write-Output '{"none":true}' }
  Start-Sleep -Milliseconds 1000
}`;

let proc = null;

function start(emit) {
  if (process.platform !== 'win32' || proc) return;
  const file = require('path').join(require('os').tmpdir(), 'kaczo-media.ps1');
  require('fs').writeFileSync(file, '\ufeff' + PS, 'utf8');
  try {
  proc = spawn('powershell.exe', ['-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-File', file], { windowsHide: true, cwd: require('os').tmpdir(), stdio: ['ignore', 'pipe', 'pipe'] });
  let buf = '';
  proc.stdout.setEncoding('utf8');
  proc.stdout.on('data', (d) => {
    buf += d;
    let i;
    while ((i = buf.indexOf('\n')) >= 0) {
      const line = buf.slice(0, i).trim(); buf = buf.slice(i + 1);
      if (!line) continue;
      try { emit('media:state', JSON.parse(line)); } catch { /* ignoruj */ }
    }
  });
  proc.on('error', () => { proc = null; });
  proc.stderr.on('data', () => {});
  proc.on('exit', () => { proc = null; });
  } catch { proc = null; }
}
function stop() {
  const p = proc; proc = null;
  if (!p) return;
  try { p.kill(); } catch { /* */ }
  // pewność, że drzewo procesów nie blokuje plików launchera podczas aktualizacji/odinstalowania
  try { spawn('taskkill', ['/PID', String(p.pid), '/T', '/F'], { windowsHide: true, stdio: 'ignore' }); } catch { /* */ }
}

module.exports = { start, stop };
