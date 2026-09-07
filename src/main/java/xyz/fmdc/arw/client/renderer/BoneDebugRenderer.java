package xyz.fmdc.arw.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class BoneDebugRenderer {

    public static void renderBoneAxes(PoseStack poseStack, MultiBufferSource bufferSource, Matrix4f boneMatrix, float axisLength) {
        if (boneMatrix == null) return;

        poseStack.pushPose();

        // 1. ボーンのグローバル行列を乗算
        poseStack.last().pose().mul(boneMatrix);

        PoseStack.Pose lastPose = poseStack.last();
        Matrix4f poseMatrix = lastPose.pose();
        Matrix3f normalMatrix = lastPose.normal();

        VertexConsumer builder = bufferSource.getBuffer(RenderType.lines());

        // X軸 (赤色: +X方向)
        builder.vertex(poseMatrix, 0, 0, 0)
                .color(255, 0, 0, 255)
                .normal(normalMatrix, 1.0f, 0.0f, 0.0f)
                .endVertex();

        builder.vertex(poseMatrix, axisLength, 0, 0)
                .color(255, 0, 0, 255)
                .normal(normalMatrix, 1.0f, 0.0f, 0.0f)
                .endVertex();

        // Y軸 (緑色: +Y方向)
        builder.vertex(poseMatrix, 0, 0, 0)
                .color(0, 255, 0, 255)
                .normal(normalMatrix, 0.0f, 1.0f, 0.0f)
                .endVertex();

        builder.vertex(poseMatrix, 0, axisLength, 0)
                .color(0, 255, 0, 255)
                .normal(normalMatrix, 0.0f, 1.0f, 0.0f)
                .endVertex();

        // Z軸 (青色: +Z方向)
        builder.vertex(poseMatrix, 0, 0, 0)
                .color(0, 0, 255, 255)
                .normal(normalMatrix, 0.0f, 0.0f, 1.0f)
                .endVertex();

        builder.vertex(poseMatrix, 0, 0, axisLength)
                .color(0, 0, 255, 255)
                .normal(normalMatrix, 0.0f, 0.0f, 1.0f)
                .endVertex();

        poseStack.popPose();
    }
}
