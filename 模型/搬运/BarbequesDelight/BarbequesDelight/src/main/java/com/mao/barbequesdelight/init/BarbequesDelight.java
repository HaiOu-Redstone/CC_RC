package com.mao.barbequesdelight.init;

import com.mao.barbequesdelight.init.registrate.BBQDBlocks;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.xkmc.l2library.base.L2Registrate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BarbequesDelight.MODID)
@Mod.EventBusSubscriber(modid = BarbequesDelight.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BarbequesDelight {

	public static final String MODID = "barbequesdelight";
	public static final L2Registrate REGISTRATE = new L2Registrate(MODID);
	public static final Logger LOGGER = LoggerFactory.getLogger(BarbequesDelight.class);

	public static final RegistryEntry<CreativeModeTab> TAB = REGISTRATE.buildModCreativeTab("main", "Barbeque's Delight",
			e -> e.icon(BBQDBlocks.BASIN::asStack));

	public BarbequesDelight() {
		BBQDBlocks.register();
	}

	public static ResourceLocation loc(String id) {
		return new ResourceLocation(MODID, id);
	}
}
