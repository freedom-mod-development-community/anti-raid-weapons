package xyz.fmdc.arw.baseclass.modelblock

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import xyz.fmdc.arw.registry.RegistryBlock

abstract class ModelNormalBlockContainer(
    val tileEntityClass: Class<out ModelNormalTileEntity>,
    properties: Properties = Properties.of()
        .mapColor(MapColor.METAL)
        .strength(3.0f, 6.0f)
        .sound(SoundType.METAL)
        .noOcclusion()
) : BaseEntityBlock(properties) {

    companion object {
        val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING
    }

    var blockName: String = ""
        private set
    var blockTextureName: String = ""
        private set

    // TileEntityのバウンディングボックス計算用
    var selectBoundsHalfWide: Double = 0.5
        private set
    var selectBoundsHeight: Double = 1.0
        private set

    // 1.20.1 の当たり判定 (VoxelShape)
    private var collisionShape: VoxelShape = Shapes.block()

    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    protected fun setBlockName(name: String) {
        this.blockName = name
    }

    protected fun setBlockTextureName(texture: String) {
        this.blockTextureName = texture
    }

    /**
     * 1.7.10 互換のブロック当たり判定設定メソッド
     * 水平幅 (width) と 高さ (height) から中央揃えの VoxelShape を生成
     */
    protected fun setBlockBoundsSize(width: Float, height: Float) {
        this.selectBoundsHalfWide = (width / 2.0).toDouble()
        this.selectBoundsHeight = height.toDouble()

        val minXZ = ((1.0 - width) / 2.0 * 16.0).coerceAtLeast(0.0)
        val maxXZ = ((1.0 + width) / 2.0 * 16.0).coerceAtMost(16.0)
        val maxY = (height * 16.0).coerceAtMost(16.0)

        this.collisionShape = Block.box(minXZ, 0.0, minXZ, maxXZ, maxY, maxXZ)
    }

    protected fun setSelectedBoundSize(width: Double, height: Double) {
        this.selectBoundsHalfWide = width / 2.0
        this.selectBoundsHeight = height
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        return defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)
    }

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return collisionShape
    }

    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.ENTITYBLOCK_ANIMATED
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        RegistryBlock.blockEntityType(tileEntityClass).create(pos, state)

    /**
     * 設置時に、BlockState の FACING と TileEntity 側の描画用角度を揃える。
     */
    override fun setPlacedBy(
        level: Level,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        stack: ItemStack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack)
        (level.getBlockEntity(pos) as? ModelNormalTileEntity)?.let { tile ->
            tile.setDirection(state.getValue(FACING))
            tile.setChanged()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return BlockEntityTicker { lvl, pos, st, blockEntity ->
            if (blockEntity is ModelNormalTileEntity) {
                blockEntity.tick(lvl, pos, st)
            }
        }
    }
}