package xyz.fmdc.arw.common.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import xyz.fmdc.arw.AntiRaidWeapons;

import java.util.Locale;
import java.util.UUID;

/**
 * クライアント側でのみスポーン・描画される仮想飛翔体のダミーEntity。
 * 当たり判定（AABB）を持たず、パーティクル・トレーサー描画およびローカルでの弾道補間のみを行います。
 * パケットロス時でもゴースト化しないよう自律TTL（自己消滅タイマー）を内蔵しています。
 */
public class ClientDummyProjectileEntity extends Entity {

    private UUID projectileId = UUID.randomUUID();
    private byte projectileType = 0; // 0: CIWS, 1: 5-INCH, 2: MISSILE
    private Vec3 velocityMetersPerSecond = Vec3.ZERO;
    private int clientTtl = 200; // 安全マージン付きTTL
    private int flightTicks = 0;

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
        this.setOldPosAndRot();
        this.velocityMetersPerSecond = initialVelocityMps;
        // パケット消失に備え、サーバーの指定寿命 + 20 Tick のセーフティマージンを持たせる
        this.clientTtl = maxAgeTicks + 20;
        this.flightTicks = 0;

        // 初期方向設定
        Vec3 dir = initialVelocityMps.normalize();
        double d0 = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        this.setYRot((float) (Math.atan2(dir.x, dir.z) * (180.0 / Math.PI)));
        this.setXRot((float) (Math.atan2(dir.y, d0) * (180.0 / Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();

        AntiRaidWeapons.LOGGER.info(
                String.format(Locale.ROOT,
                        "[ClientDummyProjectile] Initialized dummy [%s] (Type: %s, Pos: [%.2f, %.2f, %.2f], Vel: [%.2f, %.2f, %.2f] m/s, Speed: %.2f m/s, TTL: %d ticks)",
                        this.projectileId,
                        getTypeName(this.projectileType),
                        initialPos.x, initialPos.y, initialPos.z,
                        initialVelocityMps.x, initialVelocityMps.y, initialVelocityMps.z,
                        initialVelocityMps.length(),
                        this.clientTtl
                )
        );
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

    public static String getTypeName(byte type) {
        return switch (type) {
            case 0 -> "CIWS";
            case 1 -> "5-INCH";
            case 2 -> "MISSILE";
            default -> "UNKNOWN(" + type + ")";
        };
    }

    /**
     * サーバーからの着弾・破壊通知時に呼び出され、着弾ログを出力してダミーEntityを破棄します。
     *
     * @param hitPosition   着弾・起爆座標
     * @param destroyReason 破棄理由（0: HIT, 1: AIR_BURST, 2: TIMEOUT 等）
     */
    public void onImpact(Vec3 hitPosition, byte destroyReason) {
        String reasonStr = switch (destroyReason) {
            case 0 -> "HIT";
            case 1 -> "AIR_BURST";
            case 2 -> "TIMEOUT";
            default -> "REASON_" + destroyReason;
        };

        AntiRaidWeapons.LOGGER.info(
                String.format(Locale.ROOT,
                        "[ClientDummyProjectile] Impact/Destroy dummy [%s] (Type: %s, flightTicks: %d, reason: %s) at (%.2f, %.2f, %.2f)",
                        this.projectileId,
                        getTypeName(this.projectileType),
                        this.flightTicks,
                        reasonStr,
                        hitPosition.x, hitPosition.y, hitPosition.z
                )
        );

        this.discard();
    }

    @Override
    public void tick() {
        // クライアント側でのみ動作
        if (!this.level().isClientSide) {
            this.discard();
            return;
        }

        this.flightTicks++;

        // 描画補間用の直前位置・角度を記録
        this.setOldPosAndRot();

        Vec3 prevPos = this.position();

        // 1. 移動更新（m/s を 1/20 にスケールして 1Tick あたりの移動量を適用）
        Vec3 step = this.velocityMetersPerSecond.scale(1.0 / 20.0);
        Vec3 newPos = prevPos.add(step);
        this.setPos(newPos.x, newPos.y, newPos.z);

        // 2. 弾種別の簡易物理補正 & 高速飛翔線分のパーティクル補間生成
        double stepLength = step.length();

        if (this.projectileType == 0) {
            // CIWS / 機関砲弾 (900m/s = 45m/tick)
            // 高速移動線分を補間して、途切れのない光のトレーサー（曳光弾）を生成
            int steps = Math.min(10, Math.max(3, (int) Math.ceil(stepLength / 4.0)));
            for (int i = 0; i < steps; i++) {
                double fraction = (double) i / steps;
                double px = Mth.lerp(fraction, prevPos.x, newPos.x);
                double py = Mth.lerp(fraction, prevPos.y, newPos.y);
                double pz = Mth.lerp(fraction, prevPos.z, newPos.z);

                this.level().addParticle(
                        ParticleTypes.CRIT,
                        px, py, pz,
                        -step.x * 0.05, -step.y * 0.05, -step.z * 0.05
                );
            }
        } else if (this.projectileType == 1) {
            // 5インチ通常砲弾: 簡易重力減衰 (-9.8m/s^2 -> 1Tickあたり -0.49m/s)
            this.velocityMetersPerSecond = this.velocityMetersPerSecond.add(0, -9.80665 / 20.0, 0);

            // 弾道スモークトレイルの線分補間
            int steps = Math.min(8, Math.max(2, (int) Math.ceil(stepLength / 5.0)));
            for (int i = 0; i < steps; i++) {
                double fraction = (double) i / steps;
                double px = Mth.lerp(fraction, prevPos.x, newPos.x);
                double py = Mth.lerp(fraction, prevPos.y, newPos.y);
                double pz = Mth.lerp(fraction, prevPos.z, newPos.z);

                this.level().addParticle(
                        ParticleTypes.SMOKE,
                        px, py, pz,
                        0, 0.01, 0
                );
            }
        } else if (this.projectileType == 2) {
            // ミサイル: ロケット噴進エフェクト（後方に濃い白煙と火炎）
            int steps = Math.min(6, Math.max(2, (int) Math.ceil(stepLength / 6.0)));
            for (int i = 0; i < steps; i++) {
                double fraction = (double) i / steps;
                double px = Mth.lerp(fraction, prevPos.x, newPos.x);
                double py = Mth.lerp(fraction, prevPos.y, newPos.y);
                double pz = Mth.lerp(fraction, prevPos.z, newPos.z);

                this.level().addParticle(
                        ParticleTypes.FLAME,
                        px, py, pz,
                        -step.x * 0.1, -step.y * 0.1, -step.z * 0.1
                );
                this.level().addParticle(
                        ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        px, py, pz,
                        0, 0.05, 0
                );
            }
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
            AntiRaidWeapons.LOGGER.warn(
                    String.format(Locale.ROOT,
                            "[ClientDummyProjectile] Dummy [%s] (Type: %s) reached autonomous client TTL limit (flightTicks: %d). Discarding to prevent ghosting at (%.2f, %.2f, %.2f).",
                            this.projectileId,
                            getTypeName(this.projectileType),
                            this.flightTicks,
                            this.getX(), this.getY(), this.getZ()
                    )
            );
            this.discard();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        AntiRaidWeapons.LOGGER.debug(
                String.format(Locale.ROOT,
                        "[ClientDummyProjectile] Removing dummy [%s] (Type: %s, flightTicks: %d, reason: %s) at (%.2f, %.2f, %.2f)",
                        this.projectileId,
                        getTypeName(this.projectileType),
                        this.flightTicks,
                        reason,
                        this.getX(), this.getY(), this.getZ()
                )
        );
        super.remove(reason);
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
