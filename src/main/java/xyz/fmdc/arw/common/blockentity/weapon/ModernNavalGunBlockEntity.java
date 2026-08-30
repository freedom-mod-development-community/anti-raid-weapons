package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import xyz.fmdc.arw.api.control.IRemoteControllableWeapon;
import xyz.fmdc.arw.common.blockentity.AbstractSingleGunBlockEntity;

/**
 * 通常時はFCSによる全自動照準で動作し、緊急・割り込み時にはCICモニター等から手動遠隔射撃ができる近代艦砲の抽象基底クラス
 */
public abstract class ModernNavalGunBlockEntity extends AbstractSingleGunBlockEntity implements IRemoteControllableWeapon {

    protected Player remoteController = null;

    public ModernNavalGunBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Vec3 getCameraPosition() {
        return Vec3.atCenterOf(this.worldPosition).add(0.0, 2.0, 0.0);
    }

    @Override public boolean isBeingRemoteControlled() { return this.remoteController != null; }
    @Override public void startRemoteControl(Player player) { this.remoteController = player; }
    @Override public void stopRemoteControl(Player player) { this.remoteController = null; }

    @Override
    public void handleRemoteInput(float yawInput, float pitchInput, boolean triggerFire) {
        // 手動操作が割り込んだ場合はFCS指示よりこちらを優先
        setTargetYaw(yawInput);
        setTargetPitch(pitchInput);
        if (triggerFire && canFire()) {
            fire();
        }
    }
}