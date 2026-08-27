package xyz.fmdc.arw.baseclass.module.direction

import net.minecraft.core.Direction

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
}