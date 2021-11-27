package xyz.fmdc.arw.modcore.baseclass.module.direction

import xyz.fmdc.arw.modcore.getFacingFromAngle
import xyz.fmdc.arw.modcore.getHorizontalAngle

interface IDirection {
    val moduleDirection: ModuleDirection

    fun saveReversDirectionData(angDeg: Float) {
        saveReversDirectionData(angDeg.toDouble())
    }

    fun saveReversDirectionData(angDeg: Double) {
        moduleDirection.facing = getFacingFromAngle(angDeg + 180)
    }

    fun saveDirectionData(angDeg: Float) {
        saveDirectionData(angDeg.toDouble())
    }

    fun saveDirectionData(angDeg: Double) {
        moduleDirection.facing = getFacingFromAngle(angDeg)
    }

    fun getDirectionAngle(): Double {
        return moduleDirection.facing.getHorizontalAngle()
    }
}