package com.mao.barbequesdelight.content.block;

import com.mao.barbequesdelight.init.registrate.BBQDBlocks;
import dev.xkmc.l2modularblock.impl.BlockEntityBlockMethodImpl;
import dev.xkmc.l2modularblock.mult.CreateBlockStateBlockMethod;
import dev.xkmc.l2modularblock.mult.PlacementBlockMethod;
import dev.xkmc.l2modularblock.mult.ShapeUpdateBlockMethod;
import dev.xkmc.l2modularblock.mult.SurviveBlockMethod;
import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import dev.xkmc.l2modularblock.type.BlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class TrayBlock implements ShapeBlockMethod, SurviveBlockMethod, ShapeUpdateBlockMethod, CreateBlockStateBlockMethod, PlacementBlockMethod {

	public static final VoxelShape OUTER = Block.box(0, 0, 0, 16, 3, 16);

	private static final VoxelShape SHAPE = Shapes.join(OUTER,
			Block.box(1, 1, 1, 15, 3, 15),
			BooleanOp.ONLY_FIRST);

	public static final BooleanProperty SUPPORT = BooleanProperty.create("support");

	public static final BlockMethod TE = new BlockEntityBlockMethodImpl<>(BBQDBlocks.TE_TRAY, TrayBlockEntity.class);

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SUPPORT);
	}

	@Override
	public BlockState getStateForPlacement(BlockState state, BlockPlaceContext ctx) {
		return state.setValue(SUPPORT, !supported(ctx.getLevel(), ctx.getClickedPos()));
	}

	private Boolean supported(LevelReader level, BlockPos pos) {
		return Block.canSupportRigidBlock(level, pos.below());
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		return supported(level, pos) || below.is(state.getBlock());
	}

	@Override
	public BlockState updateShape(Block block, BlockState current, BlockState oldState, Direction facing, BlockState neiState, LevelAccessor level, BlockPos pos, BlockPos neiPos) {
		if (facing.getAxis().equals(Direction.Axis.Y)) {
			if (supported(level, pos)) {
				return current.setValue(SUPPORT, false);
			}
			if (level.getBlockState(pos.below()).is(block)) {
				return current.setValue(SUPPORT, true);
			}
			return Blocks.AIR.defaultBlockState();
		}
		return current;
	}

	@Nullable
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

}
