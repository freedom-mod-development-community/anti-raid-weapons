package xyz.fmdc.arw.common.blockentity.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * FCS Coreへスキャン・ロックオンデータを直接提供する近代的レーダーの抽象基底クラス
 */
public abstract class FcsRadarBlockEntity extends AbstractHRadarBlockEntity {

    public FcsRadarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract boolean isActiveRadar();
}
