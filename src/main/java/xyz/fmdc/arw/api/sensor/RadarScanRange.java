package xyz.fmdc.arw.api.sensor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import xyz.fmdc.arw.api.TrackedTarget;

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

    // 諸元プリセット
    public static final RadarScanRange SPQ9B = of(36000.0f, 137.0f, 360.0f, -10.0f, 50.0f);
    public static final RadarScanRange OPS39 = of(20000.0f, 100.0f, 360.0f, -5.0f, 15.0f);

    /**
     * 360度全周回転型の捜索レーダー用ファクトリ
     */
    public static RadarScanRange omni(float maxRange) {
        return new RadarScanRange(maxRange, 0.0f, 360.0f, -90.0f, 90.0f);
    }

    public static RadarScanRange omni(double maxRange) {
        return omni((float) maxRange);
    }

    /**
     * 水平360度全周で、垂直角（Pitch）の範囲を指定する全周レーダー用ファクトリ
     */
    public static RadarScanRange omniWithPitch(float maxRange, float minPitch, float maxPitch) {
        return new RadarScanRange(maxRange, 0.0f, 360.0f, minPitch, maxPitch);
    }

    public static RadarScanRange omniWithPitch(double maxRange, float minPitch, float maxPitch) {
        return omniWithPitch((float) maxRange, minPitch, maxPitch);
    }

    /**
     * 扇形ビームまたは照射型（指向性）レーダー用ファクトリ
     */
    public static RadarScanRange directional(float maxRange, float horizontalFov, float minPitch, float maxPitch) {
        return new RadarScanRange(maxRange, 0.0f, horizontalFov, minPitch, maxPitch);
    }

    public static RadarScanRange directional(double maxRange, float horizontalFov, float minPitch, float maxPitch) {
        return directional((float) maxRange, horizontalFov, minPitch, maxPitch);
    }

    /**
     * 近接不感帯（minRange）も指定可能な詳細ファクトリ
     */
    public static RadarScanRange of(float maxRange, float minRange, float horizontalFov, float minPitch, float maxPitch) {
        return new RadarScanRange(maxRange, minRange, horizontalFov, minPitch, maxPitch);
    }

    public static RadarScanRange of(double maxRange, double minRange, float horizontalFov, float minPitch, float maxPitch) {
        return new RadarScanRange((float) maxRange, (float) minRange, horizontalFov, minPitch, maxPitch);
    }

    public boolean isOmni() {
        return horizontalFov >= 360.0f;
    }

    /**
     * 垂直視野角（vFov: maxPitch - minPitch）を取得
     */
    public float verticalFov() {
        return maxPitch - minPitch;
    }

    /**
     * 目標位置がレーダーの探知幾何範囲（距離、Yaw、Pitch）内にあるか精密判定する。
     *
     * @param radarPos     レーダーアンテナのワールド位置
     * @param antennaYaw   レーダーアンテナのワールド絶対水平方位角 (度, 南=0, 西=90, 北=180, 東=270/-90)
     * @param antennaPitch レーダーアンテナの基準仰角 (度, 水平=0, 見上げ=正)
     * @param targetPos    目標のワールド座標
     * @return 範囲内であれば true
     */
    public boolean isInRange(Vec3 radarPos, float antennaYaw, float antennaPitch, Vec3 targetPos) {
        if (radarPos == null || targetPos == null) return false;

        double dx = targetPos.x - radarPos.x;
        double dy = targetPos.y - radarPos.y;
        double dz = targetPos.z - radarPos.z;

        // 1. 距離判定
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq < (double) minRange * minRange || distSq > (double) maxRange * maxRange) {
            return false;
        }

        // 2. 水平角 (Yaw) 判定
        if (!isOmni()) {
            // Minecraft座標系: +Z=南(0°), -X=西(90°), -Z=北(180°), +X=東(-90°)
            float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float yawDiff = Mth.wrapDegrees(targetYaw - antennaYaw);
            if (Math.abs(yawDiff) > horizontalFov / 2.0f) {
                return false;
            }
        }

        // 3. 垂直角 (Pitch) 判定
        if (minPitch > -90.0f || maxPitch < 90.0f) {
            double distHoriz = Math.sqrt(dx * dx + dz * dz);
            float targetPitch = (float) Math.toDegrees(Math.atan2(dy, distHoriz));
            float relPitch = targetPitch - antennaPitch;
            if (relPitch < minPitch || relPitch > maxPitch) {
                return false;
            }
        }

        return true;
    }

    public boolean isInRange(Vec3 radarPos, float antennaYaw, float antennaPitch, TrackedTarget target) {
        return target != null && isInRange(radarPos, antennaYaw, antennaPitch, target.getLastKnownPos());
    }

    public boolean isInRange(Vec3 radarPos, float antennaYaw, float antennaPitch, Entity target) {
        return target != null && isInRange(radarPos, antennaYaw, antennaPitch, target.position());
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
