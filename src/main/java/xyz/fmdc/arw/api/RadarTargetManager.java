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
import xyz.fmdc.arw.common.entity.AbstractMissileEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * サーバー側でワールド内のレーダー探知対象エンティティを一元管理するマスタマネージャ。
 * メモリリークおよびワールド停止・保存時のクローズ競合を防止するため、Entity インスタンス実体への強参照は保持せず、
 * プリミティブな Entity ID (int) と UUID のみを追跡する。
 */
public class RadarTargetManager {
    public static final RadarTargetManager INSTANCE = new RadarTargetManager();

    // 監視対象の Entity ID セット（高速走査用）
    private final IntSet activeEntityIds = IntSets.synchronize(new IntOpenHashSet());

    // UUID -> Entity ID 対照マップ
    private final Map<UUID, Integer> uuidToIdMap = new ConcurrentHashMap<>();

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

    /**
     * サーバー停止・ワールドアンロード時の完全解放
     */
    public void clear() {
        synchronized (activeEntityIds) {
            activeEntityIds.clear();
        }
        uuidToIdMap.clear();
    }

    /**
     * AABB粗絞り込みクエリメソッド。
     * 指定されたAABB内の生存Entityから TrackedTarget スナップショットリストを生成して返す。
     * 不等式による高速バウンディングボックス内外判定を行う。
     */
    public List<TrackedTarget> queryCandidatesInAABB(ServerLevel level, AABB aabb) {
        if (level == null || aabb == null) return Collections.emptyList();

        long gameTime = level.getGameTime();
        List<TrackedTarget> candidates = new ArrayList<>();

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

    /**
     * 対照表：UUIDからEntityIDを取得
     */
    @Nullable
    public Integer getEntityIdByUuid(UUID uuid) {
        return uuid != null ? uuidToIdMap.get(uuid) : null;
    }
}
