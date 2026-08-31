package xyz.fmdc.arw.util;

import net.minecraft.world.phys.Vec3;

public class FcsMathUtil {

    /**
     * 重力を考慮しない直線弾道（高初速砲・レーザー等）の偏差着弾点を計算
     */
    public static Vec3 calculateInterceptPosition(Vec3 shooterPos, Vec3 targetPos, Vec3 targetVel, float projectileSpeed) {
        Vec3 relPos = targetPos.subtract(shooterPos);

        double a = targetVel.lengthSqr() - (projectileSpeed * projectileSpeed);
        double b = 2.0 * relPos.dot(targetVel);
        double c = relPos.lengthSqr();

        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) {
            // 追いつかない場合は現在の目標位置を返す
            return targetPos;
        }

        double t1 = (-b - Math.sqrt(discriminant)) / (2 * a);
        double t2 = (-b + Math.sqrt(discriminant)) / (2 * a);

        double t = -1;
        if (t1 > 0 && t2 > 0) t = Math.min(t1, t2);
        else if (t1 > 0) t = t1;
        else if (t2 > 0) t = t2;

        if (t < 0) return targetPos;

        return targetPos.add(targetVel.scale(t));
    }

    /**
     * ワールド座標のベクトルからYaw/Pitch角度(度)へ変換
     */
    public static float[] calculateYawPitch(Vec3 origin, Vec3 target) {
        Vec3 dir = target.subtract(origin).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) Math.toDegrees(Math.asin(dir.y));
        return new float[]{yaw, pitch};
    }
}