package xyz.fmdc.arw.baseclass.modelblock

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer

open class ModelNormalRenderer<T : ModelNormalTileEntity>(
    val model: ModelNormalModelBase
) : BlockEntityRenderer<T> {

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

        // 2. 自己発光ブロックは周囲の明るさではなく自前のライト値を使う
        val effectiveLight = (blockEntity as? IGlowingModel)?.packedLight() ?: packedLight

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

    /**
     * OBJ モデルはブロック境界を大きくはみ出すため、視界外判定を TileEntity 側の
     * 描画バウンディングボックスに委ねる。
     */
    override fun shouldRenderOffScreen(blockEntity: T): Boolean = true
}
