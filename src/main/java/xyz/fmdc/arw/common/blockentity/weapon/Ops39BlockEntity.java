package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.fmdc.arw.api.control.IRemoteControllableWeapon;
import xyz.fmdc.arw.client.util.IYawModel;
import xyz.fmdc.arw.common.blockentity.AbstractSingleGunBlockEntity;
import xyz.fmdc.arw.common.entity.projectile.FiveInchAmmoType;
import xyz.fmdc.arw.common.entity.projectile.FiveInchShellEntity;
import xyz.fmdc.arw.registry.ModBlocks;
import xyz.fmdc.arw.registry.ModEntities;

import java.util.UUID;

public class Ops39BlockEntity extends AbstractSingleGunBlockEntity implements IYawModel, IRemoteControllableWeapon {

    private static final float YAW_TURN_SPEED = 6.0f;
    private static final float PITCH_TURN_SPEED = 5.0f;
    private static final float MIN_YAW = -180.0f;
    private static final float MAX_YAW = 180.0f;
    private static final float MIN_PITCH = -45.0f;
    private static final float MAX_PITCH = 45.0f;

    private FiveInchAmmoType currentAmmo = FiveInchAmmoType.MK80_HE_PD;
    private UUID controllerPlayerUUID = null;

    public Ops39BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.OPS39.getBEType(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, Ops39BlockEntity be) {
        be.tickSingleGun();
    }

    @Override
    public float getTargetYaw(float partialTick) {
        return getRenderTargetYaw(partialTick);
    }

    @Override
    public void fire() {
        if (!canFire()) return;
        fireProcess();
    }

    @Override
    public FiveInchAmmoType getSelectedAmmoType() {
        return this.currentAmmo;
    }

    @Override
    public EntityType<FiveInchShellEntity> getShellEntityType() {
        return ModEntities.FIVE_INCH_SHELL.get();
    }

    @Override
    public Vec3 getFiringDirection() {
        return Vec3.directionFromRotation(this.currentPitch, this.currentYaw);
    }

    @Override
    public Vec3 getMuzzleOffset() {
        return new Vec3(0.0, 1.0, 0.0);
    }

    @Override
    public int getMaxCooldownTicks() {
        return 20;
    }

    @Override
    protected boolean canFire() {
        return this.cooldownTicks <= 0;
    }

    // --- 旋回性能定義 ---
    @Override protected float getYawTurnSpeed() { return YAW_TURN_SPEED; }
    @Override protected float getPitchTurnSpeed() { return PITCH_TURN_SPEED; }
    @Override protected float getMinYaw() { return MIN_YAW; }
    @Override protected float getMaxYaw() { return MAX_YAW; }
    @Override protected float getMinPitch() { return MIN_PITCH; }
    @Override protected float getMaxPitch() { return MAX_PITCH; }

    // --- IRemoteControllableWeapon の実装 ---
    @Override
    public Vec3 getCameraPosition() {
        return Vec3.atCenterOf(this.worldPosition).add(0.0, 1.5, 0.0);
    }

    @Override
    public boolean isBeingRemoteControlled() {
        return this.controllerPlayerUUID != null;
    }

    @Override
    public void startRemoteControl(Player player) {
        this.controllerPlayerUUID = player.getUUID();
        syncToClient();
    }

    @Override
    public void stopRemoteControl(Player player) {
        this.controllerPlayerUUID = null;
        syncToClient();
    }

    @Override
    public void handleRemoteInput(float yawInput, float pitchInput, boolean triggerFire) {
        setTargetYaw(yawInput);
        setTargetPitch(pitchInput);
        if (triggerFire && canFire()) {
            fire();
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(3.0);
    }
}
