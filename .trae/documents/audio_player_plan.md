# 音频播放器（audio_player）实现计划

## 一、概要

新增一个可右键配置的「音频播放器」方块：

- 循环播放任意**已注册音效**（填写音效 id，带下拉补全）；
- 四种模式：触发（有红石持续）、反触发（无红石持续）、脉冲（红石上升沿播放一次）、无红石（忽略红石常开）；
- 可设置：范围（格）、衰减（%）、起始点（延迟秒）、截至点（播放秒数，0=整首）、播放间隔（秒，可为负实现重叠）；
- 界面实时显示所选音效的时长。

播放完全在客户端驱动（参考 `ServerFaasSoundHandler`），服务端只负责红石状态与配置存储/同步。

## 二、现状分析（可复用点）

| 用途 | 参考文件 |
| --- | --- |
| 客户端循环音效 + 每 tick 手动算音量 + 方块中心定位 | [ServerFaasSoundHandler.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ServerFaasSoundHandler.java) L119-190 |
| 服务端 `use()` 开菜单 + `NetworkHooks.openScreen` 写同步字段 | [PosterBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/poster/PosterBlock.java) L114-127、[PosterMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/PosterMenu.java) L42-55 |
| GUI 保存 → C2S 包 → 校验（BE 类型 + 8 格）→ `sendBlockUpdated` | [PosterConfigPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/PosterConfigPacket.java) |
| BE 的 NBT + `getUpdateTag/getUpdatePacket` 同步 | [PosterBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/poster/PosterBlockEntity.java) |
| 输入补全下拉（行高 12 / 5 行 / 深色配色 / 点击选中 / 过滤） | [ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java) L140-148、L696-716、L1041-1095 |
| 自绘白色面板 + EditBox 手动渲染 | [PosterScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/PosterScreen.java) L183-236 |
| 红石检测（`neighborChanged` + `getBestNeighborSignal`） | [RedstoneSenderBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Redstone_transmit/RedstoneSenderBlock.java) L55-82 |
| 音效候选全集 | `CcrcCommand.suggestSounds`：`ForgeRegistries.SOUND_EVENTS.getKeys()` |
| 单方块 BE 注册 | [ModBlockEntities.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModBlockEntities.java) L178-184 |
| 菜单注册 | [ModMenuTypes.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModMenuTypes.java) L47-52 |
| Screen 注册 | [CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java) L202-231 |
| 网络包注册 | [ModNetwork.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ModNetwork.java)（已用 0~27，20 空号；新包用 **28**） |

## 三、设计决策

已与用户确认：

1. 起始点/截至点 = **播放时间窗**（起始=开播延迟秒，截至=播放多少秒后停止；不做曲内 seek）。
2. 第四模式「无红石」= **忽略红石常开**。
3. 方块贴图 = **AI 生成简单贴图**，最终 16×16。

本计划推导的细节（执行时直接采用）：

- 模式编号：`0=触发 1=反触发 2=脉冲 3=无红石`，默认 0。
- 范围 1~64 格（默认 16）；衰减 0~100%（默认 100，0=范围内等音量，100=线性降到 0）。
- 音量公式：`volume = clamp(1 - dist/range * atten/100, 0, 1)`，`dist > range` 停止该实例。
- 起始点 0~60 秒；截至点 0~3600 秒（0=整首）；间隔 -60~3600 秒。
- 间隔含义：`下一次开始 = 本次开始 + 播放长度 + 间隔`；播放长度 = 截至点>0 ? min(截至点, 曲长) : 曲长。负间隔即重叠。
- 每个方块最多同时存在 **16** 个实例，超出则跳过本次调度。
- 音效时长由客户端解析 OGG（头取采样率、尾取 granule）得到并缓存；解析失败按 200 tick 估算调度。
- 不做曲内 seek、不做服务端播放、不加新音效注册。

## 四、新增文件

### 1. `src/main/java/com/cc_rc/block/audio_player/AudioPlayerBlock.java`

