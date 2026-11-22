package com.cc_rc.gui;

import com.cc_rc.ModMenuTypes;
import com.cc_rc.entity.NoteEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

/**
 * 便签编辑菜单（无物品槽位，仅作为打开 NoteScreen 的载体）。
 *
 * 打开流程：服务端在右键便签（NoteEntity#interact）或放置后调用
 * {@link #open(ServerPlayer, NoteEntity)}，把实体 id 与四行文字标签写进菜单同步数据；
 * 客户端 NoteScreen 据此初始化四个输入框与颜色 / 格式；「完成」时发送
 * NoteConfigPacket（C2S）写回实体。
 */
public class NoteMenu extends AbstractContainerMenu {

    /** 目标便签实体 id。 */
    public final int entityId;
    /** 当前四行文字标签（文字 / 颜色 / 格式）。 */
    public final CompoundTag data;

    public NoteMenu(int windowId, int entityId, CompoundTag data) {
        super(ModMenuTypes.NOTE.get(), windowId);
        this.entityId = entityId;
        this.data = data == null ? NoteEntity.emptyTag() : data;
    }

    /** 服务端打开：把实体 id 与文字标签写进同步数据。 */
    public static void open(ServerPlayer player, NoteEntity note) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (windowId, inventory, p) -> new NoteMenu(windowId, note.getId(), note.getTextTag()),
                Component.translatable("entity.cc_rc.note")),
                buf -> {
                    buf.writeVarInt(note.getId());
                    buf.writeNbt(note.getTextTag());
                });
    }

    @Override
    public boolean stillValid(Player player) {
        // 写入校验在网络包服务端处理（实体类型 + 距离）
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
