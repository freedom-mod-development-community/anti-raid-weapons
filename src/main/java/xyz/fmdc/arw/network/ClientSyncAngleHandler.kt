package xyz.fmdc.arw.network

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalYawPitchRotatableTileEntity
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalYawRotatableTileEntity

/**
 * [SyncAngleMessage] のクライアント側処理。
 * 専用サーバーで [Minecraft] がクラスロードされないよう、メッセージ本体から分離している。
 */
object ClientSyncAngleHandler {

    fun applyAngle(pos: BlockPos, yaw: Float, pitch: Float) {
        val level = Minecraft.getInstance().level ?: return
        when (val blockEntity = level.getBlockEntity(pos)) {
            is ModelNormalYawPitchRotatableTileEntity -> {
                blockEntity.currentYaw = yaw
                blockEntity.currentPitch = pitch
            }

            is ModelNormalYawRotatableTileEntity -> {
                blockEntity.currentYaw = yaw
            }
        }
    }
}
