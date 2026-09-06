package com.mao.barbequesdelight.content.block;

import com.mao.barbequesdelight.init.registrate.BBQDBlocks;
import dev.xkmc.l2modularblock.BlockProxy;
import dev.xkmc.l2modularblock.impl.BlockEntityBlockMethodImpl;
import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import dev.xkmc.l2modularblock.type.BlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BasinBlock implements ShapeBlockMethod {

	public static final BlockMethod TE = new BlockEntityBlockMethodImpl<>(BBQDBlocks.TE_BASIN, BasinBlockEntity.class);

	public static final VoxelShape OUTER, SHAPE_X, SHAPE_Z;

	static {
		OUTER = Block.box(1, 0, 1, 15, 4, 15);
		SHAPE_Z = Shapes.join(OUTER,
				Shapes.or(Block.box(2, 1, 2, 7.5, 4, 14),
						Block.box(8.5, 1, 2, 14, 4, 14)),
				BooleanOp.ONLY_FIRST);

		SHAPE_X = Shapes.join(OUTER,
				Shapes.or(Block.box(2, 1, 2, 14, 4, 7.5),
						Block.box(2, 1, 8.5, 14, 4, 14)),
				BooleanOp.ONLY_FIRST);

	}

	@Nullable
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return state.getValue(BlockProxy.HORIZONTAL_FACING).getAxis() == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
	}

}
