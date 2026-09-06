package com.cc_rc.network;

import com.cc_rc.entity.BaoZi;
import com.cc_rc.item.BaoZiItem;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端→服务端数据包：玩家左键投掷包子。
 * 服务端校验主手物品是否为 bao_zi 后，生成弹射物并消耗 1 个包子。
 * 带 0.5 秒（10 tick）投掷冷却：冷却期间忽略请求，防止连续投掷。
 */
public class ThrowBaoZiPacket {
    // 无字段数据，仅作为触发信号

    public static void encode(ThrowBaoZiPacket msg, FriendlyByteBuf buf) {
    }

    public static ThrowBaoZiPacket decode(FriendlyByteBuf buf) {
        return new ThrowBaoZiPacket();
    }

    public static void handle(ThrowBaoZiPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof BaoZiItem)) return;

            // 投掷冷却（权威校验，防作弊）：冷却中直接忽略本次投掷
            ItemCooldowns cooldowns = player.getCooldowns();
            if (cooldowns.isOnCooldown(stack.getItem())) return;
            cooldowns.addCooldown(stack.getItem(), BaoZiItem.THROW_COOLDOWN_TICKS);

            // 生成包子弹射物并朝玩家视线方向投出（初速 1.5，与雪球一致）
            BaoZi projectile = new BaoZi(player.level(), player);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(),
                    0.0F, 1.5F, 1.0F);
            player.level().addFreshEntity(projectile);

            // 投掷消耗 1 个包子（创造模式不消耗）
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
