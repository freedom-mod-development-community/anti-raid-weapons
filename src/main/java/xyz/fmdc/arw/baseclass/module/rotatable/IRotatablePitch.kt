package xyz.fmdc.arw.baseclass.module.rotatable

import xyz.fmdc.arw.network.PacketHandlerARW

interface IRotatablePitch {
    fun getDefaultPitchDeg(): Double {
        return 0.0
    }

    fun syncAngleSerToCli() {
        PacketHandlerARW.sendPacketAll(SyncAngleMessage(0.0, pitchDeg))
    }
}

private var backingPitchDeg: Double = 0.0
var IRotatablePitch.pitchDeg: Double
    get() = backingPitchDeg
    set(value) {
        backingPitchDeg = value
    }

