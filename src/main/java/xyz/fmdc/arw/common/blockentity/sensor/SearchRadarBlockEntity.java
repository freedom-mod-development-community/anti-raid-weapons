package xyz.fmdc.arw.common.blockentity.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;
import xyz.fmdc.arw.api.RadarTargetManager;
import xyz.fmdc.arw.api.TrackedTarget;
import xyz.fmdc.arw.api.sensor.ITrackedTargetHolder;
import xyz.fmdc.arw.network.PacketHandler;
import xyz.fmdc.arw.network.S2CSyncRadarTargetsPacket;
import xyz.fmdc.arw.registry.ModBlocks;

import java.util.*;

/**
 * 広域を周回/首振りスキャンし、複数目標（List<TargetTrack>）を出力する広域捜索レーダー（OPS-39等）
 */
public class SearchRadarBlockEntity extends HorizontalRadarBlockEntity implements ITrackedTargetHolder {

    private float currentYaw = 0.0f;
    private float prevYaw = 0.0f;
    private final float RPM = 30f;
    private final float rotationSpeed = RPM * 360 / (60*20) ; // 毎Tick回転する速度

    // 追尾中の目標リスト (UUID -> TrackedTarget)
    private final Map<UUID, TrackedTarget> trackedTargets = new HashMap<>();
    // 記憶保持時間 (例: 100 Tick = 5秒間、アンテナが戻ってくるまで記憶を維持)
    private static final long TARGET_TIMEOUT_TICKS = 40L;

    public SearchRadarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SEARCH_RADAR_BLOCK.getBEType(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SearchRadarBlockEntity be) {
        be.tickSensor();
        be.prevYaw = be.currentYaw;
        be.currentYaw = (be.currentYaw + be.rotationSpeed) % 360.0f;
        // サーバー側でのみレーダーのロジック（探知処理）を実行
        if (!level.isClientSide) {
            be.serverTick();
        }
    }

    private float getCurrentYaw(){return currentYaw + getFacing().toYRot();};

    public void serverTick(){
        Vector3f radarPos = new Vector3f(
                this.worldPosition.getX() + 0.5f,
                this.worldPosition.getY() + 1.0f, // レーダーアンテナの高さ
                this.worldPosition.getZ() + 0.5f
        );

        // グローバルリストから視界内のものを抽出
        List<Entity> detectedThisFrame = new ArrayList<>();

        for (Entity target : RadarTargetManager.INSTANCE.getGlobalTargets()) {
            if (!target.isAlive() || target.level() != this.level) continue;

            float currentPitch = 0f;
            boolean inView = true;
            //boolean inView = RadarMathUtil.isEntityInRadarFOV(
            //        radarPos, getCurrentYaw(), currentPitch,
            //        target,
            //        128.0f, // 最大探知距離 128m
            //        60.0f,  // 扇形ビームの水平角 60度
            //        30.0f   // 垂直角 30度
            //);
            if (inView) {
                detectedThisFrame.add(target);
            }
        }
        // 抽出された `detectedThisFrame` を元に、クライアントへ同期したりGUI/TTT等へ描画データを出力
        this.updateTrackedTargets(detectedThisFrame);

        // 毎Tickまたは数Tickに1回、周囲のプレイヤーにデータ送信
        syncToClients();
    }

    /**
     * 探知したエンティティリストを元に内部の追尾記憶（trackedTargets）を更新する
     */
    protected void updateTrackedTargets(List<Entity> detectedThisFrame) {
        if (this.level == null) return;
        long currentGameTime = this.level.getGameTime();

        // 1. 今回ビーム内に入ったエンティティの記憶を新規登録 / 更新
        for (Entity entity : detectedThisFrame) {
            UUID uuid = entity.getUUID();
            if (trackedTargets.containsKey(uuid)) {
                // 既に記憶にある場合は位置とタイムスタンプを最新に更新
                trackedTargets.get(uuid).update(entity, currentGameTime);
            } else {
                // 新規探知された場合は新規登録
                trackedTargets.put(uuid, new TrackedTarget(entity, currentGameTime));
                this.onTargetDiscovered(entity); // 新規捕捉時のイベント（効果音やログなど）
            }
        }

        // 2. しばらくアンテナにかからず、タイムアウトした古い目標を削除 (ロスト)
        trackedTargets.values().removeIf(target -> {
            boolean expired = target.isExpired(currentGameTime, TARGET_TIMEOUT_TICKS) || !target.getEntity().isAlive();
            if (expired) {
                this.onTargetLost(target); // 目標ロスト時の処理
            }
            return expired;
        });
    }

    protected void onTargetDiscovered(Entity entity) {
        // 新規ターゲット捕捉時の処理（例: サウンドを鳴らす、他ブロックへ通知など）
    }

    protected void onTargetLost(TrackedTarget target) {
        // ターゲットをロストした時の処理
    }

    /**
     * 外部（ミサイル管制システムやGUI、ネットワーク同期）から現在追尾中の全目標を取得するゲッター
     */
    @Override
    public Map<UUID, TrackedTarget> getTrackedTargets() {
        return this.trackedTargets;
    }

    public boolean isActiveRadar(){
        return true;
    }

    @Override
    public float getScanRange() {
        return 512.0f;
    }

    @Override
    public void performScan() {
        this.currentScanAngle = (this.currentScanAngle + rotationSpeed) % 360.0f;
        // 周囲360度の広域エンティティ探知および detectedTargets への追加処理（スケルトン）
    }
    private void syncToClients() {
        List<S2CSyncRadarTargetsPacket.TargetData> packetList = new ArrayList<>();
        for (TrackedTarget target : this.trackedTargets.values()) {
            String name = target.getEntity() != null ? target.getEntity().getType().getDescription().getString() : "Unknown";
            packetList.add(new S2CSyncRadarTargetsPacket.TargetData(
                    target.getEntityId(),
                    name,
                    target.getLastKnownPos(),
                    target.getLastKnownVelocity() != null ? target.getLastKnownVelocity() : Vec3.ZERO
            ));
        }
        // 追尾データをブロック周辺（またはワールド内）のプレイヤーにPacket送信
        PacketHandler.INSTANCE.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> this.level.getChunkAt(this.worldPosition)),
                new S2CSyncRadarTargetsPacket(this.worldPosition, packetList)
        );
    }

    //ClientRadarDataHandlerから.
    @Override
    public void updateClientTrackedTargets(List<S2CSyncRadarTargetsPacket.TargetData> dataList) {
        if (this.level == null) return;
        long currentGameTime = this.level.getGameTime();

        // 受信データで Map を更新
        this.trackedTargets.clear();
        for (S2CSyncRadarTargetsPacket.TargetData data : dataList) {
            this.trackedTargets.put(
                    data.uuid(),
                    new TrackedTarget(data.uuid(), data.name(), data.pos(), data.vel(), currentGameTime)
            );
        }
    }
}
