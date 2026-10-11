package com.kashdeya.tinyprogressions.worldgen;

import com.kashdeya.tinyprogressions.Content;
import com.kashdeya.tinyprogressions.LegacyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class LegacyPlantFeature extends Feature<NoneFeatureConfiguration> {
    public LegacyPlantFeature() { super(NoneFeatureConfiguration.CODEC); }
    private static final String[] BERRIES={"blueberry_bush","raspberry_bush","maloberry_bush","blackberry_bush"};
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level=context.level();
        if(!level.getLevel().dimension().equals(Level.OVERWORLD)) return false;
        var random=context.random();
        int x=context.origin().getX()+random.nextInt(8),z=context.origin().getZ()+random.nextInt(8);
        BlockPos origin=new BlockPos(x,level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z),z);
        var biome=level.getBiome(origin);
        boolean generated=false;
        float temperature=biome.value().getBaseTemperature();
        if(LegacyConfig.enabled("ConfigHandler.old_reed") && (temperature<.2F || temperature>1F || biome.is(BiomeTags.IS_BADLANDS))) {
            for(int attempt=0;attempt<2;attempt++) {
                BlockPos target=surface(context,x+random.nextInt(16),z+random.nextInt(16));
                var reed=Content.block("old_reed").defaultBlockState();
                if(level.isEmptyBlock(target) && reed.canSurvive(level,target)) { level.setBlock(target,reed,2); generated=true; }
            }
        }
        if(LegacyConfig.enabled("ConfigHandler.ExtraPlantGen") && (LegacyConfig.enabled("ConfigHandler.ExtraJuices") || LegacyConfig.enabled("ConfigHandler.ExtraJuicesBottles"))
                && random.nextFloat()<Math.min(1,LegacyConfig.integer("ConfigHandler.berryPlantRarity")/10F)) {
            var berry=Content.block(BERRIES[random.nextInt(4)]).defaultBlockState();
            BlockPos target=surface(context,x+random.nextInt(16),z+random.nextInt(16));
            if(level.isEmptyBlock(target) && berry.canSurvive(level,target)) { level.setBlock(target,berry,2); generated=true; }
        }
        return generated;
    }
    private static BlockPos surface(FeaturePlaceContext<?> context,int x,int z) { return new BlockPos(x,context.level().getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z),z); }
}
