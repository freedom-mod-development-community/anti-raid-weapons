package xyz.fmdc.arw.api.projectile.virtual;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 飛翔体の着弾爆発時、未ロードチャンクへの食い込みによるカスケード地形生成や
 * サーバーのラグスパイクを完全防止する安全な爆発ヘルパー。
 */
public final class SafeExplosionHelper {

    private SafeExplosionHelper() {}

    private static Explosion.BlockInteraction toBlockInteraction(Level level, Level.ExplosionInteraction interaction) {
        return switch (interaction) {
            case NONE -> Explosion.BlockInteraction.KEEP;
            case BLOCK -> level.getGameRules().getBoolean(GameRules.RULE_BLOCK_EXPLOSION_DROP_DECAY) ? Explosion.BlockInteraction.DESTROY_WITH_DECAY : Explosion.BlockInteraction.DESTROY;
            case MOB -> level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) ? (level.getGameRules().getBoolean(GameRules.RULE_BLOCK_EXPLOSION_DROP_DECAY) ? Explosion.BlockInteraction.DESTROY_WITH_DECAY : Explosion.BlockInteraction.DESTROY) : Explosion.BlockInteraction.KEEP;
            case TNT -> level.getGameRules().getBoolean(GameRules.RULE_BLOCK_EXPLOSION_DROP_DECAY) ? Explosion.BlockInteraction.DESTROY_WITH_DECAY : Explosion.BlockInteraction.DESTROY;
        };
    }

    /**
     * ロード済みチャンクのブロックのみを破壊・発火対象にクリッピングして爆発を実行します。
     * 未ロードチャンク内のブロックは破棄リストから完全に除外されます。
     */
    public static Explosion explodeSafe(
            ServerLevel level,
            @Nullable Entity exploder,
            @Nullable DamageSource damageSource,
            double x,
            double y,
            double z,
            float radius,
            boolean causesFire,
            Level.ExplosionInteraction interaction
    ) {
        // 爆心地自体が安全にロードされていない場合は即座にスキップ
        int centerChunkX = (int) Math.floor(x) >> 4;
        int centerChunkZ = (int) Math.floor(z) >> 4;
        if (!ProjectileRaycastHelper.isChunkSafeAndTicking(level, centerChunkX, centerChunkZ)) {
            return null;
        }

        Explosion.BlockInteraction blockInteraction = toBlockInteraction(level, interaction);

        // カスタムExplosionインスタンスを生成
        Explosion explosion = new Explosion(
                level,
                exploder,
                damageSource,
                (ExplosionDamageCalculator) null,
                x, y, z,
                radius,
                causesFire,
                blockInteraction
        );

        // Forge の ExplosionEvent.Start を考慮してバニラ内部の計算を実行
        if (net.minecraftforge.event.ForgeEventFactory.onExplosionStart(level, explosion)) {
            return explosion;
        }

        // バニラの爆発計算 (explode)
        explosion.explode();

        // 破壊対象ブロックリスト (toBlow) をクリッピング
        List<BlockPos> toBlow = explosion.getToBlow();
        toBlow.removeIf(blockPos -> {
            int cx = blockPos.getX() >> 4;
            int cz = blockPos.getZ() >> 4;
            return !ProjectileRaycastHelper.isChunkSafeAndTicking(level, cx, cz);
        });

        // パーティクル・サウンド・ブロック破壊の適用 (finalizeExplosion)
        explosion.finalizeExplosion(true);

        return explosion;
    }
}
