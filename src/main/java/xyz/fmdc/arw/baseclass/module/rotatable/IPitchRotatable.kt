package xyz.fmdc.arw.baseclass.module.rotatable

import xyz.fmdc.arw.network.PacketHandlerARW

interface IPitchRotatable {
    val modulePitchRotatable: ModulePitchRotatable

    fun getDefaultPitchDeg(): Double {
        return 0.0
    }

    fun syncAngleToClient() {
        PacketHandlerARW.sendPacketAll(SyncAngleMessage(0.0, pitchDeg))
    }
}

var IPitchRotatable.pitchDeg: Double
    get() = modulePitchRotatable.pitchDeg
    set(value) {
        modulePitchRotatable.pitchDeg = value
    }

var IPitchRotatable.pitchRad: Double
    get() = Math.toRadians(modulePitchRotatable.pitchDeg)
    set(value) {
        modulePitchRotatable.pitchDeg = Math.toDegrees(value)
    }


