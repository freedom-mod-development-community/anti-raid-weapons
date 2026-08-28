package xyz.fmdc.arw.client.renderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import xyz.fmdc.arw.client.GlbModelManager;
import xyz.fmdc.arw.client.util.GlbLoader;
import xyz.fmdc.arw.oto127mm.Oto127mmBlockEntity;

public class Oto127mmRenderer extends BaseNavalGunRenderer<Oto127mmBlockEntity> {

    public Oto127mmRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected GlbLoader.GlbModelData getModelData(Oto127mmBlockEntity blockEntity) {
        return GlbModelManager.INSTANCE.getModel(GlbModelManager.OTO127MM_ID);
    }
}