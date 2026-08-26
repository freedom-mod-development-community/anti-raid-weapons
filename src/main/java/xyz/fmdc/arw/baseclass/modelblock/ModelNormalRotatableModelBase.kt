package xyz.fmdc.arw.baseclass.modelblock

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import xyz.fmdc.arw.baseclass.module.direction.IDirection
import xyz.fmdc.arw.modelloder.WavefrontObject

abstract class ModelNormalRotatableModelBase<T : ModelNormalTileEntity> : ModelNormalModelBase() {

    abstract val modelName: ResourceLocation

    // 初回描画時に遅延ロード
    protected val model: WavefrontObject by lazy {
        WavefrontObject(modelName)
    }

    /**
     * 動的にテクスチャを取得する場合の抽象プロパティ/メソッド
     */
    abstract fun getTexture(tile: T): ResourceLocation

    @Suppress("UNCHECKED_CAST")
    override fun render(
        tile: ModelNormalTileEntity,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int,
        partialTicks: Float
    ) {
        val typedTile = tile as T
        val textureLocation = getTexture(typedTile)
        val vertexConsumer = bufferSource.getBuffer(RenderType.entityCutout(textureLocation))

        poseStack.pushPose()

        // 1. ブロック設置方向 (FACING) に応じたベース回転
        if (typedTile is IDirection) {
            val facingAngle = typedTile.getDirectionAngle()
            poseStack.mulPose(Axis.YP.rotationDegrees(-facingAngle.toFloat()))
        }

        // 2. 台座・固定部分の描画
        renderBaseParts(typedTile, poseStack, vertexConsumer, packedLight, packedOverlay)

        // 3. 水平旋回 (Yaw回転) の描画
        poseStack.pushPose()
        val yaw = typedTile.getYawAngle(partialTicks)
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw))
        renderYawParts(typedTile, poseStack, vertexConsumer, packedLight, packedOverlay)

        // 4. 俯仰 (Pitch回転) の描画
        poseStack.pushPose()
        val pitch = typedTile.getPitchAngle(partialTicks)
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch))
        renderPitchParts(typedTile, poseStack, vertexConsumer, packedLight, packedOverlay)

        // スタック復元
        poseStack.popPose() // Pitch pop
        poseStack.popPose() // Yaw pop
        poseStack.popPose() // Base pop
    }

    /**
     * 台座・固定部分の描画
     */
    protected open fun renderBaseParts(
        tile: T,
        poseStack: PoseStack,
        consumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int
    ) {
    }

    /**
     * 水平旋回（Yaw）部分の描画
     */
    protected open fun renderYawParts(
        tile: T,
        poseStack: PoseStack,
        consumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int
    ) {
    }

    /**
     * 俯仰（Pitch）部分の描画
     */
    protected open fun renderPitchParts(
        tile: T,
        poseStack: PoseStack,
        consumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int
    ) {
    }
}