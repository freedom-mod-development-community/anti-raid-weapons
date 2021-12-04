package xyz.fmdc.arw.spg62

import xyz.fmdc.arw.baseclass.modelblock.ModelNormalYawPitchRotatableTileEntity

class SPG62Tile : ModelNormalYawPitchRotatableTileEntity() {
    override fun getDefaultPitchDeg(): Double {
        return 45.0
    }
}
