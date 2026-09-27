package xyz.fmdc.arw.common.projectile.virtual;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.api.projectile.BallisticsEngine;
import xyz.fmdc.arw.common.projectile.telemetry.FlightTelemetryLogger;
import xyz.fmdc.arw.common.projectile.virtual.util.SafeExplosionHelper;
import xyz.fmdc.arw.common.entity.projectile.FiveInchAmmoType;
import xyz.fmdc.arw.network.PacketHandler;
import xyz.fmdc.arw.network.S2CDestroyVirtualProjectilePacket;

import java.util.UUID;

/**
 * 5インチ艦砲などの通常砲弾用の仮想飛翔体（Virtual Projectile）。
 * <p>
 * {@link BallisticsEngine} によるISA標準大気密度減衰・遷音速波抵抗・重力を含む外弾道計算を
 * サーバー側インメモリで正確に継続しながら飛行します。
 * 未ロードチャンク上空を通過する際はブロック・エンティティの走査（レイキャスト）やチャンク強制ロードを安全にスキップし、
 * ロード済みチャンクへの突入・着弾時に {@link SafeExplosionHelper} を介してカスケード地形破壊を防止しつつ安全に爆発します。
 */
public class BallisticShellProjectile extends VirtualProjectile {

    /** 外弾道計算パラメータ（口径、質量、Cd0抗力係数、揚力勾配等） */
    private final BallisticsEngine.BallisticsParams params;

    /** 弾頭の姿勢ベクトル（単位ベクトル。進行方向および迎え角の計算基準） */
    private Vec3 orientation;

    /** 装填されている5インチ砲弾の弾種（HE-PD, HE-VT, ILLUM 等） */
    private final FiveInchAmmoType ammoType;

    /** 着弾時の爆発威力（ブロック破壊および爆風半径の基準値） */
    private final float explosionPower;

    /** 砲弾直撃時にエンティティへ直接与えるダメージ量 */
    private final float directDamage;

    /** フライトテレメトリ（CSV等）の記録セッションが開始されたかどうかのフラグ */
    private boolean sessionStarted = false;

    /**
     * BallisticShellProjectile の完全初期化コンストラクタ。
     *
     * @param projectileId 仮想飛翔体固有のUUID
     * @param dimension 発射ディメンション
     * @param ownerUuid 発射元プレイヤーまたは兵器ブロックのUUID（自傷防止用）
     * @param initialPosition 初期のスポーン座標 [m]
     * @param initialVelocity 初期速度ベクトル [m/s]
     * @param initialOrientation 初期の弾頭姿勢ベクトル（単位ベクトル）
     * @param params BallisticsEngine に渡す外弾道物理パラメータ
     * @param ammoType 5インチ砲弾の弾種
     * @param explosionPower 着弾時の爆発力
     * @param directDamage 直撃時の直接ダメージ
     * @param maxAgeTicks 最大生存期間 [ticks]（タイムアウトで安全に破棄）
     */
    public BallisticShellProjectile(
            UUID projectileId,
            ResourceKey<Level> dimension,
            @Nullable UUID ownerUuid,
            Vec3 initialPosition,
            Vec3 initialVelocity,
            Vec3 initialOrientation,
            BallisticsEngine.BallisticsParams params,
            FiveInchAmmoType ammoType,
            float explosionPower,
            float directDamage,
            int maxAgeTicks
    ) {
        super(projectileId, dimension, ownerUuid, initialPosition, initialVelocity, maxAgeTicks);
        this.orientation = initialOrientation.lengthSqr() > 1.0E-6 ? initialOrientation.normalize() : initialVelocity.normalize();
        this.params = params;
        this.ammoType = ammoType;
        this.explosionPower = explosionPower;
        this.directDamage = directDamage;
    }

    /**
     * 127mm (5インチ) Naval Gun 用の仮想飛翔体を生成します（デフォルト最大寿命: 2400 ticks / 120秒）。
     *
     * @param dimension 発射ディメンション
     * @param ownerUuid 発射元UUID
     * @param initialPosition 砲口位置 [m]
     * @param initialDirection 射撃方向単位ベクトル
     * @param muzzleVelocityMps 砲口初速 [m/s] (例: Mk45 / Oto127mm の実物初速 808 m/s)
     * @param ammoType 装填されている弾種
     * @return 初期化された BallisticShellProjectile インスタンス
     */
    public static BallisticShellProjectile create5Inch(
            ResourceKey<Level> dimension,
            @Nullable UUID ownerUuid,
            Vec3 initialPosition,
            Vec3 initialDirection,
            float muzzleVelocityMps,
            FiveInchAmmoType ammoType
    ) {
        return create5Inch(dimension, ownerUuid, initialPosition, initialDirection, muzzleVelocityMps, ammoType, 2400);
    }

