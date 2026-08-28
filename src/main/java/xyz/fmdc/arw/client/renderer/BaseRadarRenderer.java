package xyz.fmdc.arw.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.client.renderer.GenericGlbRenderer;
import xyz.fmdc.arw.client.util.GlbLoader;
import xyz.fmdc.arw.client.util.IRadar;

public abstract class BaseRadarRenderer<T extends BlockEntity & IRadar> implements BlockEntityRenderer<T> {

    protected final GenericGlbRenderer glbRenderer = new GenericGlbRenderer();

    public BaseRadarRenderer(BlockEntityRendererProvider.Context context) {
        // 必要に応じて context をフィールドに保持することも可能
    }
    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        GlbLoader.GlbModelData modelData = getModelData(blockEntity);
        if (modelData == null) {
            //AntiRaidWeapons.LOGGER.warn("[ARW-DEBUG] render() は呼ばれましたが、getModelData() が null を返しました！ (BE: {})",
            //        blockEntity.getBlockPos());
            return;
        }

        String activeAnimName = blockEntity.getActiveAnimationName();
        float animTimeSeconds = blockEntity.getAnimationTimeSeconds(partialTick);

        // 汎用GLBレンダラーを呼び出し、回転制御ノード（radar / antenna / yaw 等）に回転を割り込み
        glbRenderer.render(
                modelData, poseStack, bufferSource, packedLight, packedOverlay, partialTick,
                java.util.Collections.emptyList(),
                (nodeName, stack, pTick) -> {
                    // Blender上の回転ノード名に合わせて判定
                    if ("radar".equalsIgnoreCase(nodeName) ||
                            "antenna".equalsIgnoreCase(nodeName) ||
                            "yaw".equalsIgnoreCase(nodeName)) {

                        stack.mulPose(Axis.YP.rotationDegrees(blockEntity.getRotationYaw(pTick)));
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