- `extends Block implements EntityBlock`。
- 属性：`BlockStateProperties.POWERED`（布尔）。
- `newBlockEntity` → `AudioPlayerBlockEntity`。
- `neighborChanged` / `onPlace`：仅服务端执行 `refresh`：`boolean powered = level.getBestNeighborSignal(pos) > 0;` 与当前值不同则 `level.setBlock(pos, state.setValue(POWERED, powered), 3)`（3 = 同步客户端）。
- `canConnectRedstone` 返回 true（红石线可视连接）。
- `use`：Shift 直接 PASS；否则服务端 `AudioPlayerMenu.open(serverPlayer, be)`，返回 `InteractionResult.sidedSuccess(level.isClientSide)`。

### 2. `src/main/java/com/cc_rc/block/audio_player/AudioPlayerBlockEntity.java`

- 常量：`MODE_TRIGGER/INVERSE/PULSE/ALWAYS`、边界值（见第三节）、`MAX_SOUND_ID_LENGTH = 128`。
- 字段：`String soundId = ""`、`int mode`、`float range = 16`、`int attenuation = 100`、`float startSeconds`、`float stopSeconds`、`float intervalSeconds`。
- 公开 `record Settings(String soundId, int mode, float range, int attenuation, float start, float stop, float interval)` 与 `getSettings()`（客户端用于比较配置是否变化）。
- `setConfig(...)`：统一夹取（`Mth.clamp`、`mode` 0~3、字符串按 128 截断），随后 `setChanged()`。
- NBT 键：`Sound / Mode / Range / Atten / Start / Stop / Interval`；实现 `saveAdditional`、`load`、`getUpdateTag`（`saveWithoutMetadata`）、`getUpdatePacket`（`ClientboundBlockEntityDataPacket.create(this)`）。

### 3. `src/main/java/com/cc_rc/gui/AudioPlayerMenu.java`

- 字段：`BlockPos pos` + 上述 7 项配置（public final）。
- `open(ServerPlayer, AudioPlayerBlockEntity)`：`NetworkHooks.openScreen` + `buf` 依次写 `BlockPos`、`writeUtf(soundId, 128)`、`writeVarInt(mode)`、`writeFloat(range)`、`writeVarInt(attenuation)`、`writeFloat(start)`、`writeFloat(stop)`、`writeFloat(interval)`。
- `stillValid` 返回 true；`quickMoveStack` 返回 `ItemStack.EMPTY`（照抄 PosterMenu）。

### 4. `src/main/java/com/cc_rc/gui/AudioPlayerScreen.java`

界面 **220×200**，自绘白色面板（照 PosterScreen 的 `renderBg` / `renderLabels` / 手动渲染 EditBox）。

- 第 1 行：标题「音频播放器设置」。
- 第 2 行：标签「音频名称」+ `EditBox`（x=10，y=28，宽 200，高 16）。
  - `setResponder` 里刷新候选与时长文本。
  - 输入无命名空间时按 `cc_rc:` 补齐后再解析。
- 第 3 行：时长文本（y=46）：「时长：12.3 秒」或「时长：未知」，由 `SoundDurations.getTicks(id)` 换算。
- 第 4 行：标签「模式」+ 循环 `Button`（显示当前模式中文名，点击 `mode = (mode+1)&3`）。
- 第 5 行（两个半宽输入框）：「范围（格）」「衰减（%）」。
- 第 6 行（两个半宽输入框）：「起始点（秒）」「截至点（秒，0=整首）」。
- 第 7 行：「间隔（秒，可为负）」输入框 + 右侧灰字提示「负值 = 音频重叠」。
- 第 8 行：居中「保存」按钮；按钮下方红色/绿色状态文本（如「已保存」「音频不存在」）。
- 下拉补全（复用 ConsoleProgramScreen 视觉与交互）：
  - 常量 `SUGGEST_ROW_H=12`、`SUGGEST_ROWS=5`、配色 `0xFF141414/0xFF000000/0xFFCFCFCF/0xFFFFE060/0xFF3A3A6A`。
  - 候选：`ForgeRegistries.SOUND_EVENTS.getKeys()` 的 `toString()`，排序；点击框展开全部，输入后按 `startsWith` 过滤（无结果再退回 `contains`）。
  - 位置紧贴名称框下方，宽同输入框；`mouseClicked` 优先命中下拉；`mouseScrolled` 在下拉内翻 `suggestOffset`。
