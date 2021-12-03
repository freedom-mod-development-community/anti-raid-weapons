package xyz.fmdc.arw.baseclass.module.rotatable

import xyz.fmdc.arw.network.PacketHandlerARW

interface IYawPitchRotatable : IYawRotatable, IPitchRotatable {
    override fun syncAngleToClient() {
        PacketHandlerARW.sendPacketAll(SyncAngleMessage(yawDeg, pitchDeg))
    }
}
