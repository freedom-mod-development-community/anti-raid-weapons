package xyz.fmdc.arw.baseclass.modelblock

import net.minecraft.client.renderer.LightTexture
import net.minecraft.util.Mth

/**
 * 自己発光するブロックの TileEntity に実装するインターフェース。
 * [ModelNormalRenderer] が描画時に周囲の明るさの代わりにこの値を使用する。
 */
interface IGlowingModel {

    /**
     * 発光量を 0 〜 [MAX_LIGHT] で返す。
     */
    fun getLight(): Int

    /**
     * [getLight] を 1.20.1 のパック済みライトマップ値へ変換する。
     */
    fun packedLight(): Int {
        val level = Mth.clamp(getLight() / 16, 0, 15)
        return LightTexture.pack(level, level)
    }

    companion object {
        /** 1.7.10 のライトマップ表現に合わせた最大値 (= 明るさレベル 15) */
        const val MAX_LIGHT = 240
    }
}
