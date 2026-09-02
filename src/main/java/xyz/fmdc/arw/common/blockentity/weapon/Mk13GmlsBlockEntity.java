package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import xyz.fmdc.arw.common.item.Rim66m2Item;
import xyz.fmdc.arw.common.menu.Mk13GmlsMenu;
import xyz.fmdc.arw.registry.ModBlocks;
import xyz.fmdc.arw.registry.ModEntities;
import xyz.fmdc.arw.registry.auto.ModItems;

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
    public static final float RELOAD_ANIM_DURATION = 8.0f; // 160 ticks = 8.0秒の再装填サイクル

    public static final int INVENTORY_SIZE = 9;

    // ランチャー上のミサイル搭載位置オフセット（GLBのpitchノードローカル座標）
    public static final double MOUNTED_MISSILE_X = 0.0D;
    public static final double MOUNTED_MISSILE_Y = 0.5625D; // レール前側へ 0.5625m オフセット (0.625 - 0.0625)
    public static final double MOUNTED_MISSILE_Z = 0.285D;

    // mk13-gmls.glb の pitch ノード初期平行移動量
    public static final double PITCH_PIVOT_X = 0.0D;
    public static final double PITCH_PIVOT_Y = 2.0082097D;
    public static final double PITCH_PIVOT_Z = -0.56523925D;

    private int tickCounter = 0;

    // インベントリ機能 (Forge ItemStackHandler)
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

    /**
     * ランチャー上の描画ミサイル位置（Mk13GmlsRenderer）と完全に連動したミサイル発射地点のワールド絶対座標を計算します。
     */
    @Override
    public Vec3 getLaunchPosition() {
        double pitchRad = Math.toRadians(this.currentPitch + getPitchModelOffset());
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);

        // pitch ノード内での回転（Axis.XP: X軸回転）
        double x1 = MOUNTED_MISSILE_X;
        double y1 = MOUNTED_MISSILE_Y * cosPitch - MOUNTED_MISSILE_Z * sinPitch;
        double z1 = MOUNTED_MISSILE_Y * sinPitch + MOUNTED_MISSILE_Z * cosPitch;

        // mk13-gmls.glb の pitch ノード初期平行移動量
        double x2 = x1 + PITCH_PIVOT_X;
        double y2 = y1 + PITCH_PIVOT_Y;
        double z2 = z1 + PITCH_PIVOT_Z;

        // yaw ノードでの回転（Axis.YP: Y軸回転 -currentYaw）
        double yawRad = Math.toRadians(-this.currentYaw);
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);

        double xRel = x2 * cosYaw + z2 * sinYaw;
        double yRel = y2;
        double zRel = -x2 * sinYaw + z2 * cosYaw;

        return new Vec3(
                this.worldPosition.getX() + 0.5 + xRel,
                this.worldPosition.getY() + yRel,
                this.worldPosition.getZ() + 0.5 + zRel
        );
    }

    @Override
    public Vec3 getLaunchOffset() {
        return getLaunchPosition().subtract(Vec3.atCenterOf(this.worldPosition));
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

    /**
     * インベントリ内に RIM-66M-2 が装填されているかどうかを判定します。
     */
    public boolean hasRim66M2() {
        return getRim66M2Count() > 0;
    }

    /**
     * インベントリ内の RIM-66M-2 アイテムの総数を取得します。
     */
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

    /**
     * ランチャーが現在リロード中（再装填クールダウン中またはリロード/発射アニメーション再生中）かどうかを判定します。
     */
    public boolean isReloading() {
        if (this.cooldownTicks > 0) {
            return true;
        }
        if (this.level != null && !this.runningAnimations.isEmpty()) {
            return this.runningAnimations.containsKey("reload") || this.runningAnimations.containsKey("fire");
        }
        return false;
    }

    /**
     * ランチャー中央レールにミサイルを描画するかどうか（単装ランチャー）
     * 発射中・リロード中（isReloading()）はミサイルが消失し、
     * リロード完了時にインベントリ内に RIM-66M-2 が存在する場合に描画されます。
     */
    public boolean shouldRenderMissile() {
        return !isReloading() && hasRim66M2();
    }

    /** 後方互換用エイリアス */
    public boolean shouldRenderLeftMissile() {
        return shouldRenderMissile();
    }

    /** 後方互換用エイリアス */
    public boolean shouldRenderRightMissile() {
        return shouldRenderMissile();
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

    // --- MenuProvider の実装 ---
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
