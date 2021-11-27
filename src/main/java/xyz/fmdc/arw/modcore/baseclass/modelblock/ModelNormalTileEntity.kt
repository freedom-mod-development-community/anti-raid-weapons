package xyz.fmdc.arw.modcore.baseclass.modelblock

import net.minecraft.nbt.NBTTagCompound
import xyz.fmdc.arw.modcore.baseclass.TileCustomBase
import xyz.fmdc.arw.modcore.baseclass.module.direction.IDirection
import xyz.fmdc.arw.modcore.baseclass.module.direction.ModuleDirection

open class ModelNormalTileEntity : TileCustomBase(), IDirection {
    override val moduleDirection: ModuleDirection = ModuleDirection()

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
