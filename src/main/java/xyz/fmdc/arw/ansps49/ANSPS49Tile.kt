package xyz.fmdc.arw.ansps49

import xyz.fmdc.arw.baseclass.modelblock.ModelNormalYawPitchRotatableTileEntity

class ANSPS49Tile : ModelNormalYawPitchRotatableTileEntity() {
    override fun getDefaultPitchDeg(): Double {
        return 15.0
    }
}