package com.cc_rc.client;

import com.cc_rc.CcRc;
import com.cc_rc.network.ConsoleKeypadReleasePacket;
import com.cc_rc.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端：按钮（弹起时间为 0）的「按住直到松手」跟踪。
 *
 * 按住右键时原版每 4 tick 会重复触发一次 use，服务端把按钮标记为持续按下后
 * 重复触发是幂等的，不会产生「弹起又按下」的抖动；
 * 玩家真正松开右键（或打开界面 / 退出世界）时，这里补发一个释放包让按钮弹起。
 */
@Mod.EventBusSubscriber(modid = CcRc.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ConsoleKeypadClient {

    /** 正在按住的按钮所属总线；null 表示没有按住任何「松手弹起」按钮。 */
    private static BlockPos busPos;
    /** 正在按住的按钮组件 id（-1 = 无）。 */
    private static int compId = -1;

    private ConsoleKeypadClient() {
    }

    /**
     * 记录玩家按住了某个「松手弹起」按钮（由 {@link com.cc_rc.block.console.ConsoleCompUse} 在客户端命中时调用）。
     * 切换到另一个按钮时先把上一个释放，避免它一直保持按下。
     */
    public static void press(BlockPos pos, int id) {
        if (compId >= 0 && (compId != id || !pos.equals(busPos))) {
            release();
        }
        busPos = pos.immutable();
        compId = id;
    }

    /** 松开右键、打开界面或退出世界时释放当前按钮。 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || compId < 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null
                || !mc.options.keyUse.isDown()) {
            release();
        }
    }

    /** 立刻结束按住状态并发释放包（没有连接时只清本地状态）。 */
    public static void release() {
        if (compId < 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (busPos != null && mc.getConnection() != null) {
            ModNetwork.CHANNEL.sendToServer(new ConsoleKeypadReleasePacket(busPos, compId));
        }
        busPos = null;
        compId = -1;
    }
}
