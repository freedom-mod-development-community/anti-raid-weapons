package xyz.fmdc.arw.common.blockentity.fcs;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import xyz.fmdc.arw.api.fcs.*;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;

import java.util.*;

/**
 * センサーデータの統合・偏差計算・兵装への指示を行うFCS Coreの基底クラス
 */
public abstract class AbstractFcsCoreBlockEntity extends AbstractARWBlockEntity implements IFcsNetworkNode {

    protected final List<IFcsSensorNode> connectedSensors = new ArrayList<>();
    protected final List<IFcsControllableWeapon> connectedWeapons = new ArrayList<>();

    public AbstractFcsCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tickFcs() {
        if (this.level == null || this.level.isClientSide) return;

    }

    public void registerSensor(IFcsSensorNode sensor) {
        if (!connectedSensors.contains(sensor)) {
            connectedSensors.add(sensor);
            sensor.setFcsConnected(true);
        }
    }

    public void registerWeapon(IFcsControllableWeapon weapon) {
        if (!connectedWeapons.contains(weapon)) {
            connectedWeapons.add(weapon);
            weapon.setFcsConnected(true);
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
