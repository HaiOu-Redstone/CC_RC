---
name: "ccrc-music-disc"
description: "Adds a Minecraft music disc (VLC mono OGG transcode + SoundEvent/RecordItem register + sounds.json/lang/model/tag/creative tab). Invoke when user asks to add a new music disc/record for the CC_RC mod."
---

# CC_RC Music Disc Workflow

Complete end-to-end process for adding a new music disc to the CC_RC Minecraft 1.20.1 Forge mod.

## Prerequisites

- Source audio file (`.mp3` or `.ogg`) and a 16×16 PNG disc texture in `模型/唱片/`
- VLC installed at `D:\软件\vlc\vlc.exe`
- Project root: `e:\trae\program\CC_RC`
- Modid: `cc_rc`

## Step 1 – Inventory & Naming

1. List `模型/唱片/` and confirm the exact filenames (usually `<Name>.<ext>` for audio + `<Name>.png` for texture).
2. Derive a **snake_case key** for the disc (e.g. `You_Will_Be_Perfect` → `you_will_be_perfect`). This key is reused everywhere:
   - Sound event id: `music_<key>`
   - Item id: `music_disc_<key>`
   - Audio file: `<key>.ogg`
   - Texture: `music_disc_<key>.png`
   - Item model: `music_disc_<key>.json`
3. Pick an **unused comparator output** (1–15, check `ModItems.java` to avoid collisions).

## Step 2 – Transcode Audio to MONO OGG (MANDATORY)

> **Why mono?** Minecraft does NOT apply distance attenuation or volume sliders to stereo sounds – this is the engine behaviour (MC-262858). All discs MUST be single-channel to behave like vanilla records.

Run in PowerShell (replace placeholders):

```powershell
$vlc  = "D:\软件\vlc\vlc.exe"
$src  = "e:\trae\program\CC_RC\模型\唱片\<SourceName>.mp3"
$dst  = "e:\trae\program\CC_RC\src\main\resources\assets\cc_rc\sounds\music\<key>.ogg"
$dstU = $dst.Replace('\','/')
& $vlc $src "--intf=dummy" "--no-loop" "--play-and-exit" `
  "--sout=#transcode{vcodec=none,acodec=vorb,ab=160,channels=1,samplerate=44100}:standard{access=file,mux=ogg,dst=$dstU}"
Start-Sleep -Seconds 3
```

**Verify channels = 1** with the Vorbis-identification header scanner:

```powershell
$f = "e:\trae\program\CC_RC\src\main\resources\assets\cc_rc\sounds\music\<key>.ogg"
$b = [System.IO.File]::ReadAllBytes($f); $i=0; $ch=-1; $rate=0; $gp=0
while ($i -lt $b.Length-100) {
  if ($b[$i] -eq 1 -and $b[$i+1] -eq 0x76 -and $b[$i+2] -eq 0x6f -and
      $b[$i+3] -eq 0x72 -and $b[$i+4] -eq 0x62 -and $b[$i+5] -eq 0x69 -and $b[$i+6] -eq 0x73) {
    $ch   = $b[$i+11]
    $rate = [BitConverter]::ToUInt32($b, $i+12)
    break
  }
  $i++
}
$i = $b.Length - 1
while ($i -gt 0) {
  if ($b[$i] -eq 0x4f -and $b[$i+1] -eq 0x67 -and
      $b[$i+2] -eq 0x67 -and $b[$i+3] -eq 0x53) {
    $gp = [BitConverter]::ToUInt32($b, $i+6); break
  }
  $i--
}
$ticks = [math]::Round(($gp / $rate) * 1000 / 50)
Write-Host "ch=$ch rate=$rate gp=$gp ticks=$ticks"
```

Record `ticks` (rounded, used by `RecordItem` length) and confirm `ch -eq 1`.

## Step 3 – Copy Texture

```powershell
Copy-Item "模型\唱片\<SourceName>.png" `
  "src\main\resources\assets\cc_rc\textures\item\music_disc_<key>.png" -Force
```

## Step 4 – Register SoundEvent (`ModSounds.java`)

Append **before** the `register()` helper:

```java
public static final RegistryObject<SoundEvent> MUSIC_<CONST_KEY> = register("music_<key>");
```

Where `<CONST_KEY>` is the all-uppercase underscore variant of `<key>`.

## Step 5 – Register RecordItem (`ModItems.java`)

Append:

```java
public static final RegistryObject<RecordItem> MUSIC_DISC_<CONST_KEY> = ITEMS.register("music_disc_<key>",
        () -> new RecordItem(<comparatorOutput>, ModSounds.MUSIC_<CONST_KEY>.get(),
                new Item.Properties().stacksTo(1).rarity(Rarity.RARE), <ticks>));
```

Rules:
- `comparatorOutput`: integer 1–15, unique across all discs.
- `ticks`: the value obtained in Step 2.
- Always `stacksTo(1)` + `Rarity.RARE` (vanilla standard).

## Step 6 – `sounds.json` entry

Append inside the top-level JSON object (add a comma to the previous entry):

```json
"music_<key>": {
  "sounds": [{ "name": "cc_rc:music/<key>", "stream": true }],
  "subtitle": "cc_rc.subtitle.music_<key>"
}
```

`stream: true` is REQUIRED for long music tracks.

## Step 7 – Chinese translation (`lang/zh_cn.json`)

Add four keys. Leave song description in English as-is (user translates later per project convention):

```json
"item.cc_rc.music_disc_<key>": "音乐唱片",
"item.cc_rc.music_disc_<key>.desc": "<Original English Song Name>",
"cc_rc.subtitle.music_<key>": "音乐唱片 - <Original English Song Name>",
```

## Step 8 – Item model

Create `assets/cc_rc/models/item/music_disc_<key>.json`:

```json
{
  "parent": "item/generated",
  "textures": { "layer0": "cc_rc:item/music_disc_<key>" }
}
```

## Step 9 – Music Disc tag (jukebox compatibility)

Append to `data/minecraft/tags/items/music_discs.json` → `values` array:

```json
"cc_rc:music_disc_<key>"
```

Without this tag the disc cannot be inserted into a Jukebox.

## Step 10 – Creative tab (`ModCreativeTabs.java`)

Append to the `displayItems` lambda, after the last existing disc (preserve order of insertion):

```java
output.accept(ModItems.MUSIC_DISC_<CONST_KEY>.get());
```

## Step 11 – Update development log (`DEVELOPMENT_LOG.md`)

MANDATORY (see `.trae/rules/project_rules.md`): every new disc must be added to the project development log.

1. 「三、注册物品 → 2. 物品 → 音乐唱片表」：追加一行 `music_disc_<key>`（比较器输出 / 音轨 / ticks）。
2. 「三、注册物品 → 4. 声音」：追加 `music_<key>`。
3. 更新「三、注册物品」开头的物品总数统计（每个新唱片 +1）。

## Step 12 – Build verification

```powershell
$env:JAVA_HOME = "E:\trae\java17_temp\jdk-17.0.20+8"
cd e:\trae\program\CC_RC
.\gradlew.bat compileJava
```

Expected: `BUILD SUCCESSFUL`.

## Optional: Simplify registration (batch pattern)

When the user asks for **many discs at once**, you can reduce repetition by defining a local data list, e.g.:

```java
// name / comparator / ticks
private static final Object[][] NEW_DISCS = {
    {"cutie_mew_mew_magic", 3,  3680},
    {"denise",              4,  8340},
    // ...
};
```

and iterating it inside a static block, however the existing project convention is **one explicit field per disc** (tooling / reflection friendly). Prefer the explicit style unless the user explicitly requests batch registration.
