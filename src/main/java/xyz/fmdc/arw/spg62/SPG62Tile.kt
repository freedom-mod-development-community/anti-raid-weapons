package xyz.fmdc.arw.spg62

import xyz.fmdc.arw.baseclass.modelblock.ModelNormalRotatableYawPitchTileEntity

class SPG62Tile : ModelNormalRotatableYawPitchTileEntity() {
    override fun getDefaultPitchDeg(): Double {
        return 45.0
    }
}
