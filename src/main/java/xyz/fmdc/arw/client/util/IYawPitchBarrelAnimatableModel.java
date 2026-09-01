package xyz.fmdc.arw.client.util;

import xyz.fmdc.arw.client.renderer.GenericFastGlbRenderer;

import java.util.List;

/**
 * YawとPitchをコード側から制御でき、アニメーションを名前指定で並列再生できるようにするもの.
 */
public interface IYawPitchBarrelAnimatableModel {
    float getRenderTargetYaw(float partialTick);
    float getRenderTargetPitch(float partialTick);
    float getRenderBarrelAng(float partialTick);
    

    /**
     * 現在再生中の全アニメーションとその補間時間（秒）のリストを取得します
     */
    List<GenericFastGlbRenderer.ActiveAnimation> getActiveAnimations(float partialTick);
}
