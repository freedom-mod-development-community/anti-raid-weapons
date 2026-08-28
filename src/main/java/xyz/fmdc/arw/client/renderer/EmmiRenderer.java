package xyz.fmdc.arw.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.client.GlbModelManager;
import xyz.fmdc.arw.client.util.GlbLoader;
import xyz.fmdc.arw.emmi.EmmiBlockEntity;

public class EmmiRenderer extends BaseStaticRenderer<EmmiBlockEntity> {

    public EmmiRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected GlbLoader.GlbModelData getModelData(EmmiBlockEntity blockEntity) {
        return GlbModelManager.INSTANCE.getModel(GlbModelManager.EMMI_ID);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}