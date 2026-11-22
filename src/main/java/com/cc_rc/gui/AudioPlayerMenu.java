package com.cc_rc.gui;

import com.cc_rc.ModMenuTypes;
import com.cc_rc.block.audio_player.AudioPlayerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

/**
 * 音频播放器设置菜单（无物品槽位，仅作为打开 AudioPlayerScreen 的载体）。
 *
 * 打开流程与海报一致：
 *  - 服务端在 AudioPlayerBlock.use 中调用 {@link #open(ServerPlayer, AudioPlayerBlockEntity)}，
 *    把方块坐标与当前配置写进同步数据；
 *  - 客户端由 IForgeMenuType 从同步数据还原配置，AudioPlayerScreen 据此初始化输入框；
 *  - 「保存」时由 Screen 发送 AudioPlayerConfigPacket（C2S）写回方块实体。
 */
public class AudioPlayerMenu extends AbstractContainerMenu {

    /** 目标方块坐标（服务端/客户端一致）。 */
    public final BlockPos pos;
    /** 打开时的配置快照（作为界面初始值）。 */
    public final AudioPlayerBlockEntity.Settings settings;

    public AudioPlayerMenu(int windowId, BlockPos pos, AudioPlayerBlockEntity.Settings settings) {
        super(ModMenuTypes.AUDIO_PLAYER.get(), windowId);
        this.pos = pos;
        this.settings = settings;
    }

    /** 服务端打开：把方块实体的当前配置写进同步数据。 */
    public static void open(ServerPlayer player, AudioPlayerBlockEntity be) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (windowId, inventory, p) -> new AudioPlayerMenu(windowId, be.getBlockPos(), be.getSettings()),
                Component.translatable("gui.cc_rc.audio_player.title")),
                buf -> {
                    buf.writeBlockPos(be.getBlockPos());
                    AudioPlayerBlockEntity.Settings s = be.getSettings();
                    buf.writeUtf(s.soundId(), AudioPlayerBlockEntity.MAX_SOUND_ID_LENGTH);
                    buf.writeVarInt(s.mode());
                    buf.writeFloat(s.range());
                    buf.writeVarInt(s.attenuation());
                    buf.writeFloat(s.start());
                    buf.writeFloat(s.stop());
                    buf.writeFloat(s.interval());
                    buf.writeVarInt(s.volume());
                });
    }

    @Override
    public boolean stillValid(Player player) {
        // 写入校验在网络包服务端处理（距离 + 方块类型）
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
