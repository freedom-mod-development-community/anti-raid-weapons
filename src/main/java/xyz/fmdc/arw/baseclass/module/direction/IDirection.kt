package xyz.fmdc.arw.baseclass.module.direction

import net.minecraft.core.Direction
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

interface IDirection {

    val moduleDirection: ModuleDirection

    /**
     * 現在の設置角度（度数法: 0° ~ 360°）を取得
     */
    fun getDirectionAngle(): Double {
        return moduleDirection.directionAngleDeg
    }

    /**
     * 角度データをモジュールへ保存
     */
    fun saveDirectionData(angleDeg: Double) {
        moduleDirection.directionAngleDeg = angleDeg
    }

    /**
     * 設置方向 (Direction) から角度を設定
     */
    fun setDirection(direction: Direction) {
        // Direction.toYRot() で水平角度（NORTH=180, SOUTH=0, WEST=90, EAST=270 等）を取得
        moduleDirection.directionAngleDeg = direction.toYRot().toDouble()
    }

    /**
     * プレイヤー設置時の向き初期化処理
     */
    fun onBlockPlacedBy(
        level: Level,
        placer: LivingEntity?,
        stack: ItemStack,
        state: BlockState? = null
    ) {
        // placer != null の代わりに ?.let を使用
        placer?.let { entity ->
            val facing = entity.direction.opposite
            setDirection(facing)
        }
    }
}