package xyz.fmdc.arw.network

import io.netty.buffer.ByteBuf
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import xyz.fmdc.arw.currentWorld


abstract class TileEntityMessage : IMessage {
    protected constructor()
    protected constructor(tile: TileEntity) {
        pos = tile.pos
        this.tile = tile
    }

    private lateinit var tile: TileEntity
    private lateinit var pos: BlockPos

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(tile.pos.x)
        buf.writeInt(tile.pos.y)
        buf.writeInt(tile.pos.z)
        write(buf)
    }

    abstract fun write(buf: ByteBuf)

    override fun fromBytes(buf: ByteBuf) {
        pos = BlockPos(buf.readInt(), buf.readInt(), buf.readInt())
        read(buf)
    }

    abstract fun read(buf: ByteBuf)

    protected fun getTileEntity(ctx: MessageContext): TileEntity? {
        return ctx.currentWorld.getTileEntity(pos)
    }
}
