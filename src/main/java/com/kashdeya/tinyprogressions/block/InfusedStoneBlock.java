package com.kashdeya.tinyprogressions.block;

import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FlowingFluid;

public final class InfusedStoneBlock extends Block {
    public InfusedStoneBlock(Properties properties) { super(properties); }
    @Override public boolean dropFromExplosion(Explosion explosion) { return false; }
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving) { super.onPlace(state,level,pos,old,moving); convertWater(level,pos); }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving) { convertWater(level,pos); }
    private void convertWater(Level level,BlockPos pos) {
        if(level.isClientSide) return;
        for(Direction direction:Direction.values()) {
            BlockPos neighbor=pos.relative(direction);
            var fluid=level.getFluidState(neighbor);
            if(!fluid.is(FluidTags.WATER)) continue;
            // Waterlogged blocks are left intact; converting them would destroy another block's contents.
            if(!(level.getBlockState(neighbor).getBlock() instanceof LiquidBlock)) continue;
            Block replacement=fluid.isSource() ? Blocks.OBSIDIAN : fluid.getAmount()>=4 ? Blocks.COBBLESTONE : null;
            if(replacement!=null) {
                level.setBlock(neighbor,replacement.defaultBlockState(),3);
                level.playSound(null,neighbor,SoundEvents.LAVA_EXTINGUISH,SoundSource.BLOCKS,.5F,2.6F+(level.random.nextFloat()-level.random.nextFloat())*.8F);
                if(level instanceof ServerLevel server) server.sendParticles(ParticleTypes.LARGE_SMOKE,neighbor.getX()+.5,neighbor.getY()+1.2,neighbor.getZ()+.5,8,.5,0,.5,0);
                return;
            }
        }
    }
    @Override public void stepOn(Level level,BlockPos pos,BlockState state,Entity entity) {
        var v=entity.getDeltaMovement(); entity.setDeltaMovement(v.x*.25,v.y,v.z*.25);
        if(!level.isClientSide && !entity.fireImmune() && entity instanceof LivingEntity living && !EnchantmentHelper.hasFrostWalker(living)) entity.hurt(level.damageSources().hotFloor(),1);
        super.stepOn(level,pos,state,entity);
    }
}
