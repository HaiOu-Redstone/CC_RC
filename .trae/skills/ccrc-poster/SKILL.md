---
name: "ccrc-poster"
description: "Add new poster images to the CC_RC mod (PowerShell alpha-trim + 512 resize, append to PosterImages.LIST, no new block/item). Invoke when user asks to register/add a new poster, 海报, or 宣传画 for CC_RC."
---

# CC_RC 新增海报流程

为 CC_RC（Minecraft 1.20.1 Forge）新增可选海报图片的固定流程。

**核心前提**：所有海报共用同一个方块 `cc_rc:poster` 与同一个物品 `cc_rc:poster`，
「新增一张海报」= **导入贴图 + 往图片库列表末尾追加一条记录**，不需要注册方块/物品/语言文件/创造栏。

工程根目录：`e:\trae\program\CC_RC`，modid：`cc_rc`。

---

## 步骤 1 — 确认素材与命名

1. 在用户给出的素材目录（默认 `模型/其他物品/海报/新海报/`）列出 `.png` 文件。
2. 为每张图取 **snake_case 英文 id**（贴图文件名 = id，不能重名）：
   - 贴图：`assets/cc_rc/textures/block/poster/<id>.png`
   - 列表 id：`img("<id>", "<中文名称>", <宽>, <高>)`
3. 中文名称用于 GUI 鼠标悬停提示，按素材本名填写。

## 步骤 2 — 导入贴图（缩放到最长边 512 并裁掉透明留白）

目标：`src/main/resources/assets/cc_rc/textures/block/poster/<id>.png`

顺序固定为**先等比缩小到最长边 512，再按 alpha 包围盒裁掉四周透明留白**（与既有图片一致）。

```powershell
Add-Type -AssemblyName System.Drawing
$map  = @{ '<id>' = '模型\其他物品\海报\新海报\<源文件名>.png' }   # 可一次写多组
$outDir = 'e:\trae\program\CC_RC\src\main\resources\assets\cc_rc\textures\block\poster'
foreach ($k in $map.Keys) {
    $src = Join-Path 'e:\trae\program\CC_RC' $map[$k]
    $img = [System.Drawing.Image]::FromFile($src)
    # ① 等比缩放到最长边 512
    $scale = [Math]::Min(1.0, 512.0 / [Math]::Max($img.Width, $img.Height))
    $w1 = [Math]::Max(1, [int][Math]::Round($img.Width  * $scale))
    $h1 = [Math]::Max(1, [int][Math]::Round($img.Height * $scale))
    $tmp = New-Object System.Drawing.Bitmap $w1, $h1
    $g = [System.Drawing.Graphics]::FromImage($tmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.DrawImage($img, 0, 0, $w1, $h1); $g.Dispose(); $img.Dispose()
    # ② alpha 包围盒（缩小后尺寸 ≤512，可安全用 GetPixel）
    $x0=$w1; $y0=$h1; $x1=-1; $y1=-1
    for ($y=0; $y -lt $h1; $y++) { for ($x=0; $x -lt $w1; $x++) {
        if ($tmp.GetPixel($x,$y).A -gt 0) {
            if ($x -lt $x0) {$x0=$x}; if ($y -lt $y0) {$y0=$y}
            if ($x -gt $x1) {$x1=$x}; if ($y -gt $y1) {$y1=$y} } } }
    if ($x1 -lt 0) { Write-Host "$k : 全透明，跳过" ; $tmp.Dispose(); continue }
    $w2 = $x1-$x0+1; $h2 = $y1-$y0+1
    $rect = New-Object System.Drawing.Rectangle $x0, $y0, $w2, $h2
    $crop = $tmp.Clone($rect, $tmp.PixelFormat); $tmp.Dispose()
    $dst  = Join-Path $outDir "$k.png"
    $crop.Save($dst, [System.Drawing.Imaging.ImageFormat]::Png); $crop.Dispose()
    Write-Host "$k : $w2 x $h2"
}
```

记录脚本输出的 **最终宽 × 高**，下一步要写进列表。

> **坑**：PowerShell 5.1 用 `-File` 执行「UTF-8 无 BOM」脚本时按 ANSI 解码，中文路径会乱码导致找不到文件。
> 解决：把脚本存成文件后以 base64 方式执行：
> ```powershell
> $p='e:\trae\program\CC_RC\build\import_poster.ps1'
> $b64=[Convert]::ToBase64String([IO.File]::ReadAllBytes($p))
> iex ([Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($b64)))
> ```
> 临时脚本必须放在工作区内（如 `build\`），写工作区外会被拒绝。

## 步骤 3 — 追加到图片库（`PosterImages.java`）

文件：[src/main/java/com/cc_rc/block/poster/PosterImages.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/poster/PosterImages.java)

**铁律：索引已写入方块实体 NBT，只能追加到 `LIST` 末尾，绝不能插到中间或调换顺序**
（否则旧存档里已放置的海报会变成别的图片）。

在 `LIST` 最后一项后加逗号并追加：

```java
img("<id>", "<中文名称>", <宽>, <高>),
```

- `img(...)`：整张画布即内容（导入流程已裁掉透明留白，`top = 0`）。
- `imgCrop(id, name, w, h, top)`：仅用于「256×512 画布、内容底对齐、顶部大片透明」的老素材，`top` = 内容顶部像素 y。
- 同步更新 `LIST` 上方的注释：总数「共 N 张」与索引区间说明（如 `41~44` → `41~47`）。

几何约定（渲染无需改动）：图片宽 = 界面里的「缩放倍率」格数，高 = 宽 × 内容高 / 画布宽（保持原始比例），图片顶端对齐方块顶端。

## 步骤 4 — 无需改动的部分（确认即可）

| 项目 | 说明 |
| --- | --- |
| 方块 / 物品注册 | 共用 `poster`，不新增 |
| `lang/zh_cn.json` / `en_us.json` | 图片名走 `PosterImages`，不加 key |
| `ModCreativeTabs` | 不新增条目 |
| `PosterScreen` | 7 列 × 4 行自动分页（滚轮翻行 + 滚动条），图片变多自动适配 |
| `PosterBER` | `WALL_GAP` 等为全局参数，与单张图无关 |

只检查一处：`textures/block/poster/<id>.png` 文件名必须与 `img(...)` 里的 id **完全一致**（大小写、下划线），否则游戏内是紫黑块。

## 步骤 5 — 构建验证

```powershell
$env:JAVA_HOME='E:\trae\java21'
cd e:\trae\program\CC_RC
.\gradlew build --no-daemon --console=plain --offline
```

期望 `BUILD SUCCESSFUL`（允许既有的过时 API 警告）。若构建要联网握手卡死，确认 `gradle.properties` 里已有
`systemProp.net.minecraftforge.gradle.check.certs=false` 并坚持 `--offline`。

## 步骤 6 — 同步日志（项目规则要求）

1. `DEVELOPMENT_LOG.md`：目录索引表新增一行；在该期小节里写明新增图片 id / 中文名 / 尺寸、列表总数变化。
2. `临时日志.md`：**只追加**一条简要记录（新增哪些图、列表 N→M、构建结果）。

---

## 快速清单

- [ ] 素材 id 使用 snake_case，无重名
- [ ] 贴图已先缩放最长边 512、再裁透明留白，落在 `textures/block/poster/<id>.png`
- [ ] `PosterImages.LIST` 只在末尾追加，索引区间与总数注释已更新
- [ ] 文件名与 id 完全一致
- [ ] `gradlew build --offline` 成功
- [ ] 开发日志 + 临时日志已更新
