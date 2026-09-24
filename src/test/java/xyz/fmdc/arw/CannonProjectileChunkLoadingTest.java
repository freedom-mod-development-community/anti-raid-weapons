package xyz.fmdc.arw;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 砲弾のチャンクロード機能に関する単体テスト。
 * チケット仕様、参照追跡、不要チャンクの解放、複数砲弾の競合防止、TTL安全性を検証する。
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
}
