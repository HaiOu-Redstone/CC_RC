# Convert Blockbench Bedrock (flat elements) to Java block model.
# Run from the source dir. from/to kept; rotation (22.5/45) kept; up/down uv flipped.
$ErrorActionPreference = 'Stop'
$SRC = (Get-Location).Path
$OUT = 'e:\trae\program\CC_RC\src\main\resources\assets\cc_rc\models\block\server_faas'
New-Item -ItemType Directory -Force -Path $OUT | Out-Null

function R2([double]$x) { return [math]::Round($x, 3) }

function Convert-Faas([string]$file, [string]$outname) {
    $data = Get-Content -Path (Join-Path $SRC $file) -Raw -Encoding UTF8 | ConvertFrom-Json
    $elements = @()
    foreach ($e in $data.elements) {
        $faces = @{}
        foreach ($prop in $e.faces.PSObject.Properties) {
            $key = $prop.Name
            $f = $prop.Value
            if ($null -eq $f.uv) { continue }
            $uv = @($f.uv)
            $x1 = [double]$uv[0]; $y1 = [double]$uv[1]
            $x2 = [double]$uv[2]; $y2 = [double]$uv[3]
            if ($key -eq 'up' -or $key -eq 'down') {
                $a = [math]::Round($x2, 3); $b = [math]::Round($y2, 3)
                $c = [math]::Round($x1, 3); $d = [math]::Round($y1, 3)
                $faces[$key] = @{ uv = @($a, $b, $c, $d); texture = '#0' }
            } else {
                $a = [math]::Round($x1, 3); $b = [math]::Round($y1, 3)
                $c = [math]::Round($x2, 3); $d = [math]::Round($y2, 3)
                $faces[$key] = @{ uv = @($a, $b, $c, $d); texture = '#0' }
            }
        }
        $el = @{ from = $e.from; to = $e.to; faces = $faces }
        $rot = $e.rotation
        if ($null -ne $rot -and $rot.angle -ne 0 -and $null -ne $rot.origin) {
            $el.rotation = @{ origin = $rot.origin; axis = $rot.axis; angle = $rot.angle }
        }
        $elements += $el
    }
    $model = @{
        parent = 'block/block'
        ambientocclusion = $false
        textures = @{
            '0' = "cc_rc:block/server_faas/$outname"
            particle = "cc_rc:block/server_faas/$outname"
        }
        elements = $elements
    }
    $json = $model | ConvertTo-Json -Depth 30
    Set-Content -Path (Join-Path $OUT "$outname.json") -Value $json -Encoding UTF8
    Write-Output "$outname : elements=$($elements.Count)"
}

Convert-Faas 'FAAS_1.json' 'faas_1'
Convert-Faas 'FAAS_2.json' 'faas_2'
Convert-Faas 'FAAS_3.json' 'faas_3'