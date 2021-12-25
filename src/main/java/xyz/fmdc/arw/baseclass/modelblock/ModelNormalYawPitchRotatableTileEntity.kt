package xyz.fmdc.arw.baseclass.modelblock

import net.minecraft.nbt.NBTTagCompound
import xyz.fmdc.arw.baseclass.module.rotatable.*

open class ModelNormalYawPitchRotatableTileEntity : ModelNormalTileEntity(), IYawPitchRotatable {
    override val moduleYawRotatable = ModuleYawRotatable()
    override val modulePitchRotatable = ModulePitchRotatable()

    private val strYawDeg = "yawDeg"
    private val strPitchDeg = "pitchDeg"

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        this.yawDeg = nbt.getDouble(strYawDeg)
        this.pitchDeg = nbt.getDouble(strPitchDeg)
    }

    override fun writeToNBT(nbt: NBTTagCompound): NBTTagCompound = nbt.also {
        super.writeToNBT(nbt)
        nbt.setDouble(strYawDeg, this.yawDeg)
        nbt.setDouble(strPitchDeg, this.pitchDeg)
    }
}
