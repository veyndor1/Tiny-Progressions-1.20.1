package com.kashdeya.tinyprogressions.block;

import com.kashdeya.tinyprogressions.ModFluids;
import com.kashdeya.tinyprogressions.LegacyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

public final class VasholineBlock extends LiquidBlock {
    public VasholineBlock(Properties properties) { super(ModFluids.SOURCE, properties); }
    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && living.getPersistentData().getLong("tp:wub_effect_tick") != level.getGameTime()+1) {
            living.getPersistentData().putLong("tp:wub_effect_tick",level.getGameTime()+1);
            if (living instanceof Player player) {
                if(level.getGameTime()%20==0 && player.getHealth()<player.getMaxHealth()) player.heal(LegacyConfig.decimal("ConfigHandler.vasholine_heal_amount"));
                if(LegacyConfig.enabled("ConfigHandler.wub_weakness")) player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,20,0,false,false));
                if(LegacyConfig.enabled("ConfigHandler.wub_blindness")) player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,20,0,false,false));
                if(LegacyConfig.enabled("ConfigHandler.wub_fatigue")) player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,20,0,false,false));
            } else {
                if(LegacyConfig.enabled("ConfigHandler.vasholine_mobs")) living.hurt(level.damageSources().magic(),LegacyConfig.decimal("ConfigHandler.vasholine_mobs_amount"));
                if(LegacyConfig.enabled("ConfigHandler.wub_heal_mobs")) living.heal(LegacyConfig.decimal("ConfigHandler.mob_heal_amount"));
            }
        }
        if(level.isClientSide && entity instanceof Player player && player.isLocalPlayer()) {
            player.setYRot(player.getYRot()-(level.random.nextFloat()-level.random.nextFloat())*.5F);
            player.yRotO+=(level.random.nextFloat()+level.random.nextFloat())*.5F;
        }
        super.entityInside(state, level, pos, entity);
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        if(level.isEmptyBlock(pos.above()) && level.getGameTime()%5==0) {
            double offset=random.nextDouble()*.6-.3;
            // Modern fluid surfaces are higher than the legacy fixed .8 particle origin.
            // Keep the stationary sparks visible just above the rendered liquid.
            double surface=pos.getY()+state.getFluidState().getHeight(level,pos)+.15;
            for(double[] xy:new double[][]{{-.25,offset},{.25,offset},{offset,-.25},{offset,.25}})
                level.addParticle(com.kashdeya.tinyprogressions.ModParticles.SPARKLES.get(),pos.getX()+.5+xy[0],surface,pos.getZ()+.5+xy[1],0,0,0);
        }
    }
}
