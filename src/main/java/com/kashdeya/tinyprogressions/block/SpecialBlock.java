package com.kashdeya.tinyprogressions.block;

import com.kashdeya.tinyprogressions.LegacyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class SpecialBlock extends Block {
    private final String kind;
    public SpecialBlock(Properties properties,String kind) { super(properties); this.kind=kind; }
    @Override public boolean dropFromExplosion(Explosion explosion) { return kind.equals("asphalt_block"); }
    @Override public void fallOn(Level level,BlockState state,BlockPos pos,Entity entity,float distance) {
        if(kind.equals("asphalt_block") || kind.equals("unhardened_stone")) entity.causeFallDamage(distance,3,level.damageSources().fall());
        else super.fallOn(level,state,pos,entity,distance);
    }
    @Override public void stepOn(Level level,BlockPos pos,BlockState state,Entity entity) {
        if(kind.equals("asphalt_block")) { Vec3 v=entity.getDeltaMovement(); entity.setDeltaMovement(v.x*1.8,v.y,v.z*1.8); }
        if(!level.isClientSide && kind.equals("withered_block") && entity instanceof Player player)
            player.addEffect(new MobEffectInstance(MobEffects.WITHER,LegacyConfig.integer("ConfigHandler.WitheredBlockTime")*20,0,true,LegacyConfig.enabled("ConfigHandler.WitheredBlockParticals")));
        super.stepOn(level,pos,state,entity);
    }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if(kind.equals("quick_sand")) entity.makeStuckInBlock(state,new Vec3(.25,.05,.25));
        super.entityInside(state,level,pos,entity);
    }
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos pos,Entity entity) {
        return !(kind.startsWith("reinforced_") && entity instanceof WitherBoss) && super.canEntityDestroy(state,level,pos,entity);
    }
    @Override public void onBlockExploded(BlockState state,Level level,BlockPos pos,Explosion explosion) {
        if(!kind.startsWith("reinforced_")) super.onBlockExploded(state,level,pos,explosion);
    }
    @Override public boolean skipRendering(BlockState state,BlockState adjacent,net.minecraft.core.Direction direction) {
        return kind.equals("reinforced_glass") && adjacent.is(this) || super.skipRendering(state,adjacent,direction);
    }
}
