package com.kashdeya.tinyprogressions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class OldReedBlock extends BushBlock {
    public OldReedBlock(Properties properties) { super(properties); }
    @Override protected boolean mayPlaceOn(BlockState state,BlockGetter level,BlockPos pos) { return state.is(Blocks.SAND); }
    @Override public boolean canSurvive(BlockState state,net.minecraft.world.level.LevelReader level,BlockPos pos) { return mayPlaceOn(level.getBlockState(pos.below()),level,pos.below()); }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return box(2,0,2,14,16,14); }
}
