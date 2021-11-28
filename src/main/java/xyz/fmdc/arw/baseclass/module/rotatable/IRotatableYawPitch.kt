package xyz.fmdc.arw.baseclass.module.rotatable

import xyz.fmdc.arw.network.PacketHandlerARW

interface IRotatableYawPitch : IRotatableYaw, IRotatablePitch {
    override fun syncAngleSerToCli() {
        PacketHandlerARW.sendPacketAll(SyncAngleMessage(yawDeg, pitchDeg))
    }
}
