package xyz.fmdc.arw.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.client.gui.Oto127mmScreen;
import xyz.fmdc.arw.client.renderer.*;
import xyz.fmdc.arw.client.util.IYawModel;
import xyz.fmdc.arw.client.util.IYawPitchAnimatableModel;
import xyz.fmdc.arw.registry.ModBlocks;
import xyz.fmdc.arw.registry.ModEntities;
import xyz.fmdc.arw.registry.ModMenuTypes;
import xyz.fmdc.arw.registry.auto.BlockEntry;
import xyz.fmdc.arw.registry.auto.ModBlockEntities;

@Mod.EventBusSubscriber(modid = AntiRaidWeapons.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.OTO127MM_MENU.get(), Oto127mmScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.OPS39.getBEType(), Ops39Renderer::new);
        event.registerBlockEntityRenderer(ModBlocks.SPQ9B.getBEType(), Spq9bRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.EMMI.getBEType(), EmmiRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.OTO127MM.getBEType(), Oto127mmRenderer::new);
        registerNavalGun(event, ModBlocks.MK45_MOD4, GlbModelManager.MK45MOD4_ID);
        registerNavalGun(event, ModBlocks.WW2_AA_GUN_BLOCK, GlbModelManager.MK45MOD4_ID);
        registerNavalGun(event, ModBlocks.MANNED_TANK_TURRET_BLOCK, GlbModelManager.MK45MOD4_ID);
        registerRadar(event, ModBlocks.OPTICAL_SIGHT_BLOCK, GlbModelManager.OPS39_ID);
        registerRadar(event, ModBlocks.SEARCH_RADAR_BLOCK, GlbModelManager.OPS39_ID);
        registerRadar(event, ModBlocks.TRACKING_RADAR_BLOCK, GlbModelManager.OPS39_ID);

        //entity
        event.registerEntityRenderer(ModEntities.FIVE_INCH_SHELL.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.NAVAL_SHELL.get(), NavalShellRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        // GLBマネージャーをMinecraftのリソースリロードリスナーに登録
        event.registerReloadListener(GlbModelManager.INSTANCE);
    }

    private static <BE extends BlockEntity & IYawPitchAnimatableModel> void registerNavalGun(
            EntityRenderersEvent.RegisterRenderers event,
            BlockEntry<?, BE> blockEntry,
            ResourceLocation resourceLocation) {

        event.registerBlockEntityRenderer(
                blockEntry.getBEType(), // または getBEType()
                ctx -> new BaseNavalGunRenderer<>(ctx, be -> GlbModelManager.INSTANCE.getModel(resourceLocation))
        );
    }
    private static <BE extends BlockEntity & IYawModel> void registerRadar(
            EntityRenderersEvent.RegisterRenderers event,
            BlockEntry<?, BE> blockEntry,
            ResourceLocation resourceLocation) {

        event.registerBlockEntityRenderer(
                blockEntry.getBEType(), // または getBEType()
                ctx -> new BaseRadarRenderer<>(ctx, be -> GlbModelManager.INSTANCE.getModel(resourceLocation))
        );
    }
}
