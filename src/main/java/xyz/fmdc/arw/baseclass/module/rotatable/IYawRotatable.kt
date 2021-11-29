package xyz.fmdc.arw.baseclass.module.rotatable

import xyz.fmdc.arw.network.PacketHandlerARW

interface IYawRotatable {
    val moduleYawRotatable: ModuleYawRotatable

    fun getDefaultYaw(directionAngDeg: Double): Double {
        return directionAngDeg
    }

    fun syncAngleToClient() {
        PacketHandlerARW.sendPacketAll(SyncAngleMessage(yawDeg, 0.0))
    }
}

var IYawRotatable.yawDeg: Double
    get() = moduleYawRotatable.yawDeg
    set(value) {
        moduleYawRotatable.yawDeg = value
    }
var IYawRotatable.yawRad: Double
    get() = Math.toRadians(moduleYawRotatable.yawDeg)
    set(value) {
        moduleYawRotatable.yawDeg = Math.toDegrees(value)
    }
