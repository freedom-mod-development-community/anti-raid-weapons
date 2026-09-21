package xyz.fmdc.arw.api.blockentity;

public interface IYawModel {
    /**
     * フレーム補間された現在の回転角度（Yaw）を取得
     */
    float getTargetYaw(float partialTick);
}
