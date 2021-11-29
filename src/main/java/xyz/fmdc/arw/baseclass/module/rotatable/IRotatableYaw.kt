package xyz.fmdc.arw.baseclass.module.rotatable

import xyz.fmdc.arw.network.PacketHandlerARW

interface IRotatableYaw {
    val moduleRotatableYaw: ModuleRotatableYaw

    fun getDefaultYaw(directionAngDeg: Double): Double {
        return directionAngDeg
    }

    fun syncAngleSerToCli() {
        PacketHandlerARW.sendPacketAll(SyncAngleMessage(yawDeg, 0.0))
    }
}

var IRotatableYaw.yawDeg: Double
    get() = moduleRotatableYaw.yawDeg
    set(value) {
        moduleRotatableYaw.yawDeg = value
    }
var IRotatableYaw.yawRad: Double
    get() = Math.toRadians(moduleRotatableYaw.yawDeg)
    set(value) {
        moduleRotatableYaw.yawDeg = Math.toDegrees(value)
    }
