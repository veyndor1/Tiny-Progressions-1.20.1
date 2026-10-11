package com.kashdeya.tinyprogressions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Source-derived decoration behavior shared by compressed, hardened, brick and glass blocks. */
public final class LegacyDecorativeBlock extends Block {
    private final boolean explosionDrops, explosionImmune, tripleFall, glass;
    public LegacyDecorativeBlock(Properties properties, boolean explosionDrops, boolean explosionImmune, boolean tripleFall, boolean glass) {
        super(properties); this.explosionDrops=explosionDrops; this.explosionImmune=explosionImmune; this.tripleFall=tripleFall; this.glass=glass;
    }
    @Override public boolean dropFromExplosion(Explosion explosion) { return explosionDrops && super.dropFromExplosion(explosion); }
    @Override public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (!explosionImmune) super.onBlockExploded(state,level,pos,explosion);
    }
    @Override public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return !(explosionImmune && entity instanceof Creeper) && super.canEntityDestroy(state,level,pos,entity);
    }
    @Override public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) {
        entity.causeFallDamage(distance,tripleFall ? 3 : 1,entity.damageSources().fall());
    }
    @Override public boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        return glass && adjacent.is(this) || super.skipRendering(state,adjacent,side);
    }
}
