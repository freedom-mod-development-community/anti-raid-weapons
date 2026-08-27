package xyz.fmdc.arw.baseclass.module.rotatable

import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.network.PacketDistributor
import xyz.fmdc.arw.network.PacketHandlerARW
import xyz.fmdc.arw.network.SyncAngleMessage

/**
 * Yaw および Pitch の双方の回転を制御・同期する複合インターフェース
 */
interface IYawPitchRotatable : IYawRotatable, IPitchRotatable {

    /**
     * IYawRotatable と IPitchRotatable の同名メソッドの衝突を解決し、
     * 両方の角度を1つのパケットにまとめて周囲へ同期する
     */
    override fun syncAngleToClient() {
        if (this is BlockEntity) {
            val lvl = this.level ?: return
            if (!lvl.isClientSide) {
                val yaw = this.getYawAngle()
                val pitch = this.getPitchAngle()

                PacketHandlerARW.CHANNEL.send(
                    PacketDistributor.TRACKING_CHUNK.with { lvl.getChunkAt(this.blockPos) },
                    SyncAngleMessage(this.blockPos, yaw, pitch)
                )
            }
        }
    }

    /**
     * Yaw / Pitch を同時に目標設定するヘルパー
     */
    fun setTargetAngle(yaw: Float, pitch: Float) {
        this.setYawAngle(yaw)
        this.setPitchAngle(pitch)
    }
}