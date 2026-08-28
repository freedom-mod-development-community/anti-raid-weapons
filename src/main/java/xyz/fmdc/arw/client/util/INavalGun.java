package xyz.fmdc.arw.client.util;

import xyz.fmdc.arw.client.renderer.GenericGlbRenderer;

import java.util.List;

// INavalGun.java (艦砲用)
public interface INavalGun {
    float getTargetYaw(float partialTick);
    float getTargetPitch(float partialTick);

    /**
     * 現在再生中の全アニメーションとその補間時間（秒）のリストを取得します
     */
    List<GenericGlbRenderer.ActiveAnimation> getActiveAnimations(float partialTick);
}
