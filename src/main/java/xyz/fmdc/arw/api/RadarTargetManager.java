package xyz.fmdc.arw.api;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.common.entity.AbstractMissileEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * サーバー側でワールド内のレーダー探知対象エンティティを一元管理するマスタマネージャ。
 */
public class RadarTargetManager {
    public static final RadarTargetManager INSTANCE = new RadarTargetManager();

    // 監視対象Entity IDセット（FastUtil）
    private final IntSet targetEntityIdSet = IntSets.synchronize(new IntOpenHashSet());

    // UUID <-> EntityID 対照マップ
    private final Map<UUID, Integer> uuidToEntityIdMap = new ConcurrentHashMap<>();
    private final Map<Integer, UUID> entityIdToUuidMap = new ConcurrentHashMap<>();

    // EntityID -> Entity の高速ルックアップ用マップ
    private final Map<Integer, Entity> idToEntityMap = new ConcurrentHashMap<>();

    // 既存互換用のグローバルターゲットセット（参照のみ）
    private final Set<Entity> globalTargets = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void registerEntity(Entity entity) {
        if (entity == null) return;
        if (isRadarDetectable(entity)) {
            int entityId = entity.getId();
            UUID uuid = entity.getUUID();

            targetEntityIdSet.add(entityId);
            uuidToEntityIdMap.put(uuid, entityId);
            entityIdToUuidMap.put(entityId, uuid);
            idToEntityMap.put(entityId, entity);
            globalTargets.add(entity);
        }
    }

    public void unregisterEntity(Entity entity) {
        if (entity == null) return;
        int entityId = entity.getId();
        UUID uuid = entity.getUUID();

        targetEntityIdSet.remove(entityId);
        uuidToEntityIdMap.remove(uuid);
        entityIdToUuidMap.remove(entityId);
        idToEntityMap.remove(entityId);
        globalTargets.remove(entity);
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

        for (Entity entity : idToEntityMap.values()) {
            if (entity.level() != level || !entity.isAlive()) {
                continue;
            }
            double x = entity.getX();
            double y = entity.getY();
            double z = entity.getZ();

            // 不等式による高速AABB内外チェック
            if (x >= aabb.minX && x <= aabb.maxX &&
                y >= aabb.minY && y <= aabb.maxY &&
                z >= aabb.minZ && z <= aabb.maxZ) {
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

        for (Entity entity : idToEntityMap.values()) {
            if (entity.level() != level || !entity.isAlive()) {
                continue;
            }
            double x = entity.getX();
            double y = entity.getY();
            double z = entity.getZ();

            if (x >= aabb.minX && x <= aabb.maxX &&
                y >= aabb.minY && y <= aabb.maxY &&
                z >= aabb.minZ && z <= aabb.maxZ) {
                candidates.add(entity);
            }
        }

        return candidates;
    }

    /**
     * レーダー探知対象かどうかのフィルタリング条件。
     * Monster, Player（非Spectator）, 自作ミサイル（AbstractMissileEntity）等
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
     * 既存互換用のEntityセット取得
     */
    public Set<Entity> getGlobalTargets() {
        return globalTargets;
    }

    /**
     * 監視対象Entity IDセットの取得
     */
    public IntSet getTargetEntityIdSet() {
        return targetEntityIdSet;
    }

    /**
     * 内部探索ループ用：探知対象のEntityID一覧を取得
     */
    public Set<Integer> getTargetEntityIds() {
        return idToEntityMap.keySet();
    }

    /**
     * EntityIDからEntity実体をO(1)で取得
     */
    @Nullable
    public Entity getEntityById(int entityId) {
        return idToEntityMap.get(entityId);
    }

    /**
     * 対照表：UUIDからEntityIDを取得
     */
    @Nullable
    public Integer getEntityIdByUuid(UUID uuid) {
        return uuidToEntityIdMap.get(uuid);
    }

    /**
     * 対照表：EntityIDからUUIDを取得
     */
    @Nullable
    public UUID getUuidByEntityId(int entityId) {
        return entityIdToUuidMap.get(entityId);
    }
}
