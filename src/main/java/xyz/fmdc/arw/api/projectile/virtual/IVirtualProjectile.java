package xyz.fmdc.arw.api.projectile.virtual;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * サーバー側でEntityとしてスポーンさせず、純粋な数値計算とシュレーディンガーのチャンク判定で処理される
 * 仮想飛翔体（Virtual Projectile）のインターフェース。
 */
public interface IVirtualProjectile {

    /** 飛翔体固有の一意なUUID */
    UUID getProjectileId();

    /** 飛翔体が飛行中のディメンション */
    ResourceKey<Level> getDimension();

    /** 現在位置 [m] */
    Vec3 getPosition();

    /** 1Tick前の位置 [m] */
    Vec3 getPrevPosition();

    /** 現在の速度ベクトル [m/s] */
    Vec3 getVelocity();

    /** 経過Tick数 */
    int getAgeTicks();

    /** 最大生存可能Tick数 (TTL) */
    int getMaxAgeTicks();

    /** 消滅フラグ */
    boolean isDead();

    /** 消滅させる */
    void markDead();

    /**
     * 毎Tick呼び出される更新処理。
     * @param level 飛翔体が存在するディメンションのServerLevel
     */
    void tick(ServerLevel level);

    /**
     * 衝突判定時のコールバック
     * @param level ServerLevel
     * @param hitResult 命中情報
     */
    void onHit(ServerLevel level, HitResult hitResult);

    /** 弾薬種別の判別ID (パケット同期・描画種別の決定用) */
    byte getProjectileTypeId();
}
