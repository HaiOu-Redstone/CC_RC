package com.cc_rc;

import com.cc_rc.block.Console_lever.ConsoleLever3StageBlockEntity;
import com.cc_rc.block.Console_lever.ConsoleLeverBlockEntity;
import com.cc_rc.block.Plotter.PlotterBlockEntity;
import com.cc_rc.block.Plotter.PlotterClockBlockEntity;
import com.cc_rc.block.canvas_sign.CanvasHangingSignBlockEntity;
import com.cc_rc.block.canvas_sign.CanvasSignBlockEntity;
import com.cc_rc.block.console_panel.ConsolePanelBlockEntity;
import com.cc_rc.block.digital_display.DigitalDisplayBlockEntity;
import com.cc_rc.block.digital_knob.DigitalKnobBlockEntity;
import com.cc_rc.block.digital_plotter.DigitalPlotterBlockEntity;
import com.cc_rc.block.fridge.FridgeBlockEntity;
import com.cc_rc.block.key_distributor.KeyDistributorBlockEntity;
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
}
