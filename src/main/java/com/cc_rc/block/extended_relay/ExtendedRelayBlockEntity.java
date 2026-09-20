package com.cc_rc.block.extended_relay;

import com.cc_rc.ModBlockEntities;
import dan200.computercraft.core.computer.ComputerSide;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 扩展红石继电器方块实体。
 *
 * 继电器是「总线」的远端红石端口：总线沿面朝方向在任意距离找到继电器后，
 * 通过本实体对它读写红石信号。继电器自身与总线朝向互不相关——处理信号时
 * 只依据**继电器自己的朝向**（FACING）把本地方向（top/bottom/left/right/front/back）
 * 映射为世界方向。
 *
 * 线程模型（CC 外设无延迟改造）：红石状态全部交由 {@link RelayState} 管理——
 *  - Lua 线程读写 internal 输出 / 读输入（ReentrantLock 保护，0 tick，不碰 Level）；
 *  - 主线程每 tick 调 applyOutputsToWorld() 合并应用输出变化到世界（至多一次邻居
 *    更新通知），输入由 neighborChanged 事件 + 方块 tick 刷新写入。
 *
 * side 转换：front = FACING、back = FACING 反向、left/right = FACING 逆/顺时针、
 * top = UP、bottom = DOWN（与 CC 的 DirectionUtil.toLocal 一致）。
 */
public class ExtendedRelayBlockEntity extends BlockEntity {

    /** 线程安全的红石状态（输出双缓存 + 输入 + 锁） */
    private final RelayState state = new RelayState();

    /** 显示名称文字（继承 ITextDisplay 语义；本实体复用可显示文字机制） */
    private Component text = Component.empty();

    public ExtendedRelayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXTENDED_RELAY_BE.get(), pos, state);
    }

    /** 暴露线程安全状态对象（总线按 distance 缓存此引用，Lua 线程直接读写）。 */
    public RelayState getRelayState() {
        return state;
    }

    /** 本地方向 → 世界方向（依据继电器自身 FACING）。 */
    public static Direction toWorldDirection(BlockState state, ComputerSide side) {
        Direction front = state.getValue(HorizontalDirectionalBlock.FACING);
        return switch (side) {
            case FRONT -> front;
            case BACK -> front.getOpposite();
            case LEFT -> front.getCounterClockWise();
            case RIGHT -> front.getClockWise();
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
        };
    }

    /** 世界方向 → 本地方向（依据继电器自身 FACING）。 */
    public static ComputerSide toLocalSide(BlockState state, Direction dir) {
        Direction front = state.getValue(HorizontalDirectionalBlock.FACING);
        if (dir == front) return ComputerSide.FRONT;
        if (dir == front.getOpposite()) return ComputerSide.BACK;
        if (dir == front.getCounterClockWise()) return ComputerSide.LEFT;
        if (dir == front.getClockWise()) return ComputerSide.RIGHT;
        if (dir == Direction.UP) return ComputerSide.TOP;
        return ComputerSide.BOTTOM;
    }

    /**
     * 写入某本地方向的内部输出（总线 Lua 线程调用）。
     * 线程安全：只写锁内缓存并标记待应用，**不触碰世界**——世界应用由
     * 主线程 applyOutputsToWorld() 每 tick 合并执行。
     */
    public void setOutput(ComputerSide side, int power) {
        state.setOutput(side, power);
    }

    /** 读取某本地方向的内部输出（总线 Lua getOutput/getAnalogOutput 用）。 */
    public int getOutput(ComputerSide side) {
        return state.getOutput(side);
    }

    /** 读取某本地方向从世界输入进来的信号强度（总线 Lua getInput/getAnalogInput 用）。 */
    public int getInput(ComputerSide side) {
        return state.getInput(side);
    }

    /** 主线程：把内部输出变化合并应用到世界，返回本 tick 是否有输出变化。 */
    public boolean applyOutputsToWorld(ServerLevel world, BlockState blockState) {
        int mask = state.applyOutputs();
        if (mask == 0) {
            return false;
        }
        // 通知四周邻居重新查询红石信号（红石线/机械不会自动感知 BE 内部输出变化）
        world.updateNeighborsAt(worldPosition, blockState.getBlock());
        // 有变化的方向，其对面邻居也刷新（保障输出方向红石线跨方块传导）
        for (ComputerSide side : ComputerSide.values()) {
            if ((mask & (1 << side.ordinal())) != 0) {
                world.updateNeighborsAt(worldPosition.relative(toWorldDirection(blockState, side)), blockState.getBlock());
            }
        }
        return true;
    }

    /** 重新读取四周世界的红石输入写入状态（neighborChanged 事件 / 方块 tick 调用）。 */
    public void refreshInputs(Level level, BlockState blockState) {
        for (ComputerSide side : ComputerSide.values()) {
            Direction dir = toWorldDirection(blockState, side);
            // 方向语义与 CC RedstoneUtil.getRedstoneInput 一致：getSignal(邻居位置, dir)
            // 的 dir 是"从本方块指向信号源的方向"，即中继器/拉杆等朝本方块输出时
            // 信号源方块收到 direction==dir 的查询（CHANGED：原实现误用 dir.getOpposite()
            // 导致方向相关信号源如红石中继器的输入读不到）
            int value = level.getSignal(getBlockPos().relative(dir), dir);
            state.setInput(side, value);
        }
    }

    /** 是否有任何方向的已应用输出 > 0（方块被移除时判断是否需要刷新邻居）。 */
    public boolean hasAnyOutput() {
        return state.hasAnyAppliedOutput();
    }

    /**
     * 某世界方向的对外输出（方块 getSignal/getDirectSignal 转发，读 applied 层）。
     *
     * 方向语义（用户实测校正，与 refreshInputs 的输入查询恰好对称）：
     *  - 输入（本方块查邻居）：getSignal(邻居, dir)，dir = 本方块指向邻居的方向，不取反；
     *  - 输出（邻居/红石线查本方块）：查询方调用 level.getSignal(本方块, direction) 时，
     *    direction 是"查询方指向本方块"的方向，与本方块实际输出方向相反，故此处取反——
     *    总线 setOutput("front") 写入 internal[FRONT]，relay 前方（FACING 方向）的红石线
     *    从北侧查询时传入 direction=FACING.getOpposite()，取反得 FACING → FRONT 命中 ✓
     *    （v0.0.9 曾误判此处理论上应"不取反"，用户实测前后左右上下全反，遂恢复取反）。
     */
    public int getRedstoneOutput(Direction direction) {
        return state.getAppliedOutput(toLocalSide(getBlockState(), direction.getOpposite()));
    }

    // ---------- 文字显示（兼容 ITextDisplay 风格，供总线/工具读取名称） ----------

    public void setText(Component text) {
        this.text = text == null ? Component.empty() : text;
        setChanged();
        syncNow();
    }

    public Component getText() {
        return text;
    }

    private void syncNow() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ---------- 持久化 ----------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // 输出持久化；输入为瞬态信号不落盘（避免重启读到过期输入）
        state.save(tag);
        if (!text.getString().isEmpty()) {
            tag.putString("Text", Component.Serializer.toJson(text));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        state.load(tag);
        if (tag.contains("Text")) {
            text = Component.Serializer.fromJson(tag.getString("Text"));
        }
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