package xyz.fmdc.arw.baseclass.modelblock

import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation

abstract class ModelNormalModelBaseFixedTexture<T : TileEntity> : ModelNormalModelBase<T>() {
    abstract val texture: ResourceLocation

    override fun getTexture(tile: T): ResourceLocation {
        return texture
    }
}
