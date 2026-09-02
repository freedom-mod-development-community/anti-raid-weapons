package xyz.fmdc.arw.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.client.util.FastGlbModel;
import xyz.fmdc.arw.client.util.IDirectionalBlockEntity;

import java.util.function.Function;

/**
 * 回転などのギミックを持たない、固定・静的なGLBモデル用の抽象レンダラークラス。
 */
public class BaseStaticRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    protected final GenericFastGlbRenderer glbRenderer = new GenericFastGlbRenderer();
    private final Function<T, FastGlbModel> modelProvider;

    // 1行登録用のコンストラクタ
    public BaseStaticRenderer(BlockEntityRendererProvider.Context context, Function<T, FastGlbModel> modelProvider) {
        this.modelProvider = modelProvider;
    }

    @Override
    public void render(@NotNull T blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        FastGlbModel modelData = this.modelProvider.apply(blockEntity);
        if (modelData == null) { return; }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        Direction facing = getDirectionFromBlockEntity(blockEntity);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        //poseStack.translate(-0.5, 0.0, -0.5);

        glbRenderer.render(
                modelData, poseStack, bufferSource, packedLight, packedOverlay, partialTick,
                java.util.Collections.emptyList(),null, false
        );
        poseStack.popPose();

        // 【デバッグ用】AABBのワイヤーフレーム描画
        AABB aabb = blockEntity.getRenderBoundingBox();

        // worldPosition からの相対座標に変換して描画
        Vec3 pos = Vec3.atLowerCornerOf(blockEntity.getBlockPos());
        AABB relativeAABB = aabb.move(-pos.x, -pos.y, -pos.z);

        // 赤色（1.0f, 0.0f, 0.0f, 1.0f）の線で描画
        LevelRenderer.renderLineBox(
                poseStack,
                bufferSource.getBuffer(RenderType.lines()),
                relativeAABB,
                1.0F, 0.0F, 0.0F, 1.0F
        );
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    public Direction getDirectionFromBlockEntity(BlockEntity blockEntity) {
        if (blockEntity instanceof IDirectionalBlockEntity directional) {
            return directional.getFacing();
        }
        // インターフェースを実装していない一般ブロック用のフォールバック
        return Direction.SOUTH;
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull T blockEntity) {
        // 中心ブロックが画面外（背後など）にあっても、
        // AABBが視界内に入っていれば描画処理を続行させる
        return true;
    }
}