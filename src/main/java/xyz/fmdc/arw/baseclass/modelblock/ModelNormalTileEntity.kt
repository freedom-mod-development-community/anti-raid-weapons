package xyz.fmdc.arw.baseclass.modelblock

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S35PacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.AxisAlignedBB
import xyz.fmdc.arw.baseclass.module.direction.IDirection
import xyz.fmdc.arw.baseclass.module.direction.ModuleDirection
import xyz.fmdc.arw.loadTo
import xyz.fmdc.arw.newPacketUpdateTileEntity

open class ModelNormalTileEntity : TileEntity(), IDirection {
    override val moduleDirection = ModuleDirection()

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
        if (this.blockType == null) {
            return super.getRenderBoundingBox()
        }
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

    override fun getDescriptionPacket(): Packet {
        return this.newPacketUpdateTileEntity()
    }

    override fun onDataPacket(net: NetworkManager, pkt: S35PacketUpdateTileEntity) {
        pkt.loadTo(this)
    }

    @SideOnly(Side.CLIENT)
    override fun getMaxRenderDistanceSquared(): Double {
        return Double.MAX_VALUE
    }
}
