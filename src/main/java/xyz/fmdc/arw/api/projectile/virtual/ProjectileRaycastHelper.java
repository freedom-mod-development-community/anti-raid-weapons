package xyz.fmdc.arw.api.projectile.virtual;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 超音速飛翔体（45m/tick等）のすり抜け防止およびチャンクアンロード競合防止のためのレイキャストヘルパー。
 */
public final class ProjectileRaycastHelper {

    private ProjectileRaycastHelper() {}

    /**
     * 始点から終点までの移動線分が通過するすべてのチャンク座標（ChunkPos）を順序通りに算出します。
     * 2次元のFast Voxel Traversal (DDA) アルゴリズムを採用し、斜めに高速移動した場合でも
     * 跨ぐチャンクを取りこぼしません。
     */
    public static List<ChunkPos> getIntersectingChunks(Vec3 from, Vec3 to) {
        List<ChunkPos> chunks = new ArrayList<>();

        int x0 = (int) Math.floor(from.x) >> 4;
        int z0 = (int) Math.floor(from.z) >> 4;
        int x1 = (int) Math.floor(to.x) >> 4;
        int z1 = (int) Math.floor(to.z) >> 4;

        if (x0 == x1 && z0 == z1) {
            chunks.add(new ChunkPos(x0, z0));
            return chunks;
        }

        double dx = to.x - from.x;
        double dz = to.z - from.z;

        int stepX = Integer.compare(x1, x0);
        int stepZ = Integer.compare(z1, z0);

        // 次のチャンク境界までの比率 t
        double tMaxX = (stepX > 0) ? ((x0 + 1) * 16.0 - from.x) / dx : (stepX < 0) ? (x0 * 16.0 - from.x) / dx : Double.POSITIVE_INFINITY;
        double tMaxZ = (stepZ > 0) ? ((z0 + 1) * 16.0 - from.z) / dz : (stepZ < 0) ? (z0 * 16.0 - from.z) / dz : Double.POSITIVE_INFINITY;

        double tDeltaX = (stepX != 0) ? Math.abs(16.0 / dx) : Double.POSITIVE_INFINITY;
        double tDeltaZ = (stepZ != 0) ? Math.abs(16.0 / dz) : Double.POSITIVE_INFINITY;

        int currentX = x0;
        int currentZ = z0;
        chunks.add(new ChunkPos(currentX, currentZ));

        while (currentX != x1 || currentZ != z1) {
            if (tMaxX < tMaxZ) {
                tMaxX += tDeltaX;
                currentX += stepX;
            } else {
                tMaxZ += tDeltaZ;
                currentZ += stepZ;
            }
            chunks.add(new ChunkPos(currentX, currentZ));

            // 安全策: 万一無限ループにならないよう上限キャップ
            if (chunks.size() > 64) {
                break;
            }
        }

        return chunks;
    }

    /**
     * 指定された座標のチャンクが安全にロードされており、かつエンティティTick可能かどうかを判定します。
     * 同期チャンクロードは一切引き起こしません。
     */
    public static boolean isChunkSafeAndTicking(ServerLevel level, int chunkX, int chunkZ) {
        ServerChunkCache chunkSource = level.getChunkSource();
        // getChunkNow() は同期ロードを行わず、メモリ上に存在しなければ即座に null を返す
        if (chunkSource.getChunkNow(chunkX, chunkZ) == null) {
            return false;
        }
        // 中心ブロック座標が EntityTicking レベルにあるか判定
        BlockPos centerPos = new ChunkPos(chunkX, chunkZ).getMiddleBlockPosition(64);
        return level.isPositionEntityTicking(centerPos);
    }

    /**
     * 指定線分（from -> to）について、ロード済みチャンクが存在するか確認し、
     * 存在する場合のみブロックおよびエンティティとの衝突判定を行います。
     * 未ロードチャンクのみの場合は完全スキップ（null）を返します。
     */
    @Nullable
    public static HitResult performRaycast(
            ServerLevel level,
            Vec3 from,
            Vec3 to,
            @Nullable UUID ignoredOwnerUuid,
            Predicate<Entity> entityFilter
    ) {
        List<ChunkPos> chunks = getIntersectingChunks(from, to);

        // 通過するチャンクの中に、1つでも安全にロードされているチャンクがあるか確認
        boolean anyChunkLoaded = false;
        for (ChunkPos cp : chunks) {
            if (isChunkSafeAndTicking(level, cp.x, cp.z)) {
                anyChunkLoaded = true;
                break;
            }
        }

        if (!anyChunkLoaded) {
            // シュレーディンガーの判定: 全て未ロード領域ならレイキャスト完全スキップ
            return null;
        }

        // 1. ブロック衝突判定（ClipContext）
        // レースコンディション対策: 始点終点の近傍がロードされている場合のみ実行
        BlockHitResult blockHit = null;
        int fromChunkX = (int) Math.floor(from.x) >> 4;
        int fromChunkZ = (int) Math.floor(from.z) >> 4;
        int toChunkX = (int) Math.floor(to.x) >> 4;
        int toChunkZ = (int) Math.floor(to.z) >> 4;

        if (isChunkSafeAndTicking(level, fromChunkX, fromChunkZ) || isChunkSafeAndTicking(level, toChunkX, toChunkZ)) {
            ClipContext clipContext = new ClipContext(
                    from,
                    to,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    null
            );
            BlockHitResult bHit = level.clip(clipContext);
            if (bHit.getType() != HitResult.Type.MISS) {
                // 着弾ブロックのチャンクが安全か再検証
                BlockPos hitBlockPos = bHit.getBlockPos();
                if (isChunkSafeAndTicking(level, hitBlockPos.getX() >> 4, hitBlockPos.getZ() >> 4)) {
                    blockHit = bHit;
                }
            }
        }

        Vec3 effectiveTo = (blockHit != null) ? blockHit.getLocation() : to;

        // 2. エンティティ衝突判定
        AABB scanBox = new AABB(from, effectiveTo).inflate(1.0);
        Entity closestEntity = null;
        Vec3 closestEntityHitPos = null;
        double closestDistSq = Double.MAX_VALUE;

        // 走査範囲内のエンティティを取得（ロード済みチャンク内のエンティティのみ安全に返される）
        List<Entity> candidateEntities = level.getEntities((Entity) null, scanBox, entity -> {
            if (entity.isSpectator() || !entity.isAlive() || !entity.isPickable()) {
                return false;
            }
            if (ignoredOwnerUuid != null && ignoredOwnerUuid.equals(entity.getUUID())) {
                return false;
            }
            return entityFilter.test(entity);
        });

        for (Entity candidate : candidateEntities) {
            // レースコンディション対策: エンティティの位置が安全なTickingチャンクにあるか確認
            BlockPos ePos = candidate.blockPosition();
            if (!isChunkSafeAndTicking(level, ePos.getX() >> 4, ePos.getZ() >> 4)) {
                continue;
            }

            AABB entityBb = candidate.getBoundingBox().inflate(0.3);
            var optHit = entityBb.clip(from, effectiveTo);
            if (optHit.isPresent()) {
                Vec3 hitVec = optHit.get();
                double distSq = from.distanceToSqr(hitVec);
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closestEntity = candidate;
                    closestEntityHitPos = hitVec;
                }
            }
        }

        if (closestEntity != null) {
            return new EntityHitResult(closestEntity, closestEntityHitPos);
        }

        return blockHit;
    }
}
