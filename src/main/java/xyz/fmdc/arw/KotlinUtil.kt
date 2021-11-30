package xyz.fmdc.arw

import cpw.mods.fml.common.network.simpleimpl.MessageContext
import cpw.mods.fml.relauncher.Side
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.play.server.S35PacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.MathHelper
import net.minecraft.world.World

val MessageContext.currentWorld: World
    get() = when (side!!) {
        Side.SERVER -> serverHandler.playerEntity.worldObj
        Side.CLIENT -> Minecraft.getMinecraft().theWorld
    }

private val HORIZONTALS: Array<EnumFacing> by lazy {
    arrayOf(EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.EAST)
}

private fun getHorizontal(horizontalIndexIn: Int): EnumFacing {
    return HORIZONTALS[MathHelper.abs((horizontalIndexIn % HORIZONTALS.size).toFloat())
        .toInt()]
}

/**
 * Get the Facing corresponding to the given angle (0-360). An angle of 0 is SOUTH, an angle of 90 would be WEST.
 */
fun getFacingFromAngle(angle: Float): EnumFacing {
    return getFacingFromAngle(angle.toDouble())
}

fun getFacingFromAngle(angle: Double): EnumFacing {
    return getHorizontal(MathHelper.floor_double(angle / 90.0 + 0.5) and 3)
}

fun EnumFacing.getHorizontalAngle(): Double {
    return HORIZONTALS.indexOf(this) * 90.0
}

fun TileEntity.newPacketUpdateTileEntity(): S35PacketUpdateTileEntity {
    val nbtTagCompound = NBTTagCompound()
    writeToNBT(nbtTagCompound)
    return S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, nbtTagCompound)
}

fun S35PacketUpdateTileEntity.loadTo(tileEntity: TileEntity) {
    tileEntity.readFromNBT(func_148857_g())
}
