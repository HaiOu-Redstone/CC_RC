package com.cc_rc.block.poster;

import com.cc_rc.client.WaterCycleChartClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * 水循环图（独立贴墙物品，固定 2 格宽）。
 *
 * 与海报的区别：
 *  - 墙面只用低清贴图（512x362）由方块模型直接渲染，固定 2 格宽、按原图比例 2x1.414 格，
 *    不做海报那套图片索引 / 缩放 / 偏移，因此不需要方块实体；
 *  - 右键打开高清查看界面（WaterCycleChartScreen），界面打开时才把 5000x3536 高清图上传显卡，
 *    关闭即释放，避免高清贴图常驻显存。
 *
 * 形状 / 贴墙判定 / 四向放置直接复用 PosterBlock。
 */
public class WaterCycleChartBlock extends PosterBlock {

    public WaterCycleChartBlock(Properties properties) {
        super(properties);
    }

    /** 无方块实体：图片与位置都由方块模型固定表现。 */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    /** 使用普通方块模型渲染（PosterBlock 默认 INVISIBLE，是给海报 BER 用的）。 */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** 右键打开高清查看界面（仅客户端；无服务端状态，因此不发送网络包）。 */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            // 通过 DistExecutor 间接调用客户端类，避免服务端加载 Minecraft 相关类
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> WaterCycleChartClient::open);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
