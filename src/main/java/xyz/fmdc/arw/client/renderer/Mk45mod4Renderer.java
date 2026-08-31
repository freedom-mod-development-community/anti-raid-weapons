package xyz.fmdc.arw.client.renderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import xyz.fmdc.arw.client.GlbModelManager;
import xyz.fmdc.arw.client.util.GlbLoader;
import xyz.fmdc.arw.common.blockentity.Mk45Mod4BlockEntity;

public class Mk45mod4Renderer extends BaseNavalGunRenderer<Mk45Mod4BlockEntity> {

    public Mk45mod4Renderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected GlbLoader.GlbModelData getModelData(Mk45Mod4BlockEntity blockEntity) {
        return GlbModelManager.INSTANCE.getModel(GlbModelManager.MK45MOD4_ID);
    }
}
