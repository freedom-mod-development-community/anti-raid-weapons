package xyz.fmdc.arw.baseclass

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S35PacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.AxisAlignedBB
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

open class TileCustomBase : TileEntity() {
    @SideOnly(Side.CLIENT)
    override fun getMaxRenderDistanceSquared(): Double {
        return Double.MAX_VALUE
    }

    override fun getDescriptionPacket(): Packet {
        val nbtTagCompound = NBTTagCompound()
        writeToNBT(nbtTagCompound)
        return S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, nbtTagCompound)
    }

    override fun onDataPacket(net: NetworkManager, pkt: S35PacketUpdateTileEntity) {
        readFromNBT(pkt.func_148857_g())
    }

    @SideOnly(Side.CLIENT)
    override fun getRenderBoundingBox(): AxisAlignedBB {
        val blockType = this.blockType
        return if (blockType is ModelNormalBlockContainer) {
            val selectBoundsHalfWide = blockType.selectBoundsHalfWide + 1
            val selectBoundsHeight = blockType.selectBoundsHeight + 1
            AxisAlignedBB.getBoundingBox(
                this.xCoord.toDouble() - selectBoundsHalfWide + 0.5,
                this.yCoord.toDouble(),
                this.zCoord.toDouble() - selectBoundsHalfWide + 0.5,
                this.xCoord.toDouble() + selectBoundsHalfWide + 0.5,
                this.yCoord.toDouble() + selectBoundsHeight,
                this.zCoord.toDouble() + selectBoundsHalfWide + 0.5)
        } else {
            super.getRenderBoundingBox()
        }
    }
}
