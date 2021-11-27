package xyz.fmdc.arw.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import net.minecraft.tileentity.TileEntity
import xyz.fmdc.arw.modcore.currentWorld
import xyz.fmdc.arw.vector.Vec3I


abstract class TileEntityMessage : IMessage {
    protected constructor()
    protected constructor(tile: TileEntity) {
        pos = Vec3I(tile.xCoord, tile.yCoord, tile.zCoord)
        this.tile = tile
    }

    private lateinit var tile: TileEntity
    private lateinit var pos: Vec3I

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(tile.xCoord)
        buf.writeInt(tile.yCoord)
        buf.writeInt(tile.zCoord)
        write(buf)
    }

    abstract fun write(buf: ByteBuf)

    override fun fromBytes(buf: ByteBuf) {
        pos = Vec3I(buf.readInt(), buf.readInt(), buf.readInt())
        read(buf)
    }

    abstract fun read(buf: ByteBuf)

    protected fun getTileEntity(ctx: MessageContext): TileEntity? {
        return ctx.currentWorld.getTileEntity(pos.x, pos.y, pos.z)
    }
}
