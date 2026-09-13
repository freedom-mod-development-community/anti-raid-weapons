package xyz.fmdc.arw.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

public class TrackedTarget {
    private final UUID entityId;
    private final Entity entity; // サーバー側でのみ保持（クライアントでは null の場合あり）
    private String entityTypeName; // パケット同期用の表示名
    private Vec3 lastKnownPos;
    private Vec3 lastKnownVelocity;
    private long lastSeenGameTime;

    // サーバー側：Entityから生成
    public TrackedTarget(Entity entity, long gameTime) {
        this.entityId = entity.getUUID();
        this.entity = entity;
        this.entityTypeName = entity.getType().getDescription().getString();
        this.update(entity, gameTime);
    }

    // クライアント側：S2Cパケットから生成
    public TrackedTarget(UUID uuid, String name, Vec3 pos, Vec3 velocity, long gameTime) {
        this.entityId = uuid;
        this.entity = null;
        this.entityTypeName = name;
        this.lastKnownPos = pos;
        this.lastKnownVelocity = velocity;
        this.lastSeenGameTime = gameTime;
    }

    public void update(Entity entity, long gameTime) {
        this.lastKnownPos = entity.position();
        this.lastKnownVelocity = entity.getDeltaMovement();
        this.lastSeenGameTime = gameTime;
    }

    // クライアント側でのパケット更新用
    public void updateFromPacket(Vec3 pos, Vec3 velocity, long gameTime) {
        this.lastKnownPos = pos;
        this.lastKnownVelocity = velocity;
        this.lastSeenGameTime = gameTime;
    }

    public boolean isExpired(long currentGameTime, long timeoutTicks) {
        return (currentGameTime - lastSeenGameTime) > timeoutTicks;
    }

    // ゲッター群
    public UUID getEntityId() { return entityId; }
    public Entity getEntity() { return entity; }
    public String getEntityTypeName() { return entityTypeName; }
    public Vec3 getLastKnownPos() { return lastKnownPos; }
    public Vec3 getLastKnownVelocity() { return lastKnownVelocity; }
    public long getLastSeenGameTime() { return lastSeenGameTime; }
}
