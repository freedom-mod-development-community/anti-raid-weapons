package xyz.fmdc.arw.baseclass.modelblock

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource

abstract class ModelNormalModelBase {

    /**
     * TileEntity（BlockEntity）の描画エントリーポイント
     *
     * @param tile 描画対象のTileEntity
     * @param poseStack 行列スタック
     * @param bufferSource 描画バッファプロバイダ
     * @param packedLight 環境光・ブロック光の合成ライトマップ値
     * @param packedOverlay 被弾時などのオーバーレイ値
     * @param partialTicks フレーム間の補間Tick値 (0.0F - 1.0F)
     */
    abstract fun render(
        tile: ModelNormalTileEntity,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int,
        partialTicks: Float,
    )

    open fun isGlowing(tile: ModelNormalTileEntity): Boolean {
        return false
    }
}