# Convert RGB color controller OBJ into console component model (ASCII-only script).
# Coordinate transform: (x,y,z) -> (-z,y,x), so the model's -Z front becomes +X outward.
$root = 'E:\trae\program\CC_RC'
$src = Get-ChildItem -LiteralPath $root -Recurse -Filter '*.obj' |
    Where-Object { $_.Directory.Name -like '*RGB*' -and $_.FullName -notlike '*\resources\*' } |
    Select-Object -First 1
if ($null -eq $src) { throw 'source obj not found' }
$dstDir = Join-Path $root 'src\main\resources\assets\cc_rc\models\console_parts'
$dst = Join-Path $dstDir 'rgb_color_surface.obj'
$out = New-Object System.Collections.Generic.List[string]
$out.Add('# panel_props: indicator_green_off.obj (RGB color controller)')
$out.Add('# 1 unit = 1 block, Y up, front faces +X (converted from source -Z front)')
$out.Add('mtllib knob_switch.mtl')
$group = ''
foreach ($line in Get-Content -Encoding UTF8 $src.FullName) {
    $t = $line.Trim()
    if ($t -eq '') { continue }
    $p = $t -split '\s+'
    switch ($p[0]) {
        'v' {
            $x = [double]$p[1]; $y = [double]$p[2]; $z = [double]$p[3]
            $out.Add(('v {0:0.######} {1:0.######} {2:0.######}' -f (-$z), $y, $x))
        }
        'vn' {
            $x = [double]$p[1]; $y = [double]$p[2]; $z = [double]$p[3]
            $out.Add(('vn {0:0.######} {1:0.######} {2:0.######}' -f (-$z), $y, $x))
        }
        'vt' { $out.Add($t) }
        'o' {
            $group = $p[1]
            $out.Add("o $group")
        }
        'usemtl' {
            if ($group -eq 'Lamp') { $out.Add('usemtl tint') } else { $out.Add('usemtl prop') }
        }
        'f' { $out.Add($t) }
        default {
            if ($p[0] -eq 'mtllib') { continue }
            $out.Add($t)
        }
    }
}
[System.IO.File]::WriteAllLines($dst, $out, (New-Object System.Text.UTF8Encoding($false)))
Write-Output ("written: " + $dst)
