package com.cc_rc.client;

import com.cc_rc.gui.ConsoleConfigScreen;
import com.cc_rc.network.ConsoleConfigResultPacket;
import net.minecraft.client.Minecraft;

/**
 * 控制柜配置加载结果客户端处理器（Dist.CLIENT）。
 *
 * 服务端回执到达时：如果玩家仍停留在配置界面，直接把结果码交给界面显示；
 * 界面已关闭则忽略（数据本身已经同步到方块实体，重新打开即可看到）。
 */
public final class ConsoleConfigClientHandler {

    private ConsoleConfigClientHandler() {
    }

    /** 取走暂存的结果包并派发给当前界面（由网络包通过无参方法引用调用）。 */
    public static void dispatchPending() {
        ConsoleConfigResultPacket packet = ConsoleConfigResultPacket.consumePending();
        if (packet == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ConsoleConfigScreen screen) {
            screen.onServerResult(packet.getResult());
        }
    }
}
