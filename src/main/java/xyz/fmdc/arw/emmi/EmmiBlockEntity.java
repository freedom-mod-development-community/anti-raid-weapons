package xyz.fmdc.arw.emmi;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import xyz.fmdc.arw.client.util.INodeRotatableModel;
import xyz.fmdc.arw.registry.ModBlockEntities;
import xyz.fmdc.arw.registry.ModBlocks;

public class EmmiBlockEntity extends BlockEntity{

    public EmmiBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.EMMI.getBEType(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        // ブロックの現在位置を中心に、描画判定領域を上下左右に広げる
        // 例: 上下に3ブロック、東西南北に3ブロック分領域を拡張
        return new AABB(this.worldPosition).inflate(3.0);
    }
}