package xyz.fmdc.arw.modcore.baseclass.module.rotatable

import xyz.fmdc.arw.network.PacketHandlerARW

interface IRotatableYaw {
    val moduleRotatableYaw: ModuleRotatableYaw

    var yawDeg: Double
        get() = moduleRotatableYaw.yawDeg
        set(value) {
            moduleRotatableYaw.yawDeg = value
        }

    var yawRad: Double
        get() = moduleRotatableYaw.yawRad
        set(value) {
            moduleRotatableYaw.yawRad = value
        }

    fun getDefaultYaw(directionAngDeg: Double): Double {
        return directionAngDeg
    }

    fun syncAngleSerToCli() {
        PacketHandlerARW.sendPacketAll(SyncAngleMessage(yawDeg, 0.0))
    }
}