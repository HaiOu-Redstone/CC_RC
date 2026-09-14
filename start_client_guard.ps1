# Single-instance guard for start_client.bat.
# Exits 1 if a CC_RC Minecraft client (Forge BootstrapLauncher JVM) is already
# running, otherwise exits 0. Kept pure ASCII for the same reason as the .bat.
$p = Get-CimInstance Win32_Process -Filter "Name='java.exe' or Name='javaw.exe'" |
    Where-Object { $_.CommandLine -match 'BootstrapLauncher' }
if ($p) { exit 1 } else { exit 0 }
