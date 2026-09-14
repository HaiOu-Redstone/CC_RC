package com.cc_rc.block.extended_relay;

import dan200.computercraft.core.computer.ComputerSide;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.nbt.CompoundTag;

/**
 * 扩展红石继电器的线程安全状态（仿 CC-Tweaked RedstoneState 双缓存，无延迟改造核心）。
 *
 * 布局：
 *  - internalOutputs[6]：Lua 线程（CC WorkerThread）可读写的「内部输出」——
 *    uint 外设方法不加 mainThread 时仍可线程安全直读直写（ReentrantLock 保护），0 tick；
 *  - appliedOutputs[6] ：主线程「已应用到世界」的输出，方块 getSignal 读取；
 *  - inputs[6]         ：主线程从世界读入的信号（neighborChanged 事件驱动 / 方块 tick
 *    刷新），Lua 线程锁内读取；
 *  - pendingChange     ：internal 相对 applied 是否有待应用变化（volatile 快速判断）。
 *
 * 世界同步：主线程每 tick 调 applyOutputs()，把变化合并应用到 appliedOutputs，
 * 返回已变化面的位掩码（决定 updateNeighborsAt 去向）。因此：
 *  Lua 高频写不会触碰 Level/世界（Level 非线程安全），主线程每 tick 至多一次合并，
 *  广播频率被节流，杜绝同步包/红石更新风暴。
 *
 * 持久化：输出（outputs）才是持久意愿，输入（inputs）是瞬态世界信号不落盘——
 * 避免存档后重读旧输入（原实现曾把 Inputs 落盘导致重启读到过期信号）。
 */
public class RelayState {
    private final ReentrantLock lock = new ReentrantLock();
    private final int[] internalOutputs = new int[ComputerSide.COUNT];
    private final int[] appliedOutputs = new int[ComputerSide.COUNT];
    private final int[] inputs = new int[ComputerSide.COUNT];
    private volatile boolean pendingChange = false;

    /** Lua 线程：读取某本地方向的内部输出（写后立即读＝新值）。 */
    public int getOutput(ComputerSide side) {
        lock.lock();
        try {
            return internalOutputs[side.ordinal()];
        } finally {
            lock.unlock();
        }
    }

    /** Lua 线程：写入某本地方向的内部输出（不碰世界，仅标记待应用）。 */
    public void setOutput(ComputerSide side, int power) {
        lock.lock();
        try {
            internalOutputs[side.ordinal()] = power;
            pendingChange = true;
        } finally {
            lock.unlock();
        }
    }

    /** Lua 线程：读取某本地方向从世界收到的输入。 */
    public int getInput(ComputerSide side) {
        lock.lock();
        try {
            return inputs[side.ordinal()];
        } finally {
            lock.unlock();
        }
    }

    /** 主线程：写入某本地方向从世界收到的输入（事件驱动/每 tick 刷新）。 */
    public void setInput(ComputerSide side, int power) {
        lock.lock();
        try {
            inputs[side.ordinal()] = power;
        } finally {
            lock.unlock();
        }
    }

    /** 主线程：是否有待应用的输出变化（每 tick 快速判断）。 */
    public boolean hasPendingChange() {
        return pendingChange;
    }

    /**
     * 主线程：把 internal 输出合并应用到 applied 输出。
     *
     * @return 已变化面的位掩码（bit = side.ordinal()，与 ComputerSide 序号一致）
     */
    public int applyOutputs() {
        lock.lock();
        try {
            if (!pendingChange) {
                return 0;
            }
            pendingChange = false;
            int mask = 0;
            for (int i = 0; i < ComputerSide.COUNT; i++) {
                if (appliedOutputs[i] != internalOutputs[i]) {
                    appliedOutputs[i] = internalOutputs[i];
                    mask |= (1 << i);
                }
            }
            return mask;
        } finally {
            lock.unlock();
        }
    }

    /** 主线程：方块 getSignal/getDirectSignal 读取的实际输出（方向反向语义由调用方换算）。 */
    public int getAppliedOutput(ComputerSide side) {
        lock.lock();
        try {
            return appliedOutputs[side.ordinal()];
        } finally {
            lock.unlock();
        }
    }

    /** 是否有任何方向的已应用输出 > 0（方块被移除时判断是否需要刷新邻居）。 */
    public boolean hasAnyAppliedOutput() {
        lock.lock();
        try {
            for (int i = 0; i < ComputerSide.COUNT; i++) {
                if (appliedOutputs[i] > 0) return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /** 持久化：只保存输出（输入为瞬态信号不落盘）。 */
    public void save(CompoundTag tag) {
        lock.lock();
        try {
            tag.putIntArray("Outputs", Arrays.copyOf(appliedOutputs, appliedOutputs.length));
        } finally {
            lock.unlock();
        }
    }

    /** 从存档加载输出；输入清零，等待首个主线程 tick 从世界重新刷新。 */
    public void load(CompoundTag tag) {
        lock.lock();
        try {
            Arrays.fill(internalOutputs, 0);
            Arrays.fill(appliedOutputs, 0);
            int[] saved = tag.getIntArray("Outputs");
            if (saved.length == ComputerSide.COUNT) {
                System.arraycopy(saved, 0, appliedOutputs, 0, ComputerSide.COUNT);
                System.arraycopy(saved, 0, internalOutputs, 0, ComputerSide.COUNT);
            }
            Arrays.fill(inputs, 0);
            pendingChange = false;
        } finally {
            lock.unlock();
        }
    }
}