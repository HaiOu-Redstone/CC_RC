$vlc = "D:\软件\vlc\vlc.exe"
$audioDst = "e:\trae\program\CC_RC\src\main\resources\assets\cc_rc\sounds\music"
$files = Get-ChildItem $audioDst -Filter *.ogg | Where-Object { $_.Name -notmatch '_test' }
$failed = @()
$idx = 0
foreach ($f in $files) {
    $idx++
    Write-Host "[$idx/$($files.Count)] Converting $($f.Name) ..."
    $input = $f.FullName
    $tmpOut = Join-Path $audioDst ($f.BaseName + "_mono_tmp.ogg")
    if (Test-Path $tmpOut) { Remove-Item $tmpOut -Force }
    $dst = $tmpOut.Replace('\','/')
    & $vlc $input "--intf=dummy" "--no-loop" "--play-and-exit" "--sout=#transcode{vcodec=none,acodec=vorb,ab=192,channels=1,samplerate=44100}:standard{access=file,mux=ogg,dst=$dst}" 2>&1 | Out-Null
    Start-Sleep -Milliseconds 300
    if (Test-Path $tmpOut) {
        [System.IO.File]::Move($tmpOut, $input)
        Write-Host "  OK ($([math]::Round((Get-Item $input).Length/1KB,1)) KB)"
    } else {
        Write-Host "  FAILED"
        $failed += $f.Name
    }
}
Write-Host "--- Failed files ---"
if ($failed.Count -eq 0) { Write-Host "  (none)" } else { $failed | ForEach-Object { Write-Host "  $_" } }
