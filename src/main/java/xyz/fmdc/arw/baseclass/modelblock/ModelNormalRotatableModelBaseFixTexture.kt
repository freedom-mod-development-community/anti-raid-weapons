package xyz.fmdc.arw.baseclass.modelblock

import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation

abstract class ModelNormalRotatableModelBaseFixTexture<T : TileEntity> : ModelNormalRotatableModelBase<T>() {
    abstract val texture: ResourceLocation

    override fun getTexture(tile: T): ResourceLocation {
        return texture
    }
}