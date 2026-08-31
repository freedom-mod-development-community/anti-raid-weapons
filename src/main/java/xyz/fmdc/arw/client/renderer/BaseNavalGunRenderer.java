package xyz.fmdc.arw.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.client.util.GlbLoader;
import xyz.fmdc.arw.client.util.IYawPitchAnimatableModel;

import java.util.List;
import java.util.function.Function;

public class BaseNavalGunRenderer<T extends BlockEntity & IYawPitchAnimatableModel> implements BlockEntityRenderer<T> {

    protected final GenericGlbRenderer glbRenderer = new GenericGlbRenderer();
    private final Function<T, GlbLoader.GlbModelData> modelProvider;

    // 1. 従来通りの抽象メソッドを使う場合のコンストラクタ（後換性維持）
    public BaseNavalGunRenderer(BlockEntityRendererProvider.Context context) {
        this.modelProvider = this::getModelData;
    }

    // 2. 1行でモデル指定したい場合に使用するコンストラクタ（追加）
    public BaseNavalGunRenderer(BlockEntityRendererProvider.Context context, Function<T, GlbLoader.GlbModelData> modelProvider) {
        this.modelProvider = modelProvider;
    }

    @Override
    public void render(@NotNull T blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        GlbLoader.GlbModelData modelData = this.modelProvider.apply(blockEntity);
        if (modelData == null) return;

        List<GenericGlbRenderer.ActiveAnimation> activeAnimations = blockEntity.getActiveAnimations(partialTick);

        glbRenderer.render(
                modelData, poseStack, bufferSource, packedLight, packedOverlay, partialTick,
                activeAnimations,
                (nodeName, stack, pTick) -> {
                    if ("yaw".equalsIgnoreCase(nodeName)) {
                        stack.mulPose(Axis.YP.rotationDegrees(-blockEntity.getRenderTargetYaw(pTick)));
                    } else if ("pitch".equalsIgnoreCase(nodeName)) {
                        stack.mulPose(Axis.XP.rotationDegrees(blockEntity.getRenderTargetPitch(pTick)));
                    }
                }
        );
    }

    // デフォルト実装を返し、overrideは任意にする
    protected GlbLoader.GlbModelData getModelData(T blockEntity) {
        return null;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}