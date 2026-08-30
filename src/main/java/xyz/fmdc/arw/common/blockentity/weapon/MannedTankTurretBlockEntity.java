package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import xyz.fmdc.arw.api.control.IDirectMannedWeapon;
import xyz.fmdc.arw.common.blockentity.AbstractSingleGunBlockEntity;
import xyz.fmdc.arw.registry.ModBlocks;

/**
 * 砲手として砲塔内に乗り込みつつ、FCSからリアルタイムに偏差計算アシスト（FiringSolution）を受けて射撃する近代戦車砲
 */
public class MannedTankTurretBlockEntity extends AbstractSingleGunBlockEntity implements IDirectMannedWeapon {

    private Player mountedPlayer = null;

    public MannedTankTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MANNED_TANK_TURRET_BLOCK.getBEType(), pos, state);
    }

    @Override protected float getYawTurnSpeed() { return 6.0f; }
    @Override protected float getPitchTurnSpeed() { return 4.0f; }
    @Override protected float getMinYaw() { return -180.0f; }
    @Override protected float getMaxYaw() { return 180.0f; }
    @Override protected float getMinPitch() { return -10.0f; }
    @Override protected float getMaxPitch() { return 20.0f; }

    @Override
    public void fire() {
        playAnimation("recoil", 0.4f);
    }

    @Override
    protected boolean canFire() {
        return true;
    }

    // --- IDirectMannedWeapon の実装 ---
    @Override public boolean isManned() { return this.mountedPlayer != null; }
    @Override public Player getControllingPlayer() { return this.mountedPlayer; }

    @Override
    public void mountPlayer(Player player) {
        this.mountedPlayer = player;
    }

    @Override
    public void dismountPlayer() {
        this.mountedPlayer = null;
    }

    @Override
    public void handleMannedInput(float yawDelta, float pitchDelta, boolean triggerFire) {
        // FCSの自動追従に対して手動で補正・微調整を加える処理（スケルトン）
        setTargetYaw(this.targetYaw + yawDelta);
        setTargetPitch(this.targetPitch + pitchDelta);
        if (triggerFire && canFire()) {
            fire();
        }
    }
}
