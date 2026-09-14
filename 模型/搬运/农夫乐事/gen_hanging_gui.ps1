Add-Type -AssemblyName System.Drawing

$src = "e:\trae\program\CC_RC\模型\农夫乐事\vanilla_hanging_sign_gui_oak.png"
$outDir = "e:\trae\program\CC_RC\src\main\resources\assets\cc_rc\textures\gui\hanging_signs"
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

# 16 dye colors (same RGB as vanilla DyeColor.getTextColor)
$dye = @{
  white      = @(243,244,245)
  orange     = @(241,142,50)
  magenta    = @(205,85,197)
  light_blue = @(69,190,226)
  yellow     = @(252,208,50)
  lime       = @(105,209,42)
  pink       = @(244,167,192)
  gray       = @(80,84,91)
  light_gray = @(152,152,146)
  cyan       = @(39,125,163)
  purple     = @(140,66,190)
  blue       = @(46,51,201)
  brown      = @(101,65,43)
  green      = @(71,131,34)
  red        = @(190,46,38)
  black      = @(30,30,30)
}

$base = New-Object System.Drawing.Bitmap($src)

function IsChain($px) {
  # chain: bluish-grey metal (blue clearly above red/green and overall dark)
  return ($px.B -gt $px.R + 5) -and ($px.B -gt $px.G + 5)
}

foreach ($color in $dye.Keys) {
  $t = $dye[$color]
  $out = New-Object System.Drawing.Bitmap($base.Width, $base.Height)
  for ($x=0; $x -lt $base.Width; $x++) {
    for ($y=0; $y -lt $base.Height; $y++) {
      $p = $base.GetPixel($x, $y)
      if ($p.A -eq 0) { $out.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0,0,0,0)); continue }
      if (IsChain $p) { $out.SetPixel($x, $y, $p); continue }
      # panel: recolor with brightness preserved relative to original
      $lum = ($p.R + $p.G + $p.B) / 3.0
      $factor = $lum / 175.0
      if ($factor -gt 1.4) { $factor = 1.4 }
      $nr = [int]([Math]::Min(255, $t[0] * $factor))
      $ng = [int]([Math]::Min(255, $t[1] * $factor))
      $nb = [int]([Math]::Min(255, $t[2] * $factor))
      $out.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $nr, $ng, $nb))
    }
  }
  $path = Join-Path $outDir ("canvas_" + $color + ".png")
  $out.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
  $out.Dispose()
  Write-Output ("generated " + (Split-Path $path -Leaf))
}
$base.Dispose()
Write-Output "DONE"