package xyz.fmdc.arw.common.blockentity.fcs;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.api.fcs.*;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;

import java.util.*;

/**
 * センサーデータの統合・偏差計算・兵装への指示を行うFCS Coreの基底クラス
 */
public abstract class AbstractFcsCoreBlockEntity extends AbstractARWBlockEntity implements IFcsNetworkNode {

    protected final Set<UUID> connectedNodeUuids = new LinkedHashSet<>();
    protected final Map<UUID, BlockPos> nodePositions = new HashMap<>();
    protected final List<IFcsSensorNode> connectedSensors = new ArrayList<>();
    protected final List<IFcsControllableWeapon> connectedWeapons = new ArrayList<>();

    public AbstractFcsCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tickFcs() {
        if (this.level == null || this.level.isClientSide) return;
        validateConnectedNodes();
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

    public void disconnectAll() {
        for (IFcsSensorNode sensor : connectedSensors) {
            if (this.uuid.equals(sensor.getLinkedFcsCoreUuid())) {
                sensor.setLinkedFcsCoreUuid(null);
                sensor.setLinkedFcsCorePos(null);
                sensor.setFcsConnected(false);
            }
        }
        connectedSensors.clear();

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
            list.add(idTag);
        }
        tag.put("ConnectedNodeUuids", list);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        connectedNodeUuids.clear();
        nodePositions.clear();
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
                }
            }
        }
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            connectedNodeUuids.clear();
            nodePositions.clear();
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
                    }
                }
            }
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
