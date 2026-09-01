package xyz.fmdc.arw.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.client.util.FastGlbModel;

import java.util.function.Function;

public class BaseStaticFastRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    protected final GenericFastGlbRenderer glbRenderer = new GenericFastGlbRenderer();
    private final Function<T, FastGlbModel> modelProvider;

    public BaseStaticFastRenderer(BlockEntityRendererProvider.Context context) {
        this.modelProvider = this::getModelData;
    }

    public BaseStaticFastRenderer(BlockEntityRendererProvider.Context context, Function<T, FastGlbModel> modelProvider) {
        this.modelProvider = modelProvider;
    }

    @Override
    public void render(@NotNull T blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        FastGlbModel fastModel = this.modelProvider.apply(blockEntity);
        if (fastModel == null) return;

        glbRenderer.render(
                fastModel, poseStack, bufferSource, packedLight, packedOverlay, partialTick,
                java.util.Collections.emptyList(),null
        );
    }

    protected FastGlbModel getModelData(T blockEntity) {
        return null;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}