package xyz.fmdc.arw.modcore.baseclass.modelblock

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity

class ModelNormalRenderer<T : TileEntity>(private val model: ModelNormalModelBase<T>) : TileEntitySpecialRenderer() {
    override fun renderTileEntityAt(
        tile: TileEntity,
        x: Double,
        y: Double,
        z: Double,
        renderTick: Float,
    ) {
        model.render(tile as T, x, y, z)
    }
}
