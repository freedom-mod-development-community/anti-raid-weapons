package xyz.fmdc.arw.modcore.baseclass.modelblock

import net.minecraft.nbt.NBTTagCompound
import xyz.fmdc.arw.modcore.baseclass.module.rotatable.IRotatableYawPitch
import xyz.fmdc.arw.modcore.baseclass.module.rotatable.pitchDeg
import xyz.fmdc.arw.modcore.baseclass.module.rotatable.yawDeg

open class ModelNormalRotatableYawPitchTileEntity : ModelNormalTileEntity(), IRotatableYawPitch {
    private val strYawDeg = "yawDeg"
    private val strPitchDeg = "pitchDeg"

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        this.yawDeg = nbt.getDouble(strYawDeg)
        this.pitchDeg = nbt.getDouble(strPitchDeg)
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setDouble(strYawDeg, this.yawDeg)
        nbt.setDouble(strPitchDeg, this.pitchDeg)
    }
}