    /**
     * 127mm (5インチ) Naval Gun 用の仮想飛翔体を指定の最大寿命で生成します。
     *
     * @param dimension 発射ディメンション
     * @param ownerUuid 発射元UUID
     * @param initialPosition 砲口位置 [m]
     * @param initialDirection 射撃方向単位ベクトル
     * @param muzzleVelocityMps 砲口初速 [m/s]
     * @param ammoType 装填されている弾種
     * @param maxAgeTicks 最大生存時間 [ticks]
     * @return 初期化された BallisticShellProjectile インスタンス
     */
    public static BallisticShellProjectile create5Inch(
            ResourceKey<Level> dimension,
            @Nullable UUID ownerUuid,
            Vec3 initialPosition,
            Vec3 initialDirection,
            float muzzleVelocityMps,
            FiveInchAmmoType ammoType,
            int maxAgeTicks
    ) {
        Vec3 dir = initialDirection.normalize();
        Vec3 vel = dir.scale(muzzleVelocityMps);

        // 127mm (5インチ) 艦砲弾の標準外弾道パラメータ
        // 直径: 0.127m (127mm), 全長: 0.8m, 質量: 31.75kg
        // cd0: 0.18 (ボートテール流線型弾頭の低抵抗係数), clAlpha: 0.05 (迎え角揚力勾配)
        BallisticsEngine.BallisticsParams params = new BallisticsEngine.BallisticsParams(
                0.127,  // diameter [m]
                0.8,    // length [m]
                31.75,  // mass [kg]
                0.18,   // cd0 (流線型低抵抗)
                1.15,   // cdSide (横滑り抗力係数)
                0.05,   // clAlpha (揚力傾斜)
                2.5,    // stabilityFactor (ジャイロ効果による姿勢安定化係数)
                0.0     // thrust [N] (ロケット補助なしの純粋な自由放物運動)
        );

        // 弾種に応じた爆発威力・直接ダメージの設定
        // HE-PD（瞬発信管榴弾）および HE-VT（近接信管榴弾）は強力な炸薬（5.0F / 80.0F）を持つ
        float power = 4.0F;
        float damage = 60.0F;
        if (ammoType == FiveInchAmmoType.MK80_HE_PD || ammoType == FiveInchAmmoType.MK116_HE_VT) {
            power = 5.0F;
            damage = 80.0F;
        }

        return new BallisticShellProjectile(
                UUID.randomUUID(),
                dimension,
                ownerUuid,
                initialPosition,
                vel,
                dir,
                params,
                ammoType,
                power,
                damage,
                maxAgeTicks
        );
    }

    /**
     * クライアント描画パケット用の飛翔体タイプIDを取得します。
     *
     * @return 1 (5インチ砲弾)
     */
    @Override
    public byte getProjectileTypeId() {
        return 1; // 5-INCH SHELL
    }

    /**
     * 砲弾の弾頭姿勢単位ベクトルを取得します。
     *
     * @return 姿勢ベクトル
     */
    public Vec3 getOrientation() {
        return this.orientation;
    }

    /**
     * 砲弾の弾種を取得します。
     *
     * @return 5インチ弾種
     */
    public FiveInchAmmoType getAmmoType() {
        return this.ammoType;
    }

