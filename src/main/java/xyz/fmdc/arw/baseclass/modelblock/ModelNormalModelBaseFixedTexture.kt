package xyz.fmdc.arw.baseclass.modelblock

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import xyz.fmdc.arw.modelloader.WavefrontObject

abstract class ModelNormalModelBaseFixedTexture<T : ModelNormalTileEntity> : ModelNormalModelBase() {

    // 1.7.10 と同様にサブクラス側で ResourceLocation をオーバーライド定義
    abstract val modelName: ResourceLocation
    abstract val texture: ResourceLocation

    // 初回描画時にモデルを生成・キャッシュ
    protected val model: WavefrontObject by lazy {
        WavefrontObject(modelName)
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

        // ブロックの設置方向（FACING）に応じたベース回転
        poseStack.mulPose(Axis.YP.rotationDegrees(-tile.getDirectionAngle().toFloat()))

        // モデル描画
        render(poseStack, vertexConsumer, packedLight, packedOverlay)

        poseStack.popPose()
    }

    open fun render(
        poseStack: PoseStack,
        consumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int
    ) {
        model.renderAll(poseStack, consumer, packedLight, packedOverlay)
    }
}