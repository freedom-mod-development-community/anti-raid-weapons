package xyz.fmdc.arw.client.renderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import xyz.fmdc.arw.client.GlbModelManager;
import xyz.fmdc.arw.client.util.GlbLoader;
import xyz.fmdc.arw.spq9b.Spq9bBlockEntity;

public class Spq9bRenderer extends BaseRadarRenderer<Spq9bBlockEntity> {

    public Spq9bRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected GlbLoader.GlbModelData getModelData(Spq9bBlockEntity blockEntity) {
        // GlbModelManager にロードを委譲
        return GlbModelManager.INSTANCE.getModel(GlbModelManager.SPQ9B_ID);
    }
}