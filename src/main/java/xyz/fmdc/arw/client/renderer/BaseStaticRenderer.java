package xyz.fmdc.arw.client.renderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.client.util.GlbLoader;

/**
 * 回転などのギミックを持たない、固定・静的なGLBモデル用の抽象レンダラークラス。
 */
public abstract class BaseStaticRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    protected final GenericGlbRenderer glbRenderer = new GenericGlbRenderer();

    public BaseStaticRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@NotNull T blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        GlbLoader.GlbModelData modelData = getModelData(blockEntity);
        if (modelData == null) return;

        // 静的ブロックのためアニメーション指定および回転コールバックは null 渡し
        glbRenderer.render(
                modelData, poseStack, bufferSource, packedLight, packedOverlay, partialTick,
                java.util.Collections.emptyList(), null
        );
    }

    protected abstract GlbLoader.GlbModelData getModelData(T blockEntity);

    @Override
    public int getViewDistance() {
        return 256;
    }
}