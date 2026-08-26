package xyz.fmdc.arw.baseclass.modelblock

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import xyz.fmdc.arw.baseclass.module.direction.IDirection
import xyz.fmdc.arw.baseclass.module.rotatable.IPitchRotatable
import xyz.fmdc.arw.baseclass.module.rotatable.IYawRotatable
import xyz.fmdc.arw.modelloder.WavefrontObject

abstract class ModelNormalRotatableModelBaseFixedTexture<T : ModelNormalTileEntity> : ModelNormalModelBase() {

    // 1.7.10 と同様にサブクラス側で ResourceLocation をオーバーライド定義
    abstract val modelPath: ResourceLocation
    abstract val texture: ResourceLocation

    // 初回描画時にモデルを生成・キャッシュ
    protected val model: WavefrontObject by lazy {
        WavefrontObject(modelPath)
    }

    override fun render(
        tile: ModelNormalTileEntity,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int,
        partialTicks: Float
    ) {
        val vertexConsumer = bufferSource.getBuffer(RenderType.entityCutout(texture))

        poseStack.pushPose()

        // 1. ブロックの設置方向（FACING）に応じたベース回転
        if (tile is IDirection) {
            val facingAngle = tile.getDirectionAngle()
            poseStack.mulPose(Axis.YP.rotationDegrees(-facingAngle.toFloat()))
        }

        // 2. 台座・固定部分の描画
        renderBaseParts(poseStack, vertexConsumer, packedLight, packedOverlay)

        // 3. 水平旋回部（Yaw回転）の描画
        poseStack.pushPose()
        if (tile is IYawRotatable) {
            val yaw = tile.getYawAngle(partialTicks)
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw))
        }
        renderYawParts(poseStack, vertexConsumer, packedLight, packedOverlay)

        // 4. 俯仰部（Pitch回転）の描画
        poseStack.pushPose()
        if (tile is IPitchRotatable) {
            val pitch = tile.getPitchAngle(partialTicks)
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch))
        }
        renderPitchParts(poseStack, vertexConsumer, packedLight, packedOverlay)

        // スタックの復元
        poseStack.popPose() // Pitch
        poseStack.popPose() // Yaw
        poseStack.popPose() // Base
    }

    /**
     * 台座・固定部分の描画（必要に応じてオーバーライド）
     */
    protected open fun renderBaseParts(
        poseStack: PoseStack,
        consumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int
    ) {
    }

    /**
     * 水平旋回（Yaw）部分の描画（必要に応じてオーバーライド）
     */
    protected open fun renderYawParts(
        poseStack: PoseStack,
        consumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int
    ) {
    }

    /**
     * 俯仰（Pitch）部分の描画（必要に応じてオーバーライド）
     */
    protected open fun renderPitchParts(
        poseStack: PoseStack,
        consumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int
    ) {
    }
}