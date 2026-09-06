package com.mao.barbequesdelight.init.registrate;

import com.mao.barbequesdelight.content.block.*;
import com.mao.barbequesdelight.init.BarbequesDelight;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.xkmc.l2modularblock.BlockProxy;
import dev.xkmc.l2modularblock.DelegateBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.client.model.generators.ModelFile;

public class BBQDBlocks {

	public static final BlockEntry<DelegateBlock> BASIN = BarbequesDelight.REGISTRATE.block("basin", p -> DelegateBlock.newBaseBlock(
					BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS),
					BlockProxy.HORIZONTAL, ClickStorageMethod.INS, new BasinBlock(), BasinBlock.TE
			)).blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(), pvd.models().getBuilder("block/basin")
					.parent(new ModelFile.UncheckedModelFile(BarbequesDelight.loc("custom/basin")))
					.renderType("cutout")
			)).simpleItem().defaultLoot().tag(BlockTags.MINEABLE_WITH_AXE)
			.lang("Ingredients Basin").register();

	public static final BlockEntityEntry<BasinBlockEntity> TE_BASIN = BarbequesDelight.REGISTRATE
			.blockEntity("basin", BasinBlockEntity::new).validBlock(BASIN)
			.renderer(() -> StorageTileRenderer::new).register();


	public static final BlockEntry<DelegateBlock> TRAY = BarbequesDelight.REGISTRATE.block("tray", p -> DelegateBlock.newBaseBlock(
			BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS),
			BlockProxy.HORIZONTAL, ClickStorageMethod.INS, new TrayBlock(), TrayBlock.TE
	)).blockstate((ctx, pvd) -> {
		var base = pvd.models().getBuilder("block/tray")
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/tray")));
		var tray = pvd.models().getBuilder("block/tray_support")
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/tray_support")));
		pvd.horizontalBlock(ctx.get(), s -> s.getValue(TrayBlock.SUPPORT) ? tray : base, 90);
	}).simpleItem().defaultLoot().tag(BlockTags.MINEABLE_WITH_AXE).register();


	public static final BlockEntityEntry<TrayBlockEntity> TE_TRAY = BarbequesDelight.REGISTRATE
			.blockEntity("tray", TrayBlockEntity::new).validBlock(TRAY)
			.renderer(() -> StorageTileRenderer::new).register();


	public static void register() {
	}

}
