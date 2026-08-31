package xyz.fmdc.arw.client.renderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import xyz.fmdc.arw.client.GlbModelManager;
import xyz.fmdc.arw.client.util.GlbLoader;
import xyz.fmdc.arw.common.blockentity.weapon.Ops39BlockEntity;

public class Ops39Renderer extends BaseRadarRenderer<Ops39BlockEntity> {

    public Ops39Renderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected GlbLoader.GlbModelData getModelData(Ops39BlockEntity blockEntity) {
        // GlbModelManager にロードを委譲
        return GlbModelManager.INSTANCE.getModel(GlbModelManager.OPS39_ID);
    }
}