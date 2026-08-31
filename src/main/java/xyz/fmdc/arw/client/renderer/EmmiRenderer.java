package xyz.fmdc.arw.client.renderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
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
}