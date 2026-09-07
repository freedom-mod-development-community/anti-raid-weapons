package xyz.fmdc.arw.common.blockentity.weapon.launcher;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import xyz.fmdc.arw.client.GlbModelManager;
import xyz.fmdc.arw.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;

public class BonedMissileLauncherBlockEntity extends AbstractYukkyMissileLauncherBlockEntity{

    // 旋回性能（1Tickあたりに回転できる最大角度）
    private static final float YAW_TURN_SPEED = 3.0f;   // 1Tickあたり最大3度
    private static final float PITCH_TURN_SPEED = 2.0f; // 1Tickあたり最大2度

    // 可動域制限
    private static final float MIN_YAW = -180.0f;
    private static final float MAX_YAW = 180.0f;
    private static final float MIN_PITCH = -90.0f; // 仰角（上向き）
    private static final float MAX_PITCH = 95.0f;  // 俯角（下向き）

    // 例: 2箇所のマウントポイントの状態管理
    private final List<ResourceLocation> loadedMissiles = new ArrayList<>();
    private int tick = 0;

    public BonedMissileLauncherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MK13BONE.getBEType(), pos, state);
        loadedMissiles.add(GlbModelManager.RIM66M2_ID); // 1発目: RIM-66M2
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BonedMissileLauncherBlockEntity be) {
        be.tickMissileLauncher();
        be.targetPitch = (float)Math.sin((float) be.tick / 10)*20;
        be.targetYaw = (float)Math.sin((float) be.tick / 20)*80;
        be.tick ++;
    }

    @Override
    public ResourceLocation getLauncherModelId() {
        return GlbModelManager.MK13BONE_ID;
    }

    @Override
    public List<MissileSlot> getLoadedMissileSlots() {
        List<MissileSlot> slots = new ArrayList<>();

        for (int i = 0; i < loadedMissiles.size(); i++) {
            ResourceLocation missileId = loadedMissiles.get(i);

            // 装填されている（nullでない）スロットのみリストに追加
            if (missileId != null) {
                // 例: i=0 -> "mount_point_1", i=1 -> "mount_point_2"
                String boneName = "mount_point" + (i + 1);
                slots.add(new MissileSlot(boneName, missileId));
            }
        }

        return slots;
    }

    // 発射時の処理イメージ
    public void fireSlot(int slotIndex) {
        // 1. ダミーボーンのワールド座標を取得して MissileEntity を生成・スポーン
        // 2. スロットを空にする (slot1Missile = null;) -> 次フレームから自動的に非表示に
    }

    @Override
    protected float getYawTurnSpeed() {
        return YAW_TURN_SPEED;
    }
    @Override
    protected float getPitchTurnSpeed() {
        return PITCH_TURN_SPEED;
    }
    @Override
    protected float getMinYaw() {
        return MIN_YAW;
    }
    @Override
    protected float getMaxYaw() {
        return MAX_YAW;
    }
    @Override
    protected float getMinPitch() {
        return MIN_PITCH;
    }
    @Override
    protected float getMaxPitch() {
        return MAX_PITCH;
    }
    @Override
    public int getMaxCooldownTicks() {
        return 0;
    }
    @Override
    protected boolean canFire() {
        return false;
    }
    @Override
    public void fire() {
    }
}