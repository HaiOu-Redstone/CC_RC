package com.cc_rc;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * CC:RC 模组配置文件
 * 配置文件在游戏启动时读取（ModConfig.Type.COMMON）
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = CcRc.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // 安全按钮配置
    private static final ForgeConfigSpec.IntValue SAFE_BUTTON_TIMEOUT_TICKS;
    private static final ForgeConfigSpec.IntValue SAFE_BUTTON_SIGNAL_TICKS;

    // 圆盘记录仪模式 tick 间隔配置
    private static final ForgeConfigSpec.IntValue PLOTTER_MODE2_TICKS;
    private static final ForgeConfigSpec.IntValue PLOTTER_MODE3_TICKS;
    private static final ForgeConfigSpec.IntValue PLOTTER_MODE4_TICKS;
    private static final ForgeConfigSpec.IntValue PLOTTER_MODE5_TICKS;
    private static final ForgeConfigSpec.IntValue PLOTTER_MODE6_TICKS;
    private static final ForgeConfigSpec.IntValue PLOTTER_MODE7_TICKS;
    private static final ForgeConfigSpec.IntValue PLOTTER_MODE8_TICKS;
    private static final ForgeConfigSpec.IntValue PLOTTER_MODE9_TICKS;

    // 刷卡机配置
    private static final ForgeConfigSpec.IntValue CARD_READER_ON_TICKS;
    private static final ForgeConfigSpec.ConfigValue<String> CARD_READER_A_ACCEPTS;
    private static final ForgeConfigSpec.ConfigValue<String> CARD_READER_B_ACCEPTS;
    private static final ForgeConfigSpec.ConfigValue<String> CARD_READER_C_ACCEPTS;
    private static final ForgeConfigSpec.ConfigValue<String> CARD_READER_D_ACCEPTS;
    private static final ForgeConfigSpec.ConfigValue<String> CARD_READER_E_ACCEPTS;

    // 红石信号发射器配置
    private static final ForgeConfigSpec.IntValue REDSTONE_TRANSMIT_RANGE;

    // 密码输入器破解配置
    private static final ForgeConfigSpec.IntValue PASSWORD_CRACK_TICKS;

    // 扩展红石继电器总线配置
    private static final ForgeConfigSpec.IntValue RELAY_BUS_MAX_DISTANCE;

    static {
        BUILDER.push("safe_button");

        SAFE_BUTTON_TIMEOUT_TICKS = BUILDER
                .comment("安全按钮状态2的超时时间（tick），无操作后自动回到状态1")
                .defineInRange("timeout_ticks", 200, 1, 6000);

        SAFE_BUTTON_SIGNAL_TICKS = BUILDER
                .comment("安全按钮状态3的信号持续时间（tick），之后回到状态2")
                .defineInRange("signal_ticks", 20, 1, 6000);

        BUILDER.pop();

        BUILDER.push("plotter");

        PLOTTER_MODE2_TICKS = BUILDER
                .comment("圆盘记录仪模式2的数据更新间隔（tick）")
                .defineInRange("mode2_ticks", 1, 1, 20000);

        PLOTTER_MODE3_TICKS = BUILDER
                .comment("圆盘记录仪模式3的数据更新间隔（tick）")
                .defineInRange("mode3_ticks", 5, 1, 20000);

        PLOTTER_MODE4_TICKS = BUILDER
                .comment("圆盘记录仪模式4的数据更新间隔（tick）")
                .defineInRange("mode4_ticks", 10, 1, 20000);

        PLOTTER_MODE5_TICKS = BUILDER
                .comment("圆盘记录仪模式5的数据更新间隔（tick）")
                .defineInRange("mode5_ticks", 20, 1, 20000);

        PLOTTER_MODE6_TICKS = BUILDER
                .comment("圆盘记录仪模式6的数据更新间隔（tick）")
                .defineInRange("mode6_ticks", 50, 1, 20000);

        PLOTTER_MODE7_TICKS = BUILDER
                .comment("圆盘记录仪模式7的数据更新间隔（tick）")
                .defineInRange("mode7_ticks", 100, 1, 20000);

        PLOTTER_MODE8_TICKS = BUILDER
                .comment("圆盘记录仪模式8的数据更新间隔（tick）")
                .defineInRange("mode8_ticks", 500, 1, 20000);

        PLOTTER_MODE9_TICKS = BUILDER
                .comment("圆盘记录仪模式9的数据更新间隔（tick）")
                .defineInRange("mode9_ticks", 1000, 1, 20000);

        BUILDER.pop();

        BUILDER.push("card_reader");

        CARD_READER_ON_TICKS = BUILDER
                .comment("刷卡机被正确卡片激活后，保持 on（输出红石）的持续时间（tick），之后自动变回 off")
                .defineInRange("on_ticks", 20, 1, 6000);

        CARD_READER_A_ACCEPTS = BUILDER
                .comment("A级刷卡机可识别的门禁卡等级，用大写字母A/B/C/D/E连接，例如\"A\"表示仅识别A卡")
                .define("a_reader_accepts", "A");

        CARD_READER_B_ACCEPTS = BUILDER
                .comment("B级刷卡机可识别的门禁卡等级，例如\"AB\"表示识别A卡和B卡")
                .define("b_reader_accepts", "AB");

        CARD_READER_C_ACCEPTS = BUILDER
                .comment("C级刷卡机可识别的门禁卡等级，例如\"ABC\"表示识别A/B/C卡")
                .define("c_reader_accepts", "ABC");

        CARD_READER_D_ACCEPTS = BUILDER
                .comment("D级刷卡机可识别的门禁卡等级，例如\"ABCD\"表示识别A/B/C/D卡")
                .define("d_reader_accepts", "ABCD");

        CARD_READER_E_ACCEPTS = BUILDER
                .comment("E级刷卡机可识别的门禁卡等级，例如\"ABCDE\"表示识别所有A-E卡")
                .define("e_reader_accepts", "ABCDE");

        BUILDER.pop();

        BUILDER.push("redstone_transmit");

        REDSTONE_TRANSMIT_RANGE = BUILDER
                .comment("红石信号发射器沿面向方向搜索接收器的最大距离（格），从2格（不含相邻方块）开始遍历到此值")
                .defineInRange("transmit_range", 8, 2, 64);

        BUILDER.pop();

        BUILDER.push("password_inputer");

        PASSWORD_CRACK_TICKS = BUILDER
                .comment("破解密码输入器所需时间（tick），20 tick = 1 秒，默认 400（20 秒）")
                .defineInRange("crack_ticks", 400, 20, 60000);

        BUILDER.pop();

        BUILDER.push("relay_bus");

        RELAY_BUS_MAX_DISTANCE = BUILDER
                .comment("扩展红石继电器总线沿面向方向搜索扩展红石继电器的最大距离（格），紧贴距离为 1，默认 16")
                .defineInRange("max_distance", 16, 1, 64);

        BUILDER.pop();
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int getTimeoutTicks() {
        return SAFE_BUTTON_TIMEOUT_TICKS.get();
    }

    public static int getSignalTicks() {
        return SAFE_BUTTON_SIGNAL_TICKS.get();
    }

    /**
     * 获取圆盘记录仪指定模式的数据更新间隔（tick）
     * mode=1: 被动触发模式，返回 -1（不自动运行）
     * mode=2~9: 从配置中读取
     */
    public static int getPlotterTickInterval(int mode) {
        switch (mode) {
            case 1: return -1;
            case 2: return PLOTTER_MODE2_TICKS.get();
            case 3: return PLOTTER_MODE3_TICKS.get();
            case 4: return PLOTTER_MODE4_TICKS.get();
            case 5: return PLOTTER_MODE5_TICKS.get();
            case 6: return PLOTTER_MODE6_TICKS.get();
            case 7: return PLOTTER_MODE7_TICKS.get();
            case 8: return PLOTTER_MODE8_TICKS.get();
            case 9: return PLOTTER_MODE9_TICKS.get();
            default: return -1;
        }
    }

    /** 刷卡机激活后输出红石信号的持续 tick（默认 20） */
    public static int getCardReaderOnTicks() {
        return CARD_READER_ON_TICKS.get();
    }

    /** 红石信号发射器沿面向方向搜索接收器的最大距离（格，默认 8） */
    public static int getRedstoneTransmitRange() {
        return REDSTONE_TRANSMIT_RANGE.get();
    }

    /** 破解密码输入器所需时间（tick，默认 400 = 20 秒） */
    public static int getPasswordCrackTicks() {
        return PASSWORD_CRACK_TICKS.get();
    }

    /** 扩展红石继电器总线沿面向方向搜索继电器的最大距离（格，默认 16） */
    public static int getRelayBusMaxDistance() {
        return RELAY_BUS_MAX_DISTANCE.get();
    }

    /**
     * 指定等级的刷卡机是否接受指定等级的门禁卡。
     * grade 读取自配置字符串（例如 "ABC"），匹配其中是否包含 cardLetter(A/B/C/D/E)。
     */
    public static boolean isCardAccepted(char readerLevel, char cardLetter) {
        String rule = switch (readerLevel) {
            case 'A' -> CARD_READER_A_ACCEPTS.get();
            case 'B' -> CARD_READER_B_ACCEPTS.get();
            case 'C' -> CARD_READER_C_ACCEPTS.get();
            case 'D' -> CARD_READER_D_ACCEPTS.get();
            case 'E' -> CARD_READER_E_ACCEPTS.get();
            default -> "";
        };
        return rule != null && rule.indexOf(cardLetter) >= 0;
    }
}
