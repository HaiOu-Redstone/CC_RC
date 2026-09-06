$file = "e:\trae\program\CC_RC\src\main\resources\assets\cc_rc\sounds\music\reimu_mono_test.ogg"
$bytes = [System.IO.File]::ReadAllBytes($file)
$found = -1
for ($i = 0; $i -lt $bytes.Length - 6; $i++) {
    if ($bytes[$i] -eq 0x76 -and $bytes[$i+1] -eq 0x6f -and $bytes[$i+2] -eq 0x72 -and $bytes[$i+3] -eq 0x62 -and $bytes[$i+4] -eq 0x69 -and $bytes[$i+5] -eq 0x73) {
        $found = $i
        break
    }
}
if ($found -gt 0) {
    $packetType = $bytes[$found - 1]
    $channels = $bytes[$found + 6 + 4]
    Write-Host "Channels in reimu_mono_test.ogg: $channels"
} else {
    Write-Host "vorbis not found"
}
