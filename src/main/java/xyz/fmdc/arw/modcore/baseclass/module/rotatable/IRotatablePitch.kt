package xyz.fmdc.arw.modcore.baseclass.module.rotatable

import xyz.fmdc.arw.modcore.PacketHandlerARW

interface IRotatablePitch {
    val moduleRotatablePitch: ModuleRotatablePitch

    var pitchDeg: Double
        get() = moduleRotatablePitch.pitchDeg
        set(value) {
            moduleRotatablePitch.pitchDeg = value
        }

    var pitchRad: Double
        get() = moduleRotatablePitch.pitchRad
        set(value) {
            moduleRotatablePitch.pitchRad = value
        }

    fun getDefaultPitchDeg(): Double {
        return 0.0
    }

    fun syncAngleSerToCli() {
        PacketHandlerARW.sendPacketAll(SyncAngleMessage(0.0, pitchDeg))
    }
}