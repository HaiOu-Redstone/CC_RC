package com.cc_rc.network;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.block.console.draw.ConsoleShape;
import com.cc_rc.block.console.draw.ConsoleShapeType;
import com.cc_rc.item.DrawToolItem;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * 客户端→服务端：修改一个已存在的绘图图形。
 *
 * 操作类型：
 *  - EDIT：整体改写参数（类型 / 颜色 / 线宽 / 图层 / 两端点 / 预设）；
 *  - MOVE：把图形整体移动到新的锚点（拖动图案时使用）；
 *  - DELETE：删除图形。
 *
 * 图形的创建在画图工具的服务端逻辑中完成，不经过本包。
 * 服务端会再次校验成型状态、坐标范围与预设白名单。
 */
public class ConsoleDrawPacket {

    /** 整体改写图形参数。 */
    public static final int OP_EDIT = 0;
    /** 移动图形到新的锚点。 */
    public static final int OP_MOVE = 1;
    /** 删除图形。 */
    public static final int OP_DELETE = 2;
    /** 新建图形（属性界面里第一次选择类型 / 贴图时发送）。 */
    public static final int OP_CREATE = 3;

    private final BlockPos busPos;
    private final int faceOrd;
    private final int op;
    private final int id;
    private final int layer;
    private final int typeOrd;
    private final int color;
    private final float width;
    private final float u1;
    private final float v1;
    private final float u2;
    private final float v2;
    private final String preset;
    private final float rotation;
    private final float scale;

    private ConsoleDrawPacket(BlockPos busPos, int faceOrd, int op, int id, int layer,
                              int typeOrd, int color, float width, float u1, float v1,
                              float u2, float v2, String preset, float rotation, float scale) {
        this.busPos = busPos;
        this.faceOrd = faceOrd;
        this.op = op;
        this.id = id;
        this.layer = layer;
        this.typeOrd = typeOrd;
        this.color = color;
        this.width = width;
        this.u1 = u1;
        this.v1 = v1;
        this.u2 = u2;
        this.v2 = v2;
        this.preset = preset == null ? "" : preset;
        this.rotation = rotation;
        this.scale = scale;
    }

    /** 整体改写图形参数。 */
    public static ConsoleDrawPacket edit(BlockPos busPos, Direction face, int id, int layer,
                                         ConsoleShapeType type, int color, float width,
                                         float u1, float v1, float u2, float v2,
                                         String preset, float rotation, float scale) {
        return new ConsoleDrawPacket(busPos, face.get3DDataValue(), OP_EDIT, id, layer,
                type.ordinal(), color, width, u1, v1, u2, v2, preset, rotation, scale);
    }

    /** 把图形移动到新的锚点 (u1, v1)。 */
    public static ConsoleDrawPacket move(BlockPos busPos, Direction face, int id,
                                         float u1, float v1) {
        return new ConsoleDrawPacket(busPos, face.get3DDataValue(), OP_MOVE, id, 0,
                0, 0, 0, u1, v1, u1, v1, "", 0, 1);
    }

    /** 删除图形。 */
    public static ConsoleDrawPacket delete(BlockPos busPos, Direction face, int id) {
        return new ConsoleDrawPacket(busPos, face.get3DDataValue(), OP_DELETE, id, 0,
                0, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F, "", 0, 1);
    }

    /** 新建图形（服务端创建后回执新图形的 id）。 */
    public static ConsoleDrawPacket create(BlockPos busPos, Direction face, int layer,
                                           ConsoleShapeType type, int color, float width,
                                           float u1, float v1, float u2, float v2,
                                           String preset, float rotation, float scale) {
        return new ConsoleDrawPacket(busPos, face.get3DDataValue(), OP_CREATE, 0, layer,
                type.ordinal(), color, width, u1, v1, u2, v2, preset, rotation, scale);
    }

