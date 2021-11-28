package xyz.fmdc.arw.baseclass.module.direction

import net.minecraft.util.EnumFacing
import xyz.fmdc.arw.getFacingFromAngle
import xyz.fmdc.arw.getHorizontalAngle

interface IDirection {
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

private var backingFacing: EnumFacing = EnumFacing.UP
var IDirection.facing: EnumFacing
    get() = backingFacing
    set(value) {
        backingFacing = value
    }
