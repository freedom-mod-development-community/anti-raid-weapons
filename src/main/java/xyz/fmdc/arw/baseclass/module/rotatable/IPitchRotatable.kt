package xyz.fmdc.arw.baseclass.module.rotatable

import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.network.PacketDistributor
import xyz.fmdc.arw.network.PacketHandlerARW
import xyz.fmdc.arw.network.SyncAngleMessage

interface IPitchRotatable {

    val modulePitchRotatable: ModulePitchRotatable

    fun getDefaultPitchDeg(): Double {
        return 0.0
    }

    fun getPitchAngle(): Float {
        return modulePitchRotatable.pitchAngleDeg
    }

    fun getPitchAngle(partialTicks: Float): Float {
        return modulePitchRotatable.pitchAngleDeg
    }

    fun setPitchAngle(angle: Float) {
        modulePitchRotatable.pitchAngleDeg = angle
    }

    /**
     * 周囲のプレイヤーへ角度パケットを同期
     */
    fun syncAngleToClient() {
        if (this is BlockEntity) {
            val lvl = this.level ?: return
            if (!lvl.isClientSide) {
                val yaw = (this as? IYawRotatable)?.getYawAngle() ?: 0.0f
                val pitch = this.getPitchAngle()

                // そのチャンクを見ている全クライアントにパケットを送信
                PacketHandlerARW.CHANNEL.send(
                    PacketDistributor.TRACKING_CHUNK.with { lvl.getChunkAt(this.blockPos) },
                    SyncAngleMessage(this.blockPos, yaw, pitch)
                )
            }
        }
    }
}