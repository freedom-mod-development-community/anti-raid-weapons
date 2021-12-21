package xyz.fmdc.arw

import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.MathHelper
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.relauncher.Side

val MessageContext.currentWorld: World
    get() = when (side!!) {
        Side.SERVER -> serverHandler.player.world
        Side.CLIENT -> Minecraft.getMinecraft().world
        else -> error("unknown side: $side")
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
    return getHorizontal(MathHelper.floor(angle / 90.0 + 0.5) and 3)
}

fun TileEntity.newPacketUpdateTileEntity(): SPacketUpdateTileEntity {
    val nbtTagCompound = NBTTagCompound()
    writeToNBT(nbtTagCompound)
    return SPacketUpdateTileEntity(pos, 1, nbtTagCompound)
}

fun SPacketUpdateTileEntity.loadTo(tileEntity: TileEntity) {
    tileEntity.readFromNBT(getNbtCompound())
}
