package com.cc_rc.client;

import com.cc_rc.gui.WaterCycleChartScreen;
import net.minecraft.client.Minecraft;

/**
 * 水循环图的客户端辅助方法（只有客户端会加载本类）。
 * 独立出来是为了让通用代码通过 DistExecutor 调用，避免专用服务器触碰 Minecraft 类。
 */
public final class WaterCycleChartClient {

    private WaterCycleChartClient() {
    }

    /** 打开高清查看界面。 */
    public static void open() {
        Minecraft.getInstance().setScreen(new WaterCycleChartScreen());
    }
}
