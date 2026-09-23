package xyz.fmdc.arw.api.sensor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * レーダーの探索範囲・ビーム幾何パラメータを保持するレコード。
 *
 * @param maxRange      最大探知距離 (m / blocks)
 * @param minRange      最小探知距離 (近接ブラインドゾーン, 通常 0.0f)
 * @param horizontalFov 水平視野角 (度, 360.0f で全周回転型、扇形ビーム等は 60.0f など)
 * @param minPitch      最小仰角 (度, 例: -15.0f, 全天球なら -90.0f)
 * @param maxPitch      最大仰角 (度, 例: 75.0f, 全天球なら 90.0f)
 */
public record RadarScanRange(
        float maxRange,
        float minRange,
        float horizontalFov,
        float minPitch,
        float maxPitch
) {
    public static final RadarScanRange DEFAULT = omni(512.0f);

    /**
     * 360度全周回転型の捜索レーダー用ファクトリ
     */
    public static RadarScanRange omni(float maxRange) {
        return new RadarScanRange(maxRange, 0.0f, 360.0f, -90.0f, 90.0f);
    }

    /**
     * 扇形ビームまたは照射型（指向性）レーダー用ファクトリ
     */
    public static RadarScanRange directional(float maxRange, float horizontalFov, float minPitch, float maxPitch) {
        return new RadarScanRange(maxRange, 0.0f, horizontalFov, minPitch, maxPitch);
    }

    /**
     * 近接不感帯（minRange）も指定可能な詳細ファクトリ
     */
    public static RadarScanRange of(float maxRange, float minRange, float horizontalFov, float minPitch, float maxPitch) {
        return new RadarScanRange(maxRange, minRange, horizontalFov, minPitch, maxPitch);
    }

    public boolean isOmni() {
        return horizontalFov >= 360.0f;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("MaxRange", maxRange);
        tag.putFloat("MinRange", minRange);
        tag.putFloat("HorizontalFov", horizontalFov);
        tag.putFloat("MinPitch", minPitch);
        tag.putFloat("MaxPitch", maxPitch);
        return tag;
    }

    public static RadarScanRange fromTag(CompoundTag tag) {
        if (tag == null) return DEFAULT;
        float max = tag.contains("MaxRange") ? tag.getFloat("MaxRange") : 512.0f;
        float min = tag.getFloat("MinRange");
        float hFov = tag.contains("HorizontalFov") ? tag.getFloat("HorizontalFov") : 360.0f;
        float minP = tag.contains("MinPitch") ? tag.getFloat("MinPitch") : -90.0f;
        float maxP = tag.contains("MaxPitch") ? tag.getFloat("MaxPitch") : 90.0f;
        return new RadarScanRange(max, min, hFov, minP, maxP);
    }

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeFloat(maxRange);
        buf.writeFloat(minRange);
        buf.writeFloat(horizontalFov);
        buf.writeFloat(minPitch);
        buf.writeFloat(maxPitch);
    }

    public static RadarScanRange fromNetwork(FriendlyByteBuf buf) {
        return new RadarScanRange(
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat()
        );
    }
}
