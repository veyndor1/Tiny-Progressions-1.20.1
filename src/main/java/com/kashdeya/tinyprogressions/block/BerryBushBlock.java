package com.kashdeya.tinyprogressions.block;

import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Legacy berry bushes are static bushes; their berries are supplied by loot tables. */
public final class BerryBushBlock extends BushBlock {
    private final String berry;
    public BerryBushBlock(Properties properties, String berry) { super(properties); this.berry = berry; }
    public String berry() { return berry; }
    @Override protected boolean mayPlaceOn(BlockState state,BlockGetter level,BlockPos pos) { return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT); }
    @Override public boolean canSurvive(BlockState state,net.minecraft.world.level.LevelReader level,BlockPos pos) { return mayPlaceOn(level.getBlockState(pos.below()),level,pos.below()); }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return box(2,0,2,14,16,14); }
}
