package xyz.fmdc.arw.baseclass.module.rotatable

import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Mth

class ModulePitchRotatable {
    var prevPitchAngleDeg: Float = 0.0f
    var pitchAngleDeg: Float = 0.0f
    var targetPitchAngleDeg: Float = 0.0f
    var rotSpeedPitch: Float = 3.0f

    var minPitch: Float = -10.0f
    var maxPitch: Float = 85.0f

    private val strPitch = "pitchAngleDeg"
    private val strTargetPitch = "targetPitchAngleDeg"
    private val strRotSpeedPitch = "rotSpeedPitch"

    /**
     * 描画フレーム間の補間角度を取得
     */
    fun getInterpolatedPitch(partialTicks: Float): Float {
        return Mth.lerp(partialTicks, this.prevPitchAngleDeg, this.pitchAngleDeg)
    }

    /**
     * Tick ごとの回転更新処理
     */
    fun tick() {
        this.prevPitchAngleDeg = this.pitchAngleDeg

        val clampedTarget = Mth.clamp(this.targetPitchAngleDeg, minPitch, maxPitch)
        val diff = clampedTarget - this.pitchAngleDeg

        if (Mth.abs(diff) > 0.001f) {
            val step = Mth.clamp(diff, -rotSpeedPitch, rotSpeedPitch)
            this.pitchAngleDeg = Mth.clamp(this.pitchAngleDeg + step, minPitch, maxPitch)
        } else {
            this.pitchAngleDeg = clampedTarget
        }
    }

    /**
     * 目標仰角を設定
     */
    fun setTargetPitch(pitch: Float) {
        this.targetPitchAngleDeg = Mth.clamp(pitch, minPitch, maxPitch)
    }

    /**
     * NBT 読み込み
     */
    fun readFromNBT(nbt: CompoundTag) {
        if (nbt.contains(strPitch)) {
            this.pitchAngleDeg = nbt.getFloat(strPitch)
            this.prevPitchAngleDeg = this.pitchAngleDeg
        }
        if (nbt.contains(strTargetPitch)) {
            this.targetPitchAngleDeg = nbt.getFloat(strTargetPitch)
        }
        if (nbt.contains(strRotSpeedPitch)) {
            this.rotSpeedPitch = nbt.getFloat(strRotSpeedPitch)
        }
    }

    /**
     * NBT 保存
     */
    fun writeToNBT(nbt: CompoundTag) {
        nbt.putFloat(strPitch, this.pitchAngleDeg)
        nbt.putFloat(strTargetPitch, this.targetPitchAngleDeg)
        nbt.putFloat(strRotSpeedPitch, this.rotSpeedPitch)
    }
}