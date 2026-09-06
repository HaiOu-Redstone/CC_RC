# CC：反应堆控制台（cc_rc）开发日志

> 模组 ID：`cc_rc` ｜ Minecraft Forge 1.20.1 ｜ 前置：CC: Tweaked
> 本文档按「目录 / 代码 / 注册物品」三部分组织，目录为索引，方便快速定位源码与实现。
> **版本记录（v0.0.7）**：新增 F.A.A.S服务器方块 `server_faas_1/2/3`（水平四方向放置朝向玩家，靠近时持续播放 `server_noise` 环境音效，三种变体共用 `ServerFaasBlock` 方块类，模型来自"模型/FAAS"并转为 Java 格式，另补三张方块掉落表）；版本号由 0.0.6 升至 0.0.7。**上版本记录（v0.0.6）**：新增模组 logo（`模型/logo.png`，256x256，经 mods.toml `logoFile` 声明）；版本号由 0.0.5 升至 0.0.6。**修复**：mods.toml 的 `description` 由多行字符串 `'''...'''` 改为单行 `"..."`，避免第三方启动器（HMCL/PCL2 等）解析 TOML 失败导致 logo 不显示。

---

## 一、目录（索引）

| 编号 | 分类 | 内容 | 位置 |
| --- | --- | --- | --- |
| 1 | 框架 | 模组入口与注册机制 | [二、1 模组框架](#1-模组框架与注册机制) |
| 2 | 框架 | 配置系统（安全按钮 / 圆盘记录仪 / 刷卡机） | [二、2 配置系统 Config](#2-配置系统-config) |
| 3 | 文字显示 | 控制面板文字存储与网络同步 | [二、3.1 文字存储 ConsolePanelBlockEntity](#31-文字存储consolepanelblockentity) |
| 4 | 文字显示 | 通用文字渲染 ConsolePanelBER | [二、3.2 通用文字渲染 ConsolePanelBER](#32-通用文字渲染consolepanelber) |
| 5 | 文字显示 | 命名文字（放置时自定义名称写入） | [二、3.3 命名文字的写入](#33-命名文字的写入setplacedby) |
| 6 | 文字显示 | 数码显示器 / 数字调节器双行文字 | [二、3.4 数码显示器与数字调节器显示](#34-数码显示器与数字调节器显示) |
| 7 | 红石输入 | 指示灯（电平输入 LIT） | [二、4.1 指示灯 PointLampBlock](#41-指示灯pointlampblock) |
| 8 | 红石输入 | 仪表（信号强度 0~15） | [二、4.2 仪表 MeterBlock](#42-仪表meterblock) |
| 9 | 红石输入 | 断路器底部信号自动跳闸 | [二、4.3 断路器红石输入](#43-断路器红石输入) |
| 10 | 红石输入 | 刷卡机定时自动复位 | [二、4.4 刷卡机红石输入](#44-刷卡机红石输入) |
| 11 | 红石输入 | 圆盘记录仪时钟上升沿检测 | [二、4.5 圆盘记录仪时钟（红石输入）](#45-圆盘记录仪时钟红石输入) |
| 12 | 红石输出 | 普通拉杆（继承原版 LeverBlock） | [二、5.1 普通拉杆](#51-普通拉杆) |
| 13 | 红石输出 | 3 挡位拉杆（0/8/15） | [二、5.2 3挡位拉杆](#52-3挡位拉杆consolelever3stageblock) |
| 14 | 红石输出 | 控制台按钮（按下 15 持续 20tick） | [二、5.3 控制台按钮](#53-控制台按钮consolebuttonblock) |
| 15 | 红石输出 | 安全按钮（三阶段授权输出） | [二、5.4 安全按钮](#54-安全按钮safebuttonblock) |
| 16 | 红石输出 | 断路器 / 刷卡机输出 | [二、5.5 断路器与刷卡机输出](#55-断路器与刷卡机红石输出) |
| 17 | 红石输出 | 连接方向 helper（getConnectedDirection） | [二、5.6 连接方向计算](#56-连接方向计算getconnecteddirection) |
| 18 | 划线 | 圆盘记录仪数据缓冲与采样 | [二、6.1 圆盘记录仪数据缓冲](#61-圆盘记录仪数据缓冲plotterblockentity) |
| 19 | 划线 | 圆盘记录仪趋势图渲染 | [二、6.2 圆盘记录仪渲染 PlotterBER](#62-圆盘记录仪渲染plotterber) |
| 20 | 划线 | 圆盘记录仪时钟触发扫描 | [二、6.3 圆盘记录仪时钟触发](#63-圆盘记录仪时钟触发) |
| 21 | CC 外设 | 数码显示器 / 数字调节器外设 | [二、7 CC-Tweaked 外设](#7-cc-tweaked-外设) |
| 22 | 工具 | 多功能工具（百分比切换 / 模式切换） | [二、8 多功能工具](#8-多功能工具multitoolitem) |
| 23 | 划线 | 数字圆盘记录仪（50点0~100线图 + CC外设） | [二、9 数字圆盘记录仪](#9-数字圆盘记录仪digitalplotter) |
| 24 | 移植 | 旧模组移植物品（冰冻罗非鱼/绿酒/何意味/合一味）与自定义 Tier | [二、10 旧模组移植物品](#10-旧模组移植物品modtooltiers) |
| 25 | 物品 | 描述型物品机制（DescriptionItem/SwordItem/AttackDescriptionItem）与新增月饼/压缩饼干 | [二、11 描述型物品与新增食物](#11-描述型物品与新增食物) |
| 26 | 注册 | 所有注册方块（按方块类归组） | [三、1 方块](#1-方块blocks) |
| 27 | 注册 | 所有注册物品（含唱片） | [三、2 物品](#2-物品items) |
| 28 | 注册 | 方块实体 / 声音 / 创造标签 / 弹射物实体 | [三、3~6 方块实体·声音·创造标签·弹射物实体](#3-方块实体block-entity-types) |
| 29 | 文字显示 | 大号空控制面板（3 倍字号居中文字 + 16x14x2 碰撞箱） | [二、3.2 通用文字渲染 ConsolePanelBER](#32-通用文字渲染consolepanelber) |
| 30 | 物品 | 搬运物品与搬运物品栏（箱装土豆、16 色粗布告示牌与悬挂告示牌 + 自定义方块实体，标签按注册名排序） | [二、12 搬运物品与搬运物品栏](#12-搬运物品与搬运物品栏) |
| 31 | 注册 | 搬运方块：冰箱（27 格容器 + 原版箱子 GUI）、水槽（纯装饰） | [二、13 冰箱与水槽](#13-冰箱与水槽) |
| 32 | 机制 | 自定义配方类型 cc_rc:sink_conversion（数据驱动四向转换） | [二、14 自定义配方类型](#14-自定义配方类型sink_conversion) |
| 33 | 物品/实体 | 包子 bao_zi（右键食用 +4饥饿/+2饱和；左键投掷命中爆炸弹射物，仅伤害实体不破坏方块，投掷冷却 0.5s） | [二、15 包子](#15-包子bao_zi) |
| 34 | 物品 | 法棍 baguette（食物 营养2/饱和度2 堆叠16，主手攻击伤害4 + 击退 ATTACK_KNOCKBACK=3.0，粗体棕色描述"坚如磐石"） | [二、16 法棍](#16-法棍baguette) |
| 35 | 物品 | 撬棍 crowbar（SwordItem 自带横扫，修改器伤害+19 实际总伤害20 攻速慢 暴击2.0x 耐久1024 铁砧打击音效，浅蓝粗体"物理学圣剑"+深蓝"f(x)dx"） | [二、17 撬棍](#17-撬棍crowbar) |
| 36 | 红石 | 红石信号发射器 redstone_sender（六方向，收到信号沿面向方向2~配置距离无线传输给接收器，on/off 贴图）+ 接收器 redstone_receiver（无方向，向相邻方块输出信号强度） | [二、18 红石信号发射器与接收器](#18-红石信号发射器与接收器redstone_sender--redstone_receiver) |
| 37 | 装饰/音效 | F.A.A.S服务器 server_faas_1/2/3（水平四方向放置朝向玩家，贴近持续播放 server_noise 环境音效，三种变体共用方块类） | [二、19 F.A.A.S服务器](#19-faas服务器server_faas) |
| 38 | 物品 | 说明书1 instruction_book_1（原版成书 WrittenBookItem 机制，右键打开书籍界面，固定内容：目录 + 控制面板类/圆盘记录仪/断路器/刷卡机/红石信号收发） | [二、20 说明书1](#20-说明书1instruction_book_1) |
| 39 | 物品/CC | 说明书2 instruction_book_2（原版成书机制，记录 CC 配件外设使用法：数码显示器/数字调节器/数字圆盘记录仪的每个 Lua 函数及参数类型） | [二、21 说明书2](#21-说明书2instruction_book_2) |
| 40 | 物品/武器 | 简易长矛 simple_spear（SwordItem 自带横扫，耐久130 修改器伤害+129 总伤害130 +13攻击范围 ForgeMod.ENTITY_REACH，文字颜色类似附魔金苹果 Rarity.EPIC+附魔微光，手持模型2倍大且y前移3.2，紫色描述"魔女们的秘密武器"） | [二、22 简易长矛](#22-简易长矛simple_spear) |
| 41 | 指令 | /ccrc set_count <设备种类> <数字>（仅允许设置 ids.json 已记录类型，写文件+反射同步内存）+ get_count（查询）+ list（仅查看 ids.json 内容，不扫描世界）；get_count/set_count 带类型自动补全 | [二、23 /ccrc 指令](#23-ccrc-指令ccrccommand) |

---

## 二、代码

### 1. 模组框架与注册机制

主类 [CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java) 通过 6 个 `DeferredRegister` 注册方块、物品、声音、方块实体、实体、创造标签与配方类型，构造函数中还初始化简单网络通道（`ModNetwork.init()`，C2S 投掷包子），并在 `commonSetup` 中注册 CC: Tweaked 外设提供者：

```java
@Mod(CcRc.MODID)
public class CcRc {
    public static final String MODID = "cc_rc";

    public CcRc(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModNetwork.init();
        modEventBus.addListener(this::commonSetup);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
```

客户端在 [CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java#L88-L107) 的 `ClientModEvents` 中注册 9 个方块实体渲染器（BER）与 1 个实体渲染器（`bao_zi` 复用原版 `ThrownItemRenderer`）。

**方块注册示例（[ModBlocks.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModBlocks.java)）：**

```java
public static final RegistryObject<ConsoleLeverBlock> CONSOLE_LEVER_1 = BLOCKS.register("console_lever_1",
        () -> new ConsoleLeverBlock(BlockBehaviour.Properties.of()
                .noCollission().strength(0.5F).sound(SoundType.WOOD)));
```

**物品注册示例（[ModItems.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModItems.java)）：**

```java
public static final RegistryObject<Item> CONSOLE_LEVER_1_ITEM = ITEMS.register("console_lever_1",
        () -> new BlockItem(ModBlocks.CONSOLE_LEVER_1.get(), new Item.Properties()));
```

### 2. 配置系统 Config

[Config.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/Config.java) 使用 `ForgeConfigSpec`，三个配置段：

- **safe_button**：`timeout_ticks`（默认 200，状态 2 超时回到状态 1）、`signal_ticks`（默认 20，状态 3 信号持续时间）。
- **plotter**：模式 2~9 的数据更新间隔，默认 `5/10/20/50/100/200/500/1000` tick。`getPlotterTickInterval(mode)` 中模式 1 返回 `-1`（被动触发）。
- **card_reader**：`on_ticks`（默认 20，激活后保持输出的时长）；每个等级刷卡机的可识别卡片规则，默认 `A→"A"、B→"AB"、C→"ABC"、D→"ABCD"、E→"ABCDE"`，由 `isCardAccepted(readerLevel, cardLetter)` 判断。

### 3. 文字显示

#### 3.1 文字存储（ConsolePanelBlockEntity）

[ConsolePanelBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console_panel/ConsolePanelBlockEntity.java) 是所有贴墙/贴面控制类方块实体的基类，存储待渲染文字（`Component text`）与计时（`ticksRemaining`），通过 NBT 持久化并支持网络同步：

```java
public class ConsolePanelBlockEntity extends BlockEntity {
    private Component text = Component.empty();
    private int ticksRemaining = 0;

    public void setText(Component text) { ...; sync(); }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Text", Component.Serializer.toJson(text));
        tag.putInt("TicksRemaining", ticksRemaining);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        text = Component.Serializer.fromJson(tag.getString("Text"));
        ticksRemaining = tag.getInt("TicksRemaining");
    }
    // getUpdateTag / getUpdatePacket / onDataPacket 用于客户端同步
}
```

#### 3.2 通用文字渲染（ConsolePanelBER）

[ConsolePanelBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console_panel/ConsolePanelBER.java) 提供静态方法 `renderText` 与 `pushTextTransform`，所有贴墙方块（拉杆、指示灯、仪表、按钮、圆盘记录仪）共用。核心是根据 `FACING` + `FACE`（WALL/CEILING/FLOOR）计算旋转，再以 `scale(-0.015F, -0.015F, 0.015F)` 把文字缩放为贴近方块表面的像素字：

```java
public static void pushTextTransform(BlockState state, PoseStack poseStack,
                                     float textX, float textY, float textZ) {
    Direction facing = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING);
    AttachFace face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
    poseStack.pushPose();
    poseStack.translate(0.5, 0.5, 0.5);
    if (face == AttachFace.WALL) {
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.getOpposite().toYRot()));
        poseStack.translate(textX, textY, textZ);
    } else if (face == AttachFace.CEILING) {
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        poseStack.translate(textX, textY, textZ);
    } else { // FLOOR
        poseStack.mulPose(Axis.YP.rotationDegrees(-(facing.getOpposite().toYRot() + 180) % 360));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.translate(textX, textY, textZ);
    }
    poseStack.scale(-0.015F, -0.015F, 0.015F);
}
```

文字绘制使用 `Font.drawInBatch`，居中、白色、`POLYGON_OFFSET` 模式避免与方块面 z-fighting：

```java
Font font = Minecraft.getInstance().font;
float width = font.width(text);
font.drawInBatch(text, -width / 2.0F, 0, 0xFFFFFF, false,
        poseStack.last().pose(), bufferSource, Font.DisplayMode.POLYGON_OFFSET,
        0x0F0000, packedLight);
```

默认文字位置常量：`DEFAULT_TEXT_X = 0.0F`、`DEFAULT_TEXT_Y = 0.3F`、`DEFAULT_TEXT_Z = 0.35F`。

`renderText` 提供可指定字号与纵向居中的重载（新增 `scale`、`centerY` 参数），大号空控制面板 [ConsolePanelLargeBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console_panel/ConsolePanelLargeBlock.java) 继承 `ConsolePanelBlock`，仅重写碰撞箱（16 宽 × 14 高 × 2 厚，比空控制面板左右各多 1 像素），渲染时以 `scale = 0.045F`（0.015×3，3 倍字号）且 `centerY = true`（y 方向居中，x 方向本就由 `-width/2.0F` 居中）调用：

```java
if (be.getBlockState().getBlock() instanceof ConsolePanelLargeBlock) {
    renderText(be.getBlockState(), be.getText(), poseStack, bufferSource, packedLight,
            0.0F, 0.0F, DEFAULT_TEXT_Z, 0.045F, true);
} else {
    renderText(be.getBlockState(), be.getText(), poseStack, bufferSource, packedLight);
}
```

> **变更记录（大号空控制面板）** 新增方块 `console_panel_large`（模型 `block/console_panel/console_panel_large`，贴图 `empty_console_panel_large`），放置/文字存储逻辑复用空控制面板（同一 `console_panel_be` 方块实体类型），仅碰撞箱与文字渲染（3 倍字号、x/y 居中）不同。

#### 3.3 命名文字的写入（setPlacedBy）

[ConsolePanelBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console_panel/ConsolePanelBlock.java) 继承 `FaceAttachedHorizontalDirectionalBlock`，在放置时把物品的自定义名称写入方块实体，实现“用命名牌命名后显示在面板上”：

```java
@Override
public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                        @Nullable LivingEntity placer, ItemStack stack) {
    super.setPlacedBy(level, pos, state, placer, stack);
    if (stack.hasCustomHoverName()) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ConsolePanelBlockEntity panelBE) {
            panelBE.setText(stack.getHoverName());
        }
    }
}
```

#### 3.4 数码显示器与数字调节器显示

数码显示器 [DigitalDisplayBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_display/DigitalDisplayBER.java) 与数字调节器 [DigitalKnobBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_knob/DigitalKnobBER.java) 都是完整方块，按水平朝向 `FACING` 旋转后渲染两行文字：

- 第一行（白字）：放置时自定义名称。
- 第二行（橙字，`0xFFF06020`）：数码显示器为状态文字（默认 `"----"`，CC 外设可改）；数字调节器为当前数值。

```java
// DigitalKnobBER 第二行：百分比模式仅改变显示，存储值不变
Component line2 = be.isPercentMode()
        ? Component.literal(String.format(Locale.ROOT, "%.1f%%", be.getValue() / 10.0))
        : Component.literal(Integer.toString(be.getValue()));
```

共同绘制逻辑（`drawLine`）：`translate(0, y, TEXT_Z)` + `scale(-SCALE, -SCALE, SCALE)`，`SCALE = 0.015F`、`TEXT_Z = -0.57F`，居中绘制，`PACKED_LIGHT = 0xF000F0` 全亮。

### 4. 红石输入

#### 4.1 指示灯（PointLampBlock）

[PointLampBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Point_lamp/PointLampBlock.java) 继承 `ConsolePanelBlock`，用 `LIT` 布尔属性显示亮/灭。`neighborChanged` 读取方块是否收到强红石信号并同步 `LIT`，是典型的被动红石输入：

```java
@Override
public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                            BlockPos fromPos, boolean isMoving) {
    if (!level.isClientSide) {
        boolean lit = level.hasNeighborSignal(pos);
        if (lit != state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, lit), 3);
        }
    }
}
```

#### 4.2 仪表（MeterBlock）

[MeterBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Meter/MeterBlock.java) 继承 `ConsolePanelBlock`，用 `POWER`（0~15 整数）驱动 16 种模型，输入来源为 `level.getBestNeighborSignal(pos)`（邻居最高信号强度），随信号变化实时刷新模型。

#### 4.3 断路器红石输入

[BreakerBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Breaker/BreakerBlock.java) 是完整方块，`POWERED` 表示通断。除右键手动切换外，还检测底面信号实现“自动跳闸”：当底面收到红石信号（`level.getSignal(pos.below(), Direction.UP) > 0`）时强制变为断开。

#### 4.4 刷卡机红石输入

[CardReaderBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/card_reader/CardReaderBlock.java) 贴墙放置，`use` 时检查手持的是否为 `CardItem` 且通过 `Config.isCardAccepted` 等级校验：接受则 `POWERED = true` 并 `scheduleTick`，等待配置的 `on_ticks` 后由 `tick` 自动复位为 `false`。

> **变更记录（v0.0.5）刷卡机碰撞箱 14x14x2 → 8x14x3**
> 刷卡机碰撞箱由 14×14×2 调整为 8×14×3（与模型元素一致）：宽向（平行墙面）居中 8 格、左右各空 4 格，高 14 格不变，贴墙厚度 3 格。参数区将原 `BOX_LO/BOX_HI`（宽高共用）拆分为独立的 `BOX_W_LO/W_HI`（4~12）与 `BOX_H_LO/H_HI`（1~15），`BOX_THICKNESS` 改为 3；四个方向 SHAPE 均按此缩放。

#### 4.5 圆盘记录仪时钟（红石输入）

[PlotterClockBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Plotter/PlotterClockBlockEntity.java) 存储上一次信号 `lastSignal`，在 `onNeighborChanged` 中检测**上升沿**（0 → 非 0）后向上扫描 1~8 格触发划线（详见 6.3）：

```java
public void onNeighborChanged() {
    int cur = level.getBestNeighborSignal(worldPosition);
    if (lastSignal == 0 && cur > 0) {   // 上升沿
        triggerAbove();
    }
    setLastSignal(cur);
}
```

### 5. 红石输出

#### 5.1 普通拉杆

[ConsoleLeverBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Console_lever/ConsoleLeverBlock.java) 直接继承原版 `LeverBlock`（自带红石输出与朝向逻辑），额外实现方块实体与文字显示。

#### 5.2 3挡位拉杆（ConsoleLever3StageBlock）

[ConsoleLever3StageBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Console_lever/ConsoleLever3StageBlock.java) 用自定义 `STAGE`（0/1/2）替代原版 `POWERED`，右键循环切换，输出强度随挡位变化：

```java
// 输出强度：0 / 8 / 15
@Override
public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
    return switch (state.getValue(STAGE)) {
        case 1 -> 8;
        case 2 -> 15;
        default -> 0;
    };
}

// 直接输出：挡位 > 0 时在连接方向输出 15（与按钮相同模式）
@Override
public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
    return state.getValue(STAGE) > 0
            && getConnectedDirection(state) == direction ? 15 : 0;
}
```

挡位切换时需 `onRemove` 更新新旧位置邻居，保证输出实时刷新。

#### 5.3 控制台按钮（ConsoleButtonBlock）

[ConsoleButtonBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Console_button/ConsoleButtonBlock.java) 继承 `ConsolePanelBlock`，`POWERED` 表示按下状态。按下后 `scheduleTick(20)` 自动弹起：

```java
@Override
public InteractionResult use(...) {
    if (!level.isClientSide && !state.getValue(POWERED)) {
        level.setBlock(pos, state.setValue(POWERED, true), 3);
        level.scheduleTick(pos, this, 20);   // 20 tick 后弹起
    }
    return InteractionResult.sidedSuccess(level.isClientSide);
}

@Override
public int getSignal(...)  { return state.getValue(POWERED) ? 15 : 0; }
@Override
public int getDirectSignal(...) {
    return state.getValue(POWERED) && getConnectedDirection(state) == direction ? 15 : 0;
}
```

#### 5.4 安全按钮（SafeButtonBlock）

[SafeButtonBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Safe_button/SafeButtonBlock.java) 实现三阶段授权流程：`STAGE 1 →（潜行+右键）→ 2 →（右键）→ 3`。状态 3 是授权通过，此时输出红石；随后由 `tick` 心跳（配合方块实体 `ticksRemaining`）自动回退：状态 3 持续 `signal_ticks`（默认 20）回到 2，状态 2 持续 `timeout_ticks`（默认 200）回到 1：

```java
// 心跳：每秒排程一次，递减 ticksRemaining
@Override
public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    if (level.getBlockEntity(pos) instanceof ConsolePanelBlockEntity be) {
        be.ticksRemaining--;
        // 根据当前 STAGE 在 0 时跳转到下一状态（3→2→1），并更新邻居
        level.setBlock(pos, next, 3);
        level.updateNeighborsAt(pos, this);
    }
    level.scheduleTick(pos, this, 1);   // 持续心跳
}

// 仅状态 3 输出 15
@Override
public int getSignal(...)  { return state.getValue(STAGE) == 3 ? 15 : 0; }
@Override
public int getDirectSignal(...) {
    return state.getValue(STAGE) == 3 && getConnectedDirection(state) == direction ? 15 : 0;
}
```

#### 5.5 断路器与刷卡机红石输出

- **断路器** [BreakerBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Breaker/BreakerBlock.java)：`POWERED` 为通时向后方的面与上方输出 15，`getSignal` 在 `getConnectedDirection` 对应的面返回 15，并重写 `onPlace/onRemove` 更新邻居。
- **刷卡机** [CardReaderBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/card_reader/CardReaderBlock.java)：`POWERED` 时 `getSignal = 15`，在连接方向 `getDirectSignal = 15`（强充能贴墙方块，使墙另一侧红石线/中继器点亮）。

> **修复记录（v0.0.5）刷卡机无法强充能墙方块**
> - **现象**：刷卡机四周放方块、背后放红石线，没有任何一面被强充能（红石线不亮），但能激活紧邻的红石元件；控制台拉杆/按钮/安全按钮均正常。与 FACING 方向无关。
> - **根因**：Minecraft 红石信号方法（`getSignal`/`getDirectSignal`）的 `direction` 参数是**反向**语义——实际输出到 `direction.getOpposite()` 方向（原版 `LeverBlock#getDirectSignal` Javadoc 明确注明 "directions in redstone signal related methods are backwards"）。旧代码写成 `direction == state.getValue(FACING).getOpposite()`，实际强信号流向 `FACING`（朝外/朝玩家），墙在 `FACING.getOpposite()` 方向，故墙方块永远拿不到强充能，仅剩 `getSignal` 全向 15 能激活紧邻元件。
> - **修复**：改为与参考方块（`ConsoleButtonBlock`/`SafeButtonBlock`/`ConsoleLever3StageBlock`）及原版 `LeverBlock` 一致的写法 `getConnectedDirection(state) == direction`（即 `FACING == direction`），实际强信号流向 `FACING.getOpposite()` = 墙所在方向，正确强充能墙方块与墙后红石线。

#### 5.6 连接方向计算（getConnectedDirection）

“杠杆字节码金标准”——所有贴墙输出型方块共用，把贴面（WALL/CEILING/FLOOR）换算为实际连接方向，供 `getDirectSignal` 判断输出朝向：

```java
protected Direction getConnectedDirection(BlockState state) {
    return switch (state.getValue(FACE)) {
        case CEILING -> Direction.UP;
        case FLOOR  -> Direction.DOWN;
        default     -> state.getValue(FACING);
    };
}
```

### 6. 划线（圆盘记录仪）

#### 6.1 圆盘记录仪数据缓冲（PlotterBlockEntity）

[PlotterBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Plotter/PlotterBlockEntity.java) 继承 `ConsolePanelBlockEntity`，维护长度 24 的环形数据缓冲（值 0~15）。每 `tick()` 按模式间隔右移并采样当前红石信号写入首位：

```java
private static final int DATA_LENGTH = 24;
private int[] data = new int[DATA_LENGTH];
private int mode = 4;          // 1~9，默认4（间隔20tick）
private int tickCounter = ...;

public void tick() {
    int interval = Config.getPlotterTickInterval(mode);
    if (interval <= 0) return;        // 模式1：被动触发，不自动运行
    if (--tickCounter <= 0) {
        tickCounter = interval;
        doSample();
    }
}

public void doSample() {
    System.arraycopy(data, 0, data, 1, DATA_LENGTH - 1);  // 整体右移
    data[0] = level.getBestNeighborSignal(worldPosition); // 采样红石强度
    sync();
}
```

模式由多功能工具切换，NBT 保存 `PlotterData / PlotterMode / TickCounter`。

#### 6.2 圆盘记录仪渲染（PlotterBER）

[PlotterBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Plotter/PlotterBER.java) 复用文字渲染管线，用 `█`（FULL BLOCK）字符缩放 1/4 变成 2 像素小方块，红色绘制趋势曲线：

```java
private static final float START_X = 22.0F, START_Y = 48.0F;
private static final float STEP_PIXEL = 2.0F, VALUE_PIXEL = 2.0F;
private static final float SCALE = 0.25F;          // 8px 字符 → 2px 方块
private static final int COLOR_ARGB = 0xFFFF0000;  // 纯红
private static final int PACKED_LIGHT = 0xF000F0;

private void renderPlot(int[] data, PoseStack poseStack, MultiBufferSource bufferSource) {
    poseStack.pushPose();
    poseStack.scale(SCALE, SCALE, SCALE);
    for (int i = 0; i < data.length; i++) {
        int value = Math.max(0, Math.min(15, data[i]));
        if (value <= 0) continue;    // 跳过0值
        float fx = (START_X - i * STEP_PIXEL) * INV_SCALE;        // 向左偏移
        float fy = (START_Y - value * VALUE_PIXEL) * INV_SCALE;   // 向上偏移
        font.drawInBatch(Component.literal("█"), fx, fy, COLOR_ARGB, false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL,
                0, PACKED_LIGHT);
    }
    poseStack.popPose();
}
```

绘制前调用 `ConsolePanelBER.pushTextTransform` 进入文字坐标系，先画命名文字再画趋势图。

#### 6.3 圆盘记录仪时钟触发

[PlotterClockBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Plotter/PlotterClockBlockEntity.java) 在检测到红石**上升沿**后，向上扫描 1~8 格，对其中 `mode == 1`（被动触发）的圆盘记录仪执行一次 `doSample()`：

```java
private void triggerAbove() {
    for (int dy = 1; dy <= 8; dy++) {
        BlockEntity be = level.getBlockEntity(base.above(dy));
        if (be instanceof PlotterBlockEntity pbe && pbe.getMode() == 1) {
            pbe.doSample();
        }
    }
}
```

`PlotterClockBlock` 为完整方块，在 `neighborChanged` 中调用实体 `onNeighborChanged()`，NBT 保存 `LastSignal`。

### 7. CC: Tweaked 外设

在 [CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java#L64-L81) 的 `commonSetup` 中通过 `ForgeComputerCraftAPI.registerPeripheralProvider` 注册外设，使数码显示器、数字调节器与数字圆盘记录仪可被 CC 电脑识别：

```java
ForgeComputerCraftAPI.registerPeripheralProvider(new IPeripheralProvider() {
    @Override
    public LazyOptional<IPeripheral> getPeripheral(Level world, BlockPos pos, Direction side) {
        if (world.getBlockEntity(pos) instanceof DigitalDisplayBlockEntity be)
            return LazyOptional.of(() -> new DigitalDisplayPeripheral(be));
        if (world.getBlockEntity(pos) instanceof DigitalKnobBlockEntity knobBE)
            return LazyOptional.of(() -> new DigitalKnobPeripheral(knobBE));
        if (world.getBlockEntity(pos) instanceof DigitalPlotterBlockEntity plotterBE)
            return LazyOptional.of(() -> new DigitalPlotterPeripheral(plotterBE));
        return LazyOptional.empty();
    }
});
```

- [DigitalDisplayPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_display/DigitalDisplayPeripheral.java)：`getType() = "digital_display"`，Lua 接口 `setStatus(text)` / `getStatus()`，修改橙色状态文字。
- [DigitalKnobPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_knob/DigitalKnobPeripheral.java)：`getType() = "digital_knob"`，Lua 接口 `setValue(int)` / `getValue()`，读写整数（0~1000，越界钳制；百分比模式不影响 CC 读写值）。

### 8. 多功能工具（MultiToolItem）

[MultiToolItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/MultiToolItem.java) 无耐久不可堆叠，用于切换各类方块的模式：

- **`onItemUseFirst`**：对数字调节器右键切换百分比模式开关（潜行 + 右键重置为开启），返回 `SUCCESS` 拦截原交互。
- **`useOn`**：对圆盘记录仪右键切换模式（1→2→…→9→1，潜行重置为 1），并把新模式的采样间隔以聊天消息告知玩家。

切换后消息键：`message.cc_rc.digital_knob_percent`、`message.cc_rc.plotter_mode`、`message.cc_rc.plotter_mode_passive`。

### 9. 数字圆盘记录仪（DigitalPlotter）

数字圆盘记录仪是完整方块，放置方式同数码显示器（水平四方向，屏幕朝向玩家面对方向），模型使用 Blockbench 制作的自有模型（[digital_plotter.json](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/models/block/digital_plotter/digital_plotter.json)，贴图 `textures/block/digital_plotter.png` 64×64）。与圆盘记录仪不同：**没有状态**（不自动采样、无红石触发），通过 CC 电脑读写长度 50 的整数列表（值 0~100，越界自动钳制）。

- [DigitalPlotterBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_plotter/DigitalPlotterBlock.java)：继承 `Block implements EntityBlock`，仅 `HORIZONTAL_FACING` 状态；`setPlacedBy` 把放置时物品自定义名称写入方块实体作为命名文字。
- [DigitalPlotterBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_plotter/DigitalPlotterBlockEntity.java)：直接继承 `BlockEntity`，存储 `int[50]` 数据（值 0~100）与命名文字，NBT 保存 `PlotterData / Text`，网络同步三件套。

```java
private static final int DATA_LENGTH = 50;   // 列表长度 50（圆盘记录仪 24）
private static final int MIN_VALUE = 0, MAX_VALUE = 100;

public void push(int value) {                // 写入一个新值：删除最后一位并移位
    value = Math.max(MIN_VALUE, Math.min(MAX_VALUE, value));
    System.arraycopy(data, 0, data, 1, DATA_LENGTH - 1);
    data[0] = value;
    sync();
}

public boolean setValue(int index, int value) { ... }   // 手动设置某一位，越界不写入
public Integer getValue(int index) { ... }              // 读取某一位，越界返回 null
```

- [DigitalPlotterBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_plotter/DigitalPlotterBER.java)：完整方块变换同数码显示器，命名文字（白）一行；趋势线图用 `█` 字符 NORMAL 模式绘制（同圆盘记录仪），点缩放 `PLOT_SCALE = 0.15F`、`STEP_PIXEL = 1.15F`、`VALUE_PIXEL = 0.32F`，50 点以 `START_X = 27.5F`、`START_Y = 23.0F` 居中；命名文字 `NAME_Y = 0.36F`（相对块中心，正数向上），绘图区原点 `PLOT_Y = -0.09F`。
- **朝向修正**：自有模型屏幕位于 −Z（北）侧（数码显示器在 +Z 侧），故 `blockstates/digital_plotter.json` 四朝向 y 旋转在数码显示器基础上整体 +180°（north=180 / south=0 / east=270 / west=90）；BER 沿用 `TEXT_Z=-0.57F`，与旋转 180° 后屏幕正面（模型 z=17 → 世界约 1.06）自动对齐，文字/线条落在屏幕上。
- **Z 分层**：划线平面与命名文字分离，`PLOT_Z = TEXT_Z + 1.0F/16.0F`（向块中心收 1 个模型像素 = 1/16 格），使划线比文字更靠里一层面，避免与文字重叠干扰。
- [DigitalPlotterPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_plotter/DigitalPlotterPeripheral.java)：`getType() = "digital_plotter"`，Lua 接口（索引采用 Lua 惯例 1~50，对应内部 0~49，越界不予读写）：

| Lua 方法 | 说明 |
| --- | --- |
| `push(value)` | 写入一个新值（自动删除最后一位并移位），返回写入后的首位值 |
| `setValue(index, value)` | 手动设置某一位的值，返回是否成功；索引越界返回 false |
| `getValue(index)` | 读取某一位的值；索引越界返回 nil |
| `getList()` | 读取整个列表（长度 50，索引 1~50） |

> 写入值只能为整型；超过 100 按 100 计，小于 0 按 0 计；超出列表范围（<1 或 >50）不予写入或读取。

### 10. 旧模组移植物品（ModToolTiers）

从旧模组（`cdes`，NeoForge 1.21.1 + Create Registrate）移植 5 个物品到本模组（Forge 1.20.1 + `DeferredRegister`）。由于版本不同**未复制代码**，全部按 1.20.1 API 重写：

- [ModToolTiers.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/ModToolTiers.java)：按 1.20.1 `Tier` 接口（6 个方法，无 1.21 新增的 `getIncorrectBlocksForDrops`）实现自定义材质 `FROZEN_TILAPIA`（耐久 2、挖掘速度 12、攻击加成 255、挖掘等级 0、附魔能力 0、蓝冰修复）。
- 物品注册见 [ModItems.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModItems.java#L244-L298)，模型与贴图位于 `assets/cc_rc/models/item/*.json` 与 `textures/item/*.png`。

```java
// 冰冻罗非鱼：1.20.1 用 SwordItem(Tier, 攻击修正, 攻速, Properties)，
// 修改器攻击伤害 = 修正(0) + Tier 加成(255) = +255（实际总伤害 = 基础 1 + 255 = 256）；
// 旧版用属性覆盖实现相同数值
public static final RegistryObject<SwordItem> FROZEN_TILAPIA = ITEMS.register("frozen_tilapia",
        () -> new SwordItem(ModToolTiers.FROZEN_TILAPIA, 0, -2.0F,
                new Item.Properties().stacksTo(1)));

// 冰冻罗非鱼手持模型（特殊设置）：models/item/frozen_tilapia.json 采用 item/handheld 姿态
// （第三人称工具与手臂同平面，rotation [0,-90,55]），保持原版正常大小（0.85/0.68），不放大。

// 沉船绿酒：饮用后剧毒(4)+反胃(3)，各 114514 刻
public static final RegistryObject<Item> GREEN_WINE = ITEMS.register("green_wine",
        () -> new Item(new Item.Properties().stacksTo(16)
                .food(new FoodProperties.Builder()
                        .nutrition(2).saturationMod(0.4F).alwaysEat()
                        .effect(() -> new MobEffectInstance(MobEffects.POISON, 114514, 4), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 114514, 3), 1.0F)
                        .build())));

// 何意味？：营养11/饱和度4/RARE，食用后同时获得 10 种效果
//（1.20.1 无 TRIAL_OMEN，故比旧版少一种）
public static final RegistryObject<Item> HE_YI_WEI = ITEMS.register("he_yi_wei", () -> new Item(...));

// 合一味：营养30/饱和度0.8/可随时食用
public static final RegistryObject<Item> TASTES_FOOD = ITEMS.register("tastes_food", () -> new Item(...));

// 沉船绿酒桶：普通物品，无特殊功能
public static final RegistryObject<Item> GREEN_WINE_BARREL = ITEMS.register("green_wine_barrel",
        () -> new Item(new Item.Properties()));
```

> 移植要点：① 1.20.1 无 `ItemAttributeModifiers` 组件（1.20.5+ 引入），武器属性改用 `SwordItem` 构造器参数等价实现；② 旧版 `Tier` 的攻击加成 100 在 1.21 中被属性覆盖从未生效，1.20.1 以 Tier 加成 255 + 修正 0 复现实际显示值；③ 旧版 `HE_YI_WEI` 的 `TRIAL_OMEN` 为 1.21 专属效果，1.20.1 不存在，已剔除。

### 11. 描述型物品与新增食物

参考「圆盘记录仪时钟」的悬停描述模式（`DescriptionBlockItem` + 语言 key `item.cc_rc.desc_*`），抽象出三个描述型物品基类，并新增 3 个物品（月饼 × 2、压缩饼干）：

- [DescriptionItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/DescriptionItem.java)：普通物品 + 描述，构造时可选传入 `ChatFormatting` 颜色为描述文字染色。
- [DescriptionSwordItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/DescriptionSwordItem.java)：`SwordItem` + 描述，同样支持可选颜色。
- [AttackDescriptionItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/AttackDescriptionItem.java)：普通物品 + 描述 + 主手攻击伤害属性修饰符（非武器、无耐久、可堆叠），用于"五金月饼"。
- 四个描述类（`DescriptionBlockItem` / `DescriptionItem` / `DescriptionSwordItem` / `AttackDescriptionItem`）均继承对应物品基类并在 `appendHoverText` 中追加翻译文本。

```java
// 五金月饼：普通物品注册（非武器、最大堆叠 16、无耐久），
// 修饰符 9.0 + 空手基础 1.0 = 实际攻击 10（工具提示 "+10 Attack Damage"）
public static final RegistryObject<Item> MOON_CAKE_IRON = ITEMS.register("moon_cake_iron",
        () -> new AttackDescriptionItem(new Item.Properties().stacksTo(16)
                        .food(new FoodProperties.Builder()
                                .nutrition(4).saturationMod(2F).alwaysEat()
                                .effect(() -> new MobEffectInstance(MobEffects.WITHER, 40, 1), 1.0F)
                                .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 4000, 1), 1.0F)
                                .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 4000, 0), 1.0F)
                                .build()),
                "item.cc_rc.desc_moon_cake_iron", 9.0D));

// 沉船绿酒：描述文字染色为绿色
public static final RegistryObject<Item> GREEN_WINE = ITEMS.register("green_wine",
        () -> new DescriptionItem(new Item.Properties().stacksTo(16)
                .food(...), "item.cc_rc.desc_green_wine", ChatFormatting.GREEN));
```

新物品清单与数值：

| 物品 ID | 类 | 功能 |
| --- | --- | --- |
| `moon_cake` | `Item` | 五仁月饼：营养 8 / 饱和度 4，可随时食用（饥饿值满也可，类似金苹果），食用后 200 秒（4000 刻）速度 II，堆叠 16 |
| `moon_cake_iron` | `AttackDescriptionItem` | 五金月饼：普通物品（无耐久、堆叠 16），主手攻击伤害 10，营养 4 / 饱和度 2，可随时食用（饥饿值满也可，类似金苹果），食用后 2 秒（40 刻）凋零 II + 200 秒（4000 刻）力量 II 与抗性提升，描述"机加工这一块/." |
| `ship_biscuit` | `DescriptionItem` | 压缩饼干：营养 12 / 饱和度 6，食用后 60 秒（1200 刻）饱和效果 + 20 秒（400 刻）生命恢复，描述"*量大管饱*" |

描述改动（均沿用 `item.cc_rc.desc_*` 语言 key）：

- `digital_knob`：`BlockItem` → `DescriptionBlockItem`，描述"使用多功能工具右键切换显示模式"。
- `frozen_tilapia`：`SwordItem` → `DescriptionSwordItem`，描述"三体宇宙最强武器（bushi）"，颜色 `GOLD`（金色），物品名 `Rarity.RARE`（显示蓝色）。
- `green_wine`：`Item` → `DescriptionItem`，描述"这酒能喝吗？……都绿了"，颜色 `GREEN`。
- `he_yi_wei`：`Item` → `DescriptionItem`，描述"*你想何出怎样的意味？*"，颜色 `DARK_PURPLE`（紫）。

> 数值换算：1 秒 = 20 游戏刻，速度 II = `MOVEMENT_SPEED` 等级 2（amplifier 1），凋零 II = `WITHER` 等级 2，力量 II = `DAMAGE_BOOST` 等级 2，抗性提升未指定等级用 1 级（amplifier 0），生命恢复 1 级（amplifier 0）。

### 12. 搬运物品与搬运物品栏

从外部模组（如农夫乐事）搬运装饰性物品到本模组，模型与贴图**本地化为 `cc_rc` 命名空间**（源资源引用 `farmersdelight:` 命名空间，但项目未依赖该模组）：

- **箱装土豆方块**（`potato_crate`，`Block`）：搬运自农夫乐事，无方向完整方块，木板材质（`MapColor.WOOD` 地图颜色 + `SoundType.WOOD` 木质音效，强度 2.0），仅作装饰展示用。模型复用原版 `cube_bottom_top` 父模型，贴图三张（底/侧/顶）拷贝至 `assets/cc_rc/textures/block/potato_crate/`，模型引用改写为 `cc_rc:block/potato_crate/*`；无方向方块故 blockstate 使用单变体 `""`。
- **箱装土豆物品**（`potato_crate`，`DescriptionBlockItem`）：关联上述方块（方块物品），悬停显示两条带样式描述——第一行"搬运自农夫乐事"（黄色 `YELLOW` + 斜体 `ITALIC`）标识搬运来源，第二行"！？服务器 ？！"（淡蓝 `AQUA` + 粗体 `BOLD`）。为此扩展 `DescriptionBlockItem` 支持传入多个已构建样式的 `Component` 逐行显示。
- **搬运物品栏**（`cc_rc_tab_carried`，标题"CCRC：搬运的物品"）：Forge 按注册名对模组创造标签排序，注册名 `cc_rc_tab_carried` 前缀比主标签 `cc_rc_tab` 更长，按字母序排在其后；图标为箱装土豆，仅收录搬运类物品；箱装土豆与全部粗布告示牌**不加入**主物品栏，只出现在此标签中。
- **16 色粗布告示牌与悬挂式粗布告示牌**（`<颜色>_canvas_sign` 系列，搬运自农夫乐事）：完全沿用原版告示牌机制——每个颜色注册 4 个方块（立式 `StandingSignBlock` / 壁挂 `WallSignBlock` / 天花板悬挂 `CeilingHangingSignBlock` / 壁挂悬挂 `WallHangingSignBlock`），**每种颜色注册各自独立的 `WoodType`** `"cc_rc:canvas_<颜色>"`（[ModWoodTypes.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModWoodTypes.java) 中循环注册 16 个，复用 `BlockSetType.OAK`，顺序与 `ModBlocks.CANVAS_SIGN_COLORS` 一致）。物品为两个：`<颜色>_canvas_sign`（`DescriptionSignItem`，立式/壁挂共用）与 `<颜色>_hanging_canvas_sign`（`DescriptionHangingSignItem`，天花板/壁挂悬挂共用），悬停显示**黄色斜体**描述"搬运自农夫乐事"（复用 key `item.cc_rc.desc_potato_crate_carried`）。模型/贴图本地化为 `cc_rc` 命名空间：blockstate 全部指向 `cc_rc:block/canvas_sign`（仅 particle 纹理，世界渲染由原版 SignRenderer/HangingSignRenderer 完成），物品图标用 `item/generated` + `cc_rc:item/<名>` 贴图（普通与悬挂各 16 张独立图标，均直接拷贝自农夫乐事源 `textures/item/`）。
  - **编辑文字 GUI 背景（关键）**：原版 `HangingSignEditScreen` 的编辑界面背景贴图路径由 `woodType.name()` 加 `textures/gui/hanging_signs/` 前缀拼出——木种 `cc_rc:canvas_<颜色>` 解析为 `cc_rc:textures/gui/hanging_signs/canvas_<颜色>.png`（16×16，blit 到屏幕）。该目录原未提供贴图导致编辑 GUI 紫黑。已按各色生成 16 张背景：以原版 `minecraft:textures/gui/hanging_signs/oak.png` 为模板（16×16，保留链条金属色与透明区域），面板区域按对应染料色（`DyeColor` RGB）重着色并保留明暗层次，生成脚本 `模型/农夫乐事/gen_hanging_gui.ps1`。
  - **告示牌面板贴图（关键）**：原版 `Sheets.createSignMaterial` 用 `entity/signs/<name>.png`、`createHangingSignMaterial` 用 `entity/signs/hanging/<name>.png` 作为材质路径（两者模板不同）。每种颜色注册独立 WoodType 名（如 `"cc_rc:canvas_white"`，命名空间 `cc_rc` + path `canvas_white`）后，材质路径自动解析为各色专属文件，因此必须提供 32 张贴图：
    - 普通式（16 张）：`assets/cc_rc/textures/entity/signs/canvas_<颜色>.png`
    - 悬挂式（16 张）：`assets/cc_rc/textures/entity/signs/hanging/canvas_<颜色>.png`
    32 张贴图均**直接拷贝自农夫乐事源贴图** `模型/农夫乐事/canvas_sign/signs/`（普通式 64×32、悬挂式 64×32，原版 oak 实体贴图 UV 布局），确保 UV 布局与 `SignRenderer.createSignLayer` / `HangingSignRenderer.createHangingSignLayer` 的模型完全匹配；若仅自绘整图边框会与模型 UV 采样区域错位（面板显示错乱），且缺悬挂式贴图会导致紫黑块。
  - **自定义方块实体类型**：原版 `BlockEntityType.SIGN`/`HANGING_SIGN` 的 validBlocks 不含本模组方块，`BlockEntityRenderDispatcher` 渲染前会做 `getType().isValid(blockState)` 校验并跳过渲染（告示牌透明、无文字）。故注册自定义类型 `canvas_sign_be`/`canvas_hanging_sign_be`（[ModBlockEntities.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModBlockEntities.java)），方块用 4 个自定义子类返回自定义实体，客户端为两类型注册原版 `SignRenderer`/`HangingSignRenderer`（详见「三、3 方块实体」）。模型层与材质仍由原版机制按 `WoodType` 自动生成，无需手动注册。

```java
// 箱装土豆方块：无方向完整方块，木板材质（木质地图颜色/音效），仅作装饰展示
public static final RegistryObject<Block> POTATO_CRATE = BLOCKS.register("potato_crate",
        () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.0F)
                .sound(SoundType.WOOD)));

// 箱装土豆物品：方块物品，两行带样式描述（黄斜体来源标识 + 淡蓝粗体装饰语）
public static final RegistryObject<Item> POTATO_CRATE = ITEMS.register("potato_crate",
        () -> new DescriptionBlockItem(ModBlocks.POTATO_CRATE.get(), new Item.Properties(),
                Component.translatable("item.cc_rc.desc_potato_crate_carried").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC),
                Component.translatable("item.cc_rc.desc_potato_crate").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));

// 16 色粗布告示牌：每色 4 个方块（立式/壁挂/天花板悬挂/壁挂悬挂），
// 每种颜色独立的 WoodType "cc_rc:canvas_<颜色>"（ModWoodTypes.CANVAS_TYPES[i]）
// 方块需使用自定义子类（newBlockEntity 返回 CanvasSignBlockEntity，配合自定义方块实体类型
// canvas_sign_be 通过渲染调度器的 isValid 校验，详见三、3 方块实体的说明）
// 例：立式
public static final RegistryObject<Block> WHITE_CANVAS_SIGN = BLOCKS.register("white_canvas_sign",
        () -> new CanvasStandingSignBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .noCollission()
                .strength(1.0F), ModWoodTypes.CANVAS_TYPES[0]));

// 例：物品（立式/壁挂共用 SignItem，黄斜体"搬运自农夫乐事"）
public static final RegistryObject<Item> WHITE_CANVAS_SIGN_ITEM = ITEMS.register("white_canvas_sign",
        () -> new DescriptionSignItem(new Item.Properties(),
                ModBlocks.CANVAS_SIGN_BLOCKS.get(0).get(),
                ModBlocks.CANVAS_WALL_SIGN_BLOCKS.get(0).get(),
                Component.translatable("item.cc_rc.desc_potato_crate_carried").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC)));

// 搬运物品栏：注册名 cc_rc_tab_carried 按注册名排序位于主标签之后，仅收录搬运类物品，图标为箱装土豆
public static final RegistryObject<CreativeModeTab> CC_RC_CARRIED_TAB = CREATIVE_TABS.register("cc_rc_tab_carried",
        () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.cc_rc_carried"))
                .icon(() -> new ItemStack(ModItems.POTATO_CRATE.get()))
                .displayItems((parameters, output) -> output.accept(ModItems.POTATO_CRATE.get()))
                .build());
```

### 13. 冰箱与水槽

从 Cooking for Blockheads（Cfb）搬运另外两个方块：冰箱、水槽。参考代码（`模型/搬运/冰箱`、`模型/搬运/水槽`）依赖 **Balm API**，本项目不引入 Balm，全部按纯 Forge 原版 API 重写简化：

- **冰箱（`fridge`）**：[FridgeBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/fridge/FridgeBlock.java) 为水平四方向完整方块（`FACING`），实现 `EntityBlock` + `use` 右键打开容器菜单；[FridgeBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/fridge/FridgeBlockEntity.java) 继承原版 `RandomizableContainerBlockEntity`（27 格容器，支持掉落物表与自定义命名），GUI **复用原版箱子界面**（`ChestMenu.threeRows`），破坏时 `onRemove` 调用 `Containers.dropContents` 掉落容器内容（与原版箱子一致）。
- **水槽（`sink`）**：[SinkBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/sink/SinkBlock.java) 为水平四方向完整装饰方块（`FACING`），**无方块实体**（参考代码的流体/洗锅边逻辑依赖 Balm 与 Cfb 注册表，已简化掉）。右键手持物品时通过 `RecipeManager` 查询 `cc_rc:sink_conversion` 类型配方执行四向转换：桶→水桶、玻璃瓶→水瓶、水桶→桶、水瓶→玻璃瓶（数据驱动，配方文件在 `data/cc_rc/recipes/`，详见 14 节）。背包已满时不消耗原物品并返回 `FAIL`。
- **放置朝向（修复）**：两方块模型正面（冰箱门 / 水槽柜门+水龙头）位于模型 **-Z 侧**。`getStateForPlacement` 若直接取 `context.getHorizontalDirection()`（玩家朝向），正面会朝向远处、玩家看到的是背面。已改为 **`getHorizontalDirection().getOpposite()`**（同原版熔炉 `FurnaceBlock` 逻辑），使放置后正面朝向玩家。
- **缝隙透视（修复）**：模型非完整 16×16×16 方块（冰箱元素多在 1~15 留缝、水槽元素非满格），但默认遮挡形状（`getOcclusionShape` → 默认返回完整碰撞箱形状）会让渲染器**剔除相邻完整方块朝向本方的面**，从缝隙处透视出虚空/邻块内部。已覆写 `getOcclusionShape` 返回 `Shapes.empty()`（同玻璃做法），使相邻完整方块的面正常渲染，缝隙不再透视。
- **资源**：模型/贴图本地化为 `cc_rc` 命名空间（源引用 `cookingforblockheads:` → `cc_rc:`）。冰箱模型沿用参考的完整静态模型（含门/把手/合页/密封条/层板，`parent: block/block`），贴图 4 张（side/top/back/inside）拷入 `assets/cc_rc/textures/block/fridge/`；水槽模型引用 `minecraft:block/polished_andesite`、`terracotta`、`black_terracotta` 与 `cc_rc:block/sink/sink_metal`（原 `handle` 纹理缺失，改用 `sink_metal`）。blockstate 按 `FACING` 四朝向旋转。
- **数据**：掉落表 `data/cc_rc/loot_tables/blocks/fridge.json`（掉落自身 + 复制方块实体自定义名）、`sink.json`；合成配方 `data/cc_rc/recipes/fridge.json`（箱子 + 铁门）、`sink.json`（铁锭×6 + 陶瓦×6 + 水桶，原 `balm:` 标签改写为原版物品）。

```java
// 冰箱方块：水平四方向完整方块，右键打开容器菜单（服务端），破坏时掉落容器内容
public class FridgeBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    // registerDefaultState / createBlockStateDefinition 添加 FACING
    // newBlockEntity -> new FridgeBlockEntity(pos, state)
    // use -> player.openMenu((FridgeBlockEntity) blockEntity)
    // onRemove -> Containers.dropContents(level, pos, (FridgeBlockEntity) blockEntity)
}

// 冰箱方块实体：27 格容器，复用原版箱子 GUI（ChestMenu.threeRows）
public class FridgeBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override public int getContainerSize() { return 27; }
    @Override protected Component getDefaultName() { return Component.translatable("container.cc_rc.fridge"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return ChestMenu.threeRows(id, inv, this);   // 原版箱子 3 行界面
    }
}
```

### 14. 自定义配方类型（sink_conversion）

将水槽的四向转换（桶↔水桶、玻璃瓶↔水瓶）做成**数据驱动配方**：由 [`ModRecipes.java`](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModRecipes.java) 注册自定义配方类型 `cc_rc:sink_conversion`（`RecipeType` + `RecipeSerializer` 各一），配方 JSON 放在 `data/cc_rc/recipes/`，水槽右键时经 `RecipeManager` 查询匹配。

- **为什么需要自定义配方类型**：原版 `Ingredient` 不支持 NBT 匹配，而四向转换中的"水瓶"是 `minecraft:potion` + NBT `{Potion:"minecraft:water"}`，输入需要按 NBT 区分"水瓶 vs 其他药水"，输出需要能写出带 NBT 的产物——原版 JSON 配方无法表达，故自研。
- **[SinkConversionRecipe.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/recipe/SinkConversionRecipe.java)**：实现 `Recipe<Container>`。核心是 `matchesItem(ItemStack)`（物品类型 + 输入 NBT 部分匹配——物品标签包含 inputNbt 全部键值即命中）、`getOutputCopy()`（返回含输出 NBT 的产物副本）。
- **[SinkConversionSerializer.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/recipe/SinkConversionSerializer.java)**：JSON 格式（SNBT 字符串写 NBT，`TagParser.parseTag` 解析）：

```json
{
  "type": "cc_rc:sink_conversion",
  "input":      { "item": "minecraft:glass_bottle" },
  // "input_nbt":  "{Potion:\"minecraft:water\"}",    可选：输入需包含的 NBT（部分匹配）
  "output":     { "item": "minecraft:potion", "count": 1 },
  "output_nbt": "{Potion:\"minecraft:water\"}"         可选：写入产物的 NBT
}
```

- **SinkBlock.use** 不再硬编码转换：遍历 `recipeManager.getAllRecipesFor(SINK_CONVERSION_TYPE)` 找 `matchesItem(heldItem)` 的第一个匹配配方，取其 `getOutputCopy()` 作为产物，消耗 1 个输入后给予玩家（堆叠为 1 直接替换手持、否则入背包再扣减；背包满返回 `FAIL` 不消耗），并按输入/输出类型播放装/倒水桶（瓶）音效。
- **配方文件（4 个，`data/cc_rc/recipes/`）**：

| 文件 | 转换 |
| --- | --- |
| `sink_bucket_to_water_bucket.json` | 桶 → 水桶 |
| `sink_bottle_to_water_bottle.json` | 玻璃瓶 → 水瓶（输出 NBT：Potion 水） |
| `sink_water_bucket_to_bucket.json` | 水桶 → 桶 |
| `sink_water_bottle_to_bottle.json` | 水瓶（输入 NBT：Potion 水）→ 玻璃瓶 |

### 15. 包子（bao_zi）

包子是「食物 + 投掷炸弹」二合一物品，是本模组第一个**弹射物实体**：

- **物品 [BaoZiItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/BaoZiItem.java)**：注册 `bao_zi`，最大堆叠 16，`FoodProperties` 营养 4 / 饱和度 2，右键食用；悬停描述"包子雷？"（红色斜体）。加入主创造标签。
- **弹射物实体 [BaoZi.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/BaoZi.java)**：实体类型 `cc_rc:bao_zi`（[ModEntities.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModEntities.java) 中 `EntityType.Builder.of(BaoZi::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)`），继承原版 `Snowball`，**飞行逻辑完全沿用雪球**（重力、旋转等）。重写 `onHit(HitResult)`：命中任意实体或方块时调用 `level().explode(this, x, y, z, 4.0F, Level.ExplosionInteraction.NONE)`——`NONE` 交互只造成实体爆炸伤害 + 原版爆炸粒子/音效，不破坏方块、无方块掉落，然后 `discard()`。
  - **投掷构造注意**：不能直接用 `super(level, shooter)`（原版 `Snowball(Level, LivingEntity)` 会硬编码 `EntityType.SNOWBALL`），且 `Snowball` 没有 `(EntityType, LivingEntity, Level)` 构造，故先按 `ModEntities.BAO_ZI.get()` 构造，再手动复刻 `ThrowableProjectile` 便利构造器逻辑（`setPos(shooter.getX(), shooter.getEyeY()-0.1, shooter.getZ())` + `setOwner(shooter)`）。
  - **渲染**：客户端 `ClientModEvents` 为 `bao_zi` 注册原版 `ThrownItemRenderer`（实现 `ItemSupplier`，按 `getDefaultItem()` 兜底显示 `bao_zi` 物品贴图）。
- **左键投掷**：Forge 客户端事件 `InputEvent.InteractionKeyMappingTriggered`（[ClientInputHandler.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ClientInputHandler.java)）——当 `event.isAttack()` 且主手持 `bao_zi` 时 `event.setCanceled(true)` 取消默认攻击，并通过简单网络通道（[ModNetwork.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ModNetwork.java)、[ThrowBaoZiPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ThrowBaoZiPacket.java)）发送 C2S 数据包；服务端校验主手物品后生成弹射物 `shootFromRotation(player, ...初速1.5)` 并消耗 1 个包子（创造模式不消耗）。该事件在 `Minecraft.startAttack` 的 `while(keyAttack.consumeClick())` 循环中触发，每次点击触发一次。
  - **投掷冷却（0.5 秒）**：服务端使用原版物品冷却（`player.getCooldowns()`）作权威校验——发放前 `isOnCooldown` 则忽略，否则 `addCooldown(item, 10)`（常量 `BaoZiItem.THROW_COOLDOWN_TICKS = 10` = 0.5 秒，见 [BaoZiItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/BaoZiItem.java)），防止连续投掷/作弊；客户端在发送数据包前也检查同一冷却以减少无效发包，且 HUD 会显示冷却动画。

### 16. 法棍（baguette）

法棍是「食物 + 攻击伤害 + 较强击退」的普通物品（无方块关联）：

- **物品 [BaguetteItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/BaguetteItem.java)**：注册 `baguette`，最大堆叠 16，`FoodProperties` 营养 2 / 饱和度 2，右键食用。加入主创造标签。
- **攻击伤害**：仿照 [AttackDescriptionItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/AttackDescriptionItem.java) 的属性修饰符模式，仅主手附加 `Attributes.ATTACK_DAMAGE` 修饰符 3.0（复用原版 `Item.BASE_ATTACK_DAMAGE_UUID`）——修饰符 3.0 + 空手基础 1.0 = **实际攻击 4**（工具提示 "+4 Attack Damage"）。
- **击退属性**：仅主手附加 `Attributes.ATTACK_KNOCKBACK = 3.0`（`Operation.ADDITION`，固定 UUID）。原版击退附魔每级 +1，3.0 等效击退 III，属于较强击退；无武器耐久、无攻速惩罚。
- **描述**：悬停显示"坚如磐石"（**粗体棕色**——棕色非 ChatFormatting 内置色，使用 `Style.EMPTY.withBold(true).withColor(0x835432)` 与原版棕色染料同色）。

### 17. 撬棍（crowbar）

撬棍是铁质剑类武器（[CrowbarItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/CrowbarItem.java)），参照冰冻罗非鱼的自定义 Tier 模式：

- **Tier [ModToolTiers.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/ModToolTiers.java)**：新增枚举 `CROWBAR`（耐久 1024、挖掘速度 6、**攻击伤害加成 19**、挖掘等级 2、附魔能力 10、铁锭修复）。
- **武器**：注册 `crowbar`，`CrowbarItem extends SwordItem` 天然**自带横扫攻击**；构造参数伤害修正 0 + Tier 加成 19 = **修改器攻击伤害 +19**（实际总伤害 = 基础 1 + 19 = 20）；攻速修正 **-3.0（攻击速度 1.0，挥动很慢）**；不可堆叠。
- **暴击增强**：内部静态事件类 `CrowbarItem.CritHandler` 监听 Forge 总线 `CriticalHitEvent`——主手持撬棍且为原版暴击（跳跃下落攻击）时 `setDamageModifier(2.0F)`，暴击伤害由原版 1.5 倍提升到 **2.0 倍**（en_US 文案"The Holy Sword of Physics"）。
- **打击音效**：`CritHandler` 另监听 `LivingHurtEvent`，攻击者为主手持撬棍的玩家时，在被击实体位置播放 **原版铁砧落地音效 `SoundEvents.ANVIL_LAND`**（`SoundSource.BLOCKS`，服务端广播，即"哐"的金属撞击声，与铁砧放置 `ANVIL_PLACE` 同源文件）。
- **描述两行**：`"物理学圣剑"`（浅蓝色 `AQUA` + 粗体 `BOLD`）、`"f(x)dx"`（深蓝色 `DARK_BLUE`，无删除线）。
- **手持模型（特殊设置）**：`models/item/crowbar.json` 采用 `item/handheld` 姿态（第三人称为原版手握工具姿态：工具与手臂同一平面，rotation `[0,-90,55]`，而非手持物品的垂直姿态），保持**原版正常大小**（第三人称 `0.85`、第一人称 `0.68`），不放大（仅简易长矛放大）。

### 18. 红石信号发射器与接收器（redstone_sender / redstone_receiver）

红石信号**发射器**与**接收器**组成"无线"红石传输对（[RedstoneSenderBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Redstone_transmit/RedstoneSenderBlock.java) 与 [RedstoneReceiverBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Redstone_transmit/RedstoneReceiverBlock.java)），贴图来自"模型/红石传输"：

- **发射器 `redstone_sender`**：完整方块，可朝 **x±/y±/z± 六方向**放置（`BlockStateProperties.FACING`），模型 `cube_bottom_top`（顶/底/侧贴图各一套 on/off，侧面贴图按"指向上方放置"绘制）。覆写 Forge 扩展 `canConnectRedstone=true` 连接红石线。`getStateForPlacement` 用 `getNearestLookingDirection().getOpposite()`（**面向玩家**，与原版活塞一致）。
  - `neighborChanged`（服务端）读取 `level.getBestNeighborSignal(pos)` 作为自身收到的信号强度，存入 `POWER`（0~15），**信号由 0 变非 0 切 on 贴图、由非 0 变 0 切 off 贴图**（blockstate `power=1|2|...|15` → on 模型）。
  - 信号变化时沿 `FACING` 方向**从距离 2（不含相邻方块）到配置距离**逐格扫描（`pos.relative(facing, d)`，d 从 2 起），遇 `RedstoneReceiverBlock` 即把其 `POWER` 写成相同强度。最大距离取自 `Config.getRedstoneTransmitRange()`。
- **接收器 `redstone_receiver`**：无方向完整方块（无 FACING），模型 `cube_all`（单贴图 off/on）。覆写 `canConnectRedstone=true` 连接红石线；`isSignalSource=true` 且 `getSignal` 返回 `POWER`，即**向相邻方块发出当前强度的红石信号**（可被红石线、拉杆、机器等识别）。`POWER` 由发射器直接写入，同样由 0↔非 0 切换 on/off 贴图。
- **配置 [Config.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/Config.java)**：新增 `redstone_transmit.transmit_range`（默认 **8**，范围 2~64），控制发射器沿面向方向的扫描距离。
- **blockstate**：两个方块均用 `multipart` + **管道 OR 语法**（`power=1|2|...|15`，与原版红石线的 `side|up` 同款写法）实现 0→off / 非 0→on 的模型切换；发射器另按 `FACING` 施加 `x/y` 旋转。
  - > **修复记录（放置后紫黑块）**：`when` 条件里最初写了范围语法 `power=1..15`，但 1.20.1 的 blockstate 条件解析器（`KeyValueCondition`）只支持 `|` 分隔的枚举值，不支持 `..` 范围（范围语法仅限指令参数 `BlockStateParser`），导致 blockstate 整体加载失败、世界内渲染紫黑缺失模型；物品栏模型不经过 blockstate 所以正常。已改为 `1|2|3|4|5|6|7|8|9|10|11|12|13|14|15` 修复。
  - > **修复记录（水平朝向贴图朝上）**：发射器是 `cube_bottom_top`（顶面为发射面），但水平朝向（N/S/E/W）原先只给了 `y` 旋转（绕竖轴自转），顶面仍朝上，看起来"模型方向都对着上方"。已改为给水平朝向加 `x` 旋转：`north → x:90`、`south → x:270`、`east → x:90,y:90`、`west → x:90,y:270`（参照本模组 plotter 的同款 `x` 先于 `y` 组合写法），使顶面发射面指向对应水平方向；`up/down` 保持不变（`up` 不转、`down → x:180`）。

### 19. F.A.A.S服务器（server_faas）

[ServerFaasBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/server_faas/ServerFaasBlock.java) 是三种变体共用的装饰方块类（`server_faas_1/2/3`），模型来源"模型/FAAS"（Bedrock 版转为 Java block 模型，含 22.5°/45° 旋转节点，UV 顶/底面已翻转适配），贴图 `textures/block/server_faas/faas_1~3.png`：

- **放置**：继承 `HorizontalDirectionalBlock`，水平四方向（N/S/E/W）放置，`getStateForPlacement` 用 `context.getHorizontalDirection()`（**面向玩家**，未加 `getOpposite()`），与奶龙玩偶一致。
- **音效**：靠近时持续循环播放 `server_noise`。播放逻辑由客户端监听器 [ServerFaasSoundHandler.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ServerFaasSoundHandler.java)（Dist.CLIENT）负责——每 20 tick 扫描玩家周围 16 格内的 FAAS 方块，为每个方块创建 `loop=true` 的 `AbstractTickableSoundInstance`（`FaasLoopingSound`）并交给 SoundManager 无缝循环播放；玩家远离/方块被破坏/离开世界时停止并移除实例。声音文件 `sounds/server_noise.ogg`。
  - > **修复记录（原音效断续/重叠）**：最初在 `animateTick` 中按概率（`random.nextFloat() < 0.2F`）`playLocalSound` 播放，仿原版营火/火把逻辑——但概率触发导致音效时有时无，且音效较长时会多次触发互相重叠。已改用上述"循环音效实例 + 距离开关"，移除 `animateTick` 覆写与随机播放。
- **性质**：完整方块（模型元素可超出方块边界渲染），金属音效 `SoundType.METAL`、强度 3.0/6.0、`requiresCorrectToolForDrops`；为三种变体各补一张 `loot_tables/blocks/server_faas_N.json` 使方块可掉落。

### 20. 说明书1（instruction_book_1）

[InstructionBookItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/InstructionBookItem.java) 采用**原版成书（WrittenBookItem）**样式注册的说明书物品（直接继承 `WrittenBookItem`）：

- **机制**：继承原版成书类；**右键打开书籍界面的逻辑已移交给客户端事件监听器 [InstructionBookClientHandler.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/InstructionBookClientHandler.java)**（`Dist.CLIENT` 注册，监听 `PlayerInteractEvent.RightClickItem`，仅处理 `LogicalSide.CLIENT` 侧，直接 `Minecraft.setScreen(new BookViewScreen(...))` 打开）；贴图使用 `textures/item/instruction_book_1.png`（来源"模型/其他物品/instruction_book_1.png"），模型 `models/item/instruction_book_1.json`（`item/generated` + layer0）。
- **内容注入**：`createBook()` 方法构造带完整成书 NBT 的 ItemStack——`title="说明书1"`、`author="海鸥的红石"`、`resolved=true`、`generation=0`、`pages`（每页为 `Component.Serializer.toJson(Component.literal(...))` 生成的 JSON 文本组件字符串，支持 `\n` 换行）。创造标签 `output.accept(ModItems.INSTRUCTION_BOOK_1.get().createBook())` 发放带内容的书本。
- **页面结构**（共 7 页）：封面 → 目录 → 控制面板类 → 圆盘记录仪 → 断路器 → 刷卡机 → 红石信号发射/接收器。
- **注册**：`ModItems.INSTRUCTION_BOOK_1`（`instruction_book_1`，堆叠 1）；lang 中英"说明书1 / Instruction Book 1"。
  - > **修复记录（右键无法打开）**：原版打开成书的链路为「服务端 `ServerPlayer.openItemGui` 发送 `ClientboundOpenBookPacket` → 客户端 `ClientPacketListener.handleOpenBook` 打开界面」，两处均用 `itemstack.is(Items.WRITTEN_BOOK)` **精确匹配原版成书物品**，自定义 WrittenBookItem 子类永远无法触发。已改为由客户端事件监听器 `InstructionBookClientHandler`（`Dist.CLIENT`）在右键物品时直接 `Minecraft.setScreen` 打开 `BookViewScreen`。
  - > **修复记录（服务端 ClassNotFoundException 崩溃）**：最初把 `Minecraft.getInstance().setScreen(new BookViewScreen(...))` 直接写在 `use()` 方法内，导致专用服务器（Mohist forgeserver）加载物品类时因引用客户端类 `net.minecraft.client.gui.screens.inventory.BookViewScreen` 而 `ClassNotFoundException` 崩溃（`ModItems` 注册阶段即失败）。已改为：`use()` 中**不再引用任何 `net.minecraft.client.*` 类**（只返回 `sidedSuccess`），打开界面的逻辑全部移入 `Dist.CLIENT` 的 `InstructionBookClientHandler` 事件监听，服务端不加载该客户端类。
  - > **修复记录（作者）**：成书 `author` 由 "CCRC" 改为 "海鸥的红石"。

### 21. 说明书2（instruction_book_2）

[InstructionBook2Item.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/InstructionBook2Item.java) 与说明书1同机制（继承 `WrittenBookItem`、覆写 `use()` 客户端打开 `BookViewScreen`），记录 **CC: Tweaked 配件外设的使用方法**：

- **页面结构**（共 9 页）：封面 → 目录 → 数码显示器（2 页）→ 数字调节器（2 页）→ 数字圆盘记录仪（3 页）。内容与三个外设类 [DigitalDisplayPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_display/DigitalDisplayPeripheral.java)、[DigitalKnobPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_knob/DigitalKnobPeripheral.java)、[DigitalPlotterPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_plotter/DigitalPlotterPeripheral.java) 中的 `@LuaFunction` 一一对应，精确记录每个函数名、参数类型与返回值：
  - **数码显示器** `digital_display`：`setStatus(text: string) -> string`（设置橙色状态文字）、`getStatus() -> string`（读取当前文字）。
  - **数字调节器** `digital_knob`：`setValue(value: int) -> int`（设置整数 0~1000，越界钳制，返回钳制后值）、`getValue() -> int`（读取当前值）。
  - **数字圆盘记录仪** `digital_plotter`：`push(value: int) -> int`（写入新值并移位，0~100 钳制）、`setValue(index: int, value: int) -> boolean`（设置第 index 位，索引越界返回 false）、`getValue(index: int) -> int | nil`（读取第 index 位，越界返回 nil）、`getList() -> table`（读取整表 50 元素）。索引采用 Lua 惯例 1~50。
- **内容注入**：`createBook()` 写入 `title="说明书2"`、`author="海鸥的红石"`、`resolved=true`、`generation=0`、`pages`。
- **注册**：`ModItems.INSTRUCTION_BOOK_2`（`instruction_book_2`，堆叠 1）；贴图 `textures/item/instruction_book_2.png`（来源"模型/其他物品/instruction_book_2.png"），模型 `models/item/instruction_book_2.json`；lang 中英"说明书2 / Instruction Book 2"；创造标签发放 `createBook()`。

### 22. 简易长矛（simple_spear）

[SimpleSpearItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/SimpleSpearItem.java) 为铁质剑类武器（继承 `SwordItem`，自带横扫攻击）：

- **Tier**：新增 `ModToolTiers.SIMPLE_SPEAR`——耐久 **130**、攻击伤害加成 **129**（构造修正 0，修改器显示 +129，实际总伤害 = 基础 1 + 129 = 130）、铁锭修复。
- **文字颜色类似附魔金苹果**：注册时 `Rarity.EPIC`（物品名淡紫色）+ 覆写 `isFoil()` 返回 true（物品带附魔微光光泽）。
- **+13 攻击范围**：覆写 `getAttributeModifiers`，主手追加 `ForgeMod.ENTITY_REACH`（`forge:attack_range` 别名）属性修改器 +13.0（ADDITION），攻击距离 = 默认 3.0 + 13.0 = 16.0 格（Forge 1.20.1 攻击判定读取该属性）。
- **悬停描述**：紫色文字"魔女们的秘密武器"（`desc_simple_spear`）。
- **手持模型（特殊设置）**：`models/item/simple_spear.json` 采用 `item/handheld` 姿态（第三人称工具与手臂同平面，rotation `[0,-90,55]`）+ 第一/第三人称 `scale` 2 倍（`1.7`/`1.36`），手持时模型显示二倍大；第三人称 `translation.y` 由 4.0 增至 7.2（沿贴图长度方向前移约 1/5 贴图总长度 ≈ 3.2 单位）。
- **注册**：`ModItems.SIMPLE_SPEAR`（`simple_spear`，堆叠 1）；贴图 `textures/item/simple_spear.png`（来源"模型/其他物品/simple_spear.png"），模型 `models/item/simple_spear.json`（`item/handheld`）；lang 中英"简易长矛 / Simple Spear"；创造标签加入主物品栏。

### 23. /ccrc 指令（CcrcCommand）

[CcrcCommand.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/command/CcrcCommand.java) 注册 `/ccrc` 根指令，用于查看/强制修改 CC: Tweaked 外设编号计数（配合《说明书》中"monitor_0"式命名机制的运维）：

- **`/ccrc get_count <设备种类名称>`**：查询该种类当前计数值（无权限要求）。
  - 优先读内存（反射 `ServerContext.idAssigner` 的私有 `ids` Map，已初始化时），否则读文件。
  - 未记录过该种类时显示 0，并提示"下一个新外设将从 0 开始"。
- **`/ccrc set_count <设备种类名称> <数字>`**：强制把该种类计数值改为指定值（需 OP 权限 2 级）。
  - **双写保证一致**：① 直接写 CC 计数文件 `<存档>/computercraft/ids.json`（`ServerContext.storageDir()` 定位，Gson 格式 `{"类型": 最大编号}`）；② 反射同步内存 `IDAssigner.ids`（否则 `getNextId` 会按内存旧值继续分配并覆盖文件）。
  - **存在性校验**：仅允许设置 `ids.json` 中已记录的类型（= 曾被有线调制解调器连接/分配过编号）；设置不存在或从未连接过的类型会拒绝执行并提示"可用 /ccrc list 查看已记录的类型"。
  - 提示"下一个该种类新外设将从 <值> 继续递增分配"。
- **自动补全（get_count/set_count）**：`设备种类名称` 参数注册 Brigadier `.suggests()` 建议提供器，候选 = `ids.json` 中已记录的全部类型（下拉菜单，类似原版 `/setblock` 列出方块）。
- **`/ccrc list`（OP 权限 2 级）**：**仅查看 CC 计数文件 `ids.json` 的内容**（每行 `类型 → 编号`），不做世界扫描，便于与补全候选对照。
- **实现要点**：`RegisterCommandsEvent`（FORGE 总线）注册；参数用 Brigadier `StringArgumentType.word()` + `IntegerArgumentType.integer(0)`；`sendSuccess(Supplier<Component>, boolean)` 形式反馈中文消息。

---

## 三、注册物品

> 约定：方块与物品 ID 一一对应；`BlockItem` 为普通方块物品，特殊物品使用专属类。以下按方块类归组。

### 1. 方块（Blocks）——共 99 个

| 方块 ID | 方块类 | 说明 |
| --- | --- | --- |
| `ccrc_block` | `Block` | 基础装饰方块（石质） |
| `console_lever_1` | `ConsoleLeverBlock` | 控制台拉杆（基础色），继承原版杠杆 |
| `console_lever_2` | `ConsoleLeverBlock` | 控制台拉杆（备用色） |
| `console_lever_6` | `ConsoleLever3StageBlock` | 3 挡位拉杆（0/8/15 输出） |
| `console_lever_7` | `ConsoleLever3StageBlock` | 3 挡位拉杆（同 6） |
| `point_lamp_1` | `PointLampBlock` | 指示灯 1（红石电平点亮） |
| `point_lamp_2` | `PointLampBlock` | 指示灯 2 |
| `point_lamp_3` | `PointLampBlock` | 指示灯 3 |
| `meter` | `MeterBlock` | 仪表（红石强度 0~15，16 种模型） |
| `console_panel` | `ConsolePanelBlock` | 空控制面板（仅文字显示） |
| `console_panel_large` | `ConsolePanelLargeBlock` | 大号空控制面板（16宽×14高×2厚，3倍字号居中文字，碰撞箱随朝向旋转） |
| `console_button_1` | `ConsoleButtonBlock` | 控制台按钮（按下输出 15，20tick 弹起） |
| `console_button_2` | `ConsoleButtonBlock` | 控制台按钮 2 |
| `console_button_3` | `ConsoleButtonBlock` | 控制台按钮 3 |
| `safe_button_1` | `SafeButtonBlock` | 安全按钮（三阶段授权） |
| `plotter` | `PlotterBlock` | 圆盘记录仪（24 点趋势划线） |
| `plotter_clock` | `PlotterClockBlock` | 圆盘记录仪时钟（上升沿触发划线，完整方块） |
| `breaker` | `BreakerBlock` | 断路器（完整方块，on 向后方/上方输出 15） |
| `card_reader_a` | `CardReaderBlock` | 刷卡机 A 级 |
| `card_reader_b` | `CardReaderBlock` | 刷卡机 B 级 |
| `card_reader_c` | `CardReaderBlock` | 刷卡机 C 级 |
| `card_reader_d` | `CardReaderBlock` | 刷卡机 D 级 |
| `card_reader_e` | `CardReaderBlock` | 刷卡机 E 级 |
| `digital_display` | `DigitalDisplayBlock` | 数码显示器（完整方块，双行文字 + CC 外设） |
| `digital_knob` | `DigitalKnobBlock` | 数字调节器（完整方块，四按钮 + CC 外设） |
| `digital_plotter` | `DigitalPlotterBlock` | 数字圆盘记录仪（完整方块，50点0~100线图 + CC 外设，无状态） |
| `nai_long_toy` | `NaiLongToyBlock` | 奶龙玩偶（半高装饰，水平四方向放置，底面8x8居中高14像素，右键播放声音 nai_long，放置/破坏音效同羊毛） |
| `potato_crate` | `Block` | 箱装土豆（搬运自农夫乐事，无方向完整方块，木板材质 `MapColor.WOOD` + `SoundType.WOOD`，强度 2.0，仅装饰展示） |
| `fridge` | `FridgeBlock` | 冰箱（搬运自 Cooking for Blockheads，水平四方向完整方块，27 格容器 + 原版箱子 GUI，金属音效强度 5.0/10） |
| `sink` | `SinkBlock` | 水槽（搬运自 Cooking for Blockheads，水平四方向完整装饰方块，无方块实体，石头音效强度 5.0/10） |
| `redstone_sender` | `RedstoneSenderBlock` | 红石信号发射器（六方向完整方块，连接红石线；收到外部信号时沿面向方向2~配置距离扫描接收器并同步强度，POWER>0 显示 on 贴图，金属音效） |
| `redstone_receiver` | `RedstoneReceiverBlock` | 红石信号接收器（无方向完整方块，连接红石线；isSignalSource 向相邻方块输出 POWER 强度信号，POWER>0 显示 on 贴图，金属音效） |
| `server_faas_1` | `ServerFaasBlock` | F.A.A.S服务器 1（水平四方向放置朝向玩家，贴近持续播放 server_noise，完整方块金属音效） |
| `server_faas_2` | `ServerFaasBlock` | F.A.A.S服务器 2（同上） |
| `server_faas_3` | `ServerFaasBlock` | F.A.A.S服务器 3（同上） |
| `white`/`orange`/`magenta`/`light_blue`/`yellow`/`lime`/`pink`/`gray`/`light_gray`/`cyan`/`purple`/`blue`/`brown`/`green`/`red`/`black`_canvas_sign | `StandingSignBlock` | 各色粗布告示牌（立式，原版告示牌机制，每种颜色独立 WoodType `cc_rc:canvas_<颜色>`，无碰撞箱，强度 1.0，贴图各色专属 `entity/signs/canvas_<颜色>.png`） |
| 同色 `_canvas_wall_sign` | `WallSignBlock` | 各色粗布告示牌（壁挂式，掉落对应立式块） |
| 同色 `_hanging_canvas_sign` | `CeilingHangingSignBlock` | 各色悬挂式粗布告示牌（天花板悬挂） |
| 同色 `_canvas_wall_hanging_sign` | `WallHangingSignBlock` | 各色悬挂式粗布告示牌（壁挂悬挂，掉落对应天花板悬挂块） |

> 说明：16 色 × 4 类 = 64 个告示牌方块（沿用原版 SignBlock 系列，方块实体复用原版 `BlockEntityType.SIGN` / `HANGING_SIGN`，无需新增方块实体）；`console_lever_1/2` 共用 `ConsoleLeverBlock` 类，`console_lever_6/7` 共用 `ConsoleLever3StageBlock` 类，`point_lamp_1/2/3` 共用 `PointLampBlock` 类，`console_button_1/2/3` 共用 `ConsoleButtonBlock` 类，`card_reader_a~e` 共用 `CardReaderBlock`（构造参数 grade 'A'~'E'），`server_faas_1/2/3` 共用 `ServerFaasBlock`（仅模型/贴图不同）。

### 2. 物品（Items）——共 111 个

**方块物品（67 个，`BlockItem` / `SignItem`）：**

| 物品 ID | 物品类 | 对应方块 |
| --- | --- | --- |
| `ccrc_block` | `BlockItem` | ccrc_block |
| `console_lever_1` / `console_lever_2` / `console_lever_6` / `console_lever_7` | `BlockItem` | 拉杆 1/2/6/7 |
| `point_lamp_1` / `point_lamp_2` / `point_lamp_3` | `BlockItem` | 指示灯 1/2/3 |
| `meter` | `BlockItem` | 仪表 |
| `empty_console_panel` | `BlockItem` | 空控制面板（对应方块 console_panel） |
| `empty_console_panel_large` | `BlockItem` | 大号空控制面板（对应方块 console_panel_large） |
| `console_button_1` / `console_button_2` / `console_button_3` | `BlockItem` | 按钮 1/2/3 |
| `safe_button_1` | `BlockItem` | 安全按钮 |
| `plotter` | `DescriptionBlockItem` | 圆盘记录仪（悬停显示模式说明） |
| `plotter_clock` | `DescriptionBlockItem` | 圆盘记录仪时钟（悬停显示触发说明） |
| `breaker` | `BlockItem` | 断路器 |
| `card_reader_a` ~ `card_reader_e` | `BlockItem` | 刷卡机 A~E |
| `digital_display` | `BlockItem` | 数码显示器 |
| `digital_knob` | `DescriptionBlockItem` | 数字调节器（悬停显示切换显示模式说明） |
| `digital_plotter` | `BlockItem` | 数字圆盘记录仪 |
| `nai_long_toy` | `BlockItem` | 奶龙玩偶 |
| `redstone_sender` | `BlockItem` | 红石信号发射器 |
| `redstone_receiver` | `BlockItem` | 红石信号接收器 |
| `server_faas_1` | `BlockItem` | F.A.A.S服务器 1 |
| `server_faas_2` | `BlockItem` | F.A.A.S服务器 2 |
| `server_faas_3` | `BlockItem` | F.A.A.S服务器 3 |
| `potato_crate` | `DescriptionBlockItem` | 箱装土豆（对应无方向木板材质方块；悬停两行描述：黄斜体"搬运自农夫乐事"＋淡蓝粗体"！？服务器 ？！"；仅入搬运物品栏） |
| `fridge` | `DescriptionBlockItem` | 冰箱（对应 27 格容器方块；黄斜体"搬运自 Cooking for Blockheads"；仅入搬运物品栏） |
| `sink` | `DescriptionBlockItem` | 水槽（对应纯装饰方块；黄斜体"搬运自 Cooking for Blockheads"；仅入搬运物品栏） |
| `white`/`orange`/`magenta`/`light_blue`/`yellow`/`lime`/`pink`/`gray`/`light_gray`/`cyan`/`purple`/`blue`/`brown`/`green`/`red`/`black`_canvas_sign | `DescriptionSignItem` | 各色粗布告示牌（立式/壁挂共用，黄斜体"搬运自农夫乐事"；仅入搬运物品栏） |
| 同色 `_hanging_canvas_sign` | `DescriptionHangingSignItem` | 各色悬挂式粗布告示牌（天花板/壁挂悬挂共用，黄斜体"搬运自农夫乐事"；仅入搬运物品栏） |

> 说明：16 色 × 2 = 32 个告示牌物品（复用 key `item.cc_rc.desc_potato_crate_carried` 显示黄色斜体"搬运自农夫乐事"）。

**特殊物品（20 个）：**

| 物品 ID | 物品类 | 说明 |
| --- | --- | --- |
| `multi_tool` | `MultiToolItem` | 多功能工具（不可堆叠，切换百分比模式 / 圆盘记录仪模式） |
| `instruction_book_1` | `InstructionBookItem` | 说明书1（原版成书 WrittenBookItem 机制，右键打开书籍阅读界面，固定内容：目录 + 控制面板类/圆盘记录仪/断路器/刷卡机/红石信号收发） |
| `instruction_book_2` | `InstructionBook2Item` | 说明书2（原版成书机制，记录 CC 配件外设使用法：数码显示器/数字调节器/数字圆盘记录仪，含每个 Lua 函数名与参数/返回类型） |
| `card_a` ~ `card_e` | `CardItem` | 门禁卡 A~E（不可堆叠，等级 'A'~'E'） |
| `frozen_tilapia` | `DescriptionSwordItem` | 冰冻罗非鱼（旧模组移植，修改器伤害 +255 实际总伤害 256 / 攻速 -2.0 / 耐久 2，蓝冰修复，不可堆叠，Rarity.RARE 蓝色名，金色描述"三体宇宙最强武器（bushi）"） |
| `green_wine` | `DescriptionItem` | 沉船绿酒（旧模组移植，饮用后剧毒 4 + 反胃 3，各 114514 刻，堆叠 16，绿色描述） |
| `green_wine_barrel` | `DescriptionItem` | 沉船绿酒桶（旧模组移植，普通物品，描述"*这是计划的一部分*"） |
| `he_yi_wei` | `DescriptionItem` | 何意味？（旧模组移植，营养 11 / 饱和度 4 / RARE，食用后 10 种效果，堆叠 16，紫色描述） |
| `tastes_food` | `Item` | 合一味（旧模组移植，营养 30 / 饱和度 0.8 / 可随时食用，堆叠 16） |
| `moon_cake` | `Item` | 五仁月饼（营养 8 / 饱和度 4，可随时食用，食用后 200 秒速度 II，堆叠 16） |
| `moon_cake_iron` | `AttackDescriptionItem` | 五金月饼（普通物品、无耐久，主手攻击伤害 10 / 堆叠 16，营养 4 / 饱和度 2，可随时食用，食用后 2 秒凋零 II + 200 秒力量 II 与抗性提升，描述"机加工这一块/."） |
| `ship_biscuit` | `DescriptionItem` | 压缩饼干（营养 12 / 饱和度 6，食用后 60 秒饱和效果 + 20 秒生命恢复，描述"*量大管饱*"） |
| `bao_zi` | `BaoZiItem` | 包子（食物 + 投掷炸弹：营养 4 / 饱和度 2，右键食用；左键投掷 bao_zi 弹射物命中爆炸，仅伤害实体不破坏方块；堆叠 16，红色斜体描述"包子雷？"） |
| `baguette` | `BaguetteItem` | 法棍（食物：营养 2 / 饱和度 2，堆叠 16；主手附加攻击伤害 4（修饰符 3.0 + 空手 1.0）与击退 ATTACK_KNOCKBACK=3.0，粗体棕色描述"坚如磐石"） |
| `crowbar` | `CrowbarItem` | 撬棍（铁质 SwordItem 自带横扫，修改器伤害 +19 实际总伤害 20 / 攻速慢 -3.0 / 暴击 2.0 倍 / 耐久 1024 / 铁锭修复 / 铁砧落地打击音效 / 不可堆叠；浅蓝粗体描述"物理学圣剑"+深蓝"f(x)dx"） |
| `simple_spear` | `SimpleSpearItem` | 简易长矛（铁质 SwordItem 自带横扫，耐久 130 / 修改器伤害 +129 实际总伤害 130 / +13 攻击范围 ForgeMod.ENTITY_REACH / 铁锭修复 / Rarity.EPIC 淡紫名 + isFoil 附魔微光 / 不可堆叠 / 手持模型 2 倍大且 y 前移 3.2；紫色描述"魔女们的秘密武器"） |

**音乐唱片（24 个，`RecordItem`，Rarity.RARE）：**

| 物品 ID | 比较器输出 | 音轨 |
| --- | --- | --- |
| `music_disc_level5` | 5 | level5（5260 tick） |
| `music_disc_railugun` | 1 | railugun（5140 tick） |
| `music_disc_never` | 15 | never（4320 tick） |
| `music_disc_assumptions` | 2 | assumptions（4280 tick） |
| `music_disc_conrnfield_chase` | 1 | conrnfield_chase（5860 tick） |
| `music_disc_move` | 2 | move（5560 tick） |
| `music_disc_more_one_night` | 5 | more_one_night（4400 tick） |
| `music_disc_rain` | 2 | rain（5300 tick） |
| `music_disc_end` | 2 | end（5080 tick） |
| `music_disc_underground_river` | 6 | underground_river（3900 tick） |
| `music_disc_hanezeve_caradhina` | 6 | hanezeve_caradhina（4020 tick） |
| `music_disc_cutie_mew_mew_magic` | 3 | cutie_mew_mew_magic（3680 tick） |
| `music_disc_denise` | 4 | denise（8340 tick） |
| `music_disc_gwangju` | 7 | gwangju（4160 tick） |
| `music_disc_higher` | 8 | higher（4260 tick） |
| `music_disc_king` | 9 | king（4520 tick） |
| `music_disc_marisa` | 10 | marisa（2560 tick） |
| `music_disc_mixue` | 11 | mixue（2060 tick） |
| `music_disc_raw_tell` | 12 | raw_tell（6340 tick） |
| `music_disc_reimu` | 13 | reimu（5700 tick） |
| `music_disc_you_will_be_perfect` | 14 | you_will_be_perfect（3188 tick） |
| `music_disc_bloom` | 5 | bloom（5930 tick） |
| `music_disc_jigoku_shoujo` | 6 | jigoku_shoujo（1891 tick） |
| `music_disc_the_imitation_game` | 1 | the_imitation_game（3160 tick） |

> 唱片同时注册进原版 `minecraft:tags/items/music_discs` 标签，可被唱片机播放；比较器输出值在 1~15 之间，其中 1/2/5/6 被多张唱片复用（1 = railugun、conrnfield_chase、the_imitation_game；2 = assumptions、move、rain、end；5 = level5、more_one_night、bloom；6 = underground_river、hanezeve_caradhina、jigoku_shoujo），其余 3/4/7~15 各一张。

### 3. 方块实体（Block Entity Types）——共 11 个

| 方块实体 ID | 实体类 | 支持的方块 |
| --- | --- | --- |
| `console_lever_be` | `ConsoleLeverBlockEntity` | console_lever_1、console_lever_2 |
| `console_lever_3stage_be` | `ConsoleLever3StageBlockEntity` | console_lever_6、console_lever_7 |
| `console_panel_be` | `ConsolePanelBlockEntity` | point_lamp_1/2/3、meter、console_panel、console_panel_large、console_button_1/2/3、safe_button_1 |
| `plotter_be` | `PlotterBlockEntity` | plotter |
| `plotter_clock_be` | `PlotterClockBlockEntity` | plotter_clock |
| `digital_display_be` | `DigitalDisplayBlockEntity` | digital_display |
| `digital_knob_be` | `DigitalKnobBlockEntity` | digital_knob |
| `digital_plotter_be` | `DigitalPlotterBlockEntity` | digital_plotter |
| `canvas_sign_be` | `CanvasSignBlockEntity` | 16 色 × `_canvas_sign`、`_canvas_wall_sign`（立式/壁挂粗布告示牌） |
| `canvas_hanging_sign_be` | `CanvasHangingSignBlockEntity` | 16 色 × `_hanging_canvas_sign`、`_canvas_wall_hanging_sign`（悬挂式粗布告示牌） |
| `fridge_be` | `FridgeBlockEntity` | fridge（冰箱，27 格容器） |

> 注意：`breaker`（断路器）为纯逻辑方块，**没有**方块实体，状态完全由 `BlockState`（FACING/POWERED）驱动。
>
> **告示牌为何需要自定义方块实体类型**：原版 `BlockEntityType.SIGN` / `HANGING_SIGN` 的 `validBlocks` 只包含原版告示牌方块。`BlockEntityRenderDispatcher` 渲染时会先做 `blockEntity.getType().isValid(blockState)` 校验，若方块不在该方块实体类型的有效方块集合内则直接跳过渲染，导致告示牌完全透明（无模型、无文字、亦非紫黑块）。因此为粗布告示牌注册了专用方块实体类型（工厂复用 `CanvasSignBlockEntity`/`CanvasHangingSignBlockEntity`，`getType()` 返回自定义类型），并在客户端为这两个类型注册原版 `SignRenderer`/`HangingSignRenderer`。模型层与材质仍由原版机制按 `WoodType "cc_rc:canvas"` 自动生成。

### 4. 声音（Sound Events）——共 26 个

24 个音乐声音与 24 张唱片一一对应：`music_level5`、`railugun`、`never`、`assumptions`、`conrnfield_chase`、`move`、`night`（more_one_night）、`rain`、`end`、`underground_river`、`hanezeve_caradhina`、`cutie_mew_mew_magic`、`denise`、`gwangju`、`higher`、`king`、`marisa`、`mixue`、`raw_tell`、`reimu`、`you_will_be_perfect`、`bloom`、`jigoku_shoujo`、`the_imitation_game`；另有 2 个非唱片声音：`nai_long`（奶龙玩偶语音，右键奶龙玩偶 `nai_long_toy` 时播放）、`server_noise`（F.A.A.S服务器 `server_faas_1/2/3` 的环境音效，玩家靠近时持续播放，注册于 [sounds.json](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/sounds.json)）。

### 5. 创造标签

方块与物品通过 [ModCreativeTabs.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModCreativeTabs.java) 加入标签 `cc_rc_tab`（"CC：反应堆控制台"）；另注册第二个标签 `cc_rc_tab_carried`（"CCRC：搬运的物品"），Forge 按注册名对模组创造标签排序，此前缀更长的注册名位于主标签之后，图标为箱装土豆，仅收录搬运类物品（当前为箱装土豆、冰箱、水槽与 16 色粗布告示牌/悬挂式粗布告示牌，均不加入主标签）。

### 6. 弹射物实体（Projectile Entities）——共 1 个

| 实体 ID | 实体类 | 说明 |
| --- | --- | --- |
| `bao_zi` | `BaoZi` | 包子（继承原版雪球类 `Snowball`，飞行逻辑与雪球一致；命中实体或方块时触发半径 4 的爆炸，仅伤害实体、不破坏方块，粒子/音效为原版爆炸；客户端复用原版 `ThrownItemRenderer` 渲染） |

> 实体类型注册于 [ModEntities.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModEntities.java)（`DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ...)`），在 [CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java) 构造函数中 `ModEntities.ENTITY_TYPES.register(modEventBus)`。左键投掷由客户端 `InputEvent.InteractionKeyMappingTriggered` 事件 + C2S 数据包实现（见「二、15 包子」）。
