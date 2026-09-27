package xyz.fmdc.arw.common.projectile.virtual;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.AntiRaidWeapons;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 仮想飛翔体（Virtual Projectile）をサーバー側で一括管理・Tick実行するマネージャー。
 */
public final class VirtualProjectileManager {

    private static final VirtualProjectileManager INSTANCE = new VirtualProjectileManager();

    public static VirtualProjectileManager getInstance() {
        return INSTANCE;
    }

    /** ディメンションごとのアクティブな飛翔体マップ (UUID -> VirtualProjectile) */
    private final Map<ResourceKey<Level>, Map<UUID, VirtualProjectile>> activeProjectiles = new ConcurrentHashMap<>();

    /** Tick中に非同期・外部から追加された飛翔体のキュー */
    private final Queue<VirtualProjectile> pendingAdditions = new ConcurrentLinkedQueue<>();

    private VirtualProjectileManager() {}

    /**
     * 新しい仮想飛翔体を登録します。
     */
    public void register(@NotNull VirtualProjectile projectile) {
        this.pendingAdditions.add(projectile);
    }

    /**
     * サーバーTick（ServerTickEvent.Phase.END）で呼び出され、全ディメンションの飛翔体を更新します。
     */
    public void tick(MinecraftServer server) {
        // 1. 保留中の新規飛翔体を各ディメンションマップへ格納
        VirtualProjectile newProj;
        while ((newProj = this.pendingAdditions.poll()) != null) {
            Map<UUID, VirtualProjectile> dimMap = this.activeProjectiles.computeIfAbsent(
                    newProj.getDimension(),
                    k -> new HashMap<>()
            );
            dimMap.put(newProj.getProjectileId(), newProj);
        }

        // 2. ディメンションごとに走査・更新
        for (Map.Entry<ResourceKey<Level>, Map<UUID, VirtualProjectile>> entry : this.activeProjectiles.entrySet()) {
            ResourceKey<Level> dimKey = entry.getKey();
            Map<UUID, VirtualProjectile> projMap = entry.getValue();

            if (projMap.isEmpty()) continue;

            ServerLevel level = server.getLevel(dimKey);
            if (level == null) {
                // ディメンションが存在しない/アンロードされた場合は全飛翔体を破棄
                for (VirtualProjectile p : projMap.values()) {
                    p.onServerStopping();
                }
                projMap.clear();
                continue;
            }

            // イテレータでTickおよび削除フラグ回収
            Iterator<Map.Entry<UUID, VirtualProjectile>> iterator = projMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, VirtualProjectile> projEntry = iterator.next();
                VirtualProjectile projectile = projEntry.getValue();

                try {
                    projectile.tick(level);
                } catch (Exception e) {
                    AntiRaidWeapons.LOGGER.error("Error ticking VirtualProjectile [{}]: ", projectile.getProjectileId(), e);
                    projectile.markDead();
                }

                if (projectile.isDead()) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * 特定のディメンションがアンロードされた場合の解放
     */
    public void onLevelUnload(ResourceKey<Level> dimKey) {
        Map<UUID, VirtualProjectile> map = this.activeProjectiles.remove(dimKey);
        if (map != null) {
            for (VirtualProjectile p : map.values()) {
                p.onServerStopping();
            }
            map.clear();
        }
    }

    /**
     * サーバー停止時の完全パージ処理。全飛翔体を安全に終了し、母艦へロスト信号を送る。
     */
    public void clearAndNotifyAll() {
        for (VirtualProjectile p : this.pendingAdditions) {
            p.onServerStopping();
        }
        this.pendingAdditions.clear();

        for (Map<UUID, VirtualProjectile> map : this.activeProjectiles.values()) {
            for (VirtualProjectile p : map.values()) {
                p.onServerStopping();
            }
            map.clear();
        }
        this.activeProjectiles.clear();
    }

    /** 現在アクティブな飛翔体の総数を取得（デバッグ・監視用） */
    public int getTotalCount() {
        int count = this.pendingAdditions.size();
        for (Map<UUID, VirtualProjectile> map : this.activeProjectiles.values()) {
            count += map.size();
        }
        return count;
    }
}
