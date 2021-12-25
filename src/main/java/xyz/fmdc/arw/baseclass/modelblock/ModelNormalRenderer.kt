package xyz.fmdc.arw.baseclass.modelblock

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity

class ModelNormalRenderer<T : TileEntity>(private val model: ModelNormalModelBase<T>) : TileEntitySpecialRenderer<T>() {
    override fun render(te: T, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int, alpha: Float) {
        model.render(te, x, y, z)
    }
}