- 保存：解析各输入框（逗号当小数点，非法回退原值）；音效 id 不存在则红字拒绝发送；否则 `ModNetwork.CHANNEL.sendToServer(new AudioPlayerConfigPacket(...))`。

### 5. `src/main/java/com/cc_rc/client/SoundDurations.java`

客户端专用（只在 Screen/Handler 中引用）。

```java
public static int getTicks(ResourceLocation soundEventId)  // 缓存；失败返回 -1
```

解析流程：

1. `ForgeRegistries.SOUND_EVENTS.getValue(id)`；为 null 返回 -1。
2. `SoundEvent.getSound(SoundInstance.createUnseededRandom())` → `Sound#getLocation()`。
3. 资源路径 `new ResourceLocation(ns, "sounds/" + path + ".ogg")`，`Minecraft.getInstance().getResourceManager().open(...)` 读取字节。
4. 扫描 `01 'v' 'o' 'r' 'b' 'i' 's'`，`+12` 处读 4 字节小端采样率。
5. 从末尾向前找 `OggS`，`+6` 处读 8 字节小端 granule。
6. `ticks = round(granule / rate * 20)`，至少 1。
7. 用 `HashMap<ResourceLocation,Integer>` 缓存（含 -1）。

### 6. `src/main/java/com/cc_rc/client/AudioPlayerSoundHandler.java`

`@Mod.EventBusSubscriber(modid, value=Dist.CLIENT, bus=FORGE)`，客户端 tick（`Phase.END`）。

- 内部类 `AudioPlayerSoundInstance extends AbstractTickableSoundInstance`：
  - 构造：`super(event, SoundSource.BLOCKS, createUnseededRandom())`；`relative=false`；`attenuation=Attenuation.NONE`；`looping=true`；坐标固定方块中心；初始音量按公式计算。
    注意：`AbstractSoundInstance` 没有 `isStopped()`，`looping=true` 才能保证引擎每 tick 回调 `tick()`，由我们按 `maxTicks` 主动停止。
  - `tick()`：`elapsed++`；BE 不存在 → 停止；否则更新 `volume`；`elapsed >= maxTicks` 或 `dist > range` → 停止。
  - 字段名必须避开父类的 `attenuation`（枚举）：衰减百分比字段用 `attenuationPercent`。
  - `stopNow()` 包装 protected `stop()`。
- 内部类 `Track`：`Settings settings`、`boolean prevPowered`、`boolean initialized`、`boolean active`、`long nextStartTick`、`boolean pulsePending`、`List<AudioPlayerSoundInstance> instances`。
- 静态 `Map<BlockPos, Track> TRACKS`；每 20 tick 扫描一次：
  - 玩家所在区块 ±5 区块（覆盖 80 格），`level.getChunkSource().getChunk(x, z, false)`，`instanceof LevelChunk` 后遍历 `getBlockEntities().values()`，把 `AudioPlayerBlockEntity` 加入 `TRACKS`（key 用 `immutable()`）。
  - 清理：BE 已不是该类型 / 距玩家 > 80 格 → 停止实例并移除。
- 每 tick 对每个 Track：
  1. 取 `be.getSettings()`；与 `track.settings` 不同 → 停止全部实例、`active=false`、`nextStartTick=MAX`、更新 settings（保留 `prevPowered`）。
  2. 读 `be.getBlockState().getValue(AudioPlayerBlock.POWERED)`；首次创建时 `prevPowered=powered`，避免登录即触发脉冲。
  3. `MODE_PULSE`：`!prevPowered && powered` → `pulsePending=true; nextStartTick=now+startTicks;`；到点且 `pulsePending` → 生成 1 个实例、`pulsePending=false`。
  4. `TRIGGER/INVERSE/ALWAYS`：计算 `shouldPlay`（分别 = powered / !powered / true）。
     - 上升：`nextStartTick = now + startTicks`。
     - 下降：停止该 Track 全部实例，`nextStartTick=MAX`。
     - 保持且 `now >= nextStartTick` 且实例数 < 16：生成实例；`len = stopTicks>0 ? min(stopTicks, duration) : duration`（duration 未知用 200）；`nextStartTick = now + max(1, len + intervalTicks)`。
  5. `track.prevPowered = powered`。
