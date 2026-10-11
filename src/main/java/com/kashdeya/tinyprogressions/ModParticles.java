package com.kashdeya.tinyprogressions;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.*;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES=DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES,"tp");
    public static final RegistryObject<SimpleParticleType> SPARKLES=PARTICLES.register("sparkles",() -> new SimpleParticleType(false));
}
