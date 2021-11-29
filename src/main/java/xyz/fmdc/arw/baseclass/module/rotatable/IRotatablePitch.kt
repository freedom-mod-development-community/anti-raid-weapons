package xyz.fmdc.arw.baseclass.module.rotatable

import xyz.fmdc.arw.network.PacketHandlerARW

interface IRotatablePitch {
    val moduleRotatablePitch: ModuleRotatablePitch

    fun getDefaultPitchDeg(): Double {
        return 0.0
    }

    fun syncAngleSerToCli() {
        PacketHandlerARW.sendPacketAll(SyncAngleMessage(0.0, pitchDeg))
    }
}

var IRotatablePitch.pitchDeg: Double
    get() = moduleRotatablePitch.pitchDeg
    set(value) {
        moduleRotatablePitch.pitchDeg = value
    }

var IRotatablePitch.pitchRad: Double
    get() = Math.toRadians(moduleRotatablePitch.pitchDeg)
    set(value) {
        moduleRotatablePitch.pitchDeg = Math.toDegrees(value)
    }