    /**
     * 1Tickごとの弾道運動を計算します。
     * <p>
     * ISA大気モデル・遷音速波抵抗・重力を加味してインメモリで位置・速度・姿勢を更新し、
     * テレメトリロガーへ状態を記録します。
     *
     * @param level 発射先サーバーワールド
     */
    @Override
    protected void updateMotion(ServerLevel level) {
        // 現在の姿勢ベクトルから俯仰角 (Pitch) と方位角 (Yaw) を算出（テレメトリ記録用）
        double pitch = Math.toDegrees(Math.asin(-this.orientation.y));
        double yaw = Math.toDegrees(Math.atan2(-this.orientation.x, this.orientation.z));

        // 初回Tick: テレメトリセッション開始 & 発射ログ出力
        if (!this.sessionStarted) {
            this.sessionStarted = true;
            FlightTelemetryLogger.startSession(
                    this.projectileId,
                    "BallisticShell_5Inch",
                    this.position,
                    this.velocity,
                    this.orientation,
                    (float) pitch,
                    (float) yaw
            );
            AntiRaidWeapons.LOGGER.info(
                    "[VirtualProjectile] 5-Inch Shell [{}] launched at ({}, {}, {}) with velocity {} m/s (Ammo: {})",
                    this.projectileId, this.position.x, this.position.y, this.position.z, this.velocity.length(), this.ammoType
            );
        }

        // 1Tick分 (dt = 0.05秒) の外弾道力学計算を実行
        // ISA標準大気密度減衰・遷音速波抵抗 (Mach drag)・重力加速度・姿勢減衰を統合計算
        BallisticsEngine.BallisticsState currentState = new BallisticsEngine.BallisticsState(
                this.position,
                this.velocity,
                this.orientation
        );

        BallisticsEngine.StepResult result = BallisticsEngine.stepResult(currentState, this.params, 0.05);

        this.position = result.state().position();
        this.velocity = result.state().velocity();
        this.orientation = result.state().orientation();

        // 1Tick分のフライトテレメトリデータ（位置・速度・抗力・大気密度）を記録
        FlightTelemetryLogger.recordTick(
                this.projectileId,
                this.ageTicks,
                this.position,
                this.velocity,
                this.orientation,
                (float) pitch,
                (float) yaw,
                result.airDensity(),
                result.thrustNewtons(),
                result.dragNewtons(),
                result.liftNewtons(),
                ""
        );

        // 世界の底（Void: build height - 64）へ落下した場合の安全破棄
        if (this.position.y < level.getMinBuildHeight() - 64) {
            FlightTelemetryLogger.endSession(this.projectileId, "VOID", this.position);
            markDead();
        }
    }

    /**
     * 衝突判定（ブロックまたはエンティティ）発生時の着弾処理。
     * 直撃ダメージの適用、カスケード破壊防止爆発の実行、クライアントへの同期を行います。
     *
     * @param level 発射先サーバーワールド
     * @param hitResult レイキャスト衝突結果
     */
    @Override
    public void onHit(ServerLevel level, HitResult hitResult) {
        Vec3 hitPos = hitResult.getLocation();

        // 1. 直撃エンティティへのダメージ適用
        String targetName = "Block";
        if (hitResult instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            targetName = "Entity '" + target.getName().getString() + "'";
            target.hurt(level.damageSources().thrown(null, null), this.directDamage);
        }

        AntiRaidWeapons.LOGGER.info(
                "[VirtualProjectile] 5-Inch Shell [{}] hit {} at ({}, {}, {})",
                this.projectileId, targetName, hitPos.x, hitPos.y, hitPos.z
        );
        FlightTelemetryLogger.endSession(this.projectileId, hitResult.getType().name(), hitPos);

        // 2. カスケード地形生成を防止する安全な爆発処理（フェイルセーフ4）
        // 爆心および影響範囲が未ロードチャンクに接触していても、強制ロードやラグスパイクを引き起こさずに安全に処理
        SafeExplosionHelper.explodeSafe(
                level,
                null,
                level.damageSources().explosion(null, null),
                hitPos.x,
                hitPos.y,
                hitPos.z,
                this.explosionPower,
                false,
                Level.ExplosionInteraction.MOB
        );

        // 3. 周囲のクライアントへ着弾・爆発エフェクト同期パケットを送出 (destroyReason: 0 = 着弾爆発)
        PacketHandler.sendToNear(
                level,
                hitPos,
                256.0,
                new S2CDestroyVirtualProjectilePacket(this.projectileId, hitPos, (byte) 0)
        );
    }

    /**
     * 最大生存時間（タイムアウト）到達時の処理。
     * 未着弾のまま射程限界または時間切れとなった場合、周囲に残留しないよう安全に破棄します。
     *
     * @param level 発射先サーバーワールド
     */
    @Override
    protected void onExpired(ServerLevel level) {
        AntiRaidWeapons.LOGGER.info(
                "[VirtualProjectile] 5-Inch Shell [{}] reached max lifetime ({} ticks). Discarding at ({}, {}, {})",
                this.projectileId, this.maxAgeTicks, this.position.x, this.position.y, this.position.z
        );
        FlightTelemetryLogger.endSession(this.projectileId, "TIMEOUT", this.position);

        // 寿命到達による安全消失パケットをクライアントへ通知 (destroyReason: 2 = 寿命消失)
        PacketHandler.sendToNear(
                level,
                this.position,
                128.0,
                new S2CDestroyVirtualProjectilePacket(this.projectileId, this.position, (byte) 2)
        );
    }

    /**
     * サーバー停止またはディメンションアンロード時のクリーンアップフック。
     * 未完了のテレメトリセッションを正常終了させます。
     */
    @Override
    public void onServerStopping() {
        FlightTelemetryLogger.endSession(this.projectileId, "SERVER_STOP", this.position);
        super.onServerStopping();
    }
}
