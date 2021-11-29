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


