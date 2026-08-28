package xyz.fmdc.arw.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.client.renderer.EmmiRenderer;
import xyz.fmdc.arw.client.renderer.Ops39Renderer;
import xyz.fmdc.arw.client.renderer.Oto127mmRenderer;
import xyz.fmdc.arw.client.renderer.Spq9bRenderer;
import xyz.fmdc.arw.registry.ModBlockEntities;
import xyz.fmdc.arw.registry.ModBlocks;

@Mod.EventBusSubscriber(modid = AntiRaidWeapons.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // OPS-39 の描画登録
        event.registerBlockEntityRenderer(ModBlocks.OPS39.getBEType(), Ops39Renderer::new);

        // SPQ-9B の描画登録
        event.registerBlockEntityRenderer(ModBlocks.SPQ9B.getBEType(), Spq9bRenderer::new);

        // EMMI の描画登録
        event.registerBlockEntityRenderer(ModBlocks.EMMI.getBEType(), EmmiRenderer::new);

        event.registerBlockEntityRenderer(ModBlocks.OTO127MM.getBEType(), Oto127mmRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        // GLBマネージャーをMinecraftのリソースリロードリスナーに登録
        event.registerReloadListener(GlbModelManager.INSTANCE);
    }
}