package xyz.fmdc.arw.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.client.util.FastGlbModel;
import xyz.fmdc.arw.client.util.IDirectionalBlockEntity;
import xyz.fmdc.arw.client.util.IYawModel;

import java.util.function.Function;

public class BaseRadarRenderer<T extends BlockEntity & IYawModel> implements BlockEntityRenderer<T> {

    protected final GenericFastGlbRenderer glbRenderer = new GenericFastGlbRenderer();
    private final Function<T, FastGlbModel> modelProvider;

    // 1行登録用のコンストラクタ
    public BaseRadarRenderer(BlockEntityRendererProvider.Context context, Function<T, FastGlbModel> modelProvider) {
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
                java.util.Collections.emptyList(),
                (nodeName, stack, pTick) -> {
                    if ("radar".equalsIgnoreCase(nodeName) ||
                            "antenna".equalsIgnoreCase(nodeName) ||
                            "yaw".equalsIgnoreCase(nodeName)) {

                        stack.mulPose(Axis.YP.rotationDegrees(-blockEntity.getTargetYaw(pTick)));
                    }
                },
                false
        );
        poseStack.popPose();
    }

    protected FastGlbModel getModelData(T blockEntity) {
        return null;
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
}