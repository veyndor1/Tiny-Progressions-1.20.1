package com.kashdeya.tinyprogressions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SourceStoneBlock extends Block {
    private final boolean lava;
    public SourceStoneBlock(Properties properties,boolean lava) { super(properties); this.lava=lava; }
    @Override public boolean dropFromExplosion(Explosion explosion) { return false; }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random) {
        if(random.nextInt(100)==0) {
            level.addParticle(lava ? net.minecraft.core.particles.ParticleTypes.LAVA : net.minecraft.core.particles.ParticleTypes.BUBBLE,
                    pos.getX()+random.nextDouble(),pos.getY()+1,pos.getZ()+random.nextDouble(),0,.05,0);
            if(lava) level.playLocalSound(pos.getX(),pos.getY(),pos.getZ(),net.minecraft.sounds.SoundEvents.LAVA_POP,net.minecraft.sounds.SoundSource.BLOCKS,.2F+random.nextFloat()*.2F,.9F+random.nextFloat()*.15F,false);
        }
        if(random.nextInt(200)==0) level.playLocalSound(pos.getX(),pos.getY(),pos.getZ(),lava ? net.minecraft.sounds.SoundEvents.LAVA_AMBIENT : net.minecraft.sounds.SoundEvents.WATER_AMBIENT,net.minecraft.sounds.SoundSource.BLOCKS,.2F+random.nextFloat()*.2F,.9F+random.nextFloat()*.15F,false);
        if(random.nextInt(100)==0) level.addParticle(lava ? net.minecraft.core.particles.ParticleTypes.DRIPPING_LAVA : net.minecraft.core.particles.ParticleTypes.DRIPPING_WATER,
                pos.getX()+random.nextDouble(),pos.getY(),pos.getZ()+random.nextDouble(),0,0,0);
    }
    @Override public void playerDestroy(Level level,Player player,BlockPos pos,BlockState state,BlockEntity entity,ItemStack tool) {
        super.playerDestroy(level,player,pos,state,entity,tool);
        if(!level.isClientSide && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH,tool)==0 && level.isEmptyBlock(pos))
            level.setBlock(pos,(lava ? Blocks.LAVA : Blocks.WATER).defaultBlockState(),3);
    }
    @Override public void fallOn(Level level,BlockState state,BlockPos pos,Entity entity,float distance) { entity.causeFallDamage(distance,3,level.damageSources().fall()); }
    @Override public void stepOn(Level level,BlockPos pos,BlockState state,Entity entity) {
        var movement=entity.getDeltaMovement(); entity.setDeltaMovement(movement.x*.8,movement.y,movement.z*.8);
        if(!level.isClientSide && lava && !entity.fireImmune() && entity instanceof LivingEntity living && !EnchantmentHelper.hasFrostWalker(living))
            entity.hurt(level.damageSources().hotFloor(),1);
        super.stepOn(level,pos,state,entity);
    }
}
