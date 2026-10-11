package com.kashdeya.tinyprogressions.block;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Keep old double-block identifiers while ordinary slabs use modern combined states. */
public final class LegacySlabBlock extends SlabBlock {
    private final boolean doubleAlias;
    public LegacySlabBlock(Properties properties,boolean doubleAlias) {
        super(properties); this.doubleAlias=doubleAlias;
        if(doubleAlias) registerDefaultState(defaultBlockState().setValue(TYPE,SlabType.DOUBLE));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return doubleAlias ? defaultBlockState() : super.getStateForPlacement(context); }
    @Override public boolean canBeReplaced(BlockState state,BlockPlaceContext context) { return !doubleAlias && super.canBeReplaced(state,context); }
}
