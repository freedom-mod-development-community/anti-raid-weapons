package xyz.fmdc.arw.common.blockentity.target;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.api.RadarTargetManager;
import xyz.fmdc.arw.api.TargetAffiliation;
import xyz.fmdc.arw.api.TrackedTarget;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.registry.ModBlocks;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * レーダー探知可能な標的用ブロックエンティティ。
 * レーダーシステム (RadarTargetManager / FCS) に探知対象として登録され、
 * 破壊された際に座標や破壊者（プレイヤー、爆発、投射物など）の詳細ログを出力する。
 */
public class TargetBlockEntity extends AbstractARWBlockEntity {

    private final AtomicBoolean destroyed = new AtomicBoolean(false);
    private long placedGameTime = 0L;
    private String targetName = "TARGET";
    private TargetAffiliation defaultAffiliation = TargetAffiliation.HOSTILE;

    public TargetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.TARGET_BLOCK.getBEType(), pos, state);
    }

    /**
     * 標榜の中心座標（3次元Vec3）
     */
    public Vec3 getTargetCenterPos() {
        BlockPos p = this.getBlockPos();
        return new Vec3(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
    }

    /**
     * レーダー追尾用の TrackedTarget スナップショットを生成
     */
    public TrackedTarget createTrackedTarget(long gameTime) {
        return new TrackedTarget(
                this.uuid,
                this.targetName,
                getTargetCenterPos(),
                Vec3.ZERO,
                gameTime,
                this.defaultAffiliation
        );
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
        setChanged();
    }

    public TargetAffiliation getDefaultAffiliation() {
        return defaultAffiliation;
    }

    public void setDefaultAffiliation(TargetAffiliation defaultAffiliation) {
        this.defaultAffiliation = defaultAffiliation;
        setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide) {
            if (this.placedGameTime == 0L) {
                this.placedGameTime = this.level.getGameTime();
            }
            RadarTargetManager.INSTANCE.registerTargetBlock(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide) {
            RadarTargetManager.INSTANCE.unregisterTargetBlock(this);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (this.level != null && !this.level.isClientSide) {
            RadarTargetManager.INSTANCE.unregisterTargetBlock(this);
        }
    }

    // --- 破壊検知およびログ出力処理 ---

    /**
     * プレイヤーによる採掘・破壊
     */
    public void onPlayerDestroy(Player player) {
        if (!this.destroyed.compareAndSet(false, true)) return;

        notifyDestroyed();

        String playerName = player.getScoreboardName();
        String playerUuid = player.getUUID().toString();
        String gameMode = player.isCreative() ? "Creative" : (player.isSpectator() ? "Spectator" : "Survival");
        String heldItem = player.getMainHandItem().isEmpty() ? "Empty Hand" :
                player.getMainHandItem().getHoverName().getString() + " (" + BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()) + ")";
        Vec3 playerPos = player.position();

        String destroyerInfo = String.format("%s (UUID: %s, GameMode: %s, Pos: [%.2f, %.2f, %.2f])",
                playerName, playerUuid, gameMode, playerPos.x, playerPos.y, playerPos.z);
        String details = "HeldItem: " + heldItem;

        logDestruction("PLAYER_ATTACK", playerName, "minecraft:player", destroyerInfo, details);
    }

    /**
     * 爆発（TNT、砲弾、ミサイル等）による破壊
     */
    public void onExplosionDestroy(Explosion explosion) {
        if (!this.destroyed.compareAndSet(false, true)) return;

        notifyDestroyed();

        Entity exploder = explosion.getExploder();
        Entity indirect = explosion.getIndirectSourceEntity();

        String exploderName = exploder != null ? exploder.getName().getString() : "Unknown Exploder";
        String exploderType = exploder != null ? BuiltInRegistries.ENTITY_TYPE.getKey(exploder.getType()).toString() : "Unknown";
        String causerInfo = indirect != null ? String.format("%s (%s, UUID: %s)",
                indirect.getName().getString(),
                BuiltInRegistries.ENTITY_TYPE.getKey(indirect.getType()),
                indirect.getUUID()) : "None";

        Vec3 expPos = explosion.getPosition();
        String details = expPos != null ? String.format("ExplosionPos: (%.2f, %.2f, %.2f)", expPos.x, expPos.y, expPos.z) : "N/A";

        logDestruction("EXPLOSION", exploderName, exploderType, causerInfo, details);
    }

    /**
     * 投射物（砲弾、ミサイル、矢等）の直撃による破壊
     */
    public void onProjectileDestroy(Projectile projectile, BlockHitResult hit) {
        if (!this.destroyed.compareAndSet(false, true)) return;

        notifyDestroyed();

        String projName = projectile.getName().getString();
        String projType = BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType()).toString();
        Entity owner = projectile.getOwner();
        String causerInfo = owner != null ? String.format("%s (%s, UUID: %s)",
                owner.getName().getString(),
                BuiltInRegistries.ENTITY_TYPE.getKey(owner.getType()),
                owner.getUUID()) : "None";

        Vec3 hitPos = hit.getLocation();
        Vec3 velocity = projectile.getDeltaMovement();
        String details = String.format("HitPos: (%.2f, %.2f, %.2f), ProjectileVelocity: (%.2f, %.2f, %.2f, Speed: %.2f m/t)",
                hitPos.x, hitPos.y, hitPos.z, velocity.x, velocity.y, velocity.z, velocity.length());

        logDestruction("PROJECTILE_HIT", projName, projType, causerInfo, details);
    }

    /**
     * その他の要因（コマンドによる置換、ピストン、環境等）による破壊・消滅
     */
    public void onGenericDestroy() {
        if (!this.destroyed.compareAndSet(false, true)) return;

        notifyDestroyed();

        logDestruction("REMOVED_OR_COMMAND", "Environment / Command", "N/A",
                "Block removed or replaced", "BlockState: " + this.getBlockState());
    }

    private void notifyDestroyed() {
        if (this.level != null && !this.level.isClientSide) {
            RadarTargetManager.INSTANCE.notifyTargetBlockDestroyed(this.uuid, this.level.getGameTime());
            RadarTargetManager.INSTANCE.unregisterTargetBlock(this);
        }
    }

    private void logDestruction(String cause, String destroyerName, String destroyerType,
                                @Nullable String causerInfo, @Nullable String additionalDetails) {
        if (this.level == null) return;

        BlockPos p = this.getBlockPos();
        long gameTime = this.level.getGameTime();
        long survivedTicks = this.placedGameTime > 0 ? (gameTime - this.placedGameTime) : 0;
        String dim = this.level.dimension().location().toString();

        AntiRaidWeapons.LOGGER.info("================== [ARW TARGET BLOCK DESTROYED] ==================");
        AntiRaidWeapons.LOGGER.info("Target Block UUID : {}", this.uuid);
        AntiRaidWeapons.LOGGER.info("Target Name       : {}", this.targetName);
        AntiRaidWeapons.LOGGER.info("Dimension         : {}", dim);
        AntiRaidWeapons.LOGGER.info("Block Coordinates : [X: {}, Y: {}, Z: {}] (Center: [X: {}, Y: {}, Z: {}])",
                p.getX(), p.getY(), p.getZ(), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
        AntiRaidWeapons.LOGGER.info("Destruction Cause : {}", cause);
        AntiRaidWeapons.LOGGER.info("Destroyer         : {} [Type: {}]", destroyerName, destroyerType);
        if (causerInfo != null) {
            AntiRaidWeapons.LOGGER.info("Indirect Causer   : {}", causerInfo);
        }
        if (additionalDetails != null) {
            AntiRaidWeapons.LOGGER.info("Details           : {}", additionalDetails);
        }
        AntiRaidWeapons.LOGGER.info("Game Time         : {} (Survived: {} ticks / approx. {}s)",
                gameTime, survivedTicks, String.format("%.1f", (float) survivedTicks / 20.0f));
        AntiRaidWeapons.LOGGER.info("==================================================================");

        // 1行サマリーログ（外部ログ監視ツール等での解析用）
        AntiRaidWeapons.LOGGER.info("[TargetBlock DESTROYED] Target at ({}, {}, {}) destroyed by '{}' ({}) via {} - Causer: {}",
                p.getX(), p.getY(), p.getZ(), destroyerName, destroyerType, cause, causerInfo != null ? causerInfo : "None");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("PlacedGameTime", this.placedGameTime);
        tag.putString("TargetName", this.targetName);
        tag.putInt("DefaultAffiliation", this.defaultAffiliation.ordinal());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("PlacedGameTime")) {
            this.placedGameTime = tag.getLong("PlacedGameTime");
        }
        if (tag.contains("TargetName")) {
            this.targetName = tag.getString("TargetName");
        }
        if (tag.contains("DefaultAffiliation")) {
            this.defaultAffiliation = TargetAffiliation.fromOrdinal(tag.getInt("DefaultAffiliation"));
        }
    }
}
