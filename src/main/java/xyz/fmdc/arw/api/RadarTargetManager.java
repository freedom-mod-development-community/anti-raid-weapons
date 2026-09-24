package xyz.fmdc.arw.api;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.common.blockentity.target.TargetBlockEntity;
import xyz.fmdc.arw.common.entity.AbstractMissileEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * サーバー側でワールド内のレーダー探知対象（エンティティおよび標的用ブロック）を一元管理するマスターマネージャ。
 * メモリリークおよびワールド停止・保存時のクローズ競合を防止するため、Entity インスタンス実体への強参照は保持せず、
 * プリミティブな Entity ID (int) と UUID のみを追跡する。
 * また、標的用ブロック (TargetBlockEntity) も管理し、レーダーによる探知および破壊監視を提供する。
 */
public class RadarTargetManager {
    public static final RadarTargetManager INSTANCE = new RadarTargetManager();

    // 監視対象の Entity ID セット（高速走査用）
    private final IntSet activeEntityIds = IntSets.synchronize(new IntOpenHashSet());

    // UUID -> Entity ID 対照マップ
    private final Map<UUID, Integer> uuidToIdMap = new ConcurrentHashMap<>();

    // 標的用ブロック (Target Block) のアクティブマップ (UUID -> TargetBlockEntity)
    private final Map<UUID, TargetBlockEntity> activeTargetBlocks = new ConcurrentHashMap<>();

    // 破壊された標的用ブロックの履歴 (UUID -> 破壊時のGameTime)
    private final Map<UUID, Long> destroyedBlockTimes = new ConcurrentHashMap<>();

    private RadarTargetManager() {}

    public void registerEntity(Entity entity) {
        if (!isRadarDetectable(entity)) return;
        int entityId = entity.getId();
        UUID uuid = entity.getUUID();

        uuidToIdMap.put(uuid, entityId);
        activeEntityIds.add(entityId);
    }

    public void unregisterEntity(Entity entity) {
        if (entity == null) return;
        int entityId = entity.getId();
        UUID uuid = entity.getUUID();

        uuidToIdMap.remove(uuid);
        activeEntityIds.remove(entityId);
    }

    // --- 標的用ブロック (TargetBlock) 関連 ---

    public void registerTargetBlock(TargetBlockEntity be) {
        if (be == null || be.getUuid() == null) return;
        activeTargetBlocks.put(be.getUuid(), be);
        destroyedBlockTimes.remove(be.getUuid());
    }

    public void unregisterTargetBlock(TargetBlockEntity be) {
        if (be == null || be.getUuid() == null) return;
        activeTargetBlocks.remove(be.getUuid());
    }

    public void notifyTargetBlockDestroyed(UUID uuid, long gameTime) {
        if (uuid == null) return;
        activeTargetBlocks.remove(uuid);
        destroyedBlockTimes.put(uuid, gameTime);
    }

    /**
     * 指定UUIDの標的ブロックが破壊されたかどうかを判定
     */
    public boolean isBlockTargetDestroyed(UUID uuid, long currentGameTime) {
        if (uuid == null) return false;
        Long destroyedTime = destroyedBlockTimes.get(uuid);
        if (destroyedTime != null) {
            // 破壊から5秒(100ticks)以内の場合は破壊判定として扱う
            return (currentGameTime - destroyedTime) <= 100;
        }
        return false;
    }

    public boolean isTargetBlock(UUID uuid) {
        return uuid != null && (activeTargetBlocks.containsKey(uuid) || destroyedBlockTimes.containsKey(uuid));
    }

    /**
     * サーバー停止・ワールドアンロード時の完全解放
     */
    public void clear() {
        synchronized (activeEntityIds) {
            activeEntityIds.clear();
        }
        uuidToIdMap.clear();
        activeTargetBlocks.clear();
        destroyedBlockTimes.clear();
    }

    /**
     * AABB粗絞り込みクエリメソッド。
     * 指定されたAABB内の生存Entityおよびアクティブな標的用ブロックから TrackedTarget スナップショットリストを生成して返す。
     * 不等式による高速バウンディングボックス内外判定を行う。
     */
    public List<TrackedTarget> queryCandidatesInAABB(ServerLevel level, AABB aabb) {
        if (level == null || aabb == null) return Collections.emptyList();

        long gameTime = level.getGameTime();
        List<TrackedTarget> candidates = new ArrayList<>();

        // 1. エンティティの走査
        int[] ids;
        synchronized (activeEntityIds) {
            ids = activeEntityIds.toIntArray();
        }

        for (int id : ids) {
            Entity entity = level.getEntity(id);
            if (entity == null || !entity.isAlive()) continue;

            Vec3 pos = entity.position();
            if (pos.x >= aabb.minX && pos.x <= aabb.maxX &&
                pos.y >= aabb.minY && pos.y <= aabb.maxY &&
                pos.z >= aabb.minZ && pos.z <= aabb.maxZ) {
                candidates.add(new TrackedTarget(entity, gameTime));
            }
        }

        // 2. 標的用ブロック (Target Block) の走査
        for (TargetBlockEntity targetBlock : activeTargetBlocks.values()) {
            if (targetBlock.isRemoved() || targetBlock.getLevel() != level) continue;

            Vec3 pos = targetBlock.getTargetCenterPos();
            if (pos.x >= aabb.minX && pos.x <= aabb.maxX &&
                pos.y >= aabb.minY && pos.y <= aabb.maxY &&
                pos.z >= aabb.minZ && pos.z <= aabb.maxZ) {
                candidates.add(targetBlock.createTrackedTarget(gameTime));
            }
        }

        // 3. 古い破壊ログキャッシュの定期クリーンアップ (100ticks以上経過したもの)
        destroyedBlockTimes.entrySet().removeIf(entry -> (gameTime - entry.getValue()) > 100);

        return candidates;
    }

    /**
     * AABB粗絞り込みクエリメソッド（Entity実体リスト版）。
     */
    public List<Entity> queryCandidateEntitiesInAABB(ServerLevel level, AABB aabb) {
        if (level == null || aabb == null) return Collections.emptyList();

        List<Entity> candidates = new ArrayList<>();

        int[] ids;
        synchronized (activeEntityIds) {
            ids = activeEntityIds.toIntArray();
        }

        for (int id : ids) {
            Entity entity = level.getEntity(id);
            if (entity == null || !entity.isAlive()) continue;

            Vec3 pos = entity.position();
            if (pos.x >= aabb.minX && pos.x <= aabb.maxX &&
                pos.y >= aabb.minY && pos.y <= aabb.maxY &&
                pos.z >= aabb.minZ && pos.z <= aabb.maxZ) {
                candidates.add(entity);
            }
        }

        return candidates;
    }

    /**
     * レーダー探知対象かどうかのフィルタリング条件。
     * Monster, Player（非Spectatorかつ生存）, 自作ミサイル（AbstractMissileEntity）等
     */
    public boolean isRadarDetectable(Entity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isSpectator();
        }
        if (entity instanceof Monster) {
            return true;
        }
        if (entity instanceof AbstractMissileEntity) {
            return true;
        }
        return false;
    }

    /**
     * 監視対象Entity IDセットの取得
     */
    public IntSet getTargetEntityIdSet() {
        return activeEntityIds;
    }
}
