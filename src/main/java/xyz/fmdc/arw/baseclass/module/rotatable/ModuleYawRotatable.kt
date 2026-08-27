package xyz.fmdc.arw.baseclass.module.rotatable

import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Mth

class ModuleYawRotatable {
    var prevYawAngleDeg: Float = 0.0f
    var yawAngleDeg: Float = 0.0f
    var targetYawAngleDeg: Float = 0.0f
    var rotSpeedYaw: Float = 5.0f
    var isContinuousRotating: Boolean = false

    private val strYaw = "yawAngleDeg"
    private val strTargetYaw = "targetYawAngleDeg"
    private val strRotSpeedYaw = "rotSpeedYaw"
    private val strIsContinuous = "isContinuousRotating"

    fun getInterpolatedYaw(partialTicks: Float): Float {
        return Mth.rotLerp(partialTicks, this.prevYawAngleDeg, this.yawAngleDeg)
    }

    fun tick() {
        this.prevYawAngleDeg = this.yawAngleDeg

        if (isContinuousRotating) {
            this.yawAngleDeg = Mth.wrapDegrees(this.yawAngleDeg + rotSpeedYaw)
        } else {
            val diff = Mth.degreesDifference(this.yawAngleDeg, this.targetYawAngleDeg)
            if (Mth.abs(diff) > 0.001f) {
                val step = Mth.clamp(diff, -rotSpeedYaw, rotSpeedYaw)
                this.yawAngleDeg = Mth.wrapDegrees(this.yawAngleDeg + step)
            } else {
                this.yawAngleDeg = this.targetYawAngleDeg
            }
        }
    }

    fun setTargetYaw(yaw: Float) {
        this.isContinuousRotating = false
        this.targetYawAngleDeg = Mth.wrapDegrees(yaw)
    }

    fun readFromNBT(nbt: CompoundTag) {
        if (nbt.contains(strYaw)) {
            this.yawAngleDeg = nbt.getFloat(strYaw)
            this.prevYawAngleDeg = this.yawAngleDeg
        }
        if (nbt.contains(strTargetYaw)) {
            this.targetYawAngleDeg = nbt.getFloat(strTargetYaw)
        }
        if (nbt.contains(strRotSpeedYaw)) {
            this.rotSpeedYaw = nbt.getFloat(strRotSpeedYaw)
        }
        if (nbt.contains(strIsContinuous)) {
            this.isContinuousRotating = nbt.getBoolean(strIsContinuous)
        }
    }

    fun writeToNBT(nbt: CompoundTag) {
        nbt.putFloat(strYaw, this.yawAngleDeg)
        nbt.putFloat(strTargetYaw, this.targetYawAngleDeg)
        nbt.putFloat(strRotSpeedYaw, this.rotSpeedYaw)
        nbt.putBoolean(strIsContinuous, this.isContinuousRotating)
    }
}