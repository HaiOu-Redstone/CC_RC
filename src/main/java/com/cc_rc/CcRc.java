package com.cc_rc;

import com.cc_rc.block.Console_lever.ConsoleLever3StageBER;
import com.cc_rc.block.Console_lever.ConsoleLeverBER;
import com.cc_rc.block.Plotter.PlotterBER;
import com.cc_rc.block.console_panel.ConsolePanelBER;
import com.cc_rc.block.digital_display.DigitalDisplayBER;
import com.cc_rc.block.digital_knob.DigitalKnobBER;
import com.cc_rc.network.ModNetwork;
import com.cc_rc.block.digital_display.DigitalDisplayBlockEntity;
import com.cc_rc.block.digital_display.DigitalDisplayPeripheral;
import com.cc_rc.block.digital_knob.DigitalKnobBlockEntity;
import com.cc_rc.block.digital_knob.DigitalKnobPeripheral;
import com.cc_rc.block.digital_plotter.DigitalPlotterBER;
import com.cc_rc.block.digital_plotter.DigitalPlotterBlockEntity;
import com.cc_rc.block.digital_plotter.DigitalPlotterPeripheral;
import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CcRc.MODID)
public class CcRc
{
    public static final String MODID = "cc_rc";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CcRc(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register DeferredRegisters
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);

        // 初始化简单网络通道（C2S 投掷包子）
        ModNetwork.init();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // 注册数码显示器外设：使 CC: Tweaked 的调制解调器能识别该方块
        ForgeComputerCraftAPI.registerPeripheralProvider(new IPeripheralProvider() {
            @Override
            public LazyOptional<IPeripheral> getPeripheral(Level world, BlockPos pos, Direction side) {
                if (world.getBlockEntity(pos) instanceof DigitalDisplayBlockEntity be) {
                    return LazyOptional.of(() -> new DigitalDisplayPeripheral(be));
                }
                if (world.getBlockEntity(pos) instanceof DigitalKnobBlockEntity knobBE) {
                    return LazyOptional.of(() -> new DigitalKnobPeripheral(knobBE));
                }
                if (world.getBlockEntity(pos) instanceof DigitalPlotterBlockEntity plotterBE) {
                    return LazyOptional.of(() -> new DigitalPlotterPeripheral(plotterBE));
                }
                return LazyOptional.empty();
            }
        });

        LOGGER.info("CC: 反应堆控制台 加载完成！");
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("CC: 反应堆控制台 服务器启动！");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            LOGGER.info("CC: 反应堆控制台 客户端启动！");
            // 核弹按钮贴图含透明区域，设置 cutout 渲染类型避免透明部分渲染成黑色（默认 solid 会把 alpha=0 像素写黑）
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.NUKE_BUTTON.get(), RenderType.cutout());
            // 钥匙柜/钥匙分发控制器贴图同样含透明区域
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KEY_CABINET.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KEY_DISTRIBUTOR.get(), RenderType.cutout());
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
        {
            event.registerBlockEntityRenderer(ModBlockEntities.CONSOLE_LEVER_BE.get(), ConsoleLeverBER::new);
            event.registerBlockEntityRenderer(ModBlockEntities.CONSOLE_LEVER_3STAGE_BE.get(), ConsoleLever3StageBER::new);
            event.registerBlockEntityRenderer(ModBlockEntities.CONSOLE_PANEL_BE.get(), ConsolePanelBER::new);
            event.registerBlockEntityRenderer(ModBlockEntities.PLOTTER_BE.get(), PlotterBER::new);
            event.registerBlockEntityRenderer(ModBlockEntities.DIGITAL_DISPLAY_BE.get(), DigitalDisplayBER::new);
            event.registerBlockEntityRenderer(ModBlockEntities.DIGITAL_KNOB_BE.get(), DigitalKnobBER::new);
            event.registerBlockEntityRenderer(ModBlockEntities.DIGITAL_PLOTTER_BE.get(), DigitalPlotterBER::new);
            // 粗布告示牌：复用原版 SignRenderer / HangingSignRenderer（按 WoodType 渲染）
            event.registerBlockEntityRenderer(ModBlockEntities.CANVAS_SIGN_BE.get(), SignRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.CANVAS_HANGING_SIGN_BE.get(), HangingSignRenderer::new);
            // 包子弹射物：复用原版 ThrownItemRenderer（按 getDefaultItem 显示 bao_zi 物品贴图）
            event.registerEntityRenderer(ModEntities.BAO_ZI.get(), ThrownItemRenderer::new);
        }
    }
}