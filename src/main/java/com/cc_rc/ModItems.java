package com.cc_rc;

import com.cc_rc.item.AttackDescriptionItem;
import com.cc_rc.item.BaguetteItem;
import com.cc_rc.item.BaoZiItem;
import com.cc_rc.item.CardItem;
import com.cc_rc.item.CrowbarItem;
import com.cc_rc.item.DescriptionBlockItem;
import com.cc_rc.item.DescriptionHangingSignItem;
import com.cc_rc.item.DescriptionItem;
import com.cc_rc.item.DescriptionSignItem;
import com.cc_rc.item.DescriptionSwordItem;
import com.cc_rc.item.InstructionBookItem;
import com.cc_rc.item.InstructionBook2Item;
import com.cc_rc.item.ModToolTiers;
import com.cc_rc.item.MultiToolItem;
import com.cc_rc.item.PasswordCrackerItem;
import com.cc_rc.item.SimpleSpearItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CcRc.MODID);

    // Block items
    public static final RegistryObject<Item> CCRC_BLOCK_ITEM = ITEMS.register("ccrc_block",
            () -> new BlockItem(ModBlocks.CCRC_BLOCK.get(), new Item.Properties()));

    // 控制台拉杆 1 物品
    public static final RegistryObject<Item> CONSOLE_LEVER_1_ITEM = ITEMS.register("console_lever_1",
            () -> new BlockItem(ModBlocks.CONSOLE_LEVER_1.get(), new Item.Properties()));

    // 控制台拉杆 2 物品
    public static final RegistryObject<Item> CONSOLE_LEVER_2_ITEM = ITEMS.register("console_lever_2",
            () -> new BlockItem(ModBlocks.CONSOLE_LEVER_2.get(), new Item.Properties()));

    // 控制台拉杆 6 物品（3挡位）
    public static final RegistryObject<Item> CONSOLE_LEVER_6_ITEM = ITEMS.register("console_lever_6",
            () -> new BlockItem(ModBlocks.CONSOLE_LEVER_6.get(), new Item.Properties()));

    // 控制台拉杆 7 物品（3挡位）
    public static final RegistryObject<Item> CONSOLE_LEVER_7_ITEM = ITEMS.register("console_lever_7",
            () -> new BlockItem(ModBlocks.CONSOLE_LEVER_7.get(), new Item.Properties()));

    // 指示灯 1 物品
    public static final RegistryObject<Item> POINT_LAMP_1_ITEM = ITEMS.register("point_lamp_1",
            () -> new BlockItem(ModBlocks.POINT_LAMP_1.get(), new Item.Properties()));

    // 指示灯 2 物品
    public static final RegistryObject<Item> POINT_LAMP_2_ITEM = ITEMS.register("point_lamp_2",
            () -> new BlockItem(ModBlocks.POINT_LAMP_2.get(), new Item.Properties()));

    // 指示灯 3 物品
    public static final RegistryObject<Item> POINT_LAMP_3_ITEM = ITEMS.register("point_lamp_3",
            () -> new BlockItem(ModBlocks.POINT_LAMP_3.get(), new Item.Properties()));

    // 仪表 Meter 物品
    public static final RegistryObject<Item> METER_ITEM = ITEMS.register("meter",
            () -> new BlockItem(ModBlocks.METER.get(), new Item.Properties()));

    // 空控制面板物品
    public static final RegistryObject<Item> EMPTY_CONSOLE_PANEL_ITEM = ITEMS.register("empty_console_panel",
            () -> new BlockItem(ModBlocks.CONSOLE_PANEL.get(), new Item.Properties()));

    // 大号空控制面板物品
    public static final RegistryObject<Item> EMPTY_CONSOLE_PANEL_LARGE_ITEM = ITEMS.register("empty_console_panel_large",
            () -> new BlockItem(ModBlocks.CONSOLE_PANEL_LARGE.get(), new Item.Properties()));

    // 控制台按钮 1 物品
    public static final RegistryObject<Item> CONSOLE_BUTTON_1_ITEM = ITEMS.register("console_button_1",
            () -> new BlockItem(ModBlocks.CONSOLE_BUTTON_1.get(), new Item.Properties()));

    // 控制台按钮 2 物品
    public static final RegistryObject<Item> CONSOLE_BUTTON_2_ITEM = ITEMS.register("console_button_2",
            () -> new BlockItem(ModBlocks.CONSOLE_BUTTON_2.get(), new Item.Properties()));

    // 控制台按钮 3 物品
    public static final RegistryObject<Item> CONSOLE_BUTTON_3_ITEM = ITEMS.register("console_button_3",
            () -> new BlockItem(ModBlocks.CONSOLE_BUTTON_3.get(), new Item.Properties()));

    // 控制台按钮 4 物品
    public static final RegistryObject<Item> CONSOLE_BUTTON_4_ITEM = ITEMS.register("console_button_4",
            () -> new BlockItem(ModBlocks.CONSOLE_BUTTON_4.get(), new Item.Properties()));

    // 控制台按钮 5 物品
    public static final RegistryObject<Item> CONSOLE_BUTTON_5_ITEM = ITEMS.register("console_button_5",
            () -> new BlockItem(ModBlocks.CONSOLE_BUTTON_5.get(), new Item.Properties()));

    // 密码输入器物品
    public static final RegistryObject<Item> PASSWORD_INPUTER_ITEM = ITEMS.register("password_inputer",
            () -> new BlockItem(ModBlocks.PASSWORD_INPUTER.get(), new Item.Properties()));

    // 破解器物品（手持右键密码输入器开始破解，不可堆叠）
    public static final RegistryObject<Item> PASSWORD_CRACKER_ITEM = ITEMS.register("password_cracker",
            () -> new PasswordCrackerItem(new Item.Properties().stacksTo(1)));

    // 核弹按钮物品
    public static final RegistryObject<Item> NUKE_BUTTON_ITEM = ITEMS.register("nuke_button",
            () -> new BlockItem(ModBlocks.NUKE_BUTTON.get(), new Item.Properties()));

    // 核弹发射钥匙 1 / 2（右键核弹按钮消耗并推进状态，不可堆叠）
    public static final RegistryObject<Item> KEY_1 = ITEMS.register("key_1",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> KEY_2 = ITEMS.register("key_2",
            () -> new Item(new Item.Properties().stacksTo(1)));

    // 安全按钮 1 物品
    public static final RegistryObject<Item> SAFE_BUTTON_1_ITEM = ITEMS.register("safe_button_1",
            () -> new BlockItem(ModBlocks.SAFE_BUTTON_1.get(), new Item.Properties()));

    // 钥匙柜物品
    public static final RegistryObject<Item> KEY_CABINET_ITEM = ITEMS.register("key_cabinet",
            () -> new BlockItem(ModBlocks.KEY_CABINET.get(), new Item.Properties()));

    // 钥匙分发控制器物品
    public static final RegistryObject<Item> KEY_DISTRIBUTOR_ITEM = ITEMS.register("key_distributor",
            () -> new BlockItem(ModBlocks.KEY_DISTRIBUTOR.get(), new Item.Properties()));

    // 圆盘记录仪物品（悬停时显示切换模式说明）
    public static final RegistryObject<Item> PLOTTER_ITEM = ITEMS.register("plotter",
            () -> new DescriptionBlockItem(ModBlocks.PLOTTER.get(), new Item.Properties(),
                    "item.cc_rc.desc_plotter"));

    // 圆盘记录仪时钟物品（悬停时显示触发说明）
    public static final RegistryObject<Item> PLOTTER_CLOCK_ITEM = ITEMS.register("plotter_clock",
            () -> new DescriptionBlockItem(ModBlocks.PLOTTER_CLOCK.get(), new Item.Properties(),
                    "item.cc_rc.desc_plotter_clock"));

    // 断路器物品
    public static final RegistryObject<Item> BREAKER_ITEM = ITEMS.register("breaker",
            () -> new BlockItem(ModBlocks.BREAKER.get(), new Item.Properties()));

    // 数码显示器物品
    public static final RegistryObject<Item> DIGITAL_DISPLAY_ITEM = ITEMS.register("digital_display",
            () -> new BlockItem(ModBlocks.DIGITAL_DISPLAY.get(), new Item.Properties()));

    // 数字调节器物品（悬停时显示切换显示模式说明）
    public static final RegistryObject<Item> DIGITAL_KNOB_ITEM = ITEMS.register("digital_knob",
            () -> new DescriptionBlockItem(ModBlocks.DIGITAL_KNOB.get(), new Item.Properties(),
                    "item.cc_rc.desc_digital_knob"));

    // 数字圆盘记录仪物品
    public static final RegistryObject<Item> DIGITAL_PLOTTER_ITEM = ITEMS.register("digital_plotter",
            () -> new BlockItem(ModBlocks.DIGITAL_PLOTTER.get(), new Item.Properties()));

    // 奶龙玩偶物品
    public static final RegistryObject<Item> NAI_LONG_TOY_ITEM = ITEMS.register("nai_long_toy",
            () -> new BlockItem(ModBlocks.NAI_LONG_TOY.get(), new Item.Properties()));

    // 红石信号发射器 / 接收器物品（方块物品）
    public static final RegistryObject<Item> REDSTONE_SENDER_ITEM = ITEMS.register("redstone_sender",
            () -> new BlockItem(ModBlocks.REDSTONE_SENDER.get(), new Item.Properties()));
    public static final RegistryObject<Item> REDSTONE_RECEIVER_ITEM = ITEMS.register("redstone_receiver",
            () -> new BlockItem(ModBlocks.REDSTONE_RECEIVER.get(), new Item.Properties()));

    // 多功能工具物品 - 不可堆叠，无耐久，无工具属性
    public static final RegistryObject<Item> MULTI_TOOL = ITEMS.register("multi_tool",
            () -> new MultiToolItem(new Item.Properties().stacksTo(1)));

    // Music discs (comparatorOutput, sound, properties, lengthInTicks)
    public static final RegistryObject<RecordItem> MUSIC_DISC_LEVEL5 = ITEMS.register("music_disc_level5",
            () -> new RecordItem(5, ModSounds.MUSIC_LEVEL5.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 5260));

    public static final RegistryObject<RecordItem> MUSIC_DISC_RAILUGUN = ITEMS.register("music_disc_railugun",
            () -> new RecordItem(1, ModSounds.MUSIC_RAILUGUN.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 5140));

    public static final RegistryObject<RecordItem> MUSIC_DISC_NEVER = ITEMS.register("music_disc_never",
            () -> new RecordItem(15, ModSounds.MUSIC_NEVER.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4320));

    public static final RegistryObject<RecordItem> MUSIC_DISC_ASSUMPTIONS = ITEMS.register("music_disc_assumptions",
            () -> new RecordItem(2, ModSounds.MUSIC_ASSUMPTIONS.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4280));

    public static final RegistryObject<RecordItem> MUSIC_DISC_CONRNFIELD_CHASE = ITEMS.register("music_disc_conrnfield_chase",
            () -> new RecordItem(1, ModSounds.MUSIC_CONRNFIELD_CHASE.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 5860));

    public static final RegistryObject<RecordItem> MUSIC_DISC_MOVE = ITEMS.register("music_disc_move",
            () -> new RecordItem(2, ModSounds.MUSIC_MOVE.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 5560));

    public static final RegistryObject<RecordItem> MUSIC_DISC_MORE_ONE_NIGHT = ITEMS.register("music_disc_more_one_night",
            () -> new RecordItem(5, ModSounds.MUSIC_NIGHT.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4400));

    public static final RegistryObject<RecordItem> MUSIC_DISC_RAIN = ITEMS.register("music_disc_rain",
            () -> new RecordItem(2, ModSounds.MUSIC_RAIN.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 5300));

    public static final RegistryObject<RecordItem> MUSIC_DISC_END = ITEMS.register("music_disc_end",
            () -> new RecordItem(2, ModSounds.MUSIC_END.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 5080));

    public static final RegistryObject<RecordItem> MUSIC_DISC_UNDERGROUND_RIVER = ITEMS.register("music_disc_underground_river",
            () -> new RecordItem(6, ModSounds.MUSIC_UNDERGROUND_RIVER.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3900));

    public static final RegistryObject<RecordItem> MUSIC_DISC_HANEZEVE_CARADHINA = ITEMS.register("music_disc_hanezeve_caradhina",
            () -> new RecordItem(6, ModSounds.MUSIC_HANEZEVE_CARADHINA.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4020));

    // 新增 10 张音乐唱片（comparatorOutput 不与现有 1/2/5/6/15 冲突）
    public static final RegistryObject<RecordItem> MUSIC_DISC_CUTIE_MEW_MEW_MAGIC = ITEMS.register("music_disc_cutie_mew_mew_magic",
            () -> new RecordItem(3, ModSounds.MUSIC_CUTIE_MEW_MEW_MAGIC.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3680));

    public static final RegistryObject<RecordItem> MUSIC_DISC_DENISE = ITEMS.register("music_disc_denise",
            () -> new RecordItem(4, ModSounds.MUSIC_DENISE.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 8340));

    public static final RegistryObject<RecordItem> MUSIC_DISC_GWANGJU = ITEMS.register("music_disc_gwangju",
            () -> new RecordItem(7, ModSounds.MUSIC_GWANGJU.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4160));

    public static final RegistryObject<RecordItem> MUSIC_DISC_HIGHER = ITEMS.register("music_disc_higher",
            () -> new RecordItem(8, ModSounds.MUSIC_HIGHER.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4260));

    public static final RegistryObject<RecordItem> MUSIC_DISC_KING = ITEMS.register("music_disc_king",
            () -> new RecordItem(9, ModSounds.MUSIC_KING.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4520));

    public static final RegistryObject<RecordItem> MUSIC_DISC_MARISA = ITEMS.register("music_disc_marisa",
            () -> new RecordItem(10, ModSounds.MUSIC_MARISA.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 2560));

    public static final RegistryObject<RecordItem> MUSIC_DISC_MIXUE = ITEMS.register("music_disc_mixue",
            () -> new RecordItem(11, ModSounds.MUSIC_MIXUE.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 2060));

    public static final RegistryObject<RecordItem> MUSIC_DISC_RAW_TELL = ITEMS.register("music_disc_raw_tell",
            () -> new RecordItem(12, ModSounds.MUSIC_RAW_TELL.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 6340));

    public static final RegistryObject<RecordItem> MUSIC_DISC_REIMU = ITEMS.register("music_disc_reimu",
            () -> new RecordItem(13, ModSounds.MUSIC_REIMU.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 5700));

    public static final RegistryObject<RecordItem> MUSIC_DISC_YOU_WILL_BE_PERFECT = ITEMS.register("music_disc_you_will_be_perfect",
            () -> new RecordItem(14, ModSounds.MUSIC_YOU_WILL_BE_PERFECT.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3188));

    // 新增 3 张音乐唱片（comparatorOutput 复用 1~15 中已存在的值）
    public static final RegistryObject<RecordItem> MUSIC_DISC_BLOOM = ITEMS.register("music_disc_bloom",
            () -> new RecordItem(5, ModSounds.MUSIC_BLOOM.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 5930));

    public static final RegistryObject<RecordItem> MUSIC_DISC_JIGOKU_SHOUJO = ITEMS.register("music_disc_jigoku_shoujo",
            () -> new RecordItem(6, ModSounds.MUSIC_JIGOKU_SHOUJO.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 1891));

    public static final RegistryObject<RecordItem> MUSIC_DISC_THE_IMITATION_GAME = ITEMS.register("music_disc_the_imitation_game",
            () -> new RecordItem(1, ModSounds.MUSIC_THE_IMITATION_GAME.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3160));

    // 新增 6 张音乐唱片（GitHub issue #1，comparatorOutput 复用 3/4/7/8/9/10）
    public static final RegistryObject<RecordItem> MUSIC_DISC_BIT = ITEMS.register("music_disc_bit",
            () -> new RecordItem(3, ModSounds.MUSIC_BIT.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3187));

    public static final RegistryObject<RecordItem> MUSIC_DISC_BROKEN_BOY = ITEMS.register("music_disc_broken_boy",
            () -> new RecordItem(4, ModSounds.MUSIC_BROKEN_BOY.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4847));

    public static final RegistryObject<RecordItem> MUSIC_DISC_PANIC_TRACK = ITEMS.register("music_disc_panic_track",
            () -> new RecordItem(7, ModSounds.MUSIC_PANIC_TRACK.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3075));

    public static final RegistryObject<RecordItem> MUSIC_DISC_RESONANCE = ITEMS.register("music_disc_resonance",
            () -> new RecordItem(8, ModSounds.MUSIC_RESONANCE.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4254));

    public static final RegistryObject<RecordItem> MUSIC_DISC_ROLLER_MOBSTER = ITEMS.register("music_disc_roller_mobster",
            () -> new RecordItem(9, ModSounds.MUSIC_ROLLER_MOBSTER.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4286));

    public static final RegistryObject<RecordItem> MUSIC_DISC_SABOTAGE = ITEMS.register("music_disc_sabotage",
            () -> new RecordItem(10, ModSounds.MUSIC_SABOTAGE.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4540));

    // 新增 2 张音乐唱片（comparatorOutput 复用 11/12）
    public static final RegistryObject<RecordItem> MUSIC_DISC_FRIENDS_WINE = ITEMS.register("music_disc_friends_wine",
            () -> new RecordItem(11, ModSounds.MUSIC_FRIENDS_WINE.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 5145));

    public static final RegistryObject<RecordItem> MUSIC_DISC_AIR = ITEMS.register("music_disc_air",
            () -> new RecordItem(12, ModSounds.MUSIC_AIR.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 7003));

    // 刷卡机物品（A ~ E 共 5 种）
    public static final RegistryObject<Item> CARD_READER_A_ITEM = ITEMS.register("card_reader_a",
            () -> new BlockItem(ModBlocks.CARD_READER_A.get(), new Item.Properties()));

    public static final RegistryObject<Item> CARD_READER_B_ITEM = ITEMS.register("card_reader_b",
            () -> new BlockItem(ModBlocks.CARD_READER_B.get(), new Item.Properties()));

    public static final RegistryObject<Item> CARD_READER_C_ITEM = ITEMS.register("card_reader_c",
            () -> new BlockItem(ModBlocks.CARD_READER_C.get(), new Item.Properties()));

    public static final RegistryObject<Item> CARD_READER_D_ITEM = ITEMS.register("card_reader_d",
            () -> new BlockItem(ModBlocks.CARD_READER_D.get(), new Item.Properties()));

    public static final RegistryObject<Item> CARD_READER_E_ITEM = ITEMS.register("card_reader_e",
            () -> new BlockItem(ModBlocks.CARD_READER_E.get(), new Item.Properties()));

    // 门禁卡物品（A ~ E 共 5 种，不可堆叠）
    public static final RegistryObject<CardItem> CARD_A = ITEMS.register("card_a",
            () -> new CardItem('A', new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CardItem> CARD_B = ITEMS.register("card_b",
            () -> new CardItem('B', new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CardItem> CARD_C = ITEMS.register("card_c",
            () -> new CardItem('C', new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CardItem> CARD_D = ITEMS.register("card_d",
            () -> new CardItem('D', new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CardItem> CARD_E = ITEMS.register("card_e",
            () -> new CardItem('E', new Item.Properties().stacksTo(1)));

    // ==================== 旧模组（cdes，1.21）移植物品 ====================

    // 冰冻罗非鱼：彩蛋武器。1.20.1 用 SwordItem(Tier, 攻击修正, 攻速, Properties) 构造器，
    // 修改器攻击伤害 = 修正(0) + ModToolTiers 加成(255) = +255（实际总伤害 = 基础 1 + 255 = 256），
    // 攻速 -2.0，耐久 2，蓝冰修复。
    // 旧版通过属性覆盖实现相同效果；1.20.1 无 ItemAttributeModifiers 组件，故用构造器参数等价实现。
    // 悬停时显示描述"三体宇宙最强武器（bushi）"（金色）。Rarity.RARE 使物品名显示为蓝色（稀有）。
    public static final RegistryObject<SwordItem> FROZEN_TILAPIA = ITEMS.register("frozen_tilapia",
            () -> new DescriptionSwordItem(ModToolTiers.FROZEN_TILAPIA, 0, -2.0F,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                    "item.cc_rc.desc_frozen_tilapia", ChatFormatting.GOLD));

    // 沉船绿酒：饮用后给予剧毒（等级4）与反胃（等级3），各持续 114514 游戏刻（约95分钟），
    // 为整蛊食材，营养仅 2、饱食度 0.4。悬停描述文字为绿色（"这酒能喝吗？……都绿了"）。
    public static final RegistryObject<Item> GREEN_WINE = ITEMS.register("green_wine",
            () -> new DescriptionItem(new Item.Properties().stacksTo(16)
                    .food(new FoodProperties.Builder()
                            .nutrition(2)
                            .saturationMod(0.4F)
                            .alwaysEat()
                            .effect(() -> new MobEffectInstance(MobEffects.POISON, 114514, 4), 1.0F)
                            .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 114514, 3), 1.0F)
                            .build()),
                    "item.cc_rc.desc_green_wine", ChatFormatting.GREEN));

    // 沉船绿酒桶：普通物品（原版可堆叠 64），用于酿造/合成的原料，无特殊功能。
    // 悬停显示描述"*这是计划的一部分*"。
    public static final RegistryObject<Item> GREEN_WINE_BARREL = ITEMS.register("green_wine_barrel",
            () -> new DescriptionItem(new Item.Properties(), "item.cc_rc.desc_green_wine_barrel"));

    // 何意味？：究极"效果大杂烩"食物（营养 11、饱和度 4、可随时食用），食用后同时获得 10 种状态效果
    //（200 游戏刻：潮涌/海豚/村庄英雄/幸运/失明/黑暗/反胃/漂浮/挖掘疲劳，300 游戏刻：缓降），
    // 稀有度 RARE。注：旧版含 TRIAL_OMEN（1.21 专属），1.20.1 不存在该效果故移除此项。
    // 悬停描述文字为紫色（"*你想何出怎样的意味？*"）。最大堆叠 16。
    public static final RegistryObject<Item> HE_YI_WEI = ITEMS.register("he_yi_wei",
            () -> new DescriptionItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)
                    .food(new FoodProperties.Builder()
                            .nutrition(11)
                            .saturationMod(4F)
                            .alwaysEat()
                            .effect(() -> new MobEffectInstance(MobEffects.CONDUIT_POWER, 200, 1), 1.0F)
                            .effect(() -> new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 200, 1), 1.0F)
                            .effect(() -> new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 200, 1), 1.0F)
                            .effect(() -> new MobEffectInstance(MobEffects.LUCK, 200, 1), 1.0F)
                            .effect(() -> new MobEffectInstance(MobEffects.BLINDNESS, 200, 1), 1.0F)
                            .effect(() -> new MobEffectInstance(MobEffects.DARKNESS, 200, 1), 1.0F)
                            .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 200, 1), 1.0F)
                            .effect(() -> new MobEffectInstance(MobEffects.LEVITATION, 200, 1), 1.0F)
                            .effect(() -> new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 1), 1.0F)
                            .effect(() -> new MobEffectInstance(MobEffects.SLOW_FALLING, 300, 1), 1.0F)
                            .build()),
                    "item.cc_rc.desc_he_yi_wei", ChatFormatting.DARK_PURPLE));

    // 合一味：大份便当式食物（营养 30、饱和度 0.8、可随时食用），一次顶一整排饥饿条。最大堆叠 16。
    public static final RegistryObject<Item> TASTES_FOOD = ITEMS.register("tastes_food",
            () -> new Item(new Item.Properties().stacksTo(16)
                    .food(new FoodProperties.Builder()
                            .nutrition(30)
                            .saturationMod(0.8F)
                            .alwaysEat()
                            .build())));

    // ==================== 新增物品：月饼 / 压缩饼干 ====================

    // 五仁月饼：食物（营养 8、饱和度 4），可随时食用（饥饿值满也可，类似金苹果），
    // 食用后获得 200 秒（4000 刻）速度 II。最大堆叠 16。
    public static final RegistryObject<Item> MOON_CAKE = ITEMS.register("moon_cake",
            () -> new Item(new Item.Properties().stacksTo(16)
                    .food(new FoodProperties.Builder()
                            .nutrition(8)
                            .saturationMod(4F)
                            .alwaysEat()
                            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 4000, 1), 1.0F)
                            .build())));

    // 五金月饼：普通物品注册（非武器），最大堆叠 16、无耐久损耗。
    // 通过属性修饰符在主手附加攻击伤害：修饰符值 9.0 + 空手基础 1.0 = 实际攻击 10（工具提示 "+10 Attack Damage"）。
    // 食物营养 4、饱和度 2，可随时食用（饥饿值满也可，类似金苹果），
    // 食用后获得：2 秒（40 刻）凋零 II、200 秒（4000 刻）力量 II 与抗性提升。
    // 悬停时显示描述"机加工这一块/."。
    public static final RegistryObject<Item> MOON_CAKE_IRON = ITEMS.register("moon_cake_iron",
            () -> new AttackDescriptionItem(new Item.Properties().stacksTo(16)
                            .food(new FoodProperties.Builder()
                                    .nutrition(4)
                                    .saturationMod(2F)
                                    .alwaysEat()
                                    .effect(() -> new MobEffectInstance(MobEffects.WITHER, 40, 1), 1.0F)
                                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 4000, 1), 1.0F)
                                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 4000, 0), 1.0F)
                                    .build()),
                    "item.cc_rc.desc_moon_cake_iron", 9.0D));

    // 压缩饼干：食物（营养 12、饱和度 6），食用后获得 60 秒（1200 刻）饱和效果与 20 秒（400 刻）生命恢复。
    // 悬停时显示描述"*量大管饱*"。
    public static final RegistryObject<Item> SHIP_BISCUIT = ITEMS.register("ship_biscuit",
            () -> new DescriptionItem(new Item.Properties().stacksTo(64)
                    .food(new FoodProperties.Builder()
                            .nutrition(12)
                            .saturationMod(6F)
                            .effect(() -> new MobEffectInstance(MobEffects.SATURATION, 1200, 0), 1.0F)
                            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 400, 0), 1.0F)
                            .build()),
                    "item.cc_rc.desc_ship_biscuit"));

    // ==================== 包子（bao_zi） ====================

    // 包子：食物 + 投掷炸弹二合一。
    // 右键食用（营养 4 / 饱和度 2）；左键投掷出 bao_zi 弹射物（命中爆炸，仅伤害实体不破坏方块）。
    // 悬停描述"包子雷？"（红色斜体）。最大堆叠 16。
    public static final RegistryObject<Item> BAO_ZI = ITEMS.register("bao_zi",
            () -> new BaoZiItem(new Item.Properties().stacksTo(16)
                    .food(new FoodProperties.Builder()
                            .nutrition(4)
                            .saturationMod(2F)
                            .build())));

    // ==================== 法棍（baguette） ====================

    // 法棍：普通食物（营养 2 / 饱和度 2），可堆叠 16；
    // BaguetteItem 在主手附加攻击伤害（修饰符 3.0 + 空手 1.0 = 实际 4）与击退（ATTACK_KNOCKBACK = 3.0）。
    // 悬停描述"坚如磐石"（粗体棕色）。
    public static final RegistryObject<Item> BAGUETTE = ITEMS.register("baguette",
            () -> new BaguetteItem(new Item.Properties().stacksTo(16)
                    .food(new FoodProperties.Builder()
                            .nutrition(2)
                            .saturationMod(2F)
                            .build()), 3.0D, 3.0D));

    // ==================== 撬棍（crowbar） ====================

    // 撬棍：铁质 SwordItem（自带横扫），修改器攻击伤害 +19（实际总伤害 = 基础 1 + 19 = 20）、
    // 攻速慢（修正 -3.0）、暴击 2.0 倍、耐久 1024、铁锭修复，不可堆叠。
    // 悬停描述两行："物理学圣剑"（深蓝色粗体）、"f(x)dx"（白色删除线）。
    public static final RegistryObject<Item> CROWBAR = ITEMS.register("crowbar",
            () -> new CrowbarItem(ModToolTiers.CROWBAR, 0, -3.0F,
                    new Item.Properties().stacksTo(1)));

    // ==================== 简易长矛（simple_spear） ====================

    // 简易长矛：铁质 SwordItem（自带横扫），耐久 130、修改器攻击伤害 +129
    // （实际总伤害 = 基础 1 + 129 = 130）、+13 攻击范围（ForgeMod.ENTITY_REACH，
    // 由 SimpleSpearItem 覆写 getAttributeModifiers 追加）、
    // Rarity.EPIC（物品名淡紫色，类似附魔金苹果）+ isFoil 附魔光泽，铁锭修复，不可堆叠。
    // 悬停描述：紫色"魔女们的秘密武器"。
    public static final RegistryObject<Item> SIMPLE_SPEAR = ITEMS.register("simple_spear",
            () -> new SimpleSpearItem(ModToolTiers.SIMPLE_SPEAR, 0, -2.4F,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // 说明书 1：采用原版成书（WrittenBookItem）机制，右键打开原版书籍阅读界面。
    // 固定内容（目录 + 控制面板类/圆盘记录仪/断路器/刷卡机/红石信号收发等页）由
    // InstructionBookItem.createBook() 写入成书 NBT；创造标签发放带内容的本子。
    public static final RegistryObject<InstructionBookItem> INSTRUCTION_BOOK_1 = ITEMS.register("instruction_book_1",
            () -> new InstructionBookItem(new Item.Properties().stacksTo(1)));

    // 说明书 2：同样采用原版成书机制，记录 CC: Tweaked 配件外设的使用方法
    // （数码显示器/数字调节器/数字圆盘记录仪的每个 Lua 函数及参数类型），
    // 内容由 InstructionBook2Item.createBook() 写入成书 NBT。
    public static final RegistryObject<InstructionBook2Item> INSTRUCTION_BOOK_2 = ITEMS.register("instruction_book_2",
            () -> new InstructionBook2Item(new Item.Properties().stacksTo(1)));

    // ==================== 搬运物品（原农夫乐事等外部模组，模型已转移至 cc_rc 命名空间） ====================

    // 箱装土豆：搬运自农夫乐事的方块物品（对应箱装土豆方块，无方向、木板材质）。
    // 悬停时显示两条描述：第一行"搬运自农夫乐事"（黄色斜体）标识来源；
    // 第二行"！？服务器 ？！"（淡蓝粗体）。
    public static final RegistryObject<Item> POTATO_CRATE = ITEMS.register("potato_crate",
            () -> new DescriptionBlockItem(ModBlocks.POTATO_CRATE.get(), new Item.Properties(),
                    Component.translatable("item.cc_rc.desc_potato_crate_carried").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC),
                    Component.translatable("item.cc_rc.desc_potato_crate").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));

    // 冰箱：搬运自 Cooking for Blockheads 的方块物品（27 格容器，右键开原版箱子 GUI）。
    // 悬停时显示黄色斜体来源描述"搬运自 Cooking for Blockheads"。
    public static final RegistryObject<Item> FRIDGE_ITEM = ITEMS.register("fridge",
            () -> new DescriptionBlockItem(ModBlocks.FRIDGE.get(), new Item.Properties(),
                    Component.translatable("item.cc_rc.desc_carried_cfb").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC)));

    // 水槽：搬运自 Cooking for Blockheads 的方块物品（纯装饰）。
    // 悬停时显示黄色斜体来源描述"搬运自 Cooking for Blockheads"。
    public static final RegistryObject<Item> SINK_ITEM = ITEMS.register("sink",
            () -> new DescriptionBlockItem(ModBlocks.SINK.get(), new Item.Properties(),
                    Component.translatable("item.cc_rc.desc_carried_cfb").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC)));

    // ==================== F.A.A.S 服务器物品 ====================

    // F.A.A.S 服务器 1/2/3 方块物品（对应三种模型变体，共用方块类 ServerFaasBlock）。
    public static final RegistryObject<Item> SERVER_FAAS_1_ITEM = ITEMS.register("server_faas_1",
            () -> new BlockItem(ModBlocks.SERVER_FAAS_1.get(), new Item.Properties()));

    public static final RegistryObject<Item> SERVER_FAAS_2_ITEM = ITEMS.register("server_faas_2",
            () -> new BlockItem(ModBlocks.SERVER_FAAS_2.get(), new Item.Properties()));

    public static final RegistryObject<Item> SERVER_FAAS_3_ITEM = ITEMS.register("server_faas_3",
            () -> new BlockItem(ModBlocks.SERVER_FAAS_3.get(), new Item.Properties()));

    // ==================== 搬运告示牌物品（原农夫乐事 canvas_sign） ====================

    // 每个颜色注册两个物品：
    // - <color>_canvas_sign：粗布告示牌，SignItem（立式/壁挂共用），黄色斜体描述"搬运自农夫乐事"；
    // - <color>_hanging_canvas_sign：悬挂式粗布告示牌，HangingSignItem（天花板/壁挂悬挂共用），同一描述。
    // 描述复用已有 key item.cc_rc.desc_potato_crate_carried。
    public static final List<RegistryObject<Item>> CANVAS_SIGN_ITEMS = new ArrayList<>();
    public static final List<RegistryObject<Item>> HANGING_CANVAS_SIGN_ITEMS = new ArrayList<>();

    static {
        for (int i = 0; i < ModBlocks.CANVAS_SIGN_COLORS.length; i++) {
            String color = ModBlocks.CANVAS_SIGN_COLORS[i];
            final int idx = i;
            Component carriedDesc = Component.translatable("item.cc_rc.desc_potato_crate_carried")
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC);

            CANVAS_SIGN_ITEMS.add(ITEMS.register(color + "_canvas_sign",
                    () -> new DescriptionSignItem(new Item.Properties(),
                            ModBlocks.CANVAS_SIGN_BLOCKS.get(idx).get(),
                            ModBlocks.CANVAS_WALL_SIGN_BLOCKS.get(idx).get(),
                            carriedDesc)));

            HANGING_CANVAS_SIGN_ITEMS.add(ITEMS.register(color + "_hanging_canvas_sign",
                    () -> new DescriptionHangingSignItem(
                            ModBlocks.HANGING_CANVAS_SIGN_BLOCKS.get(idx).get(),
                            ModBlocks.WALL_HANGING_CANVAS_SIGN_BLOCKS.get(idx).get(),
                            new Item.Properties(),
                            carriedDesc)));
        }
    }

    // ==================== 错误生物刷怪蛋 ====================

    // 错误生物（error_mob）刷怪蛋：主色红（实体贴图主色 220,40,40），
    // 次色深灰蓝（背景 30,30,40）。实体不自然生成，只能靠本刷怪蛋或刷怪笼召唤。
    // 使用 ForgeSpawnEggItem（接受 Supplier 惰性解析实体类型），避免在物品注册
    // 阶段直接 get() 实体导致 "Registry Object not present" 崩溃。
    public static final RegistryObject<Item> ERROR_MOB_SPAWN_EGG = ITEMS.register("error_mob_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(() -> ModEntities.ERROR_MOB.get(),
                    // 主色：实体红色字样（约 0xDC2828）
                    0xDC2828,
                    // 次色：背景深灰蓝（约 0x1E1E28）
                    0x1E1E28,
                    new Item.Properties()));

    // 错误生物变种（null）刷怪蛋：主色暗紫灰（NULL 贴图 140,130,180），次色深紫褐。
    public static final RegistryObject<Item> ERROR_MOB_NULL_SPAWN_EGG = ITEMS.register("error_mob_null_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(() -> ModEntities.ERROR_MOB_NULL.get(),
                    0x8C82B4,
                    0x282338,
                    new Item.Properties()));

    // 错误生物变种（warn）刷怪蛋：主色琥珀黄（WARN 贴图 230,170,60），次色深棕褐。
    public static final RegistryObject<Item> ERROR_MOB_WARN_SPAWN_EGG = ITEMS.register("error_mob_warn_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(() -> ModEntities.ERROR_MOB_WARN.get(),
                    0xE6AA3C,
                    0x372D14,
                    new Item.Properties()));

    // ==================== 编辑工具 ====================

    // 编辑工具（edit_tool）：主手右键 ITextDisplay 方块打开文字编辑 GUI；
    // 副手放置可显示名称的方块时自动打开编辑界面。不可堆叠。
    public static final RegistryObject<Item> EDIT_TOOL = ITEMS.register("edit_tool",
            () -> new com.cc_rc.item.EditToolItem(new Item.Properties()
                    .stacksTo(1)));

    // ==================== 盖金蜗牛与金鹰 ====================

    // 金鹰（golden_eagle）：可堆叠 64 的普通物品；手持金鹰可吸引（TemptGoal）
    // 与繁殖（BreedGoal）盖金蜗牛。贴图来源「模型/生物/蜗牛/金鹰.png」。
    public static final RegistryObject<Item> GOLDEN_EAGLE = ITEMS.register("golden_eagle",
            () -> new Item(new Item.Properties().stacksTo(64)));

    // 盖金蜗牛刷怪蛋：主色金褐（外壳贴图主色），次色深褐（蜗牛身体暗部）。
    // 使用 ForgeSpawnEggItem + Supplier 惰性解析，避免注册阶段 get() 崩溃。
    public static final RegistryObject<Item> GAJIN_SPAWN_EGG = ITEMS.register("gajin_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(() -> ModEntities.GAJIN.get(),
                    0xD8B24A,
                    0x5A3A1E,
                    new Item.Properties()));

    // ==================== 扩展红石继电器 ====================

    // 扩展红石继电器方块物品
    public static final RegistryObject<Item> EXTENDED_RELAY_ITEM = ITEMS.register("extended_relay",
            () -> new BlockItem(ModBlocks.EXTENDED_RELAY.get(), new Item.Properties()));

    // 扩展红石继电器总线方块物品
    public static final RegistryObject<Item> RELAY_BUS_ITEM = ITEMS.register("relay_bus",
            () -> new BlockItem(ModBlocks.RELAY_BUS.get(), new Item.Properties()));
}