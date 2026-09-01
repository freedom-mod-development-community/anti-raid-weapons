package xyz.fmdc.arw.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.NoopRenderer;
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
import xyz.fmdc.arw.client.gui.Mk13GmlsScreen;
import xyz.fmdc.arw.client.gui.Oto127mmScreen;
import xyz.fmdc.arw.client.renderer.*;
import xyz.fmdc.arw.client.util.IYawModel;
import xyz.fmdc.arw.client.util.IYawPitchAnimatableModel;
import xyz.fmdc.arw.registry.ModBlocks;
import xyz.fmdc.arw.registry.ModEntities;
import xyz.fmdc.arw.registry.ModMenuTypes;
import xyz.fmdc.arw.registry.auto.BlockEntry;

@Mod.EventBusSubscriber(modid = AntiRaidWeapons.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.OTO127MM_MENU.get(), Oto127mmScreen::new);
            MenuScreens.register(ModMenuTypes.MK13_GMLS_MENU.get(), Mk13GmlsScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        //sensor
        registerRadar(event, ModBlocks.OPS39, GlbModelManager.OPS39_ID);
        registerRadar(event, ModBlocks.SPQ9B, GlbModelManager.SPQ9B_ID);
        registerRadar(event, ModBlocks.OPTICAL_SIGHT_BLOCK, GlbModelManager.OPS39_ID);
        registerRadar(event, ModBlocks.SEARCH_RADAR_BLOCK, GlbModelManager.OPS39_ID);
        registerRadar(event, ModBlocks.TRACKING_RADAR_BLOCK, GlbModelManager.OPS39_ID);

        //decoration
        registerDecoration(event, ModBlocks.EMMI, GlbModelManager.EMMI_ID);
        registerDecoration(event, ModBlocks.ATAGO, GlbModelManager.ATAGO_ID);

        //naval gun
        registerNavalGun(event, ModBlocks.OTO127MM, GlbModelManager.OTO127MM_ID);
        registerNavalGun(event, ModBlocks.MK45_MOD4, GlbModelManager.MK45MOD4_ID);
        registerNavalGun(event, ModBlocks.WW2_AA_GUN_BLOCK, GlbModelManager.MK45MOD4_ID);
        registerNavalGun(event, ModBlocks.MANNED_TANK_TURRET_BLOCK, GlbModelManager.MK45MOD4_ID);
        registerNavalGun(event, ModBlocks.PHALANX, GlbModelManager.PHALANX_ID);

        //missile launcher
        registerMissileLauncher(event, ModBlocks.MK13_GMLS_BLOCK, GlbModelManager.MK13GMLS_ID);

        //entity
        event.registerEntityRenderer(ModEntities.FIVE_INCH_SHELL.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.RIM_66M2.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        // GLBマネージャーをMinecraftのリソースリロードリスナーに登録
        event.registerReloadListener(GlbModelManager.INSTANCE);
    }

    private static <BE extends BlockEntity & IYawPitchAnimatableModel> void registerMissileLauncher(
            EntityRenderersEvent.RegisterRenderers event,
            BlockEntry<?, BE> blockEntry,
            ResourceLocation resourceLocation) {

        event.registerBlockEntityRenderer(
                blockEntry.getBEType(),
                ctx -> new BaseNavalGunRenderer<>(ctx, be -> GlbModelManager.INSTANCE.getFastModel(resourceLocation))
        );
    }

    private static <BE extends BlockEntity & IYawPitchAnimatableModel> void registerNavalGun(
            EntityRenderersEvent.RegisterRenderers event,
            BlockEntry<?, BE> blockEntry,
            ResourceLocation resourceLocation) {

        event.registerBlockEntityRenderer(
                blockEntry.getBEType(), // または getBEType()
                ctx -> new BaseNavalGunRenderer<>(ctx, be -> GlbModelManager.INSTANCE.getFastModel(resourceLocation))
        );
    }
    private static <BE extends BlockEntity & IYawModel> void registerRadar(
            EntityRenderersEvent.RegisterRenderers event,
            BlockEntry<?, BE> blockEntry,
            ResourceLocation resourceLocation) {

        event.registerBlockEntityRenderer(
                blockEntry.getBEType(), // または getBEType()
                ctx -> new BaseRadarRenderer<>(ctx, be -> GlbModelManager.INSTANCE.getFastModel(resourceLocation))
        );
    }

    private static <BE extends BlockEntity> void registerDecoration(
            EntityRenderersEvent.RegisterRenderers event,
            BlockEntry<?, BE> blockEntry,
            ResourceLocation resourceLocation) {

        event.registerBlockEntityRenderer(
                blockEntry.getBEType(), // または getBEType()
                ctx -> new BaseStaticRenderer<>(ctx, be -> GlbModelManager.INSTANCE.getFastModel(resourceLocation))
        );
    }
}