- `startTicks/stopTicks/intervalTicks = Math.round(秒 * 20)`。
- 关服/切维度：`level == null` 或 level 变化时停全部并清空。

### 7. `src/main/java/com/cc_rc/network/AudioPlayerConfigPacket.java`

字段同 Menu；`encode/decode` 顺序一致；`handle` 中：

- `ServerPlayer` 非空；`level.getBlockEntity(pos) instanceof AudioPlayerBlockEntity`；
- 距离 ≤ 8 格；
- 音效 id 规范化（无 `:` 补 `cc_rc:`），`ForgeRegistries.SOUND_EVENTS.containsKey` 不通过则丢弃；
- `be.setConfig(...)`；`level.sendBlockUpdated(pos, state, state, 3)`。

## 五、修改文件

| 文件 | 改动 |
| --- | --- |
| `ModBlocks.java` | 在红石传输区块（L289 附近）注册 `AUDIO_PLAYER`：`Properties.of().mapColor(MapColor.METAL).strength(2.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)` |
| `ModBlockEntities.java` | 末尾加 `AUDIO_PLAYER_BE`（`BlockEntityType.Builder.of(AudioPlayerBlockEntity::new, ModBlocks.AUDIO_PLAYER.get())`） |
| `ModItems.java` | 加 `AUDIO_PLAYER_ITEM = ITEMS.register("audio_player", () -> new BlockItem(ModBlocks.AUDIO_PLAYER.get(), new Item.Properties()))` |
| `ModCreativeTabs.java` | 在 `REDSTONE_RECEIVER_ITEM` 后 `output.accept(ModItems.AUDIO_PLAYER_ITEM.get())` |
| `ModMenuTypes.java` | 加 `AUDIO_PLAYER`（`IForgeMenuType.create` 按 Menu 的写入顺序读取） |
| `ModNetwork.java` | `CHANNEL.registerMessage(28, AudioPlayerConfigPacket.class, ...)` |
| `CcRc.java` | client setup 里 `MenuScreens.register(ModMenuTypes.AUDIO_PLAYER.get(), com.cc_rc.gui.AudioPlayerScreen::new)` |
| `lang/zh_cn.json`、`lang/en_us.json` | 方块名 + GUI 文案（标题/音频名称/时长/模式/范围/衰减/起始点/截至点/间隔/保存/提示/四种模式名/未知） |
| `DEVELOPMENT_LOG.md` | 「二、代码」新增「音频播放器」小节：方块/BE/Menu/Screen/客户端调度/时长解析/网络包/四模式与参数语义 |
| `临时日志.md` | 末尾追加一条本次实现记录（只追加） |

## 六、资源文件

1. `assets/cc_rc/blockstates/audio_player.json`
   ```json
   { "variants": {
       "powered=false": { "model": "cc_rc:block/audio_player" },
       "powered=true":  { "model": "cc_rc:block/audio_player" } } }
   ```
2. `assets/cc_rc/models/block/audio_player.json`：`parent: block/cube_all`，`textures.all: cc_rc:block/audio_player`。
3. `assets/cc_rc/models/item/audio_player.json`：`parent: cc_rc:block/audio_player`。
4. `assets/cc_rc/textures/block/audio_player.png`：
   - 用 `GenerateImage` 生成 1920×1920（提示词：16×16 像素风、深色金属广播机面板、扬声器格栅 + 波形指示灯、平涂色块、正面无透视）。
   - PowerShell + `System.Drawing` 缩到 16×16（`InterpolationMode.NearestNeighbor`、`PixelOffsetMode.Half`、`SmoothingMode.None`），保存 32bppArgb PNG；若颜色过噪则改用一次高质量双三次缩到 64 再最近邻到 16。
5. `data/cc_rc/loot_tables/blocks/audio_player.json`：照 `server_faas_1.json` 的自掉落模板（rolls 1 + `minecraft:item` + `survives_explosion`）。

## 七、假设与限制

