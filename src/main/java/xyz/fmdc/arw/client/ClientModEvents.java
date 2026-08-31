package xyz.fmdc.arw.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.client.renderer.*;
import xyz.fmdc.arw.registry.ModBlocks;
import xyz.fmdc.arw.registry.ModEntities;

@Mod.EventBusSubscriber(modid = AntiRaidWeapons.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.OPS39.getBEType(), Ops39Renderer::new);
        event.registerBlockEntityRenderer(ModBlocks.SPQ9B.getBEType(), Spq9bRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.EMMI.getBEType(), EmmiRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.OTO127MM.getBEType(), Oto127mmRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.MK45_MOD4.getBEType(), Mk45mod4Renderer::new);

        event.registerEntityRenderer(ModEntities.NAVAL_SHELL.get(), NavalShellRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        // GLBマネージャーをMinecraftのリソースリロードリスナーに登録
        event.registerReloadListener(GlbModelManager.INSTANCE);
    }
}
