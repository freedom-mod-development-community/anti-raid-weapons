package xyz.fmdc.arw.baseclass.module.rotatable

import xyz.fmdc.arw.network.PacketHandlerARW

interface IRotatableYaw {
    fun getDefaultYaw(directionAngDeg: Double): Double {
        return directionAngDeg
    }

    fun syncAngleSerToCli() {
        PacketHandlerARW.sendPacketAll(SyncAngleMessage(yawDeg, 0.0))
    }
}

private var backingYawDeg: Double = 0.0
var IRotatableYaw.yawDeg: Double
    get() = backingYawDeg
    set(value) {
        backingYawDeg = value
    }