- 不使用原版 `SoundInstance.delay` 做起始延迟（语义随引擎版本变化），由客户端调度器在正确 tick 才创建实例。
- 循环靠「按曲长 + 间隔重复创建实例」实现，不设 `looping=true`；因此整首播放时接缝处可能有 1 tick 级别空隙（可接受）。
- 音效时长来自文件解析，仅支持 OGG（原版音效均为 OGG）；解析失败不影响播放，仅影响界面显示与自动循环排期。
- 红石状态用 `POWERED` 方块状态同步；脉冲上升沿由客户端比较上一 tick 状态得到。

## 八、验证步骤

1. `$env:JAVA_HOME='E:\trae\java21'; .\gradlew build --no-daemon --console=plain --offline` → `BUILD SUCCESSFUL`。
2. 进游戏：
   - 创造模式物品栏可见「音频播放器」，放置/破坏正常并掉落自身。
   - 右键打开界面；音频名称输入 `music_level5` 时下拉出现 `cc_rc:music_level5`，选中后显示对应时长（≈263 秒）。
   - 分别测试四种模式：触发（给红石播放、断红石停）、反触发（无红石播放、给红石停）、脉冲（每次上升沿播放一次）、无红石（始终播放）。
   - 测试范围/衰减：走远到范围外声音消失，`衰减=0` 时范围内音量恒定。
   - 测试起始点（延迟 N 秒才响）、截至点（N 秒后停）、间隔 0（首尾相接）、负间隔（能听到明显重叠，且实例数不超过 16）。
   - 保存后重进世界，配置保持。
3. 服务端/dedicated 不加载 `AudioPlayerScreen`、`SoundDurations`、`AudioPlayerSoundHandler`（`Dist.CLIENT` 隔离）。

## 九、当前进度与剩余工作

### 已完成（文件均已写入磁盘）

- 方块 / 方块实体：`AudioPlayerBlock.java`、`AudioPlayerBlockEntity.java`
- 菜单 / 界面 / 时长解析：`AudioPlayerMenu.java`、`AudioPlayerScreen.java`、`SoundDurations.java`
- 客户端调度 / 网络包：`AudioPlayerSoundHandler.java`、`AudioPlayerConfigPacket.java`
- 注册：`ModBlocks`（L308）、`ModBlockEntities`（L242）、`ModItems`（L176）、`ModCreativeTabs`（L109）、`ModMenuTypes`（L55）、`ModNetwork`（28 号包 L157）、`CcRc`（L208 注册 Screen）
- 资源：`blockstates/audio_player.json`、`models/block/audio_player.json`、`models/item/audio_player.json`、`loot_tables/blocks/audio_player.json`、`lang/zh_cn.json`、`lang/en_us.json`

### 剩余工作（按顺序执行）

1. **修复 `AudioPlayerSoundHandler.java` 的字段命名冲突（会导致编译失败）**
   - 第 274 行 `private final float attenuation;` → 改名 `private final float attenuationPercent;`
   - 第 284 行 `this.attenuationPercent = attenuation;` → `this.attenuationPercent = attenuationPercent;`（构造参数同步改名为 `attenuationPercent`）
   - 第 289 行 `this.attenuation = SoundInstance.Attenuation.NONE;` 保持不变（父类枚举字段）
   - 第 315 行 `this.attenuationPercent / 100.0F` 保持不变
2. **方块贴图转 PNG**：把 `assets/cc_rc/textures/block/audio_player.jpg`（1920×1920）用 PowerShell + `System.Drawing` 缩到 16×16，保存为 `audio_player.png`（NearestNeighbor + PixelOffsetMode.Half + SmoothingMode.None）；确认尺寸后删除多余 `.jpg`。
3. **编译验证**：`$env:JAVA_HOME='E:\trae\java21'; .\gradlew build --no-daemon --console=plain --offline` → `BUILD SUCCESSFUL`；如有报错按第八节排查。
4. **更新 `DEVELOPMENT_LOG.md`「二、代码」**：新增「音频播放器」小节（方块/BE/Menu/Screen/客户端调度/时长解析/网络包/四模式与参数语义）。
5. **追加 `临时日志.md`** 一条实现记录（只追加）。
