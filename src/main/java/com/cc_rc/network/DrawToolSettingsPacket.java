package com.cc_rc.network;

import com.cc_rc.block.console.draw.ConsoleDrawPresets;
import com.cc_rc.block.console.draw.ConsoleShapeType;
import com.cc_rc.item.DrawToolItem;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端→服务端：保存画图工具的设置。
 *
 * 设置（颜色 / 线宽 / 缩放 / 图层 / 类型 / 贴图 / 快速绘画 / 吸附整数 / 平直线优先）
 * 保存在画图工具物品的 NBT 上；快速绘画时服务端直接读取这份 NBT 生成图案，
 * 因此界面改完必须同步到服务端。服务端只写玩家手上真实持有的画图工具。
 */
public class DrawToolSettingsPacket {

    private final int color;
    private final float width;
    private final float scale;
    private final int layer;
    private final int typeOrd;
    private final String preset;
    private final boolean quick;
    private final boolean snap;
    private final boolean straight;

    public DrawToolSettingsPacket(DrawToolItem.Settings settings) {
        this.color = settings.color();
        this.width = settings.width();
        this.scale = settings.scale();
        this.layer = settings.layer();
        this.typeOrd = settings.type().ordinal();
        this.preset = settings.preset();
        this.quick = settings.quick();
        this.snap = settings.snap();
        this.straight = settings.straight();
    }

    private DrawToolSettingsPacket(int color, float width, float scale, int layer,
                                   int typeOrd, String preset, boolean quick,
                                   boolean snap, boolean straight) {
        this.color = color;
        this.width = width;
        this.scale = scale;
        this.layer = layer;
        this.typeOrd = typeOrd;
        this.preset = preset == null ? "" : preset;
        this.quick = quick;
        this.snap = snap;
        this.straight = straight;
    }

    public static void encode(DrawToolSettingsPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.color);
        buf.writeFloat(msg.width);
        buf.writeFloat(msg.scale);
        buf.writeVarInt(msg.layer);
        buf.writeVarInt(msg.typeOrd);
        buf.writeUtf(msg.preset, 32);
        buf.writeBoolean(msg.quick);
        buf.writeBoolean(msg.snap);
        buf.writeBoolean(msg.straight);
    }

    public static DrawToolSettingsPacket decode(FriendlyByteBuf buf) {
        return new DrawToolSettingsPacket(buf.readInt(), buf.readFloat(), buf.readFloat(),
                buf.readVarInt(), buf.readVarInt(), buf.readUtf(32),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(DrawToolSettingsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ItemStack stack = DrawToolItem.findHeld(player);
            if (stack.isEmpty()) {
                return;
            }
            // 贴图 id 必须在服务端白名单内
            String preset = ConsoleDrawPresets.byId(msg.preset) == null
                    ? ConsoleDrawPresets.DEFAULT_ID : msg.preset;
            DrawToolItem.writeSettings(stack, new DrawToolItem.Settings(msg.color & 0xFFFFFF,
                    msg.width, msg.scale, msg.layer,
                    ConsoleShapeType.byOrdinal(msg.typeOrd), preset,
                    msg.quick, msg.snap, msg.straight));
        });
        ctx.get().setPacketHandled(true);
    }
}
