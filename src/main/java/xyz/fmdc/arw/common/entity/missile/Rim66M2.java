package xyz.fmdc.arw.common.entity.missile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.fmdc.arw.common.entity.AbstractMissileEntity;
import xyz.fmdc.arw.registry.ModEntities;

import java.util.List;

/**
 * RIM-66M-2 (SM-2 Block III / Standard Missile 2 MR) エンティティ.
 * <p>
 * 米海軍・海上自衛隊等のイージス艦やターター艦（Mk 13 GMLS等）から発射される中距離艦対空ミサイル。
 * 超高速迎撃性能（最高速度 60m/tick）、Mk 104 ロケットモーターによる加速、
 * Mk 115 爆風破片弾頭およびMk 45 近接信管を忠実に再現。
 */
public class Rim66M2 extends AbstractMissileEntity {

    public Rim66M2(EntityType<? extends Rim66M2> type, Level level) {
        super(type, level);

        // 指定諸元
        this.maxSpeed = 60.0F;           // 最高移動速度 : 60.0m/tick
        this.acceleration = 0.08F;       // 推力加速度 : 0.08 blocks/tick^2
        this.motorBurnTicks = 120;       // モーター燃焼時間 : 120 tick (6.0秒)
        this.maxLifeTicks = 2400;        // 最大寿命 : 2400 tick (120.0秒)

        // 迎撃誘導・信管・弾頭諸元
        this.turnRate = 0.12F;           // 旋回追従係数（高機動迎撃）
        this.proximityFuseRadius = 5.0D; // 近接信管作動半径（ブロック単位）
        this.proximityFuseArmTicks = 15; // 近接信管安全待機時間（0.75秒）
        this.explosionPower = 8.0F;      // 爆発威力（約62kg HE弾頭 Mk 115 相当）
        this.directDamage = 200.0F;      // 直撃物理ダメージ
    }

    public Rim66M2(Level level, double x, double y, double z) {
        this(ModEntities.RIM_66M2.get(), level);
        this.setPos(x, y, z);
    }

    /**
     * 超高速飛翔（最大60m/tick）におけるトンネリングすり抜けを防止する近接信管判定.
     * 前tickの位置から現在位置までの移動線分（スイープAABB）を包含して目標を検知します。
     */
    @Override
    protected void checkProximityFuse() {
        Vec3 motion = this.getDeltaMovement();
        AABB currentBox = this.getBoundingBox().inflate(this.proximityFuseRadius);
        AABB prevBox = this.getBoundingBox().move(-motion.x, -motion.y, -motion.z).inflate(this.proximityFuseRadius);
        AABB sweptBox = currentBox.minmax(prevBox);

        List<LivingEntity> nearby = this.level().getEntitiesOfClass(
                LivingEntity.class,
                sweptBox,
                e -> e.isAlive() && !e.isSpectator() && !e.equals(getOwner()) && !this.isPassengerOfSameVehicle(e)
        );

        if (!nearby.isEmpty()) {
            explode();
        }
    }

    /**
     * 超高速飛翔に対応した連続スモークトレイル・ロケット噴煙エフェクト.
     */
    @Override
    protected void spawnFlightParticles() {
        Vec3 motion = this.getDeltaMovement();
        double speed = motion.length();
        if (speed < 0.01) return;

        Vec3 pos = this.position();
        // 飛翔速度に応じて補間パーティクル数を算出し、途切れのない排気煙を生成
        int steps = Math.min((int) Math.ceil(speed * 1.5), 24);
        Vec3 stepVec = motion.scale(-1.0 / steps);

        for (int i = 0; i < steps; i++) {
            double px = pos.x + stepVec.x * i;
            double py = pos.y + stepVec.y * i;
            double pz = pos.z + stepVec.z * i;

            this.level().addParticle(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    px, py, pz,
                    stepVec.x * 0.05, stepVec.y * 0.05, stepVec.z * 0.05
            );

            // ノズル直近にロケット噴射炎を配置
            if (i < 3) {
                this.level().addParticle(
                        ParticleTypes.FLAME,
                        px, py, pz,
                        0, 0, 0
                );
            }
        }
    }
}
