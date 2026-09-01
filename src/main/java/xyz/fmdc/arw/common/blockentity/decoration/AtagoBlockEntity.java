package xyz.fmdc.arw.common.blockentity.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.fmdc.arw.client.util.IDirectionalBlockEntity;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.registry.ModBlocks;

public class AtagoBlockEntity extends AbstractARWBlockEntity  implements IDirectionalBlockEntity {

    public AtagoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ATAGO.getBEType(), pos, state);
    }

    @Override
    public Direction getFacing(){
        return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public AABB getRenderBoundingBox() {
        Direction dir = getFacing();

        // 南北(NORTH/SOUTH)か東西(EAST/WEST)かでX軸・Z軸のサイズを切り替え
        boolean isNorthSouth = (dir == Direction.NORTH || dir == Direction.SOUTH);

        double xSize = isNorthSouth ? 25.0 : 180.0;
        double zSize = isNorthSouth ? 180.0 : 25.0;
        double ySize = 100.0; // Y軸（高さ方向）の範囲（必要に応じて調整）

        // ブロックの中心座標 (x + 0.5, y + 0.5, z + 0.5) を基準に各軸の全幅を指定してAABBを生成
        Vec3 center = Vec3.atCenterOf(this.worldPosition);
        return AABB.ofSize(center, xSize, ySize, zSize);
    }
}
