package xyz.fmdc.arw.api;

import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

public class RadarMathUtil {

    /**
     * エンティティがレーダーの視界（FOV）内に入っているか判定する
     *
     * @param radarPos レーダーのワールド位置 (ブロック中心など)
     * @param yawDeg   レーダーの現在のYaw角度 (度)
     * @param pitchDeg レーダーの現在のPitch角度 (度)
     * @param target   判定対象のEntity
     * @param maxRange 最大探知距離 (ブロック数)
     * @param fovHoriz 水平視野角 (度) - 例: 60.0f (左右30度ずつ)
     * @param fovVert  垂直視野角 (度) - 例: 40.0f (上下20度ずつ)
     * @return 視界内であれば true
     */
    public static boolean isEntityInRadarFOV(Vector3f radarPos, float yawDeg, float pitchDeg,
                                             Entity target, float maxRange,
                                             float fovHoriz, float fovVert) {

        // 1. レーダーからターゲットへの相対ベクトルを計算
        Vector3f targetPos = new Vector3f(
                (float) target.getX(),
                (float) target.getY(),
                (float) target.getZ()
        );
        Vector3f dirToTarget = new Vector3f(targetPos).sub(radarPos);

        float distanceSquared = dirToTarget.lengthSquared();

        // 探知範囲外なら即座に除外
        if (distanceSquared > maxRange * maxRange || distanceSquared < 0.0001f) {
            return false;
        }

        // 距離を正規化 (単位ベクトル化)
        dirToTarget.normalize();

        // 2. レーダーの現在の向き (Yaw / Pitch) から視線単位ベクトルを計算
        // Minecraftの角度系 (Yaw: Y軸周り, Pitch: X軸周り)
        float yawRad = (float) Math.toRadians(-yawDeg);
        float pitchRad = (float) Math.toRadians(-pitchDeg);

        // 視線ベクトルの算出
        float cosPitch = (float) Math.cos(pitchRad);
        Vector3f forwardVector = new Vector3f(
                (float) (-Math.sin(yawRad) * cosPitch),
                (float) Math.sin(pitchRad),
                (float) (-Math.cos(yawRad) * cosPitch)
        ).normalize();

        // 3. 水平方向・垂直方向の角度差を判定

        // --- A. 水平 (Yaw) 角度差の判定 (XZ平面に投影) ---
        Vector3f forwardXZ = new Vector3f(forwardVector.x, 0, forwardVector.z).normalize();
        Vector3f targetXZ = new Vector3f(dirToTarget.x, 0, dirToTarget.z).normalize();

        // 内積からなす角の余弦(cos)を取得
        float dotHoriz = forwardXZ.dot(targetXZ);
        // クランプ処理（数値誤差対策）
        dotHoriz = Math.max(-1.0f, Math.min(1.0f, dotHoriz));
        float angleHorizDeg = (float) Math.toDegrees(Math.acos(dotHoriz));

        if (angleHorizDeg > fovHoriz / 2.0f) {
            return false; // 水平FOV外
        }

        // --- B. 垂直 (Pitch) 角度差の判定 ---
        // 前方ベクトルとターゲットベクトルの全体のなす角を判定 (簡易実装)
        float dotTotal = forwardVector.dot(dirToTarget);
        dotTotal = Math.max(-1.0f, Math.min(1.0f, dotTotal));
        float angleVertDeg = (float) Math.toDegrees(Math.acos(dotTotal));

        if (angleVertDeg > fovVert / 2.0f) {
            return false; // 垂直FOV外
        }

        return true; // すべての条件をクリア（探知成功）
    }
}
