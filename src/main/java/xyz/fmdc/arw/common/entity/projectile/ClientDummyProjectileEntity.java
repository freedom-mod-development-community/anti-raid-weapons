package xyz.fmdc.arw.common.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import xyz.fmdc.arw.registry.ModEntities;

import java.util.UUID;

/**
 * クライアント側でのみスポーン・描画される仮想飛翔体のダミーEntity。
 * 当たり判定を持たず、パーティクル・トレーサー描画およびローカルでの弾道補間のみを行います。
 * パケットロス時でもゴースト化しないよう自律TTL（自己消滅タイマー）を内蔵しています。
 */
public class ClientDummyProjectileEntity extends Entity {

    private UUID projectileId = UUID.randomUUID();
    private byte projectileType = 0; // 0: CIWS, 1: 5-INCH, 2: MISSILE
    private Vec3 velocityMetersPerSecond = Vec3.ZERO;
    private int clientTtl = 200; // 安全マージン付きTTL

    public ClientDummyProjectileEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.blocksBuilding = false;
    }

    public void initData(
            UUID projectileId,
            byte projectileType,
            Vec3 initialPos,
            Vec3 initialVelocityMps,
            int maxAgeTicks,
            CompoundTag extra
    ) {
        this.projectileId = projectileId;
        this.projectileType = projectileType;
        this.setPos(initialPos.x, initialPos.y, initialPos.z);
        this.velocityMetersPerSecond = initialVelocityMps;
        // パケット消失に備え、サーバーの指定寿命 + 20 Tick のセーフティマージンを持たせる
        this.clientTtl = maxAgeTicks + 20;

        // 初期方向設定
        Vec3 dir = initialVelocityMps.normalize();
        double d0 = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        this.setYRot((float) (Math.atan2(dir.x, dir.z) * (180.0 / Math.PI)));
        this.setXRot((float) (Math.atan2(dir.y, d0) * (180.0 / Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public UUID getProjectileId() {
        return this.projectileId;
    }

    public byte getProjectileType() {
        return this.projectileType;
    }

    public Vec3 getVelocityMetersPerSecond() {
        return this.velocityMetersPerSecond;
    }

    @Override
    public void tick() {
        // クライアント側でのみ動作
        if (!this.level().isClientSide) {
            this.discard();
            return;
        }

        this.xOld = this.getX();
        this.yOld = this.getY();
        this.zOld = this.getZ();

        // 1. 移動更新（m/s を 1/20 にスケールして 1Tick あたりの移動量を適用）
        Vec3 step = this.velocityMetersPerSecond.scale(1.0 / 20.0);
        this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);

        // 2. 弾種別の簡易物理補正 & エフェクト
        if (this.projectileType == 0) {
            // CIWS / 機関砲弾: 直進 + 高速トレイサー（黄緑・橙の火花パーティクル）
            this.level().addParticle(
                    ParticleTypes.CRIT,
                    this.getX(), this.getY(), this.getZ(),
                    -step.x * 0.1, -step.y * 0.1, -step.z * 0.1
            );
        } else if (this.projectileType == 1) {
            // 5インチ通常砲弾: 簡易重力減衰 (-9.8m/s^2 -> 1Tickあたり -0.49m/s)
            this.velocityMetersPerSecond = this.velocityMetersPerSecond.add(0, -9.80665 / 20.0, 0);
            this.level().addParticle(
                    ParticleTypes.SMOKE,
                    this.getX(), this.getY(), this.getZ(),
                    0, 0, 0
            );
        } else if (this.projectileType == 2) {
            // ミサイル: ロケット噴進エフェクト（濃い白煙と炎）
            this.level().addParticle(
                    ParticleTypes.FLAME,
                    this.getX() - step.x * 0.5, this.getY() - step.y * 0.5, this.getZ() - step.z * 0.5,
                    0, 0, 0
            );
            this.level().addParticle(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    this.getX() - step.x * 0.8, this.getY() - step.y * 0.8, this.getZ() - step.z * 0.8,
                    0, 0.05, 0
            );
        }

        // 姿勢（Yaw/Pitch）を進行方向に更新
        if (step.lengthSqr() > 1.0E-5) {
            Vec3 dir = step.normalize();
            double d0 = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
            this.setYRot((float) (Math.atan2(dir.x, dir.z) * (180.0 / Math.PI)));
            this.setXRot((float) (Math.atan2(dir.y, d0) * (180.0 / Math.PI)));
        }

        // 3. ゴースト化防止の自律TTLカウントダウン
        this.clientTtl--;
        if (this.clientTtl <= 0) {
            this.discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
