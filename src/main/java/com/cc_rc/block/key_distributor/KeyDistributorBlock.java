package com.cc_rc.block.key_distributor;

import com.cc_rc.ModItems;
import com.cc_rc.block.key_cabinet.KeyCabinetBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 钥匙分发控制器方块
 * 完整方块（六面同贴图），带方块实体存储已录入的钥匙柜记录列表。
 * 红石逻辑（方块 tick 心跳 + BE 上升沿检测，仅服务端）：
 *   - 信号 ≤5：遍历记录，校验各坐标处是否仍是钥匙柜，不是则删除该条记录（持续执行）；
 *   - 信号从 ≤5 跳变到 >5：只分发一次——记录 ≥2 时随机取两条不同记录，在其对应钥匙柜
 *     前方生成高亮、不会自然消失的钥匙1 / 钥匙2 掉落物（一个脉冲只出 1+2 两把钥匙）。
 */
public class KeyDistributorBlock extends Block implements EntityBlock {

    /** 信号阈值：>5 触发分发，≤5 触发记录校验清理 */
    public static final int TRIGGER_STRENGTH = 5;

    public KeyDistributorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KeyDistributorBlockEntity(pos, state);
    }

    /** 放置或邻居变化时启动 1-tick 心跳（此后在 tick 中持续自调度）。 */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean moved) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 1);
        }
    }

    /** 心跳：驱动 BE 的红石逻辑并持续自调度。 */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide) return;
        if (level.getBlockEntity(pos) instanceof KeyDistributorBlockEntity be) {
            be.onTick();
        }
        level.scheduleTick(pos, this, 1);
    }

    /**
     * 记录校验清理：记录指向的位置不再是钥匙柜时删除该条记录。
     */
    public void validateRecords(Level level, KeyDistributorBlockEntity be) {
        for (KeyCabinetRecord record : be.getRecords()) {
            if (!(level.getBlockState(record.pos()).getBlock() instanceof KeyCabinetBlock)) {
                be.removeRecord(record.pos());
            }
        }
    }

    /**
     * 随机分发：记录 ≥2 时随机取两条不同记录，分别生成钥匙1 / 钥匙2 掉落物。
     */
    public void distributeKeys(Level level, KeyDistributorBlockEntity be) {
        List<KeyCabinetRecord> records = be.getRecords();
        if (records.size() < 2) return;

        List<KeyCabinetRecord> shuffled = new java.util.ArrayList<>(records);
        Collections.shuffle(shuffled, new Random(level.random.nextLong() ^ level.getGameTime()));
        spawnKey(level, shuffled.get(0), new ItemStack(ModItems.KEY_1.get()));
        spawnKey(level, shuffled.get(1), new ItemStack(ModItems.KEY_2.get()));
    }

    /**
     * 在钥匙柜格内柜门方向生成带高亮、不会自然消失的钥匙掉落物：
     * 以钥匙柜所在格中心为基准，向柜门方向（FACING 方向本身，新模型柜门朝外一侧）偏移 0.3 格，落在格内偏柜门侧。
     */
    private void spawnKey(Level level, KeyCabinetRecord record, ItemStack stack) {
        if (!(level.getBlockState(record.pos()).getBlock() instanceof KeyCabinetBlock)) return;
        // 新模型柜门朝 FACING 方向（FACING 指向柜门朝外侧），钥匙落在格内柜门侧
        Direction doorDir = record.facing();
        double x = record.pos().getX() + 0.5D + doorDir.getStepX() * 0.3D;
        double y = record.pos().getY() + 0.5D;
        double z = record.pos().getZ() + 0.5D + doorDir.getStepZ() * 0.3D;
        ItemEntity itemEntity = new ItemEntity(level, x, y, z, stack);
        itemEntity.setDeltaMovement(0.0D, 0.0D, 0.0D); // 无初速度，生成时静止（构造器默认赋随机散布速度）
        itemEntity.setGlowingTag(true);            // 高亮描边属性
        itemEntity.lifespan = Integer.MAX_VALUE;   // 不会自然消失（默认 5 分钟）
        itemEntity.setPickUpDelay(10);
        level.addFreshEntity(itemEntity);
    }
}