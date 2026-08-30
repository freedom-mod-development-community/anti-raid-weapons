package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import xyz.fmdc.arw.api.control.IRemoteControllableWeapon;
import xyz.fmdc.arw.common.blockentity.AbstractSingleGunBlockEntity;
import xyz.fmdc.arw.registry.ModBlocks;

/**
 * FCSの自動追従機能を持たず、遠隔カメラ映像越しにプレイヤーが直接マウス等で動かす小口径RWS
 */
public class ManualRwsGunBlockEntity extends AbstractSingleGunBlockEntity implements IRemoteControllableWeapon {

    private Player controllingPlayer = null;

    public ManualRwsGunBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MANUAL_RWS_GUN_BLOCK.getBEType(), pos, state);
    }

    @Override protected float getYawTurnSpeed() { return 12.0f; }
    @Override protected float getPitchTurnSpeed() { return 10.0f; }
    @Override protected float getMinYaw() { return -180.0f; }
    @Override protected float getMaxYaw() { return 180.0f; }
    @Override protected float getMinPitch() { return -20.0f; }
    @Override protected float getMaxPitch() { return 60.0f; }

    @Override
    public void fire() {
        playAnimation("fire", 0.1f);
    }

    @Override
    protected boolean canFire() {
        return false;
    }

    // --- IRemoteControllableWeapon の実装 ---
    @Override
    public Vec3 getCameraPosition() {
        return Vec3.atCenterOf(this.worldPosition).add(0.0, 1.2, 0.0);
    }

    @Override public boolean isBeingRemoteControlled() { return this.controllingPlayer != null; }
    @Override public void startRemoteControl(Player player) { this.controllingPlayer = player; }
    @Override public void stopRemoteControl(Player player) { this.controllingPlayer = null; }

    @Override
    public void handleRemoteInput(float yawInput, float pitchInput, boolean triggerFire) {
        setTargetYaw(yawInput);
        setTargetPitch(pitchInput);
        if (triggerFire) {
            fire();
        }
    }
}
