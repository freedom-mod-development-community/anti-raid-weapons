package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import xyz.fmdc.arw.api.control.IDirectMannedWeapon;
import xyz.fmdc.arw.registry.ModBlocks;

/**
 * FCSを持たず、プレイヤーが直接座席に乗り込んで目視で操作・射撃を行う旧式機銃・高角砲
 */
public class WWIIAntiAircraftGunBlockEntity extends StandaloneManualWeaponBlockEntity implements IDirectMannedWeapon {

    private Player mountedPlayer = null;

    public WWIIAntiAircraftGunBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.WW2_AA_GUN_BLOCK.getBEType(), pos, state);
    }

    @Override protected float getYawTurnSpeed() { return 8.0f; } // 手動旋回スピード
    @Override protected float getPitchTurnSpeed() { return 6.0f; }
    @Override protected float getMinYaw() { return -180.0f; }
    @Override protected float getMaxYaw() { return 180.0f; }
    @Override protected float getMinPitch() { return -10.0f; }
    @Override protected float getMaxPitch() { return 85.0f; }

    @Override
    public void fire() {
        playAnimation("fire", 0.2f);
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
        addManualInput(yawDelta, pitchDelta);
        if (triggerFire) {
            fire();
        }
    }
}
