package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import xyz.fmdc.arw.client.util.IDirectionalBlockEntity;
import xyz.fmdc.arw.common.blockentity.AbstractMissileLauncherBlockEntity;
import xyz.fmdc.arw.common.entity.AbstractMissileEntity;
import xyz.fmdc.arw.common.item.Rim66m2Item;
import xyz.fmdc.arw.common.menu.Mk13GmlsMenu;
import xyz.fmdc.arw.registry.ModBlocks;
import xyz.fmdc.arw.registry.ModEntities;
import xyz.fmdc.arw.registry.auto.ModItems;

public class BonedMissileLauncherBlockEntity extends AbstractMissileLauncherBlockEntity implements MenuProvider, IDirectionalBlockEntity {

    // 旋回性能（1Tickあたりに回転できる最大角度）
    private static final float YAW_TURN_SPEED = 3.0f;
    private static final float PITCH_TURN_SPEED = 2.0f;

    // 可動域制限
    private static final float MIN_YAW = -180.0f;
    private static final float MAX_YAW = 180.0f;
    private static final float MIN_PITCH = -65.0f;
    private static final float MAX_PITCH = 15.0f;

    private static final float AIM_TOLERANCE = 1.0f;
    // ランチャー上のミサイル搭載位置オフセット（GLBのpitchノードローカル座標）
    public static final double MOUNTED_MISSILE_X = 0.0D;
    public static final double MOUNTED_MISSILE_Y = -0.25D; // レール前側へ 0.5625m オフセット (0.625 - 0.0625)
    public static final double MOUNTED_MISSILE_Z = 0.585D;

    public static final float FIRE_ANIM_DURATION = 1.0f;
    public static final float RELOAD_ANIM_DURATION = 8.0f;

    public static final int INVENTORY_SIZE = 9;

    // --- ボーン（ノード）構成データ ---
    // 1. Pitchノード原点から見たミサイル装填基準オフセット
    private static final Vector3f MISSILE_MOUNT_OFFSET = new Vector3f(0.0f, 0.5625f, 0.285f);
    // 2. Yawノード原点から見たPitchピボット（回転軸）オフセット
    private static final Vector3f PITCH_PIVOT_OFFSET = new Vector3f(0.0f, 2.0082097f, -0.56523925f);

    private int tickCounter = 0;

    private final ItemStackHandler inventory = new ItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                syncToClient();
            }
        }
    };
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> this.inventory);

    public BonedMissileLauncherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MK13BONE.getBEType(), pos, state);
        this.limitYaw = false;
        animationDurations.put("fire", FIRE_ANIM_DURATION);
        animationDurations.put("reload", RELOAD_ANIM_DURATION);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BonedMissileLauncherBlockEntity be) {
        be.tickMissileLauncher();

        be.currentPitch = ((float) be.tickCounter / 10) % 30;
        be.currentYaw = -((float) be.tickCounter / 7) % 47;

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

    /**
     * GLBのボーン構造（Body -> Yaw -> Pitch）に沿ってアフィン変換行列を組み上げ、
     * 発射座標（ワールド絶対座標）を計算します。
     */
    @Override
    public Vec3 getLaunchPosition() {
        // 変換行列の初期化（単位行列）
        Matrix4f transform = new Matrix4f();

        // 1. Yaw（Y軸回転）を適用
        transform.rotateY((float) Math.toRadians(-this.currentYaw));

        // 2. Pitchピボットへ移動
        transform.translate(PITCH_PIVOT_OFFSET);

        // 3. Pitch（X軸回転）を適用（モデルの姿勢補正込み）
        float totalPitch = this.currentPitch + getPitchModelOffset();
        transform.rotateX((float) Math.toRadians(totalPitch));

        // 4. Pitch座標系におけるミサイル位置をワールドローカル座標へ変換
        Vector3f localPos = transform.transformPosition(new Vector3f(MISSILE_MOUNT_OFFSET));

        // 5. ブロック中心のワールド絶対座標を加算
        return new Vec3(
                this.worldPosition.getX() + 0.5D + localPos.x(),
                this.worldPosition.getY() + localPos.y(),
                this.worldPosition.getZ() + 0.5D + localPos.z()
        );
    }

    @Override
    public Direction getFacing() {
        return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public Vec3 getLaunchOffset() {
        return getLaunchPosition().subtract(Vec3.atCenterOf(this.worldPosition));
    }

    @Override
    public int getMaxCooldownTicks() {
        return 160;
    }

    @Override
    protected boolean canFire() {
        return this.cooldownTicks <= 0 && isAimAligned(AIM_TOLERANCE);
    }

    @Override
    protected EntityType<? extends AbstractMissileEntity> getMissileEntityType() {
        return ModEntities.RIM_66M2.get();
    }

    @Override protected float getYawTurnSpeed() { return YAW_TURN_SPEED; }
    @Override protected float getPitchTurnSpeed() { return PITCH_TURN_SPEED; }
    @Override protected float getMinYaw() { return MIN_YAW; }
    @Override protected float getMaxYaw() { return MAX_YAW; }
    @Override protected float getMinPitch() { return MIN_PITCH; }
    @Override protected float getMaxPitch() { return MAX_PITCH; }

    @Override
    public float getPitchModelOffset() {
        return 90.0f;
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    public boolean hasRim66M2() {
        return getRim66M2Count() > 0;
    }

    public int getRim66M2Count() {
        int count = 0;
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            ItemStack stack = this.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && (stack.getItem() instanceof Rim66m2Item || stack.getItem() == ModItems.RIM_66M2.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public boolean isReloading() {
        if (this.cooldownTicks > 0) return true;
        if (this.level != null && !this.runningAnimations.isEmpty()) {
            return this.runningAnimations.containsKey("reload") || this.runningAnimations.containsKey("fire");
        }
        return false;
    }

    public boolean shouldRenderMissile() {
        return true;//!isReloading() && hasRim66M2();
    }

    public boolean shouldRenderLeftMissile() { return shouldRenderMissile(); }
    public boolean shouldRenderRightMissile() { return shouldRenderMissile(); }

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

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        CompoundTag tag = pkt.getTag();
        if (tag != null && tag.contains("Inventory")) {
            this.inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag.contains("Inventory")) {
            this.inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.arw.mk13-gmls");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new Mk13GmlsMenu(id, playerInventory, this);
    }
}