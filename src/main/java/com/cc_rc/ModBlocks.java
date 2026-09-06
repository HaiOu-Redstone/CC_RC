package com.cc_rc;

import com.cc_rc.block.Breaker.BreakerBlock;
import com.cc_rc.block.Console_button.ConsoleButtonBlock;
import com.cc_rc.block.Console_lever.ConsoleLever3StageBlock;
import com.cc_rc.block.Console_lever.ConsoleLeverBlock;
import com.cc_rc.block.Meter.MeterBlock;
import com.cc_rc.block.Point_lamp.PointLampBlock;
import com.cc_rc.block.Safe_button.SafeButtonBlock;
import com.cc_rc.block.Plotter.PlotterBlock;
import com.cc_rc.block.Plotter.PlotterClockBlock;
import com.cc_rc.block.card_reader.CardReaderBlock;
import com.cc_rc.block.digital_display.DigitalDisplayBlock;
import com.cc_rc.block.digital_knob.DigitalKnobBlock;
import com.cc_rc.block.digital_plotter.DigitalPlotterBlock;
import com.cc_rc.block.console_panel.ConsolePanelBlock;
import com.cc_rc.block.console_panel.ConsolePanelLargeBlock;
import com.cc_rc.block.canvas_sign.CanvasCeilingHangingSignBlock;
import com.cc_rc.block.canvas_sign.CanvasStandingSignBlock;
import com.cc_rc.block.canvas_sign.CanvasWallHangingSignBlock;
import com.cc_rc.block.canvas_sign.CanvasWallSignBlock;
import com.cc_rc.block.fridge.FridgeBlock;
import com.cc_rc.block.nai_long_toy.NaiLongToyBlock;
import com.cc_rc.block.Redstone_transmit.RedstoneReceiverBlock;
import com.cc_rc.block.Redstone_transmit.RedstoneSenderBlock;
import com.cc_rc.block.server_faas.ServerFaasBlock;
import com.cc_rc.block.sink.SinkBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CcRc.MODID);

    public static final RegistryObject<Block> CCRC_BLOCK = BLOCKS.register("ccrc_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F)
                    .requiresCorrectToolForDrops()));

    // 控制台拉杆 1 - 基础颜色
    public static final RegistryObject<ConsoleLeverBlock> CONSOLE_LEVER_1 = BLOCKS.register("console_lever_1",
            () -> new ConsoleLeverBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)));

    // 控制台拉杆 2 - 备用颜色（功能相同，使用相同方块类）
    public static final RegistryObject<ConsoleLeverBlock> CONSOLE_LEVER_2 = BLOCKS.register("console_lever_2",
            () -> new ConsoleLeverBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)));

    // 控制台拉杆 6 - 3挡位
    public static final RegistryObject<ConsoleLever3StageBlock> CONSOLE_LEVER_6 = BLOCKS.register("console_lever_6",
            () -> new ConsoleLever3StageBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)));

    // 控制台拉杆 7 - 3挡位（功能相同，使用相同方块类）
    public static final RegistryObject<ConsoleLever3StageBlock> CONSOLE_LEVER_7 = BLOCKS.register("console_lever_7",
            () -> new ConsoleLever3StageBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)));

    // 指示灯 1
    public static final RegistryObject<PointLampBlock> POINT_LAMP_1 = BLOCKS.register("point_lamp_1",
            () -> new PointLampBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)));

    // 指示灯 2
    public static final RegistryObject<PointLampBlock> POINT_LAMP_2 = BLOCKS.register("point_lamp_2",
            () -> new PointLampBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)));

    // 指示灯 3
    public static final RegistryObject<PointLampBlock> POINT_LAMP_3 = BLOCKS.register("point_lamp_3",
            () -> new PointLampBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)));

    // 仪表 Meter
    public static final RegistryObject<MeterBlock> METER = BLOCKS.register("meter",
            () -> new MeterBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)));

    // 控制面板（空面板，仅放置和文字显示）
    public static final RegistryObject<ConsolePanelBlock> CONSOLE_PANEL = BLOCKS.register("console_panel",
            () -> new ConsolePanelBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)));

    // 大号空控制面板（比空控制面板更宽，文字 3 倍大且 x/y 居中）
    public static final RegistryObject<ConsolePanelLargeBlock> CONSOLE_PANEL_LARGE = BLOCKS.register("console_panel_large",
            () -> new ConsolePanelLargeBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)));

    // 控制台按钮 1
    public static final RegistryObject<ConsoleButtonBlock> CONSOLE_BUTTON_1 = BLOCKS.register("console_button_1",
            () -> new ConsoleButtonBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.STONE)));

    // 控制台按钮 2
    public static final RegistryObject<ConsoleButtonBlock> CONSOLE_BUTTON_2 = BLOCKS.register("console_button_2",
            () -> new ConsoleButtonBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.STONE)));

    // 控制台按钮 3
    public static final RegistryObject<ConsoleButtonBlock> CONSOLE_BUTTON_3 = BLOCKS.register("console_button_3",
            () -> new ConsoleButtonBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.STONE)));

    // 安全按钮 1
    public static final RegistryObject<SafeButtonBlock> SAFE_BUTTON_1 = BLOCKS.register("safe_button_1",
            () -> new SafeButtonBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.STONE)));

    // 圆盘记录仪
    public static final RegistryObject<PlotterBlock> PLOTTER = BLOCKS.register("plotter",
            () -> new PlotterBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)));

    // 圆盘记录仪时钟（完整方块，六面同贴图，检测脉冲上升沿触发上方模式1圆盘记录仪划线）
    public static final RegistryObject<PlotterClockBlock> PLOTTER_CLOCK = BLOCKS.register("plotter_clock",
            () -> new PlotterClockBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    // 断路器（完整方块，可水平四方向放置，右键切换 on/off，on 向后方和上方输出 15 信号，底面收到信号自动跳闸）
    public static final RegistryObject<BreakerBlock> BREAKER = BLOCKS.register("breaker",
            () -> new BreakerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F)
                    .sound(SoundType.METAL)));

    // 刷卡机 A 级（只能贴墙，碰撞箱14x14x2，使用正确卡激活 on，向后方输出15信号，定时自动off）
    public static final RegistryObject<CardReaderBlock> CARD_READER_A = BLOCKS.register("card_reader_a",
            () -> new CardReaderBlock('A', BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.METAL)));

    // 刷卡机 B 级
    public static final RegistryObject<CardReaderBlock> CARD_READER_B = BLOCKS.register("card_reader_b",
            () -> new CardReaderBlock('B', BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.METAL)));

    // 刷卡机 C 级
    public static final RegistryObject<CardReaderBlock> CARD_READER_C = BLOCKS.register("card_reader_c",
            () -> new CardReaderBlock('C', BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.METAL)));

    // 刷卡机 D 级
    public static final RegistryObject<CardReaderBlock> CARD_READER_D = BLOCKS.register("card_reader_d",
            () -> new CardReaderBlock('D', BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.METAL)));

    // 刷卡机 E 级
    public static final RegistryObject<CardReaderBlock> CARD_READER_E = BLOCKS.register("card_reader_e",
            () -> new CardReaderBlock('E', BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.METAL)));

    // 数码显示器（完整方块，水平四方向放置，显示两行文字：命名白字 + 固定橙字）
    public static final RegistryObject<DigitalDisplayBlock> DIGITAL_DISPLAY = BLOCKS.register("digital_display",
            () -> new DigitalDisplayBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    // 数字调节器（完整方块，水平四方向放置，正面四按钮调节0~1000整数，支持CC外设读写）
    public static final RegistryObject<DigitalKnobBlock> DIGITAL_KNOB = BLOCKS.register("digital_knob",
            () -> new DigitalKnobBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    // 数字圆盘记录仪（完整方块，水平四方向放置，复用圆盘记录仪模型，显示命名文字+50点0~100线图，支持CC外设读写）
    public static final RegistryObject<DigitalPlotterBlock> DIGITAL_PLOTTER = BLOCKS.register("digital_plotter",
            () -> new DigitalPlotterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    // 奶龙玩偶（半高装饰方块，可水平四方向放置，碰撞箱底面8x8像素居中高14像素，右键播放声音 nai_long，放置/破坏音效同原版羊毛）
    public static final RegistryObject<NaiLongToyBlock> NAI_LONG_TOY = BLOCKS.register("nai_long_toy",
            () -> new NaiLongToyBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.8F)
                    .sound(SoundType.WOOL)));

    // ==================== 红石信号传输 ====================

    // 红石信号发射器（完整方块，可朝 x±/y±/z± 六方向放置，连接红石线；收到外部信号时沿面向方向
    // 2~配置距离遍历寻找接收器并同步信号强度，POWER>0 显示 on 贴图）
    public static final RegistryObject<RedstoneSenderBlock> REDSTONE_SENDER = BLOCKS.register("redstone_sender",
            () -> new RedstoneSenderBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F)
                    .sound(SoundType.METAL)));

    // 红石信号接收器（无方向完整方块，连接红石线；向相邻方块输出当前信号强度，POWER>0 显示 on 贴图）
    public static final RegistryObject<RedstoneReceiverBlock> REDSTONE_RECEIVER = BLOCKS.register("redstone_receiver",
            () -> new RedstoneReceiverBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F)
                    .sound(SoundType.METAL)));

    // ==================== 搬运方块（原农夫乐事等外部模组） ====================

    // 箱装土豆方块（搬运自农夫乐事）：无方向完整方块，木板材质（木质音效、木质地图颜色），仅作装饰展示用。
    public static final RegistryObject<Block> POTATO_CRATE = BLOCKS.register("potato_crate",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)));

    // 冰箱（搬运自 Cooking for Blockheads）：水平四方向放置的完整方块（金属音效），
    // 27 格容器（FridgeBlockEntity），右键打开原版箱子样式 GUI，破坏时掉落容器内容。
    public static final RegistryObject<FridgeBlock> FRIDGE = BLOCKS.register("fridge",
            () -> new FridgeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.METAL)));

    // 水槽（搬运自 Cooking for Blockheads）：水平四方向放置的完整装饰方块（石头音效），
    // 无方块实体/容器/流体逻辑，模型为完整静态模型。
    public static final RegistryObject<SinkBlock> SINK = BLOCKS.register("sink",
            () -> new SinkBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.STONE)));

    // ==================== F.A.A.S 服务器 ====================

    // F.A.A.S 服务器 1/2/3（三种模型变体，共用同一方块类 ServerFaasBlock）：
    // 水平四方向放置（朝向玩家），靠近时持续播放 server_noise 环境音效（仿原版营火/火把）。
    public static final RegistryObject<ServerFaasBlock> SERVER_FAAS_1 = BLOCKS.register("server_faas_1",
            () -> new ServerFaasBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    public static final RegistryObject<ServerFaasBlock> SERVER_FAAS_2 = BLOCKS.register("server_faas_2",
            () -> new ServerFaasBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    public static final RegistryObject<ServerFaasBlock> SERVER_FAAS_3 = BLOCKS.register("server_faas_3",
            () -> new ServerFaasBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    // ==================== 搬运告示牌（原农夫乐事 canvas_sign，使用原版告示牌机制） ====================

    // 16 种染料色（顺序同原版染料物品），每种颜色注册 4 个方块：
    // 立式 / 壁挂式（普通告示牌，StandingSignBlock/WallSignBlock）、
    // 天花板悬挂式 / 壁挂悬挂式（悬挂告示牌，CeilingHangingSignBlock/WallHangingSignBlock）。
    // 每种颜色使用各自的自定义 WoodType "cc_rc:canvas_<颜色>"（ModWoodTypes.CANVAS_TYPES[i]），
    // 使原版 Sheets 按 WoodType 解析到各色专属贴图（entity/signs/canvas_<颜色>.png 等）。
    // 原版 LayerDefinitions 会为已注册的 WoodType 自动生成告示牌模型层，无需额外注册。
    public static final String[] CANVAS_SIGN_COLORS = ModWoodTypes.CANVAS_SIGN_COLORS;

    public static final List<RegistryObject<Block>> CANVAS_SIGN_BLOCKS = new ArrayList<>();
    public static final List<RegistryObject<Block>> CANVAS_WALL_SIGN_BLOCKS = new ArrayList<>();
    public static final List<RegistryObject<Block>> HANGING_CANVAS_SIGN_BLOCKS = new ArrayList<>();
    public static final List<RegistryObject<Block>> WALL_HANGING_CANVAS_SIGN_BLOCKS = new ArrayList<>();

    static {
        for (int i = 0; i < CANVAS_SIGN_COLORS.length; i++) {
            String color = CANVAS_SIGN_COLORS[i];
            net.minecraft.world.level.block.state.properties.WoodType woodType = ModWoodTypes.CANVAS_TYPES[i];
            RegistryObject<Block> sign = BLOCKS.register(color + "_canvas_sign",
                    () -> new CanvasStandingSignBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .noCollission()
                            .strength(1.0F), woodType));
            CANVAS_SIGN_BLOCKS.add(sign);

            CANVAS_WALL_SIGN_BLOCKS.add(BLOCKS.register(color + "_canvas_wall_sign",
                    () -> new CanvasWallSignBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .noCollission()
                            .strength(1.0F)
                            .lootFrom(sign), woodType)));

            RegistryObject<Block> hanging = BLOCKS.register(color + "_hanging_canvas_sign",
                    () -> new CanvasCeilingHangingSignBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .noCollission()
                            .strength(1.0F), woodType));
            HANGING_CANVAS_SIGN_BLOCKS.add(hanging);

            WALL_HANGING_CANVAS_SIGN_BLOCKS.add(BLOCKS.register(color + "_canvas_wall_hanging_sign",
                    () -> new CanvasWallHangingSignBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .noCollission()
                            .strength(1.0F)
                            .lootFrom(hanging), woodType)));
        }
    }
}