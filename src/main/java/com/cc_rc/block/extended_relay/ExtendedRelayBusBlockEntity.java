package com.cc_rc.block.extended_relay;

import com.cc_rc.Config;
import com.cc_rc.ModBlockEntities;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 扩展红石继电器总线方块实体。
 *
 * 总线是 CC: Tweaked 外设宿主（外设类型 "redstone_relay_bus"，见
 * ExtendedRelayBusPeripheral）。它自身不直接输出红石，而是沿**自己的朝向**
 * （FACING，六方向）在任意距离（紧贴=1，最大可配置，默认 16）搜索扩展红石
 * 继电器，对其读写红石信号：
 *  - setOutput(distance, side, on/off) / setAnalogOutput(distance, side, value)：
 *    找到距离 distance 处的继电器，把信号写到该继电器侧边（侧边为相对
 *    **继电器朝向**的本地方向）；
 *  - getOutput / getAnalogOutput / getInput / getAnalogInput：读取继电器对应侧。
 *
 * 无延迟改造：Lua 线程严禁访问 Level（非线程安全），因此这里维护
 * 「distance → 继电器线程安全状态」快照缓存：
 *  - 主线程每 tick 的 rebuildCache() 顺总线方向探测每个距离，把远端继电器的
 *    {@link RelayState} 收集成不可变 Map（volatile 引用整体替换）；
 *  - Lua 线程读 volatile 引用拿到快照 Map，再对 RelayState 加锁读写
 *    （0 tick，零 Level 访问）。
 * 缓存失效：继电器被拆除/重放/换位后，由主线程下一 tick 重建缓存纠正（≤1 tick）。
 */
public class ExtendedRelayBusBlockEntity extends BlockEntity {

    /** 距离(1~max) → 继电器线程安全状态；主线程每 tick 重建（不可变，volatile 整体替换） */
    private volatile Map<Integer, RelayState> relayCache = Map.of();

    public ExtendedRelayBusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RELAY_BUS_BE.get(), pos, state);
    }

    /** Lua 线程：按距离取继电器状态（读 volatile 快照，零 Level 访问）。 */
    public RelayState getRelayState(int distance) {
        if (distance < 1) return null;
        return relayCache.get(distance);
    }

    /** 主线程 tick：每 tick 重建继电器状态快照缓存（探测有效性 + 绑定状态对象）。 */
    public static void tick(Level level, BlockPos pos, BlockState state, ExtendedRelayBusBlockEntity be) {
        be.rebuildCache();
    }

    /** 顺总线 FACING 方向逐个距离探测并收集继电器状态（主线程调用）。 */
    private void rebuildCache() {
        if (level == null || level.isClientSide) return;
        int max = Config.getRelayBusMaxDistance();
        Direction facing = getBlockState().getValue(ExtendedRelayBusBlock.FACING);
        Map<Integer, RelayState> map = new HashMap<>();
        for (int d = 1; d <= max; d++) {
            BlockPos target = getBlockPos().relative(facing, d);
            // 未加载的区块不缓存（视为无继电器），避免触发区块加载
            if (!level.isLoaded(target)) continue;
            if (level.getBlockEntity(target) instanceof ExtendedRelayBlockEntity relay) {
                map.put(d, relay.getRelayState());
            }
        }
        relayCache = Map.copyOf(map);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
    }
}