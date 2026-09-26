package xyz.fmdc.arw.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class RadarMathUtil {

    /**
     * エンティティがレーダーの視野（ビームFOV）内に入っているか判定する
     *
     * @param radarPos レーダーのワールド位置 (ブロック中心など)
     * @param yawDeg   レーダーアンテナのワールド合成Yaw角度 (度, 南=0, 西=90, 北=180, 東=270)
     * @param pitchDeg レーダーアンテナのワールドPitch角度 (度, 水平=0, 見上げ=+度, 見下ろし=-度)
     * @param target   判定対象のEntity
     * @param maxRange 最大探知距離 (ブロック数)
     * @param fovHoriz 水平視野角 (度) - 例: 30.0f (左右15度ずつ)
     * @param fovVert  垂直視野角 (度) - 例: 40.0f (上下20度ずつ)
     * @return 視野内であれば true
     */
    public static boolean isEntityInRadarFOV(Vector3f radarPos, float yawDeg, float pitchDeg,
                                             Entity target, float maxRange,
                                             float fovHoriz, float fovVert) {

        // 1. レーダーからターゲットへの相対ベクトルを計算
        double dx = target.getX() - radarPos.x;
        double dy = target.getY() - radarPos.y;
        double dz = target.getZ() - radarPos.z;

        double distanceSquared = dx * dx + dy * dy + dz * dz;

        // 探知範囲外なら即座に除外
        if (distanceSquared > (double) maxRange * maxRange || distanceSquared < 0.0001) {
            return false;
        }

        // 全周かつ全天球であれば角度計算不要
        if (fovHoriz >= 360.0f && fovVert >= 180.0f) {
            return true;
        }

        // 2. Minecraft標準の視線単位ベクトルを算出
        // MinecraftのdirectionFromRotation(pitch, yaw):
        // pitch: 正で見下ろし、負で見上げのMinecraft仕様に合わせる場合は符号に注意
        // ここでは通常仕様（水平0度、ピッチ正で見上げ）を想定
        Vec3 forward = Vec3.directionFromRotation(-pitchDeg, yawDeg).normalize();

        // 3. ターゲット方向の正規化ベクトル
        double dist = Math.sqrt(distanceSquared);
        double targetDirX = dx / dist;
        double targetDirY = dy / dist;
        double targetDirZ = dz / dist;

        // --- A. 水平 (Yaw) 角度差の判定 (XZ平面) ---
        if (fovHoriz < 360.0f) {
            double fwdLenXZ = Math.sqrt(forward.x * forward.x + forward.z * forward.z);
            double tgtLenXZ = Math.sqrt(targetDirX * targetDirX + targetDirZ * targetDirZ);

            if (fwdLenXZ > 0.0001 && tgtLenXZ > 0.0001) {
                double dotHoriz = (forward.x * targetDirX + forward.z * targetDirZ) / (fwdLenXZ * tgtLenXZ);
                dotHoriz = Math.max(-1.0, Math.min(1.0, dotHoriz));
                double angleHorizDeg = Math.toDegrees(Math.acos(dotHoriz));

                if (angleHorizDeg > (double) fovHoriz / 2.0) {
                    return false; // 水平FOV外
                }
            }
        }

        // --- B. 垂直 (Pitch) 角度差の判定 ---
        if (fovVert < 180.0f) {
            double dotTotal = forward.x * targetDirX + forward.y * targetDirY + forward.z * targetDirZ;
            dotTotal = Math.max(-1.0, Math.min(1.0, dotTotal));
            double angleVertDeg = Math.toDegrees(Math.acos(dotTotal));

            if (angleVertDeg > (double) fovVert / 2.0) {
                return false; // 垂直FOV外
            }
        }

        return true;
    }
}