    public static void encode(ConsoleDrawPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.busPos);
        buf.writeVarInt(msg.faceOrd);
        buf.writeVarInt(msg.op);
        buf.writeVarInt(msg.id);
        buf.writeVarInt(msg.layer);
        buf.writeVarInt(msg.typeOrd);
        buf.writeInt(msg.color);
        buf.writeFloat(msg.width);
        buf.writeFloat(msg.u1);
        buf.writeFloat(msg.v1);
        buf.writeFloat(msg.u2);
        buf.writeFloat(msg.v2);
        buf.writeUtf(msg.preset, 32);
        buf.writeFloat(msg.rotation);
        buf.writeFloat(msg.scale);
    }

    public static ConsoleDrawPacket decode(FriendlyByteBuf buf) {
        return new ConsoleDrawPacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readInt(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readUtf(32), buf.readFloat(), buf.readFloat());
    }

    public static void handle(ConsoleDrawPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(msg.busPos);
            if (!(be instanceof ConsoleBusBlockEntity bus)) {
                return;
            }
            // 距离校验：防止远处伪造编辑包（以总线方块中心为准）。
            // 这里必须先回执再返回：OP_CREATE 的客户端会先置 creating=true，只认
            // ConsoleDrawCreatedPacket 才复位；静默 return 会让界面上的类型按钮与
            // 预设格永久失效，玩家只能关掉界面重开。
            if (player.distanceToSqr(bus.getBlockPos().getCenter()) > 100.0D) {
                if (msg.op == OP_CREATE) {
                    ModNetwork.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new ConsoleDrawCreatedPacket(msg.busPos, -1));
                }
                return;
            }

            Direction[] faces = Direction.values();
            Direction face = faces[Math.max(0, Math.min(faces.length - 1, msg.faceOrd))];
            int status;
            switch (msg.op) {
                case OP_EDIT -> status = bus.applyDrawEdit(msg.id, msg.layer,
                        ConsoleShapeType.byOrdinal(msg.typeOrd), msg.color, msg.width,
                        msg.u1, msg.v1, msg.u2, msg.v2, msg.preset, msg.rotation, msg.scale);
                case OP_MOVE -> status = bus.applyDrawSetAnchor(msg.id, msg.u1, msg.v1);
                case OP_DELETE -> status = bus.applyDrawDelete(msg.id);
                case OP_CREATE -> {
                    ConsoleShape created = bus.applyDrawAdd(face, msg.layer,
                            ConsoleShapeType.byOrdinal(msg.typeOrd), msg.color, msg.width,
                            msg.u1, msg.v1, msg.u2, msg.v2, msg.preset, msg.rotation,
                            msg.scale);
                    status = created == null
                            ? ConsoleBusBlockEntity.DRAW_BAD_DATA
                            : ConsoleBusBlockEntity.DRAW_OK;
                    // 回执新图形 id（-1 = 创建失败），属性界面据此从「未创建」切换到编辑状态
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new ConsoleDrawCreatedPacket(msg.busPos,
                                    created == null ? -1 : created.id));
                }
                default -> status = ConsoleBusBlockEntity.DRAW_BAD_DATA;
            }
            // 保存 / 创建成功后，把颜色、线宽、缩放、图层记回手上画图工具的 NBT
            if (status == ConsoleBusBlockEntity.DRAW_OK
                    && (msg.op == OP_EDIT || msg.op == OP_CREATE)) {
                DrawToolItem.remember(player, msg.color, msg.width, msg.scale, msg.layer);
            }
            switch (status) {
                case ConsoleBusBlockEntity.DRAW_NOT_FORMED ->
                        hint(player, "message.cc_rc.console.need_form");
                case ConsoleBusBlockEntity.DRAW_LOCKED -> hint(player, "message.cc_rc.draw.locked");
                case ConsoleBusBlockEntity.DRAW_OUTSIDE -> hint(player, "message.cc_rc.draw.outside");
                case ConsoleBusBlockEntity.DRAW_LIMIT -> hint(player, "message.cc_rc.draw.limit");
                case ConsoleBusBlockEntity.DRAW_BAD_DATA -> hint(player, "message.cc_rc.draw.bad");
                default -> {
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void hint(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
