package xyz.fmdc.arw.baseclass.modelblock

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

open class ModelNormalRenderer<T : ModelNormalTileEntity>(
    val model: ModelNormalModelBase
) : BlockEntityRenderer<T> {

    /**
     * BlockEntityRendererProvider 対応コンストラクタ
     */
    constructor(
        context: BlockEntityRendererProvider.Context,
        model: ModelNormalModelBase
    ) : this(model)

    override fun render(
        blockEntity: T,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        poseStack.pushPose()

        // 1. ブロックの中心位置へ原点を移動 (x+0.5, y+0.0, z+0.5)
        poseStack.translate(0.5, 0.0, 0.5)

        // 2. 発光モデル (IGlowingModel) の判定とライト値の決定
        val effectiveLight = if (model is IGlowingModel && model.isGlowing(blockEntity)) {
            LightTexture.FULL_BRIGHT
        } else {
            packedLight
        }

        // 3. モデル側の render を実行
        model.render(
            tile = blockEntity,
            poseStack = poseStack,
            bufferSource = bufferSource,
            packedLight = effectiveLight,
            packedOverlay = packedOverlay,
            partialTicks = partialTick
        )

        poseStack.popPose()
    }
}