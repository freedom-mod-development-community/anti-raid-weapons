package xyz.fmdc.arw.modcore.baseclass.module.rotatable

import xyz.fmdc.arw.modcore.PacketHandlerARW

interface IRotatableYawPitch : IRotatableYaw, IRotatablePitch {
    override fun syncAngleSerToCli() {
        PacketHandlerARW.sendPacketAll(SyncAngleMessage(yawDeg, pitchDeg))
    }
}