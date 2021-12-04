package xyz.fmdc.arw.baseclass.module.direction

import net.minecraft.util.EnumFacing
import xyz.fmdc.arw.getFacingFromAngle
import xyz.fmdc.arw.getHorizontalAngle

interface IDirection {
    val moduleDirection: ModuleDirection

    fun saveReversDirectionData(angDeg: Float) {
        saveReversDirectionData(angDeg.toDouble())
    }

    fun saveReversDirectionData(angDeg: Double) {
        facing = getFacingFromAngle(angDeg + 180)
    }

    fun saveDirectionData(angDeg: Float) {
        saveDirectionData(angDeg.toDouble())
    }

    fun saveDirectionData(angDeg: Double) {
        facing = getFacingFromAngle(angDeg)
    }

    fun getDirectionAngle(): Double {
        return facing.getHorizontalAngle()
    }
}

var IDirection.facing: EnumFacing
    get() = moduleDirection.facing
    set(value) {
        moduleDirection.facing = value
    }
