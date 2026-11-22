package com.cc_rc;

import com.cc_rc.block.Console_lever.ConsoleLever3StageBlockEntity;
import com.cc_rc.block.Console_lever.ConsoleLeverBlockEntity;
import com.cc_rc.block.Plotter.PlotterBlockEntity;
import com.cc_rc.block.Plotter.PlotterClockBlockEntity;
import com.cc_rc.block.block_detector.BlockDetectorBlockEntity;
import com.cc_rc.block.canvas_sign.CanvasHangingSignBlockEntity;
import com.cc_rc.block.canvas_sign.CanvasSignBlockEntity;
import com.cc_rc.block.console_panel.ConsolePanelBlockEntity;
import com.cc_rc.block.data_unit.DataUnitBlockEntity;
import com.cc_rc.block.digital_display.DigitalDisplayBlockEntity;
import com.cc_rc.block.digital_knob.DigitalKnobBlockEntity;
import com.cc_rc.block.digital_plotter.DigitalPlotterBlockEntity;
import com.cc_rc.block.fridge.FridgeBlockEntity;
import com.cc_rc.block.key_distributor.KeyDistributorBlockEntity;
import com.cc_rc.block.console.ConsoleBlockEntity;
import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.block.poster.PosterBlockEntity;
import com.cc_rc.block.graffiti.GraffitiBlockEntity;
import com.cc_rc.block.graffiti.GraffitiShapeBlockEntity;
import com.cc_rc.block.audio_player.AudioPlayerBlockEntity;
import com.cc_rc.block.extended_relay.ExtendedRelayBlockEntity;
import com.cc_rc.block.extended_relay.ExtendedRelayBusBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CcRc.MODID);

    // 控制台拉杆方块实体类型（同时支持 console_lever_1 和 console_lever_2）
    public static final RegistryObject<BlockEntityType<ConsoleLeverBlockEntity>> CONSOLE_LEVER_BE =
            BLOCK_ENTITY_TYPES.register("console_lever_be",
                    () -> BlockEntityType.Builder.of(
                            ConsoleLeverBlockEntity::new,
                            ModBlocks.CONSOLE_LEVER_1.get(),
                            ModBlocks.CONSOLE_LEVER_2.get()
                    ).build(null));

    // 3挡位控制台拉杆方块实体类型（同时支持 console_lever_6 和 console_lever_7）
    public static final RegistryObject<BlockEntityType<ConsoleLever3StageBlockEntity>> CONSOLE_LEVER_3STAGE_BE =
            BLOCK_ENTITY_TYPES.register("console_lever_3stage_be",
                    () -> BlockEntityType.Builder.of(
                            ConsoleLever3StageBlockEntity::new,
                            ModBlocks.CONSOLE_LEVER_6.get(),
                            ModBlocks.CONSOLE_LEVER_7.get()
                    ).build(null));

    // 控制面板方块实体类型（支持 point_lamp_1, 2, 3, meter, console_panel, console_panel_large, console_button_1~5, safe_button_1, password_inputer）
    public static final RegistryObject<BlockEntityType<ConsolePanelBlockEntity>> CONSOLE_PANEL_BE =
            BLOCK_ENTITY_TYPES.register("console_panel_be",
                    () -> BlockEntityType.Builder.of(
                            ConsolePanelBlockEntity::new,
                            ModBlocks.POINT_LAMP_1.get(),
                            ModBlocks.POINT_LAMP_2.get(),
                            ModBlocks.POINT_LAMP_3.get(),
                            ModBlocks.METER.get(),
                            ModBlocks.CONSOLE_PANEL.get(),
                            ModBlocks.CONSOLE_PANEL_LARGE.get(),
                            ModBlocks.CONSOLE_BUTTON_1.get(),
                            ModBlocks.CONSOLE_BUTTON_2.get(),
                            ModBlocks.CONSOLE_BUTTON_3.get(),
                            ModBlocks.CONSOLE_BUTTON_4.get(),
                            ModBlocks.CONSOLE_BUTTON_5.get(),
                            ModBlocks.SAFE_BUTTON_1.get(),
                            ModBlocks.PASSWORD_INPUTER.get()
                    ).build(null));

    // 圆盘记录仪方块实体类型
    public static final RegistryObject<BlockEntityType<PlotterBlockEntity>> PLOTTER_BE =
            BLOCK_ENTITY_TYPES.register("plotter_be",
                    () -> BlockEntityType.Builder.of(
                            PlotterBlockEntity::new,
                            ModBlocks.PLOTTER.get()
                    ).build(null));

    // 圆盘记录仪时钟方块实体类型
    public static final RegistryObject<BlockEntityType<PlotterClockBlockEntity>> PLOTTER_CLOCK_BE =
            BLOCK_ENTITY_TYPES.register("plotter_clock_be",
                    () -> BlockEntityType.Builder.of(
                            PlotterClockBlockEntity::new,
                            ModBlocks.PLOTTER_CLOCK.get()
                    ).build(null));

    // 数码显示器方块实体类型
    public static final RegistryObject<BlockEntityType<DigitalDisplayBlockEntity>> DIGITAL_DISPLAY_BE =
            BLOCK_ENTITY_TYPES.register("digital_display_be",
                    () -> BlockEntityType.Builder.of(
                            DigitalDisplayBlockEntity::new,
                            ModBlocks.DIGITAL_DISPLAY.get()
                    ).build(null));

    // 数字调节器方块实体类型
    public static final RegistryObject<BlockEntityType<DigitalKnobBlockEntity>> DIGITAL_KNOB_BE =
            BLOCK_ENTITY_TYPES.register("digital_knob_be",
                    () -> BlockEntityType.Builder.of(
                            DigitalKnobBlockEntity::new,
                            ModBlocks.DIGITAL_KNOB.get()
                    ).build(null));

    // 数字圆盘记录仪方块实体类型
    public static final RegistryObject<BlockEntityType<DigitalPlotterBlockEntity>> DIGITAL_PLOTTER_BE =
            BLOCK_ENTITY_TYPES.register("digital_plotter_be",
                    () -> BlockEntityType.Builder.of(
                            DigitalPlotterBlockEntity::new,
                            ModBlocks.DIGITAL_PLOTTER.get()
                    ).build(null));

    // 粗布告示牌方块实体类型（16 色 × 立式/壁挂 = 32 个方块）。
    // 必须自定义类型：原版 BlockEntityType.SIGN 的 validBlocks 不含本模组方块，
    // 渲染调度器会因 BlockEntityType.isValid 校验失败而跳过渲染（告示牌透明）。
    public static final RegistryObject<BlockEntityType<CanvasSignBlockEntity>> CANVAS_SIGN_BE =
            BLOCK_ENTITY_TYPES.register("canvas_sign_be",
                    () -> {
                        List<Block> blocks = new ArrayList<>();
                        ModBlocks.CANVAS_SIGN_BLOCKS.forEach(r -> blocks.add(r.get()));
                        ModBlocks.CANVAS_WALL_SIGN_BLOCKS.forEach(r -> blocks.add(r.get()));
                        return BlockEntityType.Builder.of(CanvasSignBlockEntity::new,
                                blocks.toArray(new Block[0])).build(null);
                    });

    // 悬挂式粗布告示牌方块实体类型（16 色 × 天花板悬挂/壁挂悬挂 = 32 个方块）
    public static final RegistryObject<BlockEntityType<CanvasHangingSignBlockEntity>> CANVAS_HANGING_SIGN_BE =
            BLOCK_ENTITY_TYPES.register("canvas_hanging_sign_be",
                    () -> {
                        List<Block> blocks = new ArrayList<>();
                        ModBlocks.HANGING_CANVAS_SIGN_BLOCKS.forEach(r -> blocks.add(r.get()));
                        ModBlocks.WALL_HANGING_CANVAS_SIGN_BLOCKS.forEach(r -> blocks.add(r.get()));
                        return BlockEntityType.Builder.of(CanvasHangingSignBlockEntity::new,
                                blocks.toArray(new Block[0])).build(null);
                    });

    // 冰箱方块实体类型（27 格容器，使用原版箱子 GUI，RandomizableContainerBlockEntity 提供物品存取/掉落物表支持）
    public static final RegistryObject<BlockEntityType<FridgeBlockEntity>> FRIDGE_BE =
            BLOCK_ENTITY_TYPES.register("fridge_be",
                    () -> BlockEntityType.Builder.of(
                            FridgeBlockEntity::new,
                            ModBlocks.FRIDGE.get()
                    ).build(null));

    // 钥匙分发控制器方块实体类型（存储已录入的钥匙柜记录列表）
    public static final RegistryObject<BlockEntityType<KeyDistributorBlockEntity>> KEY_DISTRIBUTOR_BE =
            BLOCK_ENTITY_TYPES.register("key_distributor_be",
                    () -> BlockEntityType.Builder.of(
                            KeyDistributorBlockEntity::new,
                            ModBlocks.KEY_DISTRIBUTOR.get()
                    ).build(null));

    // 扩展红石继电器方块实体类型（总线远端红石端口：6 向输入/输出）
    public static final RegistryObject<BlockEntityType<ExtendedRelayBlockEntity>> EXTENDED_RELAY_BE =
            BLOCK_ENTITY_TYPES.register("extended_relay_be",
                    () -> BlockEntityType.Builder.of(
                            ExtendedRelayBlockEntity::new,
                            ModBlocks.EXTENDED_RELAY.get()
                    ).build(null));

    // 扩展红石继电器总线方块实体类型（CC 外设宿主：沿朝向搜索继电器）
    public static final RegistryObject<BlockEntityType<ExtendedRelayBusBlockEntity>> RELAY_BUS_BE =
            BLOCK_ENTITY_TYPES.register("relay_bus_be",
                    () -> BlockEntityType.Builder.of(
                            ExtendedRelayBusBlockEntity::new,
                            ModBlocks.RELAY_BUS.get()
                    ).build(null));

    // 数据单元方块实体类型（存名称 + 数据列表，CC 外设宿主）
    public static final RegistryObject<BlockEntityType<DataUnitBlockEntity>> DATA_UNIT_BE =
            BLOCK_ENTITY_TYPES.register("data_unit_be",
                    () -> BlockEntityType.Builder.of(
                            DataUnitBlockEntity::new,
                            ModBlocks.DATA_UNIT.get()
                    ).build(null));

    // 海报方块实体类型（单方块 poster：存图片索引/缩放/偏移，客户端 PosterBER 按其绘制贴图）
    public static final RegistryObject<BlockEntityType<PosterBlockEntity>> POSTER_BE =
            BLOCK_ENTITY_TYPES.register("poster_be",
                    () -> BlockEntityType.Builder.of(
                            PosterBlockEntity::new,
                            ModBlocks.POSTER.get()
                    ).build(null));

    // 涂鸦文字方块实体类型（单方块 graffiti：存文字/颜色/大小/偏移/字体格式，客户端 GraffitiBER 绘制）
    public static final RegistryObject<BlockEntityType<GraffitiBlockEntity>> GRAFFITI_BE =
            BLOCK_ENTITY_TYPES.register("graffiti_be",
                    () -> BlockEntityType.Builder.of(
                            GraffitiBlockEntity::new,
                            ModBlocks.GRAFFITI.get()
                    ).build(null));

    // 涂鸦形状方块实体类型（单方块 graffiti_shape：存类型/预设/颜色/大小/线宽/旋转/偏移，客户端 BER 绘制）
    public static final RegistryObject<BlockEntityType<GraffitiShapeBlockEntity>> GRAFFITI_SHAPE_BE =
            BLOCK_ENTITY_TYPES.register("graffiti_shape_be",
                    () -> BlockEntityType.Builder.of(
                            GraffitiShapeBlockEntity::new,
                            ModBlocks.GRAFFITI_SHAPE.get()
                    ).build(null));

    // 方块探测器方块实体类型（空实体，仅作 CC 外设承载）
    public static final RegistryObject<BlockEntityType<BlockDetectorBlockEntity>> BLOCK_DETECTOR_BE =
            BLOCK_ENTITY_TYPES.register("block_detector_be",
                    () -> BlockEntityType.Builder.of(
                            BlockDetectorBlockEntity::new,
                            ModBlocks.BLOCK_DETECTOR.get()
                    ).build(null));

    // 控制柜结构方块实体（只存所属总线 + 结构状态，供 BER 画网格、拆除时解除成型）
    public static final RegistryObject<BlockEntityType<ConsoleBlockEntity>> CONSOLE_BLOCK_BE =
            BLOCK_ENTITY_TYPES.register("controller_block_be",
                    () -> BlockEntityType.Builder.of(
                            ConsoleBlockEntity::new,
                            ModBlocks.CONSOLE_BLOCK.get(),
                            ModBlocks.CONSOLE_BLOCK_BLUE.get(),
                            ModBlocks.CONSOLE_BLOCK_BLACK.get()
                    ).build(null));

    // 控制柜总线方块实体（结构三态 + 最小角/尺寸/格数，低频巡检结构完整性）
    public static final RegistryObject<BlockEntityType<ConsoleBusBlockEntity>> CONSOLE_BUS_BE =
            BLOCK_ENTITY_TYPES.register("controller_bus_be",
                    () -> BlockEntityType.Builder.of(
                            ConsoleBusBlockEntity::new,
                            ModBlocks.CONSOLE_BUS.get()
                    ).build(null));

    // 斜面控制台方块实体（只存「所属总线 + 宿主方块/面」，做视觉与交互映射）
    public static final RegistryObject<BlockEntityType<com.cc_rc.block.console.ConsoleSlopeBlockEntity>>
            CONSOLE_SLOPE_BE =
            BLOCK_ENTITY_TYPES.register("controller_slope_be",
                    () -> BlockEntityType.Builder.of(
                            com.cc_rc.block.console.ConsoleSlopeBlockEntity::new,
                            ModBlocks.CONTROLLER_SLOPE.get(),
                            // 蓝色 / 黑色变体共用同一方块实体类型
                            ModBlocks.CONTROLLER_SLOPE_BLUE.get(),
                            ModBlocks.CONTROLLER_SLOPE_BLACK.get()
                    ).build(null));

    // 音频播放器方块实体（存音效 id / 模式 / 范围 / 衰减 / 起始 / 截至 / 间隔）
    public static final RegistryObject<BlockEntityType<AudioPlayerBlockEntity>> AUDIO_PLAYER_BE =
            BLOCK_ENTITY_TYPES.register("audio_player_be",
                    () -> BlockEntityType.Builder.of(
                            AudioPlayerBlockEntity::new,
                            ModBlocks.AUDIO_PLAYER.get()
                    ).build(null));
}
