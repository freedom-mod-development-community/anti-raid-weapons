package xyz.fmdc.arw.client.util;

import org.joml.Quaternionf;

public interface INodeRotatableModel {
    /**
     * 指定されたノード名の描画用補間済みクォータニオンを取得します。
     * @param nodeName GLB内のノード名 (小文字)
     * @param partialTick フレーム間補間値
     * @return 回転クォータニオン (データがない場合は null)
     */
    Quaternionf getNodeRotation(String nodeName, float partialTick);
}