package com.cc_rc.block.console;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 多方块控制台的结构校验工具。
 *
 * 结构定义：若干个「控制台方块」（{@link ConsoleBlock}）在六向相邻关系下连通，
 * 且恰好填满它们的最小外接长方体（实心、无空洞、无凸起）；控制台总线（{@link ConsoleBusBlock}）
 * 必须与其中任一控制台方块相邻，且不能同时连接两个互不相连的结构。
 *
 * 关键简化：不保存成员列表。成型后只需记录「最小角 + 三边长度」，
 * 成员即为该长方体范围内全部控制台方块，可随时推导。
 */
public final class ConsoleStructureHelper {

    /** 结构体积（格）上限：兼顾判定耗时、配置大小与渲染量。 */
    public static final int MAX_VOLUME = 4096;

    private ConsoleStructureHelper() {
    }

    /** 校验失败原因（对应界面红字提示）。 */
    public enum Error {
        /** 校验通过。 */
        NONE,
        /** 总线周围没有控制台方块。 */
        NO_BLOCK,
        /** 总线同时相邻多个互不连通的控制台结构。 */
        MULTIPLE,
        /** 结构体积超过 {@link #MAX_VOLUME}。 */
        TOO_LARGE,
        /** 结构不是实心长方体（有空洞或凸起）。 */
        NOT_RECTANGLE
    }

    /** 校验结果：成功时携带最小角与三边长度。 */
    public static final class Result {
        public final Error error;
        public final BlockPos origin;
        public final int sizeX;
        public final int sizeY;
        public final int sizeZ;
        public final int count;
        /** 结构颜色（由第一个种子方块决定）；校验失败时为默认色。 */
        public final ConsoleColor color;

        private Result(Error error, BlockPos origin, int sizeX, int sizeY, int sizeZ, int count,
                       ConsoleColor color) {
            this.error = error;
            this.origin = origin;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.count = count;
            this.color = color;
        }

        public boolean ok() {
            return this.error == Error.NONE;
        }

        private static Result fail(Error error) {
            return new Result(error, null, 0, 0, 0, 0, ConsoleColor.DEFAULT);
        }
    }

