package com.kashdeya.tinyprogressions.block;

import com.kashdeya.tinyprogressions.LegacyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class EnderOreBlock extends DropExperienceBlock {
    public EnderOreBlock(Properties properties) { super(properties,UniformInt.of(1,5)); }
    @Override public void playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player) {
        if(!level.isClientSide && LegacyConfig.enabled("ConfigHandler.ender_mite") && level.random.nextInt(100)<=LegacyConfig.integer("ConfigHandler.endermite_spawn")) {
            var mite=EntityType.ENDERMITE.create(level);
            if(mite!=null) { mite.moveTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,level.random.nextFloat()*360,0); level.addFreshEntity(mite); mite.playAmbientSound(); }
        }
        super.playerWillDestroy(level,pos,state,player);
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        for(int i=0;i<4;i++) level.addParticle(ParticleTypes.PORTAL,pos.getX()+random.nextDouble(),pos.getY()+random.nextDouble(),pos.getZ()+random.nextDouble(),(random.nextDouble()-.5)*.5,(random.nextDouble()-.5)*.5,(random.nextDouble()-.5)*.5);
    }
}
