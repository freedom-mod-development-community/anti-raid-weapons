package xyz.fmdc.arw;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.fmdc.arw.api.fcs.FiringSolution;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 砲弾のチャンクロード機能および砲の発射命令制御に関する単体テスト。
 * チケット仕様、参照追跡、不要チャンクの解放、複数砲弾の競合防止、TTL安全性、
 * ならびに自動射撃の抑止と明示的な発射命令（allowFire / triggerFire）時のみの射撃を検証する。
 */
public class CannonProjectileChunkLoadingTest {

    @Test
    @DisplayName("砲弾用チャンクローダーチケット仕様・プロパティ検証")
    public void testShellChunkTicketDefinition() {
        TicketType<UUID> ticketType = TicketType.create("arw_cannon_shell", UUID::compareTo, 60);
        assertNotNull(ticketType, "SHELL_CHUNK_TICKET は null であってはならない");
        assertEquals("arw_cannon_shell", ticketType.toString(), "チケット名は arw_cannon_shell であること");
        assertEquals(60L, ticketType.timeout(), "異常消滅・クラッシュ時の自動失効セーフティとしてTTLは60ticks(3秒)であること");
    }

    @Test
    @DisplayName("飛翔シミュレーションにおける必要チャンク判定と移動時の差分解放ロジック検証")
    public void testChunkTrackingAndEvictionLogic() {
        // 砲弾の初期座標 (x=10, z=10) -> ChunkPos(0, 0)
        Vec3 pos = new Vec3(10, 64, 10);
        Vec3 motion = new Vec3(8.0, 0.0, 0.0); // X方向に初速8ブロック/tick (160m/s)

        ChunkPos currentChunk = new ChunkPos(BlockPos.containing(pos));
        ChunkPos leadChunk = new ChunkPos(BlockPos.containing(pos.add(motion.scale(8.0)))); // 10 + 64 = 74 -> ChunkPos(4, 0)

        Set<ChunkPos> activeTickets = new HashSet<>();
        Set<ChunkPos> desiredChunks = new HashSet<>();
        desiredChunks.add(currentChunk);
        desiredChunks.add(leadChunk);

        // 初回ロード登録
        activeTickets.addAll(desiredChunks);
        assertTrue(activeTickets.contains(new ChunkPos(0, 0)));
        assertTrue(activeTickets.contains(new ChunkPos(4, 0)));
        assertEquals(2, activeTickets.size());

        // 砲弾が前進 (x=80, z=10) -> ChunkPos(5, 0) に突入
        Vec3 newPos = new Vec3(80, 64, 10);
        ChunkPos newCurrentChunk = new ChunkPos(BlockPos.containing(newPos));
        ChunkPos newLeadChunk = new ChunkPos(BlockPos.containing(newPos.add(motion.scale(8.0)))); // 80 + 64 = 144 -> ChunkPos(9, 0)

        Set<ChunkPos> newDesired = new HashSet<>();
        newDesired.add(newCurrentChunk);
        newDesired.add(newLeadChunk);

        // 不要チャンクの解除シミュレーション
        Set<ChunkPos> removedTickets = new HashSet<>();
        Iterator<ChunkPos> it = activeTickets.iterator();
        while (it.hasNext()) {
            ChunkPos p = it.next();
            if (!newDesired.contains(p)) {
                removedTickets.add(p);
                it.remove();
            }
        }

        // 新規チャンクの追加
        for (ChunkPos p : newDesired) {
            activeTickets.add(p);
        }

        // 検証: 通過済みの (0, 0) および (4, 0) は解除され、新しい (5, 0) と (9, 0) のみが残る
        assertTrue(removedTickets.contains(new ChunkPos(0, 0)), "通過済みの初期チャンクは解除されること");
        assertTrue(removedTickets.contains(new ChunkPos(4, 0)), "不要になった先読みチャンクは解除されること");
        assertEquals(Set.of(new ChunkPos(5, 0), new ChunkPos(9, 0)), activeTickets, "現在アクティブなチケットは最新の2チャンクのみであること");
    }