    /**
     * 以总线为中心校验结构。
     *
     * 步骤：
     *  1. 找总线六邻居中的控制台方块作为种子（没有则 NO_BLOCK），并记下其颜色；
     *  2. 从第一个种子六向泛洪收集「同色」的连通控制台方块（超过上限即 TOO_LARGE）；
     *  3. 其余种子若不在同一集合中 → 总线同时连着多个结构，或相邻了异色方块（MULTIPLE）；
     *  4. 求最小外接长方体，体积必须等于收集数量，否则不是实心长方体（NOT_RECTANGLE）。
     */
    public static Result validate(Level level, BlockPos busPos) {
        // 1. 收集种子（总线六邻居中的控制台方块），颜色取第一个种子
        BlockPos first = null;
        ConsoleColor color = ConsoleColor.DEFAULT;
        for (Direction dir : Direction.values()) {
            BlockPos next = busPos.relative(dir);
            BlockState seedState = level.getBlockState(next);
            if (isConsole(seedState)) {
                if (first == null) {
                    first = next;
                    color = colorOf(seedState);
                }
            }
        }
        if (first == null) {
            return Result.fail(Error.NO_BLOCK);
        }

        // 2. 六向泛洪收集所有「同色」连通的控制台方块（异色视为不连通）
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(first);
        queue.add(first);

        int minX = first.getX();
        int minY = first.getY();
        int minZ = first.getZ();
        int maxX = minX;
        int maxY = minY;
        int maxZ = minZ;

        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = cur.relative(dir);
                if (visited.contains(next)) {
                    continue;
                }
                BlockState nextState = level.getBlockState(next);
                if (!isConsole(nextState) || colorOf(nextState) != color) {
                    continue;
                }
                if (visited.size() >= MAX_VOLUME) {
                    // 已经达到上限还在扩张，说明结构太大
                    return Result.fail(Error.TOO_LARGE);
                }
                visited.add(next);
                queue.add(next);
                minX = Math.min(minX, next.getX());
                minY = Math.min(minY, next.getY());
                minZ = Math.min(minZ, next.getZ());
                maxX = Math.max(maxX, next.getX());
                maxY = Math.max(maxY, next.getY());
                maxZ = Math.max(maxZ, next.getZ());
            }
        }

        // 3. 其它种子必须属于同一连通体：异色方块虽相邻但不连通，同样视为 MULTIPLE
        for (Direction dir : Direction.values()) {
            BlockPos next = busPos.relative(dir);
            if (isConsole(level, next) && !visited.contains(next)) {
                return Result.fail(Error.MULTIPLE);
            }
        }

        // 4. 外接长方体必须被完全填满
        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        long volume = (long) sizeX * sizeY * sizeZ;
        if (volume > MAX_VOLUME) {
            return Result.fail(Error.TOO_LARGE);
        }
        if (volume != visited.size()) {
            return Result.fail(Error.NOT_RECTANGLE);
        }

        return new Result(Error.NONE, new BlockPos(minX, minY, minZ),
                sizeX, sizeY, sizeZ, visited.size(), color);
    }

    /**
     * 低频巡检用：外接长方体范围内是否仍全部是同色的控制台方块。
     *
     * @param color 结构颜色（成型时记录）；范围内出现异色方块即视为结构失效
     */
    public static boolean stillValid(Level level, BlockPos origin, int sizeX, int sizeY, int sizeZ,
                                     int count, ConsoleColor color) {
        if (origin == null || sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            return false;
        }
        int found = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin,
                origin.offset(sizeX - 1, sizeY - 1, sizeZ - 1))) {
            BlockState state = level.getBlockState(pos);
            if (!isConsole(state) || colorOf(state) != color) {
                return false;
            }
            found++;
        }
        return found == count;
    }

    /** 判定某坐标是否为本模组的控制台结构方块（含蓝 / 黑颜色变体）。 */
    public static boolean isConsole(Level level, BlockPos pos) {
        return isConsole(level.getBlockState(pos));
    }

    /** 判定方块状态是否为本模组的控制台结构方块（含蓝 / 黑颜色变体）。 */
    public static boolean isConsole(BlockState state) {
        return state.getBlock() instanceof ConsoleBlock;
    }

    /** 控制台方块的颜色；非控制台方块返回默认色。 */
    public static ConsoleColor colorOf(BlockState state) {
        return state.getBlock() instanceof ConsoleBlock block ? block.color : ConsoleColor.DEFAULT;
    }

    /**
     * 找某个控制台方块所属的总线。
     * 优先读取方块实体里记录的 busPos（成型时写入），失效时退回 3x3x3 邻域扫描。
     */
    public static BlockPos findBus(Level level, BlockPos memberPos) {
        if (level.getBlockEntity(memberPos) instanceof ConsoleBlockEntity member
                && member.getBusPos() != null) {
            BlockPos busPos = member.getBusPos();
            if (level.getBlockEntity(busPos) instanceof ConsoleBusBlockEntity) {
                return busPos;
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos pos = memberPos.offset(dx, dy, dz);
                    if (level.getBlockEntity(pos) instanceof ConsoleBusBlockEntity) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    /** 某个控制台方块是否属于「完全成型（锁定）」结构。 */
    public static boolean isLocked(Level level, BlockPos memberPos) {
        BlockPos busPos = findBus(level, memberPos);
        return busPos != null
                && level.getBlockEntity(busPos) instanceof ConsoleBusBlockEntity bus
                && bus.getFormState() == ConsoleBusBlockEntity.State.FULL;
    }

    /** 结构发生变动（方块被放置/破坏）时，通知所属总线回到未成型。 */
    public static void invalidateFor(Level level, BlockPos memberPos) {
        BlockPos busPos = findBus(level, memberPos);
        if (busPos != null && level.getBlockEntity(busPos) instanceof ConsoleBusBlockEntity bus) {
            bus.invalidateStructure();
        }
    }
}
