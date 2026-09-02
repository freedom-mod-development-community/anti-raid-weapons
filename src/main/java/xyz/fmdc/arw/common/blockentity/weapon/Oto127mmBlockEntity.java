package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.api.control.IRemoteControllableWeapon;
import xyz.fmdc.arw.common.blockentity.AbstractSingleGunBlockEntity;
import xyz.fmdc.arw.common.entity.projectile.FiveInchAmmoType;
import xyz.fmdc.arw.common.entity.projectile.FiveInchShellEntity;
import xyz.fmdc.arw.common.menu.Oto127mmMenu;
import xyz.fmdc.arw.registry.ModBlocks;
import xyz.fmdc.arw.registry.ModEntities;
import xyz.fmdc.arw.registry.ModSounds;

import java.util.UUID;

public class Oto127mmBlockEntity extends AbstractSingleGunBlockEntity implements IRemoteControllableWeapon, MenuProvider {

    // 旋回性能（1Tickあたりに回転できる最大角度）
    private static final float YAW_TURN_SPEED = 3.0f;   // 1Tickあたり最大3度
    private static final float PITCH_TURN_SPEED = 2.0f; // 1Tickあたり最大2度

    // 可動域制限
    private static final float MIN_YAW = -45.0f;
    private static final float MAX_YAW = 45.0f;
    private static final float MIN_PITCH = -65.0f; // マイナスが仰角（上向き）
    private static final float MAX_PITCH = 15.0f;

    public static final float FIRE_ANIM_DURATION = 1.0f;
    public static final float RELOAD_ANIM_DURATION = 2.33f;

    public static final int INVENTORY_SIZE = 9;

    private int tickCounter = 0;
    private FiveInchAmmoType currentAmmo = FiveInchAmmoType.MK80_HE_PD;
    private UUID controllerPlayerUUID = null;

    // インベントリ機能 (Forge ItemStackHandler)
    private final ItemStackHandler inventory = new ItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> this.inventory);

    public Oto127mmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.OTO127MM.getBEType(), pos, state);
        this.limitYaw = true;
        animationDurations.put("fire", FIRE_ANIM_DURATION);
        animationDurations.put("reload", RELOAD_ANIM_DURATION);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, Oto127mmBlockEntity be) {
        // 共通の武器旋回・アニメーション・クールダウン処理を実行
        be.tickSingleGun();
        //be.currentPitch = -((float) be.tickCounter / 10) % 30;

        // テスト用：発射処理
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
        fireProcess();
    }

    @Override
    public FiveInchAmmoType getSelectedAmmoType() {
        return this.currentAmmo;
    }

    public void setSelectedAmmoType(FiveInchAmmoType ammoType) {
        this.currentAmmo = ammoType;
        this.setChanged();
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
        float barrelLength = 3.5f;
        float pivotHeight = 1.64f;
        Vec3 dir = getFiringDirection();
        return new Vec3(
                dir.x * barrelLength,
                dir.y * barrelLength + pivotHeight,
                dir.z * barrelLength
        );
    }

    @Override
    public int getMaxCooldownTicks() {
        return 30;
    }

    @Override
    public SoundEvent getFireSound() {
        return ModSounds.OTO127_FIRE.get();
    }

    @Override
    protected boolean canFire() {
        return this.cooldownTicks <= 0;
    }

    // --- 旋回性能定義のオーバーライド ---
    @Override protected float getYawTurnSpeed() { return YAW_TURN_SPEED; }
    @Override protected float getPitchTurnSpeed() { return PITCH_TURN_SPEED; }
    @Override protected float getMinYaw() { return MIN_YAW; }
    @Override protected float getMaxYaw() { return MAX_YAW; }
    @Override protected float getMinPitch() { return MIN_PITCH; }
    @Override protected float getMaxPitch() { return MAX_PITCH; }

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
        return Component.translatable("block.arw.oto127mm");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new Oto127mmMenu(id, playerInventory, this);
    }

    // --- IRemoteControllableWeapon の実装 ---
    @Override
    public Vec3 getCameraPosition() {
        return Vec3.atCenterOf(this.worldPosition).add(0.0, 1.8, 0.5);
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
        return new AABB(this.worldPosition).inflate(5.0);
    }
}
