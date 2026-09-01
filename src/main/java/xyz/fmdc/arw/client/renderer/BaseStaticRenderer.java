package xyz.fmdc.arw.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.client.util.GlbLoader;

import java.util.function.Function;

/**
 * 回転などのギミックを持たない、固定・静的なGLBモデル用の抽象レンダラークラス。
 */
public class BaseStaticRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    protected final GenericGlbRenderer glbRenderer = new GenericGlbRenderer();
    private final Function<T, GlbLoader.GlbModelData> modelProvider;

    public BaseStaticRenderer(BlockEntityRendererProvider.Context context) {
        this.modelProvider = this::getModelData;
    }

    // 1行登録用のコンストラクタ
    public BaseStaticRenderer(BlockEntityRendererProvider.Context context, Function<T, GlbLoader.GlbModelData> modelProvider) {
        this.modelProvider = modelProvider;
    }

    @Override
    public void render(@NotNull T blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        GlbLoader.GlbModelData modelData = this.modelProvider.apply(blockEntity);
        if (modelData == null) { return; }

        glbRenderer.render(
                modelData, poseStack, bufferSource, packedLight, packedOverlay, partialTick,
                java.util.Collections.emptyList(),null
        );
    }

    protected GlbLoader.GlbModelData getModelData(T blockEntity) {
        return null;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}