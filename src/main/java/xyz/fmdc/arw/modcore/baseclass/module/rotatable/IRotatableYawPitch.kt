package xyz.fmdc.arw.modcore.baseclass.module.rotatable

import xyz.fmdc.arw.network.PacketHandlerARW

interface IRotatableYawPitch : IRotatableYaw, IRotatablePitch {
    override fun syncAngleSerToCli() {
        PacketHandlerARW.sendPacketAll(SyncAngleMessage(yawDeg, pitchDeg))
    }
}