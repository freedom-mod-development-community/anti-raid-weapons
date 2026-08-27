package xyz.fmdc.arw.baseclass.module.rotatable

import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.network.PacketDistributor
import xyz.fmdc.arw.network.PacketHandlerARW
import xyz.fmdc.arw.network.SyncAngleMessage

interface IYawRotatable {

    val moduleYawRotatable: ModuleYawRotatable

    fun getYawAngle(): Float {
        return moduleYawRotatable.yawAngleDeg
    }

    fun getYawAngle(partialTicks: Float): Float {
        return moduleYawRotatable.yawAngleDeg
    }

    fun setYawAngle(angle: Float) {
        moduleYawRotatable.yawAngleDeg = angle
    }

    fun syncAngleToClient() {
        if (this is BlockEntity) {
            val lvl = this.level ?: return
            if (!lvl.isClientSide) {
                val yaw = this.getYawAngle()
                val pitch = (this as? IPitchRotatable)?.getPitchAngle() ?: 0.0f

                PacketHandlerARW.CHANNEL.send(
                    PacketDistributor.TRACKING_CHUNK.with { lvl.getChunkAt(this.blockPos) },
                    SyncAngleMessage(this.blockPos, yaw, pitch)
                )
            }
        }
    }
}