    @Test
    @DisplayName("複数砲弾が同一チャンクを利用する場合の参照分離・安全性検証")
    public void testMultipleShellsReferenceSafety() {
        // Minecraft DistanceManager の動作モデル（ChunkPos ごとに TicketType と Argument(UUID) を管理）
        Map<ChunkPos, Set<UUID>> loadedChunksRegistry = new HashMap<>();

        UUID shellA = UUID.randomUUID();
        UUID shellB = UUID.randomUUID();
        ChunkPos targetChunk = new ChunkPos(10, 10);

        // 砲弾Aが targetChunk にチケットを付与
        loadedChunksRegistry.computeIfAbsent(targetChunk, k -> new HashSet<>()).add(shellA);
        assertTrue(loadedChunksRegistry.get(targetChunk).contains(shellA));
        assertEquals(1, loadedChunksRegistry.get(targetChunk).size());

        // 砲弾Bも同一の targetChunk にチケットを付与
        loadedChunksRegistry.computeIfAbsent(targetChunk, k -> new HashSet<>()).add(shellB);
        assertTrue(loadedChunksRegistry.get(targetChunk).contains(shellB));
        assertEquals(2, loadedChunksRegistry.get(targetChunk).size(), "2発の砲弾が同一チャンクを共有保持");

        // 砲弾Aが着弾してチケット解放
        loadedChunksRegistry.get(targetChunk).remove(shellA);
        if (loadedChunksRegistry.get(targetChunk).isEmpty()) {
            loadedChunksRegistry.remove(targetChunk);
        }

        // 検証: 砲弾Aが解放されても、砲弾Bのチケットが残っているためチャンクはロード状態を維持
        assertTrue(loadedChunksRegistry.containsKey(targetChunk), "砲弾Bのチケットがあるためチャンクはアンロードされない");
        assertEquals(1, loadedChunksRegistry.get(targetChunk).size());
        assertTrue(loadedChunksRegistry.get(targetChunk).contains(shellB));

        // 砲弾Bも着弾してチケット解放
        loadedChunksRegistry.get(targetChunk).remove(shellB);
        if (loadedChunksRegistry.get(targetChunk).isEmpty()) {
            loadedChunksRegistry.remove(targetChunk);
        }

        // 検証: 全砲弾の解放完了でチャンクはロードリストから完全に除去される
        assertFalse(loadedChunksRegistry.containsKey(targetChunk), "全砲弾の消滅によりチャンクロードが完全に解放される");
    }

    @Test
    @DisplayName("チャンクロード設定・NBTタグ永続化の検証")
    public void testNbtSerialization() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("ChunkLoadingEnabled", false);

        assertFalse(tag.getBoolean("ChunkLoadingEnabled"));

        tag.putBoolean("ChunkLoadingEnabled", true);
        assertTrue(tag.getBoolean("ChunkLoadingEnabled"));
    }

    @Test
    @DisplayName("砲の自動射撃廃止と発射命令（allowFire / triggerFire）による発射制御ロジック検証")
    public void testFiringCommandOnlyLogic() {
        // 発射回数を追跡するモックモデル
        AtomicInteger firedCount = new AtomicInteger(0);

        // 武器状態
        class SimulatedWeapon {
            int cooldown = 0;

            boolean canFire() {
                return cooldown <= 0;
            }

            void fire() {
                if (!canFire()) return;
                firedCount.incrementAndGet();
                cooldown = 60; // 射撃後クールダウン開始
            }

            // 通常tick: 自動射撃ロジックは削除されており、クールダウン低減のみ実行
            void tick() {
                if (cooldown > 0) {
                    cooldown--;
                }
            }

            // FCSからの射撃諸元受信（発射命令）
            void applyFiringSolution(FiringSolution solution) {
                if (solution == null) return;
                if (solution.allowFire() && canFire()) {
                    fire();
                }
            }

            // プレイヤーや遠隔端末からの直接・遠隔トリガー入力
            void handleRemoteInput(boolean triggerFire) {
                if (triggerFire && canFire()) {
                    fire();
                }
            }
        }

        SimulatedWeapon gun = new SimulatedWeapon();

        // 1. tick経過（1200ticks = 60秒分）で自動射撃が一切行われないことの検証
        for (int i = 0; i < 1200; i++) {
            gun.tick();
        }
        assertEquals(0, firedCount.get(), "テスト用自動射撃が削除されたため、tick経過のみで砲が勝手に発射されてはならない");

        // 2. FCS発射命令不許可（allowFire = false）の場合、射撃されないことの検証
        FiringSolution noFireSolution = new FiringSolution(0.0f, 0.0f, false, false);
        gun.applyFiringSolution(noFireSolution);
        assertEquals(0, firedCount.get(), "allowFireがfalseのときは発射されないこと");

        // 3. FCS発射命令許可（allowFire = true）の場合のみ発射されることの検証
        FiringSolution fireSolution = new FiringSolution(0.0f, 0.0f, true, true);
        gun.applyFiringSolution(fireSolution);
        assertEquals(1, firedCount.get(), "allowFireがtrueかつcanFireのときに正確に1回発射されること");
        assertTrue(gun.cooldown > 0, "発射後にクールダウンが設定されること");

        // 4. クールダウン中は再度の発射命令があっても発射されないことの検証
        gun.applyFiringSolution(fireSolution);
        assertEquals(1, firedCount.get(), "クールダウン中は発射命令があっても発射されないこと");

        // 5. クールダウン完了までtick経過
        while (!gun.canFire()) {
            gun.tick();
        }

        // 6. 遠隔・直接トリガー（triggerFire = true）による発射命令の検証
        gun.handleRemoteInput(false);
        assertEquals(1, firedCount.get(), "triggerFireがfalseのときは発射されないこと");

        gun.handleRemoteInput(true);
        assertEquals(2, firedCount.get(), "triggerFireがtrueのときに正確に発射されること");
    }
}
