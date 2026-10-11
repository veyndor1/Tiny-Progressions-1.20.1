package com.kashdeya.tinyprogressions.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraftforge.registries.*;

public final class ModWorldgen {
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(ForgeRegistries.FEATURES,"tp");
    public static final RegistryObject<LegacyOreFeature> ORES=FEATURES.register("legacy_ore",LegacyOreFeature::new);
    public static final RegistryObject<LegacyPlantFeature> PLANTS=FEATURES.register("legacy_plants",LegacyPlantFeature::new);
    public record OreGroup(String group) implements FeatureConfiguration {
        public static final Codec<OreGroup> CODEC=Codec.STRING.fieldOf("group").xmap(OreGroup::new,OreGroup::group).codec();
    }
}
