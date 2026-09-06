package com.cc_rc.block.server_faas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * F.A.A.S服务器
 *
 * 放置：水平四方向（NORTH/SOUTH/EAST/WEST = z- / z+ / x+ / x-），朝向 = 玩家放置时水平朝向（面向玩家）。
 * 三种变体（faas_1/faas_2/faas_3）共用本方块类，仅模型与贴图不同。
 * 音效：靠近时持续循环播放 server_noise。播放逻辑不放在本类（不使用火把/营火的
 * 随机 animateTick 概率播放，避免断续与长音重叠），改由客户端事件监听器
 * com.cc_rc.client.ServerFaasSoundHandler 负责：玩家进入范围时创建并播放循环音效
 * 实例，远离或方块消失时停止。
 * 碰撞箱：完整方块（0,0,0 ~ 16,16,16），模型元素可超出方块边界渲染。
 */
public class ServerFaasBlock extends HorizontalDirectionalBlock {
    public ServerFaasBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }
}