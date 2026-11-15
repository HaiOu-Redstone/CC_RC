package com.cc_rc;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CcRc.MODID);

    public static final RegistryObject<CreativeModeTab> CC_RC_TAB = CREATIVE_TABS.register("cc_rc_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cc_rc"))
                    .icon(() -> new ItemStack(ModItems.CCRC_BLOCK_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.CONSOLE_LEVER_1_ITEM.get());
                        output.accept(ModItems.CONSOLE_LEVER_2_ITEM.get());
                        output.accept(ModItems.CONSOLE_LEVER_6_ITEM.get());
                        output.accept(ModItems.CONSOLE_LEVER_7_ITEM.get());
                        output.accept(ModItems.POINT_LAMP_1_ITEM.get());
                        output.accept(ModItems.POINT_LAMP_2_ITEM.get());
                        output.accept(ModItems.POINT_LAMP_3_ITEM.get());
                        output.accept(ModItems.METER_ITEM.get());
                        output.accept(ModItems.EMPTY_CONSOLE_PANEL_ITEM.get());
                        output.accept(ModItems.EMPTY_CONSOLE_PANEL_LARGE_ITEM.get());
                        output.accept(ModItems.CONSOLE_BUTTON_1_ITEM.get());
                        output.accept(ModItems.CONSOLE_BUTTON_2_ITEM.get());
                        output.accept(ModItems.CONSOLE_BUTTON_3_ITEM.get());
                        output.accept(ModItems.CONSOLE_BUTTON_4_ITEM.get());
                        output.accept(ModItems.CONSOLE_BUTTON_5_ITEM.get());
                        output.accept(ModItems.PASSWORD_INPUTER_ITEM.get());
                        output.accept(ModItems.PASSWORD_CRACKER_ITEM.get());
                        output.accept(ModItems.NUKE_BUTTON_ITEM.get());
                        output.accept(ModItems.KEY_1.get());
                        output.accept(ModItems.KEY_2.get());
                        output.accept(ModItems.KEY_CABINET_ITEM.get());
                        output.accept(ModItems.KEY_DISTRIBUTOR_ITEM.get());
                        output.accept(ModItems.SAFE_BUTTON_1_ITEM.get());
                        output.accept(ModItems.PLOTTER_ITEM.get());
                        output.accept(ModItems.PLOTTER_CLOCK_ITEM.get());
                        output.accept(ModItems.BREAKER_ITEM.get());
                        output.accept(ModItems.DIGITAL_DISPLAY_ITEM.get());
                        output.accept(ModItems.DIGITAL_KNOB_ITEM.get());
                        output.accept(ModItems.DIGITAL_PLOTTER_ITEM.get());
                        output.accept(ModItems.DATA_UNIT_ITEM.get());
                        output.accept(ModItems.BLOCK_DETECTOR_ITEM.get());
                        // 海报（单方块，右键设置图片/缩放/偏移）
                        output.accept(ModItems.POSTER_ITEM.get());
                        // 多方块控制柜：结构方块 + 总线方块 + 辅助计算机
                        output.accept(ModItems.CONSOLE_BLOCK_ITEM.get());
                        output.accept(ModItems.CONSOLE_BLOCK_BLUE_ITEM.get());
                        output.accept(ModItems.CONSOLE_BLOCK_BLACK_ITEM.get());
                        output.accept(ModItems.CONSOLE_BUS_ITEM.get());
                        output.accept(ModItems.CONTROLLER_AUX_ITEM.get());
                        // 控制柜表面组件（仪表 / 数字显示屏 / 指示灯 / 按钮面板 / 发信终端）
                        output.accept(ModItems.CONSOLE_GAUGE.get());
                        output.accept(ModItems.CONSOLE_NUMITRON.get());
                        output.accept(ModItems.CONSOLE_GRAPH.get());
                        // 圆盘记录仪 L（带笔的独立模型）
                        output.accept(ModItems.CONSOLE_GRAPH_L.get());
                        output.accept(ModItems.CONSOLE_INDICATOR.get());
                        output.accept(ModItems.CONSOLE_KEYPAD.get());
                        output.accept(ModItems.CONSOLE_TERMINAL.get());
                        // 第四期追加：旋钮开关 / 拉杆开关 / 立式仪表 / 弧形仪表
                        output.accept(ModItems.CONSOLE_KNOB_SWITCH.get());
                        output.accept(ModItems.CONSOLE_LEVER_SWITCH.get());
                        output.accept(ModItems.CONSOLE_GAUGE_V.get());
                        output.accept(ModItems.CONSOLE_GAUGE_ARC.get());
                        // RGB 颜色控制器（第三十三期）
                        output.accept(ModItems.CONSOLE_RGB.get());
                        // 阀门式拉杆 + 旋转按钮（第三十四期）
                        output.accept(ModItems.CONSOLE_LEVER_F.get());
                        output.accept(ModItems.CONSOLE_BUTTON_X.get());
                        // 三状态开关 + 双色指示灯（第三十五期）
                        output.accept(ModItems.CONSOLE_T_SWITCH.get());
                        output.accept(ModItems.CONSOLE_T_LEVER.get());
                        output.accept(ModItems.CONSOLE_LAMP_D.get());
                        // 扇形刻度仪表（gauge_f）
                        output.accept(ModItems.CONSOLE_GAUGE_F.get());
                        // 同步指示器（synchroscope）
                        output.accept(ModItems.CONSOLE_SYNCHROSCOPE.get());
                        // 安全按钮（带玻璃罩）
                        output.accept(ModItems.CONSOLE_BUTTON_S.get());
                        // 钥匙开关 + 控制柜钥匙
                        output.accept(ModItems.CONSOLE_SWITCH_K.get());
                        output.accept(ModItems.CONSOLE_KEY.get());
                        // 警报器（灯 + 静音 + 重置）
                        output.accept(ModItems.CONSOLE_ALARM.get());
                        output.accept(ModItems.NAI_LONG_TOY_ITEM.get());
                        output.accept(ModItems.REDSTONE_SENDER_ITEM.get());
                        output.accept(ModItems.REDSTONE_RECEIVER_ITEM.get());
                        // 扩展红石继电器 + 总线
                        output.accept(ModItems.EXTENDED_RELAY_ITEM.get());
                        output.accept(ModItems.RELAY_BUS_ITEM.get());
                        output.accept(ModItems.MULTI_TOOL.get());
                        // 编辑工具：编辑可显示名称的方块文字
                        output.accept(ModItems.EDIT_TOOL.get());
                        // 画图工具：在控制柜表面绘制图案
                        output.accept(ModItems.DRAW_TOOL.get());
                        output.accept(ModItems.MUSIC_DISC_LEVEL5.get());
                        output.accept(ModItems.MUSIC_DISC_RAILUGUN.get());
                        output.accept(ModItems.MUSIC_DISC_NEVER.get());
                        output.accept(ModItems.MUSIC_DISC_ASSUMPTIONS.get());
                        output.accept(ModItems.MUSIC_DISC_CONRNFIELD_CHASE.get());
                        output.accept(ModItems.MUSIC_DISC_MOVE.get());
                        output.accept(ModItems.MUSIC_DISC_MORE_ONE_NIGHT.get());
                        output.accept(ModItems.MUSIC_DISC_RAIN.get());
                        output.accept(ModItems.MUSIC_DISC_END.get());
                        output.accept(ModItems.MUSIC_DISC_UNDERGROUND_RIVER.get());
                        output.accept(ModItems.MUSIC_DISC_HANEZEVE_CARADHINA.get());
                        output.accept(ModItems.MUSIC_DISC_CUTIE_MEW_MEW_MAGIC.get());
                        output.accept(ModItems.MUSIC_DISC_DENISE.get());
                        output.accept(ModItems.MUSIC_DISC_GWANGJU.get());
                        output.accept(ModItems.MUSIC_DISC_HIGHER.get());
                        output.accept(ModItems.MUSIC_DISC_KING.get());
                        output.accept(ModItems.MUSIC_DISC_MARISA.get());
                        output.accept(ModItems.MUSIC_DISC_MIXUE.get());
                        output.accept(ModItems.MUSIC_DISC_RAW_TELL.get());
                        output.accept(ModItems.MUSIC_DISC_REIMU.get());
                        output.accept(ModItems.MUSIC_DISC_YOU_WILL_BE_PERFECT.get());
                        output.accept(ModItems.MUSIC_DISC_BLOOM.get());
                        output.accept(ModItems.MUSIC_DISC_JIGOKU_SHOUJO.get());
                        output.accept(ModItems.MUSIC_DISC_THE_IMITATION_GAME.get());
                        output.accept(ModItems.MUSIC_DISC_BIT.get());
                        output.accept(ModItems.MUSIC_DISC_BROKEN_BOY.get());
                        output.accept(ModItems.MUSIC_DISC_PANIC_TRACK.get());
                        output.accept(ModItems.MUSIC_DISC_RESONANCE.get());
                        output.accept(ModItems.MUSIC_DISC_ROLLER_MOBSTER.get());
                        output.accept(ModItems.MUSIC_DISC_SABOTAGE.get());
                        output.accept(ModItems.MUSIC_DISC_FRIENDS_WINE.get());
                        output.accept(ModItems.MUSIC_DISC_AIR.get());
                        output.accept(ModItems.MUSIC_DISC_LEVEL.get());
                        output.accept(ModItems.CARD_READER_A_ITEM.get());
                        output.accept(ModItems.CARD_READER_B_ITEM.get());
                        output.accept(ModItems.CARD_READER_C_ITEM.get());
                        output.accept(ModItems.CARD_READER_D_ITEM.get());
                        output.accept(ModItems.CARD_READER_E_ITEM.get());
                        output.accept(ModItems.CARD_A.get());
                        output.accept(ModItems.CARD_B.get());
                        output.accept(ModItems.CARD_C.get());
                        output.accept(ModItems.CARD_D.get());
                        output.accept(ModItems.CARD_E.get());
                        output.accept(ModItems.FROZEN_TILAPIA.get());
                        output.accept(ModItems.GREEN_WINE.get());
                        output.accept(ModItems.GREEN_WINE_BARREL.get());
                        output.accept(ModItems.HE_YI_WEI.get());
                        output.accept(ModItems.TASTES_FOOD.get());
                        output.accept(ModItems.MOON_CAKE.get());
                        output.accept(ModItems.MOON_CAKE_IRON.get());
                        output.accept(ModItems.SHIP_BISCUIT.get());
                        output.accept(ModItems.BAO_ZI.get());
                        output.accept(ModItems.BAGUETTE.get());
                        output.accept(ModItems.CROWBAR.get());
                        output.accept(ModItems.SIMPLE_SPEAR.get());
                        output.accept(ModItems.CCRC_BLOCK_ITEM.get());
                        // 错误生物刷怪蛋（error_mob + 变种 null/warn）
                        output.accept(ModItems.ERROR_MOB_SPAWN_EGG.get());
                        output.accept(ModItems.ERROR_MOB_NULL_SPAWN_EGG.get());
                        output.accept(ModItems.ERROR_MOB_WARN_SPAWN_EGG.get());
                        // 金鹰（吸引/繁殖盖金蜗牛）+ 盖金蜗牛刷怪蛋
                        output.accept(ModItems.GOLDEN_EAGLE.get());
                        output.accept(ModItems.GAJIN_SPAWN_EGG.get());
                        output.accept(ModItems.EVIL_GAJIN_SPAWN_EGG.get());
                        output.accept(ModItems.SERVER_FAAS_1_ITEM.get());
                        output.accept(ModItems.SERVER_FAAS_2_ITEM.get());
                        output.accept(ModItems.SERVER_FAAS_3_ITEM.get());
                        // 说明书 1：发放带固定内容的成书（原版书籍阅读界面）
                        output.accept(ModItems.INSTRUCTION_BOOK_1.get().createBook());
                        // 说明书 2：发放带固定内容的成书（CC 配件外设使用方法）
                        output.accept(ModItems.INSTRUCTION_BOOK_2.get().createBook());
                    })
                    .build());

    // 搬运物品栏：Forge 按注册名对模组创造标签排序，注册名 "cc_rc_tab_carried"
    // 在 "cc_rc_tab" 之后（前缀更长），从而确保搬运物品栏显示在主物品栏之后。
    // 用于收编从外部模组（如农夫乐事）搬运来的展示物品，图标为箱装土豆。
    public static final RegistryObject<CreativeModeTab> CC_RC_CARRIED_TAB = CREATIVE_TABS.register("cc_rc_tab_carried",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cc_rc_carried"))
                    .icon(() -> new ItemStack(ModItems.POTATO_CRATE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.POTATO_CRATE.get());
                        // 冰箱、水槽（原 Cooking for Blockheads）
                        output.accept(ModItems.FRIDGE_ITEM.get());
                        output.accept(ModItems.SINK_ITEM.get());
                        // 16 色粗布告示牌与悬挂式粗布告示牌（原农夫乐事）
                        for (int i = 0; i < ModItems.CANVAS_SIGN_ITEMS.size(); i++) {
                            output.accept(ModItems.CANVAS_SIGN_ITEMS.get(i).get());
                            output.accept(ModItems.HANGING_CANVAS_SIGN_ITEMS.get(i).get());
                        }
                    })
                    .build());
}