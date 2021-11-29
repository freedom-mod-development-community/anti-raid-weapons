package xyz.fmdc.arw.baseclass.modelblock

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB
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

    @SideOnly(Side.CLIENT)
    override fun getRenderBoundingBox(): AxisAlignedBB {
        val blockType = this.blockType as ModelNormalBlockContainer
        val selectBoundsHalfWide = blockType.selectBoundsHalfWide + 1
        val selectBoundsHeight = blockType.selectBoundsHeight + 1
        return AxisAlignedBB.getBoundingBox(
            this.xCoord.toDouble() - selectBoundsHalfWide + 0.5,
            this.yCoord.toDouble(),
            this.zCoord.toDouble() - selectBoundsHalfWide + 0.5,
            this.xCoord.toDouble() + selectBoundsHalfWide + 0.5,
            this.yCoord.toDouble() + selectBoundsHeight,
            this.zCoord.toDouble() + selectBoundsHalfWide + 0.5)
    }
}
