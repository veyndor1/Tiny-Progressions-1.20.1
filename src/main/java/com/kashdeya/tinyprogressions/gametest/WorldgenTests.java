package com.kashdeya.tinyprogressions.gametest;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("tp")
@PrefixGameTestTemplate(false)
public final class WorldgenTests {
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void set(String name, Object value) { ((net.minecraftforge.common.ForgeConfigSpec.ConfigValue)com.kashdeya.tinyprogressions.LegacyConfig.SPEC.getValues().get(java.util.Arrays.asList(name.split("\\.")))).set(value); }
    @GameTest(template="empty",timeoutTicks=200) public static void orePlacementRespectsConfigurationAndDimension(GameTestHelper helper) {
        String[] names={"ConfigHandler.ender_ore", "ConfigHandler.ender_ore_min", "ConfigHandler.ender_ore_max", "ConfigHandler.ender_ore_frequency", "ConfigHandler.ender_ore_size"};
        Object[] saved=java.util.Arrays.stream(names).map(com.kashdeya.tinyprogressions.LegacyConfig::value).toArray();
        var level=helper.getLevel();
        var origin=helper.absolutePos(new net.minecraft.core.BlockPos(80,0,80));
        var min=new net.minecraft.core.BlockPos(origin.getX()-8,64,origin.getZ()-8);
        var max=new net.minecraft.core.BlockPos(origin.getX()+32,80,origin.getZ()+32);
        try {
            set(names[0],true); set(names[1],64); set(names[2],80); set(names[3],64); set(names[4],16);
            for(var pos:net.minecraft.core.BlockPos.betweenClosed(min,max)) level.setBlock(pos,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
            var feature=com.kashdeya.tinyprogressions.worldgen.ModWorldgen.ORES.get();
            var context=new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(java.util.Optional.empty(),level,level.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(1220201),origin,new com.kashdeya.tinyprogressions.worldgen.ModWorldgen.OreGroup("ender"));
            helper.assertTrue(feature.place(context),"Configured ore attempts must actually place ender ore");
            int count=0;
            for(var pos:net.minecraft.core.BlockPos.betweenClosed(min,max)) if(level.getBlockState(pos).is(com.kashdeya.tinyprogressions.Content.block("ender_ore"))) count++;
            helper.assertTrue(count>0,"Real modern ore generation must replace stone");
            set(names[0],false);
            helper.assertTrue(!feature.place(context),"Disabled ore must stop generating");
            set(names[0],true); set(names[2],64);
            helper.assertTrue(!feature.place(context),"Empty height range must return safely without nextInt(0)");
            var netherContext=new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(java.util.Optional.empty(),level,level.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(1),origin,new com.kashdeya.tinyprogressions.worldgen.ModWorldgen.OreGroup("nether_wub"));
            helper.assertTrue(!feature.place(netherContext),"Nether ore must not generate in an overworld level");
        } finally {
            for(int i=0;i<names.length;i++) set(names[i],saved[i]);
            for(var pos:net.minecraft.core.BlockPos.betweenClosed(min,max)) level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        }
        helper.succeed();
    }
    @GameTest(template="empty") public static void dynamicFeaturesAndBiomeModifiersLoad(GameTestHelper helper) {
        var registry=helper.getLevel().registryAccess();
        var configured=registry.registryOrThrow(Registries.CONFIGURED_FEATURE);
        var placed=registry.registryOrThrow(Registries.PLACED_FEATURE);
        for(String name:new String[]{"ore_wub","ore_ender","ore_lava","ore_water","ore_charcoal","ore_nether_wub","ore_nether_lava","legacy_plants"}) {
            var id=ResourceLocation.tryBuild("tp",name);
            helper.assertTrue(configured.containsKey(id) && placed.containsKey(id),"Modern configured and placed feature must exist: "+id);
        }
        var biomes=registry.registryOrThrow(Registries.BIOME);
        var plains=biomes.get(ResourceLocation.tryBuild("minecraft","plains"));
        var nether=biomes.get(ResourceLocation.tryBuild("minecraft","nether_wastes"));
        var overworldFeature=placed.get(ResourceLocation.tryBuild("tp","ore_ender"));
        var netherFeature=placed.get(ResourceLocation.tryBuild("tp","ore_nether_wub"));
        helper.assertTrue(plains.getGenerationSettings().features().stream().flatMap(set -> set.stream()).anyMatch(holder -> holder.value()==overworldFeature),"Overworld biome modifier must attach ender ore");
        helper.assertTrue(nether.getGenerationSettings().features().stream().flatMap(set -> set.stream()).anyMatch(holder -> holder.value()==netherFeature),"Nether biome modifier must attach Nether Wub ore");
        helper.assertTrue(plains.getGenerationSettings().features().stream().flatMap(set -> set.stream()).noneMatch(holder -> holder.value()==netherFeature),"Nether ore must not leak into overworld biomes");
        helper.succeed();
    }
}
