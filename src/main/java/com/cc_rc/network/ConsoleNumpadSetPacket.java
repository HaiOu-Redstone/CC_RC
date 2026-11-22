package com.cc_rc.network;

import com.cc_rc.block.console.ConsoleBusBlockEntity;
import com.cc_rc.block.console.ConsoleSurface;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端→服务端：数字键盘（numpad）提交输入数值。
 *
 * 玩家空手右键数字键盘打开输入界面，点击「确认」后由本包把数值发给服务端；
 * 「放弃」/ Esc 只清空输入框并关闭界面，不发包，组件数值保持不变。
 * 服务端会重新校验成型状态、组件类型与玩家距离后写入，并回写绑定的浮点变量。
 */
public class ConsoleNumpadSetPacket {

    private final BlockPos busPos;
    private final int compId;
    private final float value;

    public ConsoleNumpadSetPacket(BlockPos busPos, int compId, float value) {
        this.busPos = busPos;
        this.compId = compId;
        this.value = value;
    }

    public static void encode(ConsoleNumpadSetPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.busPos);
        buf.writeVarInt(msg.compId);
        buf.writeFloat(msg.value);
    }

    public static ConsoleNumpadSetPacket decode(FriendlyByteBuf buf) {
        return new ConsoleNumpadSetPacket(buf.readBlockPos(), buf.readVarInt(), buf.readFloat());
    }

    public static void handle(ConsoleNumpadSetPacket msg, Supplier<NetworkEvent.Context> ctx) {
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
            ConsoleBusBlockEntity.CompEntry existing = bus.findComp(msg.compId);
            if (existing == null || bus.getOrigin() == null) {
                return;
            }
            // 距离校验：以服务端保存的组件位置为准，防止远处伪造包
            Vec3 point = ConsoleSurface.toWorld(bus.getOrigin(), bus.getSizeX(), bus.getSizeY(),
                    bus.getSizeZ(), existing.face, bus.getBusFacing(),
                    existing.u, existing.v, 0.0D);
            if (player.distanceToSqr(point) > 64.0D) {
                return;
            }
            bus.applyNumpadSet(msg.compId, msg.value);
        });
        ctx.get().setPacketHandled(true);
    }
}
