package com.kashdeya.tinyprogressions.client;

import com.kashdeya.tinyprogressions.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="tp",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientParticles {
    @SubscribeEvent public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SPARKLES.get(),sprites -> (options,level,x,y,z,dx,dy,dz) -> new Sparkle(level,x,y,z,sprites));
    }
    private static final class Sparkle extends SimpleAnimatedParticle {
        Sparkle(ClientLevel level,double x,double y,double z,SpriteSet sprites) {
            // Matches the old firework Spark: zero velocity, .91 friction and -.004 gravity.
            super(level,x,y,z,sprites,.1F); xd=yd=zd=0;
            setColor(.9F,.9F,0); alpha=.5F;
            lifetime=48+random.nextInt(12); quadSize*=.75F; setSpriteFromAge(sprites);
        }
        @Override public int getLightColor(float partialTick) { return 15728880; }
    }
}
