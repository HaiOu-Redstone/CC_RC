package com.cc_rc.block.data_unit;

import com.cc_rc.ModBlockEntities;
import com.cc_rc.block.ITextDisplay;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 数据单元方块实体
 *
 * 存储两类数据：
 *   - 名称（字符串，默认空；可通过编辑工具右键修改，即 ITextDisplay 的 text）
 *   - 数据（整数列表，初始为空；支持 CC 外设读写整个列表 / 读写某一位）
 * 通过 NBT 持久化，支持网络同步。
 *
 * 线程模型（CC 外设无延迟改造，与数字调节器/圆盘记录仪一致）：
 *   - name / data 均为 volatile 引用，Lua 线程（CC WorkerThread）可直读直写（0 tick）；
 *   - 写操作采用「写时复制」——列表只新建副本原子替换引用，永不修改共享数组；
 *   - 写后只置 dirty（volatile），由主线程 tick 节流合并「存档 + 客户端同步」
 *     （每 tick 至多一次），避免 Lua 高频写造成同步包风暴。
 */
public class DataUnitBlockEntity extends BlockEntity implements ITextDisplay {
    /** 列表长度上限（防 Lua 写入超长列表导致存档膨胀） */
    public static final int MAX_LIST_LENGTH = 1024;

    /** 名称（volatile：Lua 线程写、主线程存档/同步读） */
    private volatile String name = "";
    /** 数据列表（volatile 引用 + 写时复制，读方拿到不可变快照） */
    private volatile int[] data = new int[0];
    /** 待广播标记（volatile：Lua 线程置位，主线程 tick 消费并清除） */
    private volatile boolean dirty = false;

    public DataUnitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DATA_UNIT_BE.get(), pos, state);
    }

    /** 读取名称（字符串） */
    public String getName() {
        return name;
    }

    /** 设置名称（线程安全：仅写 volatile 字段并置脏） */
    public void setName(String name) {
        this.name = name == null ? "" : name;
        dirty = true;
    }

    /** ITextDisplay：编辑工具右键修改名称 */
    @Override
    public void setText(Component text) {
        setName(text == null ? "" : text.getString());
    }

    /** ITextDisplay：读取名称 */
    @Override
    public Component getText() {
        return Component.literal(name);
    }

    /** 读取整个列表（返回当前不可变快照） */
    public int[] getData() {
        return data;
    }

    /** 读取列表长度 */
    public int size() {
        return data.length;
    }

    /** 替换整个列表（长度超上限返回 false，不修改；写时复制） */
    public boolean setList(int[] list) {
        if (list == null || list.length > MAX_LIST_LENGTH) {
            return false;
        }
        data = list.clone();
        dirty = true;
        return true;
    }

    /**
     * 设置列表某一位（Lua 索引 1 起，内部 0 起）：
     *   - 索引在现有范围内：直接覆盖该位
     *   - 索引 == 列表长度：追加一位
     *   - 索引超出或列表已达上限：返回 false 不修改
     * 写时复制；中间空缺位补 0。
     */
    public boolean setValue(int index, int value) {
        if (index < 0 || index >= MAX_LIST_LENGTH || index > data.length) {
            return false;
        }
        int[] copy = Arrays.copyOf(data, Math.max(data.length, index + 1));
        copy[index] = value;
        data = copy;
        dirty = true;
        return true;
    }

    /** 读取列表某一位（索引越界返回 null） */
    @Nullable
    public Integer getValue(int index) {
        if (index < 0 || index >= data.length) {
            return null;
        }
        return data[index];
    }

    /**
     * 主线程 tick：dirty 时合并广播——每 tick 至多一次「标记存档 + 客户端同步」。
     * 写序约束：字段写在前、dirty=true 在后（volatile happens-before）。
     */
    public static void tick(Level level, BlockPos pos, BlockState state, DataUnitBlockEntity be) {
        if (be.dirty) {
            be.dirty = false;
            be.setChanged();
            if (!level.isClientSide) {
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!name.isEmpty()) {
            tag.putString("Name", name);
        }
        tag.putIntArray("Data", data);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        name = tag.contains("Name") ? tag.getString("Name") : "";
        int[] saved = tag.getIntArray("Data");
        if (saved.length > MAX_LIST_LENGTH) {
            saved = Arrays.copyOf(saved, MAX_LIST_LENGTH);
        }
        data = saved;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }
}
