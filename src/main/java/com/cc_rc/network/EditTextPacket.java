package com.cc_rc.network;

import com.cc_rc.block.ITextDisplay;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端→服务端数据包：编辑工具（edit_tool）提交方块显示文字的新内容。
 *
 * 携带目标方块坐标与输入框文本（可能为空）：服务端校验目标方块实体实现了
 * ITextDisplay（可显示名称的方块）后调用 setText 更新文字并同步到客户端。
 * 空文本 = 清除显示。
 */
public class EditTextPacket {

    private final BlockPos pos;
    private final String text;

    public EditTextPacket(BlockPos pos, String text) {
        this.pos = pos;
        this.text = text == null ? "" : text;
    }

    public static void encode(EditTextPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.text, 128);
    }

    public static EditTextPacket decode(FriendlyByteBuf buf) {
        return new EditTextPacket(buf.readBlockPos(), buf.readUtf(128));
    }

    public static void handle(EditTextPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();

            // 服务端权威校验：目标方块实体必须是可显示名称的方块实体
            BlockEntity be = level.getBlockEntity(msg.pos);
            if (!(be instanceof ITextDisplay display)) return;

            // 距离校验（防作弊）：玩家必须离目标足够近才能编辑
            double maxDistSq = 8.0 * 8.0;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > maxDistSq) {
                return;
            }

            // 写入新文字（空文本 = 清空显示；Component.literal 单行文本）
            String normalized = msg.text.replace("\n", "").replace("\r", "").trim();
            display.setText(normalized.isEmpty() ? Component.empty() : Component.literal(normalized));
        });
        ctx.get().setPacketHandled(true);
    }
}