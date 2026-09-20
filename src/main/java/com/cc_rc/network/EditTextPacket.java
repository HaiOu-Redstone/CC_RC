package com.cc_rc.network;

import com.cc_rc.block.ITextDisplay;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端→服务端数据包：编辑工具（edit_tool）提交方块显示文字的新内容与样式。
 *
 * 携带目标方块坐标、输入框文本（可能为空）与完整文字样式
 * （颜色 RGB + 粗体/斜体/下划线/删除线）：服务端校验目标方块实体实现了
 * ITextDisplay（可显示名称的方块）后，按文本+样式重建 Component 并调用 setText
 * 更新文字、同步到客户端。空文本 = 清除显示。
 */
public class EditTextPacket {

    private final BlockPos pos;
    private final String text;
    private final int color;                 // RGB 颜色值；-1 = 不设置颜色（恢复默认白）
    private final boolean bold;
    private final boolean italic;
    private final boolean underline;
    private final boolean strikethrough;

    public EditTextPacket(BlockPos pos, String text, int color,
                          boolean bold, boolean italic, boolean underline, boolean strikethrough) {
        this.pos = pos;
        this.text = text == null ? "" : text;
        this.color = color;
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.strikethrough = strikethrough;
    }

    public static void encode(EditTextPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.text, 128);
        buf.writeInt(msg.color);
        buf.writeBoolean(msg.bold);
        buf.writeBoolean(msg.italic);
        buf.writeBoolean(msg.underline);
        buf.writeBoolean(msg.strikethrough);
    }

    public static EditTextPacket decode(FriendlyByteBuf buf) {
        return new EditTextPacket(buf.readBlockPos(), buf.readUtf(128),
                buf.readInt(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean());
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

            // 写入新文字与样式（空文本 = 清空显示）
            String normalized = msg.text.replace("\n", "").replace("\r", "").trim();
            if (normalized.isEmpty()) {
                display.setText(Component.empty());
                return;
            }
            Style style = Style.EMPTY.withBold(msg.bold).withItalic(msg.italic)
                    .withUnderlined(msg.underline).withStrikethrough(msg.strikethrough);
            if (msg.color >= 0) {
                style = style.withColor(TextColor.fromRgb(msg.color));
            }
            display.setText(Component.literal(normalized).withStyle(style));
        });
        ctx.get().setPacketHandled(true);
    }
}