package xyz.fmdc.arw.baseclass.modelblock

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import xyz.fmdc.arw.modelloader.WavefrontObject

/**
 * OBJ の `base` / `yaw` / `pitch` グループを、それぞれ固定部・水平旋回部・俯仰部として描画する基底クラス。
 * 対応するグループが OBJ に存在しない場合は単に描画されない。
 */
abstract class ModelNormalRotatableModelBaseFixedTexture<T : ModelNormalTileEntity> : ModelNormalModelBase() {

    abstract val modelPath: ResourceLocation
    abstract val texture: ResourceLocation

    /**
     * 俯仰部の回転軸位置。OBJ の `pitch` グループが原点付近でモデリングされている場合に、
     * 回転前の平行移動量として適用する。不要なら null。
     */
    protected open val pitchPivot: Vec3? = null

    // 初回描画時に生成・キャッシュ
    protected val model: WavefrontObject by lazy {
        WavefrontObject(modelPath)
    }

    final override fun render(
        tile: ModelNormalTileEntity,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int,
        partialTicks: Float
    ) {
        val consumer = bufferSource.getBuffer(RenderType.entityCutout(texture))

        poseStack.pushPose()

        // 1. ブロックの設置方向（FACING）に応じたベース回転
        poseStack.mulPose(Axis.YP.rotationDegrees(-tile.getDirectionAngle().toFloat()))

        // 2. 台座・固定部分
        renderBaseParts(poseStack, consumer, packedLight, packedOverlay)

        // 3. 水平旋回部（Yaw回転）
        poseStack.pushPose()
        poseStack.mulPose(Axis.YP.rotationDegrees(tile.getYawAngle(partialTicks)))
        renderYawParts(poseStack, consumer, packedLight, packedOverlay)

        // 4. 俯仰部（Pitch回転）
        poseStack.pushPose()
        pitchPivot?.let { poseStack.translate(it.x, it.y, it.z) }
        poseStack.mulPose(Axis.XP.rotationDegrees(tile.getPitchAngle(partialTicks)))
        renderPitchParts(poseStack, consumer, packedLight, packedOverlay)

        poseStack.popPose() // Pitch
        poseStack.popPose() // Yaw
        poseStack.popPose() // Base
    }

    protected open fun renderBaseParts(
        poseStack: PoseStack,
        consumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int
    ) {
        model.renderPart(PART_BASE, poseStack, consumer, packedLight, packedOverlay)
    }

    protected open fun renderYawParts(
        poseStack: PoseStack,
        consumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int
    ) {
        model.renderPart(PART_YAW, poseStack, consumer, packedLight, packedOverlay)
    }

    protected open fun renderPitchParts(
        poseStack: PoseStack,
        consumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int
    ) {
        model.renderPart(PART_PITCH, poseStack, consumer, packedLight, packedOverlay)
    }

    private companion object {
        const val PART_BASE = "base"
        const val PART_YAW = "yaw"
        const val PART_PITCH = "pitch"
    }
}
