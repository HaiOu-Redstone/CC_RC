package com.cc_rc.client;

import com.cc_rc.CcRc;
import com.cc_rc.item.BaoZiItem;
import com.cc_rc.network.ModNetwork;
import com.cc_rc.network.ThrowBaoZiPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端输入拦截：当玩家主手持 bao_zi 且按下攻击键（左键）时，
 * 取消默认攻击行为并发送 C2S 数据包通知服务端投掷包子。
 * 事件在 Minecraft.startAttack 的 while(keyAttack.consumeClick()) 循环中触发，每次点击触发一次。
 */
@Mod.EventBusSubscriber(modid = CcRc.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientInputHandler {
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof BaoZiItem) {
            // 取消默认左键攻击/挖掘，仅触发投掷（仍保留挥动手臂动画）
            event.setCanceled(true);
            // 冷却中不发送投掷数据包（服务端仍有权威二次校验）
            if (!player.getCooldowns().isOnCooldown(stack.getItem())) {
                ModNetwork.CHANNEL.sendToServer(new ThrowBaoZiPacket());
            }
        }
    }
}
