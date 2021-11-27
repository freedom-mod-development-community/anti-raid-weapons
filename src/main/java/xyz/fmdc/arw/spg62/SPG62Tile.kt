package xyz.fmdc.arw.spg62

import xyz.fmdc.arw.modcore.baseclass.modelblock.ModelNormalRotatableYawPitchTileEntity

class SPG62Tile : ModelNormalRotatableYawPitchTileEntity() {
    override fun getDefaultPitchDeg(): Double {
        return 45.0
    }
}