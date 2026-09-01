package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.common.blockentity.AbstractMissileLauncherBlockEntity;
import xyz.fmdc.arw.common.entity.AbstractMissileEntity;
import xyz.fmdc.arw.common.menu.Mk13GmlsMenu;
import xyz.fmdc.arw.registry.ModBlocks;
import xyz.fmdc.arw.registry.ModEntities;

/**
 * Mk 13 GMLS (Guided Missile Launching System) ブロックエンティティ.
 * 単装ミサイルランチャーの旋回・俯仰・アニメーション・装填管理を行います。
 * RIM-66 SM-2 Block III を発射します。
 */
public class Mk13GmlsBlockEntity extends AbstractMissileLauncherBlockEntity implements MenuProvider {

    // 旋回性能（1Tickあたりに回転できる最大角度）
    private static final float YAW_TURN_SPEED = 3.0f;   // 1Tickあたり最大3度
    private static final float PITCH_TURN_SPEED = 2.0f; // 1Tickあたり最大2度

    // 可動域制限
    private static final float MIN_YAW = -180.0f;
    private static final float MAX_YAW = 180.0f;
    private static final float MIN_PITCH = -65.0f; // 仰角（上向き）
    private static final float MAX_PITCH = 15.0f;  // 俯角（下向き）

    /** 渡された目標角度への到達判定許容誤差（度） */
    private static final float AIM_TOLERANCE = 1.0f;

    public static final float FIRE_ANIM_DURATION = 1.0f;
    public static final float RELOAD_ANIM_DURATION = 2.33f;

    public static final int INVENTORY_SIZE = 9;

    private int tickCounter = 0;

    // インベントリ機能 (Forge ItemStackHandler)
    private final ItemStackHandler inventory = new ItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> this.inventory);

    public Mk13GmlsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MK13_GMLS_BLOCK.getBEType(), pos, state);
        this.limitYaw = false;
        animationDurations.put("fire", FIRE_ANIM_DURATION);
        animationDurations.put("reload", RELOAD_ANIM_DURATION);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, Mk13GmlsBlockEntity be) {
        // 共通のミサイルランチャー旋回・アニメーション・クールダウン処理を実行
        be.tickMissileLauncher();

        // テスト用：定期発射処理
        if (!level.isClientSide) {
            if (be.tickCounter % 120 == 0) {
                be.fire();
            }
        }
        be.tickCounter++;
    }

    @Override
    public void fire() {
        if (!canFire()) return;
        playAnimation("fire", FIRE_ANIM_DURATION);
        playAnimation("reload", RELOAD_ANIM_DURATION);
        launchMissile();
    }

    @Override
    public Vec3 getLaunchOffset() {
        float railLength = 3.0f;
        double pivotHeight = 1.8;
        Vec3 dir = getFiringDirection();
        return new Vec3(
                dir.x * railLength,
                dir.y * railLength + pivotHeight,
                dir.z * railLength
        );
    }

    @Override
    public int getMaxCooldownTicks() {
        return 160; // 約8秒（160 ticks）の次弾装填・サイクル時間
    }

    @Override
    public SoundEvent getLaunchSound() {
        return SoundEvents.GENERIC_EXPLODE;
    }

    @Override
    protected boolean canFire() {
        // クールダウン完了かつ、指示された目標角度への旋回・俯仰が完了している（誤差許容範囲内）場合のみ発射可能
        return this.cooldownTicks <= 0 && isAimAligned(AIM_TOLERANCE);
    }

    @Override
    protected EntityType<? extends AbstractMissileEntity> getMissileEntityType() {
        return ModEntities.RIM_66M2.get();
    }

    // --- 旋回性能定義のオーバーライド ---
    @Override protected float getYawTurnSpeed() { return YAW_TURN_SPEED; }
    @Override protected float getPitchTurnSpeed() { return PITCH_TURN_SPEED; }
    @Override protected float getMinYaw() { return MIN_YAW; }
    @Override protected float getMaxYaw() { return MAX_YAW; }
    @Override protected float getMinPitch() { return MIN_PITCH; }
    @Override protected float getMaxPitch() { return MAX_PITCH; }

    @Override
    public float getPitchModelOffset() {
        return 90.0f; // 設置時・初期姿勢で直立しているpitchモデルを90度下げる補正
    }

    // --- インベントリ & Capability ---
    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    public void drops() {
        if (this.level != null && !this.level.isClientSide) {
            SimpleContainer container = new SimpleContainer(this.inventory.getSlots());
            for (int i = 0; i < this.inventory.getSlots(); i++) {
                container.setItem(i, this.inventory.getStackInSlot(i));
            }
            Containers.dropContents(this.level, this.worldPosition, container);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", this.inventory.serializeNBT());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            this.inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
    }

    // --- MenuProvider の実装 ---
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.arw.mk13_gmls");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new Mk13GmlsMenu(id, playerInventory, this);
    }
}
