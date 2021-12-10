package xyz.fmdc.arw.cicelectric

import xyz.fmdc.arw.baseclass.modelblock.IGlowingModel
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalTileEntity

class CICElectricTile: ModelNormalTileEntity(), IGlowingModel {
    override fun getLight(): Int {
        return 240
    }
}
