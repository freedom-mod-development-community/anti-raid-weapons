package xyz.fmdc.arw.client.util;

public interface IRadar {
    /**
     * フレーム補間された現在の回転角度（Yaw）を取得
     */
    float getRotationYaw(float partialTick);

    /**
     * 定常回転以外に再生したいアニメーションがあれば指定（無ければ null）
     */
    default String getActiveAnimationName() {
        return null;
    }

    default float getAnimationTimeSeconds(float partialTick) {
        return 0.0f;
    }
}
