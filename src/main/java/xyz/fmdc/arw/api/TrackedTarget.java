package xyz.fmdc.arw.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * レーダー探知目標のスナップショットを保持するデータクラス。
 * メモリリークおよびワールド停止時のクローズ競合を防止するため、Entity インスタンス実体への強参照は保持しない。
 */
public class TrackedTarget {
    private final UUID entityId;
    private String entityTypeName; // パケット同期用の表示名
    private Vec3 lastKnownPos;
    private Vec3 lastKnownVelocity;
    private long lastSeenGameTime;

    // サーバー側：Entity からスナップショットを生成
    public TrackedTarget(Entity entity, long gameTime) {
        this.entityId = entity.getUUID();
        this.entityTypeName = entity.getType().getDescription().getString();
        this.lastKnownPos = entity.position();
        this.lastKnownVelocity = entity.getDeltaMovement();
        this.lastSeenGameTime = gameTime;
    }

    // クライアント側：S2Cパケットから生成
    public TrackedTarget(UUID uuid, String name, Vec3 pos, Vec3 velocity, long gameTime) {
        this.entityId = uuid;
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

    /**
     * @deprecated Entity実体は保持されません。サーバー側で必要な場合は {@link ServerLevel#getEntity(UUID)} 等を使用してください。
     */
    @Deprecated
    @Nullable
    public Entity getEntity() { return null; }

    @Nullable
    public Entity getEntity(ServerLevel level) {
        return level != null ? level.getEntity(this.entityId) : null;
    }

    public String getEntityTypeName() { return entityTypeName; }
    public Vec3 getLastKnownPos() { return lastKnownPos; }
    public Vec3 getLastKnownVelocity() { return lastKnownVelocity; }
    public long getLastSeenGameTime() { return lastSeenGameTime; }
}
