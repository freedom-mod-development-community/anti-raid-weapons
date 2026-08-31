package xyz.fmdc.arw.client.util;

import xyz.fmdc.arw.client.renderer.GenericGlbRenderer;

import java.util.List;
/**
 * YawとPitchをコード側から制御でき、アニメーションを名前指定で並列再生できるようにするもの.
 */
public interface IYawPitchAnimatableModel {
    float getRenderTargetYaw(float partialTick);
    float getRenderTargetPitch(float partialTick);

    /**
     * 現在再生中の全アニメーションとその補間時間（秒）のリストを取得します
     */
    List<GenericGlbRenderer.ActiveAnimation> getActiveAnimations(float partialTick);
}
