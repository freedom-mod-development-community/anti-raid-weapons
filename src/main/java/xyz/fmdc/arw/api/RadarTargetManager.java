package xyz.fmdc.arw.api;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RadarTargetManager {
    public static final RadarTargetManager INSTANCE = new RadarTargetManager();

    // 探知対象となるEntityのセット（既存互換用）
    private final Set<Entity> globalTargets = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // UUID <-> EntityID 対照表
    private final Map<UUID, Integer> uuidToEntityIdMap = new ConcurrentHashMap<>();
    private final Map<Integer, UUID> entityIdToUuidMap = new ConcurrentHashMap<>();

    // EntityID -> Entity の高速ルックアップ用マップ
    private final Map<Integer, Entity> idToEntityMap = new ConcurrentHashMap<>();

    public void registerEntity(Entity entity) {
        if (isRadarDetectable(entity)) {
            globalTargets.add(entity);
            int entityId = entity.getId();
            UUID uuid = entity.getUUID();

            uuidToEntityIdMap.put(uuid, entityId);
            entityIdToUuidMap.put(entityId, uuid);
            idToEntityMap.put(entityId, entity);
        }
    }

    public void unregisterEntity(Entity entity) {
        globalTargets.remove(entity);
        int entityId = entity.getId();
        UUID uuid = entity.getUUID();

        uuidToEntityIdMap.remove(uuid);
        entityIdToUuidMap.remove(entityId);
        idToEntityMap.remove(entityId);
    }

    /**
     * 既存互換用のEntityセット取得
     */
    public Set<Entity> getGlobalTargets() {
        return globalTargets;
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

    private boolean isRadarDetectable(Entity entity) {
        // 判定条件 (例: LivingEntity, または自作のMissileEntityなど)
        return true;
    }
}
