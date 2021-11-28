package xyz.fmdc.arw.baseclass.modelblock

import net.minecraft.nbt.NBTTagCompound
import xyz.fmdc.arw.baseclass.TileCustomBase
import xyz.fmdc.arw.baseclass.module.direction.IDirection

open class ModelNormalTileEntity : TileCustomBase(), IDirection {
    private val strDirectionAngDeg = "directionAngDeg"

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        this.saveDirectionData(nbt.getDouble(strDirectionAngDeg))
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setDouble(strDirectionAngDeg, this.getDirectionAngle())
    }
}
