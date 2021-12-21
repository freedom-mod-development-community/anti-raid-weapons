package xyz.fmdc.arw.baseclass.modelblock

import net.minecraft.nbt.NBTTagCompound
import xyz.fmdc.arw.baseclass.module.rotatable.IYawRotatable
import xyz.fmdc.arw.baseclass.module.rotatable.ModuleYawRotatable
import xyz.fmdc.arw.baseclass.module.rotatable.yawDeg

open class ModelNormalYawRotatableTileEntity : ModelNormalTileEntity(), IYawRotatable {
    override val moduleYawRotatable = ModuleYawRotatable()

    private val strYawDeg = "yawDeg"

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        this.yawDeg = nbt.getDouble(strYawDeg)
    }

    override fun writeToNBT(nbt: NBTTagCompound) = nbt.also {
        super.writeToNBT(nbt)
        nbt.setDouble(strYawDeg, this.yawDeg)
    }
}
