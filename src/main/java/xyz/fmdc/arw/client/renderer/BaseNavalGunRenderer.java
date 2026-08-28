package xyz.fmdc.arw.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.client.util.GlbLoader;
import xyz.fmdc.arw.client.util.INavalGun;

import java.util.List;

public abstract class BaseNavalGunRenderer<T extends BlockEntity & INavalGun> implements BlockEntityRenderer<T> {

    protected final GenericGlbRenderer glbRenderer = new GenericGlbRenderer();

    public BaseNavalGunRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@NotNull T blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        GlbLoader.GlbModelData modelData = getModelData(blockEntity);
        if (modelData == null) return;

        // INavalGun 経由で再生中アニメーションのリストを取得
        List<GenericGlbRenderer.ActiveAnimation> activeAnimations = blockEntity.getActiveAnimations(partialTick);

        // 汎用GLBレンダラーの呼び出し
        glbRenderer.render(
                modelData, poseStack, bufferSource, packedLight, packedOverlay, partialTick,
                activeAnimations,
                (nodeName, stack, pTick) -> {
                    if ("yaw".equalsIgnoreCase(nodeName)) {
                        stack.mulPose(Axis.YP.rotationDegrees(blockEntity.getTargetYaw(pTick)));
                    } else if ("pitch".equalsIgnoreCase(nodeName)) {
                        stack.mulPose(Axis.XP.rotationDegrees(blockEntity.getTargetPitch(pTick)));
                    }
                }
        );
    }

    protected abstract GlbLoader.GlbModelData getModelData(T blockEntity);

    @Override
    public int getViewDistance() {
        return 256;
    }
}