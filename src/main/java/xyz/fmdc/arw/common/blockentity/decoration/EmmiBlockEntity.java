package xyz.fmdc.arw.common.blockentity.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import xyz.fmdc.arw.client.util.IDirectionalBlockEntity;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.registry.ModBlocks;

public class EmmiBlockEntity extends AbstractARWBlockEntity  implements IDirectionalBlockEntity {

    public EmmiBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.EMMI.getBEType(), pos, state);
    }

    @Override
    public Direction getFacing(){
        return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public AABB getRenderBoundingBox() {
        // ブロックの現在位置を中心に、描画判定領域を上下左右に広げる
        // 例: 上下に3ブロック、東西南北に3ブロック分領域を拡張
        return new AABB(this.worldPosition).inflate(3.0);
    }
}
