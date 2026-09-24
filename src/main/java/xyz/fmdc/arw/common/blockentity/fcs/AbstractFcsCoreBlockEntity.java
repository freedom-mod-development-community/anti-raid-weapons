package xyz.fmdc.arw.common.blockentity.fcs;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.api.RadarTargetManager;
import xyz.fmdc.arw.api.TrackedTarget;
import xyz.fmdc.arw.api.fcs.*;
import xyz.fmdc.arw.api.sensor.ITrackedTargetHolder;
import xyz.fmdc.arw.api.sensor.RadarScanRange;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.network.PacketHandler;
import xyz.fmdc.arw.network.S2CSyncRadarTargetsPacket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * センサーデータの統合・偏差計算・兵装への指示を行うFCS Coreの基底クラス。
 * 接続中レーダー群の探知範囲を包括するAABBによる粗取得と精密幾何判定（Track Fusion）、
 * および目標リストの一元保持とクライアント同期を担当する。
 */
public abstract class AbstractFcsCoreBlockEntity extends AbstractARWBlockEntity
        implements IFcsNetworkNode, ITrackedTargetHolder {

    protected final Set<UUID> connectedNodeUuids = new LinkedHashSet<>();
    protected final Map<UUID, BlockPos> nodePositions = new HashMap<>();
    protected final List<IFcsSensorNode> connectedSensors = new ArrayList<>();
    protected final List<IFcsControllableWeapon> connectedWeapons = new ArrayList<>();
    protected final Map<UUID, RadarScanRange> sensorScanRanges = new HashMap<>();

    // FCSコアが一元保持する確定目標リスト (UUID -> TrackedTarget)
    protected final Map<UUID, TrackedTarget> fcsTrackedTargets = new ConcurrentHashMap<>();

    // 目標の追尾喪失タイムアウト（40 Ticks = 2秒）
    protected static final long TARGET_TIMEOUT_TICKS = 40L;

    // Tick間引き制御カウンター（2Tickに1回再計算）
    protected int scanTicker = 0;
    protected static final int SCAN_INTERVAL_TICKS = 2;

    public AbstractFcsCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tickFcs() {
        if (this.level == null || this.level.isClientSide) return;
        validateConnectedNodes();

        scanTicker++;
        if (scanTicker >= SCAN_INTERVAL_TICKS) {
            scanTicker = 0;
            scanAndFuseTargets();
        }
    }

    /**
     * 稼働中センサーの情報を保持する内部レコード
     */
    private record ActiveSensor(Vec3 pos, float yaw, float pitch, RadarScanRange range) {}

    /**
     * 接続・通電中の全レーダーの探知範囲を包含する包括エリア（AABB）を算出し、
     * マスタから候補を取得して各レーダーの幾何判定（OR条件合成）を実施、目標を一元保持・同期する。
     */
    protected void scanAndFuseTargets() {
        if (!(this.level instanceof ServerLevel serverLevel)) return;

        // 1. 稼働中（電源ON）の全センサーの情報を収集
        List<ActiveSensor> activeSensors = new ArrayList<>();
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

        for (IFcsSensorNode sensor : connectedSensors) {
            if (!sensor.isPowered()) continue;

            BlockPos pos = nodePositions.get(sensor.getNetworkId());
            if (pos == null && sensor instanceof BlockEntity be) {
                pos = be.getBlockPos();
            }
            if (pos == null) continue;

            RadarScanRange range = sensorScanRanges.getOrDefault(sensor.getNetworkId(), sensor.getScanRange());
            float r = range.maxRange();

            Vec3 sensorPos = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            float yaw = sensor.getAntennaYaw();
            float pitch = sensor.getAntennaPitch();

            activeSensors.add(new ActiveSensor(sensorPos, yaw, pitch, range));

            minX = Math.min(minX, pos.getX() - r);
            minY = Math.min(minY, pos.getY() - r);
            minZ = Math.min(minZ, pos.getZ() - r);
            maxX = Math.max(maxX, pos.getX() + r + 1.0);
            maxY = Math.max(maxY, pos.getY() + r + 1.0);
            maxZ = Math.max(maxZ, pos.getZ() + r + 1.0);
        }

        long gameTime = serverLevel.getGameTime();

        // 稼働中センサーが存在しない場合は全目標をクリアしてクライアント同期
        if (activeSensors.isEmpty()) {
            if (!fcsTrackedTargets.isEmpty()) {
                fcsTrackedTargets.clear();
                syncTargetsToClients();
            }
            return;
        }

        // 2. 包括AABBによる候補Entityの粗取得（Coarse Filter）
        AABB combinedBounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        List<TrackedTarget> candidates = RadarTargetManager.INSTANCE.queryCandidatesInAABB(serverLevel, combinedBounds);

        // 3. 各レーダーの幾何判定による精密判定（Fine Filter / Track Fusion）
        Set<UUID> detectedUuidsThisScan = new HashSet<>();

        for (TrackedTarget candidate : candidates) {
            Vec3 targetPos = candidate.getLastKnownPos();
            if (targetPos == null) continue;

            // 各レーダーの探知範囲をOR合成評価
            for (ActiveSensor sensor : activeSensors) {
                if (sensor.range.isInRange(sensor.pos, sensor.yaw, sensor.pitch, targetPos)) {
                    UUID id = candidate.getEntityId();
                    detectedUuidsThisScan.add(id);

                    TrackedTarget existing = fcsTrackedTargets.get(id);
                    if (existing != null) {
                        if (candidate.getEntity() != null) {
                            existing.update(candidate.getEntity(), gameTime);
                        } else {
                            existing.updateFromPacket(candidate.getLastKnownPos(), candidate.getLastKnownVelocity(), gameTime);
                        }
                    } else {
                        fcsTrackedTargets.put(id, candidate);
                    }
                    break; // OR条件：いずれか1つに入っていれば確定
                }
            }
        }

        // 4. タイムアウトおよび生存外エンティティの除去
        fcsTrackedTargets.values().removeIf(target -> {
            boolean expired = target.isExpired(gameTime, TARGET_TIMEOUT_TICKS);
            boolean dead = target.getEntity() != null && !target.getEntity().isAlive();
            return expired || dead;
        });

        // 5. クライアント同期パケット送信
        syncTargetsToClients();
    }

    /**
     * FCSコアの確定目標リストをクライアントへS2C送信
     */
    protected void syncTargetsToClients() {
        if (this.level == null || this.level.isClientSide) return;

        List<S2CSyncRadarTargetsPacket.TargetData> packetList = new ArrayList<>(this.fcsTrackedTargets.size());
        for (TrackedTarget target : this.fcsTrackedTargets.values()) {
            String name = target.getEntity() != null
                    ? target.getEntity().getType().getDescription().getString()
                    : (target.getEntityTypeName() != null ? target.getEntityTypeName() : "Unknown");
            packetList.add(new S2CSyncRadarTargetsPacket.TargetData(
                    target.getEntityId(),
                    name,
                    target.getLastKnownPos(),
                    target.getLastKnownVelocity() != null ? target.getLastKnownVelocity() : Vec3.ZERO
            ));
        }

        PacketHandler.INSTANCE.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> this.level.getChunkAt(this.worldPosition)),
                new S2CSyncRadarTargetsPacket(this.worldPosition, packetList)
        );
    }

    // --- ITrackedTargetHolder 実装 ---

    @Override
    public Map<UUID, TrackedTarget> getTrackedTargets() {
        return this.fcsTrackedTargets;
    }

    @Override
    public void updateClientTrackedTargets(List<S2CSyncRadarTargetsPacket.TargetData> dataList) {
        if (this.level == null) return;
        long currentGameTime = this.level.getGameTime();

        this.fcsTrackedTargets.clear();
        for (S2CSyncRadarTargetsPacket.TargetData data : dataList) {
            this.fcsTrackedTargets.put(
                    data.uuid(),
                    new TrackedTarget(data.uuid(), data.name(), data.pos(), data.vel(), currentGameTime)
            );
        }
    }

    /**
     * 既存互換用
     */
    public Map<UUID, TrackedTarget> getCombinedTrackedTargets() {
        return getTrackedTargets();
    }

    public boolean registerDevice(BlockEntity device) {
        if (device == null || device.isRemoved() || device == this) return false;

        UUID targetUuid = null;
        if (device instanceof AbstractARWBlockEntity arwBE) {
            targetUuid = arwBE.getUuid();
        } else if (device instanceof IFcsNetworkNode networkNode) {
            targetUuid = networkNode.getNetworkId();
        }

        if (targetUuid == null) return false;

        // 既にこのFCSコアに登録されている場合
        if (connectedNodeUuids.contains(targetUuid)) {
            return false;
        }

        // 他のFCSコアに既にリンクされている場合は上書き禁止
        if (device instanceof IFcsNetworkNode networkNode) {
            UUID existingCoreUuid = networkNode.getLinkedFcsCoreUuid();
            if (existingCoreUuid != null && !existingCoreUuid.equals(this.uuid)) {
                return false;
            }
        }
        if (device instanceof AbstractARWBlockEntity arwBE) {
            UUID existingCoreUuid = arwBE.getLinkedFcsCoreUuid();
            if (existingCoreUuid != null && !existingCoreUuid.equals(this.uuid)) {
                return false;
            }
        }

        if (device instanceof IFcsNetworkNode networkNode) {
            networkNode.setLinkedFcsCoreUuid(this.uuid);
            networkNode.setLinkedFcsCorePos(this.worldPosition);
            networkNode.setFcsConnected(true);
        }

        if (device instanceof AbstractARWBlockEntity arwBE) {
            arwBE.setLinkedFcsCoreUuid(this.uuid);
            arwBE.setLinkedFcsCorePos(this.worldPosition);
            arwBE.syncToClient();
        }

        if (device instanceof IFcsSensorNode sensor) {
            registerSensor(sensor);
        }
        if (device instanceof IFcsControllableWeapon weapon) {
            registerWeapon(weapon);
        }

        connectedNodeUuids.add(targetUuid);
        nodePositions.put(targetUuid, device.getBlockPos());
        syncToClient();
        setChanged();
        return true;
    }

    public boolean registerDevice(UUID uuid) {
        if (uuid == null || uuid.equals(this.uuid) || connectedNodeUuids.contains(uuid)) return false;
        connectedNodeUuids.add(uuid);
        syncToClient();
        setChanged();
        return true;
    }

    public boolean unregisterDevice(UUID uuid) {
        if (uuid == null) return false;
        if (connectedNodeUuids.remove(uuid)) {
            BlockPos nodePos = nodePositions.remove(uuid);
            sensorScanRanges.remove(uuid);

            if (nodePos != null && this.level != null && this.level.isLoaded(nodePos)) {
                BlockEntity be = this.level.getBlockEntity(nodePos);
                if (be instanceof IFcsNetworkNode node) {
                    if (this.uuid.equals(node.getLinkedFcsCoreUuid())) {
                        node.setLinkedFcsCoreUuid(null);
                        node.setLinkedFcsCorePos(null);
                        node.setFcsConnected(false);
                    }
                }
                if (be instanceof AbstractARWBlockEntity arwBE) {
                    if (this.uuid.equals(arwBE.getLinkedFcsCoreUuid())) {
                        arwBE.setLinkedFcsCoreUuid(null);
                        arwBE.setLinkedFcsCorePos(null);
                        arwBE.syncToClient();
                    }
                }
            }

            connectedSensors.removeIf(sensor -> {
                if (uuid.equals(sensor.getNetworkId())) {
                    if (this.uuid.equals(sensor.getLinkedFcsCoreUuid())) {
                        sensor.setLinkedFcsCoreUuid(null);
                        sensor.setLinkedFcsCorePos(null);
                        sensor.setFcsConnected(false);
                    }
                    return true;
                }
                return false;
            });
            connectedWeapons.removeIf(weapon -> {
                if (uuid.equals(weapon.getNetworkId())) {
                    if (this.uuid.equals(weapon.getLinkedFcsCoreUuid())) {
                        weapon.setLinkedFcsCoreUuid(null);
                        weapon.setLinkedFcsCorePos(null);
                        weapon.setFcsConnected(false);
                    }
                    return true;
                }
                return false;
            });

            syncToClient();
            setChanged();
            return true;
        }
        return false;
    }

    public boolean unregisterDevice(BlockEntity be) {
        if (be instanceof AbstractARWBlockEntity arwBE) {
            return unregisterDevice(arwBE.getUuid());
        } else if (be instanceof IFcsNetworkNode networkNode) {
            return unregisterDevice(networkNode.getNetworkId());
        }
        return false;
    }

    public boolean isDeviceRegistered(UUID uuid) {
        return uuid != null && connectedNodeUuids.contains(uuid);
    }

    public boolean isDeviceRegistered(BlockEntity be) {
        if (be instanceof AbstractARWBlockEntity arwBE) {
            return isDeviceRegistered(arwBE.getUuid());
        } else if (be instanceof IFcsNetworkNode networkNode) {
            return isDeviceRegistered(networkNode.getNetworkId());
        }
        return false;
    }

    public Set<UUID> getConnectedNodeUuids() {
        return Collections.unmodifiableSet(connectedNodeUuids);
    }

    public Map<UUID, BlockPos> getNodePositions() {
        return Collections.unmodifiableMap(nodePositions);
    }

    public List<IFcsSensorNode> getConnectedSensors() {
        return Collections.unmodifiableList(connectedSensors);
    }

    public List<IFcsControllableWeapon> getConnectedWeapons() {
        return Collections.unmodifiableList(connectedWeapons);
    }

    public void registerSensor(IFcsSensorNode sensor) {
        if (!connectedSensors.contains(sensor)) {
            connectedSensors.add(sensor);
            sensor.setFcsConnected(true);
        }
        connectedNodeUuids.add(sensor.getNetworkId());
        sensorScanRanges.put(sensor.getNetworkId(), sensor.getScanRange());
        if (sensor instanceof BlockEntity be) {
            nodePositions.put(sensor.getNetworkId(), be.getBlockPos());
        }
    }

    public void registerWeapon(IFcsControllableWeapon weapon) {
        if (!connectedWeapons.contains(weapon)) {
            connectedWeapons.add(weapon);
            weapon.setFcsConnected(true);
        }
        connectedNodeUuids.add(weapon.getNetworkId());
        if (weapon instanceof BlockEntity be) {
            nodePositions.put(weapon.getNetworkId(), be.getBlockPos());
        }
    }

    // --- 探索範囲（RadarScanRange）管理 ---

    public void updateSensorScanRange(UUID sensorUuid, RadarScanRange range) {
        if (sensorUuid != null && range != null) {
            this.sensorScanRanges.put(sensorUuid, range);
            syncToClient();
            setChanged();
        }
    }

    @Nullable
    public RadarScanRange getSensorScanRange(UUID sensorUuid) {
        return this.sensorScanRanges.get(sensorUuid);
    }

    public Map<UUID, RadarScanRange> getSensorScanRanges() {
        return Collections.unmodifiableMap(sensorScanRanges);
    }

    /**
     * 稼働中（電源ON）の全センサーにおける最大探知距離を取得
     */
    public float getMaxActiveDetectionRange() {
        float max = 0.0f;
        for (IFcsSensorNode sensor : connectedSensors) {
            if (sensor.isPowered()) {
                RadarScanRange range = sensorScanRanges.getOrDefault(sensor.getNetworkId(), sensor.getScanRange());
                if (range.maxRange() > max) {
                    max = range.maxRange();
                }
            }
        }
        return max;
    }

    // --- センサー電源管理 ---

    public void setSensorPower(UUID sensorUuid, boolean power) {
        if (sensorUuid == null) return;
        for (IFcsSensorNode sensor : connectedSensors) {
            if (sensorUuid.equals(sensor.getNetworkId())) {
                sensor.setPowered(power);
                return;
            }
        }
        if (this.level != null && nodePositions.containsKey(sensorUuid)) {
            BlockPos pos = nodePositions.get(sensorUuid);
            if (this.level.isLoaded(pos)) {
                BlockEntity be = this.level.getBlockEntity(pos);
                if (be instanceof IFcsSensorNode sensor) {
                    sensor.setPowered(power);
                }
            }
        }
    }

    public void setAllSensorsPower(boolean power) {
        for (IFcsSensorNode sensor : connectedSensors) {
            sensor.setPowered(power);
        }
        if (this.level != null) {
            for (UUID uuid : connectedNodeUuids) {
                BlockPos pos = nodePositions.get(uuid);
                if (pos != null && this.level.isLoaded(pos)) {
                    BlockEntity be = this.level.getBlockEntity(pos);
                    if (be instanceof IFcsSensorNode sensor && !connectedSensors.contains(sensor)) {
                        sensor.setPowered(power);
                    }
                }
            }
        }
    }

    public boolean isSensorPowered(UUID sensorUuid) {
        if (sensorUuid == null) return false;
        for (IFcsSensorNode sensor : connectedSensors) {
            if (sensorUuid.equals(sensor.getNetworkId())) {
                return sensor.isPowered();
            }
        }
        if (this.level != null && nodePositions.containsKey(sensorUuid)) {
            BlockPos pos = nodePositions.get(sensorUuid);
            if (this.level.isLoaded(pos)) {
                BlockEntity be = this.level.getBlockEntity(pos);
                if (be instanceof IFcsSensorNode sensor) {
                    return sensor.isPowered();
                }
            }
        }
        return false;
    }

    public void disconnectAll() {
        for (IFcsSensorNode sensor : connectedSensors) {
            if (this.uuid.equals(sensor.getLinkedFcsCoreUuid())) {
                sensor.setLinkedFcsCoreUuid(null);
                sensor.setLinkedFcsCorePos(null);
                sensor.setFcsConnected(false);
            }
        }
        connectedSensors.clear();
        sensorScanRanges.clear();

        for (IFcsControllableWeapon weapon : connectedWeapons) {
            if (this.uuid.equals(weapon.getLinkedFcsCoreUuid())) {
                weapon.setLinkedFcsCoreUuid(null);
                weapon.setLinkedFcsCorePos(null);
                weapon.setFcsConnected(false);
            }
        }
        connectedWeapons.clear();

        if (this.level != null) {
            for (BlockPos pos : nodePositions.values()) {
                if (this.level.isLoaded(pos)) {
                    BlockEntity be = this.level.getBlockEntity(pos);
                    if (be instanceof IFcsNetworkNode node) {
                        if (this.uuid.equals(node.getLinkedFcsCoreUuid())) {
                            node.setLinkedFcsCoreUuid(null);
                            node.setLinkedFcsCorePos(null);
                            node.setFcsConnected(false);
                        }
                    }
                    if (be instanceof AbstractARWBlockEntity arwBE) {
                        if (this.uuid.equals(arwBE.getLinkedFcsCoreUuid())) {
                            arwBE.setLinkedFcsCoreUuid(null);
                            arwBE.setLinkedFcsCorePos(null);
                            arwBE.syncToClient();
                        }
                    }
                }
            }
        }
        connectedNodeUuids.clear();
        nodePositions.clear();
        fcsTrackedTargets.clear();
        syncTargetsToClients();
        syncToClient();
        setChanged();
    }

    @Override
    public void setRemoved() {
        disconnectAll();
        super.setRemoved();
    }

    protected void validateConnectedNodes() {
        if (this.level == null) return;

        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, BlockPos> entry : nodePositions.entrySet()) {
            UUID id = entry.getKey();
            BlockPos pos = entry.getValue();
            if (this.level.isLoaded(pos)) {
                BlockEntity be = this.level.getBlockEntity(pos);
                if (be == null || be.isRemoved()) {
                    toRemove.add(id);
                } else if (be instanceof AbstractARWBlockEntity arw && !id.equals(arw.getUuid())) {
                    toRemove.add(id);
                } else if (be instanceof IFcsNetworkNode node && !id.equals(node.getNetworkId())) {
                    toRemove.add(id);
                }
            }
        }

        for (UUID id : toRemove) {
            unregisterDevice(id);
        }

        connectedSensors.removeIf(sensor -> {
            if (!connectedNodeUuids.contains(sensor.getNetworkId())) {
                if (this.uuid.equals(sensor.getLinkedFcsCoreUuid())) {
                    sensor.setLinkedFcsCoreUuid(null);
                    sensor.setLinkedFcsCorePos(null);
                    sensor.setFcsConnected(false);
                }
                sensorScanRanges.remove(sensor.getNetworkId());
                return true;
            }
            return sensor instanceof BlockEntity be && be.isRemoved();
        });
        connectedWeapons.removeIf(weapon -> {
            if (!connectedNodeUuids.contains(weapon.getNetworkId())) {
                if (this.uuid.equals(weapon.getLinkedFcsCoreUuid())) {
                    weapon.setLinkedFcsCoreUuid(null);
                    weapon.setLinkedFcsCorePos(null);
                    weapon.setFcsConnected(false);
                }
                return true;
            }
            return weapon instanceof BlockEntity be && be.isRemoved();
        });

        for (Map.Entry<UUID, BlockPos> entry : nodePositions.entrySet()) {
            UUID id = entry.getKey();
            BlockPos pos = entry.getValue();
            if (this.level.isLoaded(pos) && connectedNodeUuids.contains(id)) {
                BlockEntity be = this.level.getBlockEntity(pos);
                if (be instanceof IFcsSensorNode sensor && !connectedSensors.contains(sensor)) {
                    connectedSensors.add(sensor);
                    sensor.setLinkedFcsCoreUuid(this.uuid);
                    sensor.setLinkedFcsCorePos(this.worldPosition);
                    sensor.setFcsConnected(true);
                    sensorScanRanges.put(id, sensor.getScanRange());
                }
                if (be instanceof IFcsControllableWeapon weapon && !connectedWeapons.contains(weapon)) {
                    connectedWeapons.add(weapon);
                    weapon.setLinkedFcsCoreUuid(this.uuid);
                    weapon.setLinkedFcsCorePos(this.worldPosition);
                    weapon.setFcsConnected(true);
                }
                if (be instanceof IFcsNetworkNode node && !node.isConnectedToFcs()) {
                    node.setLinkedFcsCoreUuid(this.uuid);
                    node.setLinkedFcsCorePos(this.worldPosition);
                    node.setFcsConnected(true);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (UUID id : connectedNodeUuids) {
            CompoundTag idTag = new CompoundTag();
            idTag.putUUID("UUID", id);
            BlockPos pos = nodePositions.get(id);
            if (pos != null) {
                idTag.put("Pos", NbtUtils.writeBlockPos(pos));
            }
            RadarScanRange range = sensorScanRanges.get(id);
            if (range != null) {
                idTag.put("ScanRange", range.toTag());
            }
            list.add(idTag);
        }
        tag.put("ConnectedNodeUuids", list);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        connectedNodeUuids.clear();
        nodePositions.clear();
        sensorScanRanges.clear();
        if (tag.contains("ConnectedNodeUuids", Tag.TAG_LIST)) {
            ListTag list = tag.getList("ConnectedNodeUuids", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag idTag = list.getCompound(i);
                if (idTag.hasUUID("UUID")) {
                    UUID id = idTag.getUUID("UUID");
                    connectedNodeUuids.add(id);
                    if (idTag.contains("Pos")) {
                        nodePositions.put(id, NbtUtils.readBlockPos(idTag.getCompound("Pos")));
                    }
                    if (idTag.contains("ScanRange")) {
                        sensorScanRanges.put(id, RadarScanRange.fromTag(idTag.getCompound("ScanRange")));
                    }
                }
            }
        }
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag != null) {
            load(tag);
        }
    }

    @Override
    public UUID getNetworkId() {
        return this.uuid;
    }

    @Override
    public boolean isConnectedToFcs() {
        return true; // FCS Core自身は常にオンライン
    }

    @Override
    public void setFcsConnected(boolean connected) {}
}
