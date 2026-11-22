package com.cc_rc.network;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
import io.netty.handler.codec.DecoderException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * 客户端→服务端：上传一份控制柜配置文件内容（JSON 文本）。
 *
 * 服务器环境下配置文件保存在玩家客户端电脑，加载时由客户端读出后通过本包上传；
 * 服务端只信任自己这一侧的校验结果（成型状态 / 尺寸 / 朝向 / 内容合法性），
 * 校验通过后整体替换总线数据，并把结果码回执给客户端界面。
 */
public class ConsoleConfigUploadPacket {

    /** JSON 文本允许的最大字节数（UTF-8），防止异常包占用过多内存。 */
    public static final int MAX_JSON_BYTES = 1_048_576;

    private final BlockPos pos;
    private final String json;

    public ConsoleConfigUploadPacket(BlockPos pos, String json) {
        this.pos = pos;
        this.json = json == null ? "" : json;
    }

    public static void encode(ConsoleConfigUploadPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        // 直接写 UTF-8 字节串：writeUtf 对编码后 32 KiB 长度有限制，大配置会被拒绝
        byte[] data = msg.json.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(data.length);
        buf.writeBytes(data);
    }

    public static ConsoleConfigUploadPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int length = buf.readInt();
        if (length < 0 || length > MAX_JSON_BYTES) {
            throw new DecoderException("Console config JSON too large: " + length);
        }
        byte[] data = new byte[length];
        buf.readBytes(data);
        return new ConsoleConfigUploadPacket(pos, new String(data, StandardCharsets.UTF_8));
    }

    public static void handle(ConsoleConfigUploadPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(msg.pos);
            // 每条失败路径都必须回执：客户端 sendConfig() 会置 waiting=true 并禁用
            // save/load/back 三个按钮，只有收到 ConsoleConfigResultPacket 才会复位。
            // 静默 return 会让界面永久锁在「已上传，等待服务器校验…」，只能 Esc 退出。
            if (!(be instanceof ConsoleBusBlockEntity bus)) {
                reply(player, msg.pos, ConsoleBusBlockEntity.CFG_INVALID);
                return;
            }
            // 距离校验：防止远程改写别人的控制柜
            if (player.distanceToSqr(Vec3.atCenterOf(msg.pos)) > 256.0D) {
                reply(player, msg.pos, ConsoleBusBlockEntity.CFG_INVALID);
                return;
            }
            int result = bus.applyConfigJson(msg.json);
            // 回执给该玩家客户端：界面据此显示成功 / 失败原因
            reply(player, msg.pos, result);
        });
        ctx.get().setPacketHandled(true);
    }

    /** 把处理结果回执给发起请求的玩家（客户端据此解除等待状态）。 */
    private static void reply(ServerPlayer player, net.minecraft.core.BlockPos pos, int result) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ConsoleConfigResultPacket(pos, result));
    }
}
