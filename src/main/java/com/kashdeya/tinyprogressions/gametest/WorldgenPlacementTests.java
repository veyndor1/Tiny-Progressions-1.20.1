package com.kashdeya.tinyprogressions.gametest;

import com.kashdeya.tinyprogressions.*;
import com.kashdeya.tinyprogressions.worldgen.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.gametest.*;

@GameTestHolder("tp")
@PrefixGameTestTemplate(false)
public final class WorldgenPlacementTests {
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void set(String key,Object value) { ((ForgeConfigSpec.ConfigValue)LegacyConfig.SPEC.getValues().get(java.util.Arrays.asList(key.split("\\.")))).set(value); }
    @GameTest(template="empty",timeoutTicks=200) public static void netherVeinsReplaceNetherrackAndOverworldVeinsGenerateBelowZero(GameTestHelper helper) {
        String[] keys={"ConfigHandler.vasholine","ConfigHandler.nether_wub","ConfigHandler.nether_wub_block_min","ConfigHandler.nether_wub_block_max","ConfigHandler.nether_wub_block_frequency","ConfigHandler.nether_wub_block_count",
                "ConfigHandler.ender_ore","ConfigHandler.ender_ore_min","ConfigHandler.ender_ore_max","ConfigHandler.ender_ore_frequency","ConfigHandler.ender_ore_size"};
        Object[] saved=java.util.Arrays.stream(keys).map(LegacyConfig::value).toArray();
        ServerLevel nether=helper.getLevel().getServer().getLevel(Level.NETHER);
        var origin=helper.absolutePos(new BlockPos(160,0,160));
        var min=new BlockPos(origin.getX()-8,64,origin.getZ()-8);
        var max=new BlockPos(origin.getX()+32,80,origin.getZ()+32);
        var belowMin=new BlockPos(origin.getX()-8,-32,origin.getZ()-8);
        var belowMax=new BlockPos(origin.getX()+32,-16,origin.getZ()+32);
        try {
            set(keys[0],true); set(keys[1],true); set(keys[2],64); set(keys[3],80); set(keys[4],64); set(keys[5],16);
            for(var pos:BlockPos.betweenClosed(min,max)) nether.setBlock(pos,Blocks.NETHERRACK.defaultBlockState(),2);
            var feature=ModWorldgen.ORES.get();
            var netherContext=new FeaturePlaceContext<>(java.util.Optional.empty(),nether,nether.getChunkSource().getGenerator(),RandomSource.create(331),origin,new ModWorldgen.OreGroup("nether_wub"));
            helper.assertTrue(feature.place(netherContext),"Nether Wub ore must actually generate in Netherrack");
            helper.assertTrue(java.util.stream.StreamSupport.stream(BlockPos.betweenClosed(min,max).spliterator(),false).anyMatch(pos -> nether.getBlockState(pos).is(Content.block("nether_wub_ore"))),"Generated Nether vein must contain the correct ore");
            set(keys[6],true); set(keys[7],-32); set(keys[8],-16); set(keys[9],64); set(keys[10],16);
            for(var pos:BlockPos.betweenClosed(belowMin,belowMax)) helper.getLevel().setBlock(pos,Blocks.DEEPSLATE.defaultBlockState(),2);
            var belowContext=new FeaturePlaceContext<>(java.util.Optional.empty(),helper.getLevel(),helper.getLevel().getChunkSource().getGenerator(),RandomSource.create(442),origin,new ModWorldgen.OreGroup("ender"));
            helper.assertTrue(feature.place(belowContext),"Modern negative height settings must generate ores below Y=0");
        } finally {
            for(int i=0;i<keys.length;i++) set(keys[i],saved[i]);
            for(var pos:BlockPos.betweenClosed(min,max)) nether.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            for(var pos:BlockPos.betweenClosed(belowMin,belowMax)) helper.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        }
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200) public static void berryGenerationUsesSupportedSurfaceAndHonorsDisableFlag(GameTestHelper helper) {
        String[] keys={"ConfigHandler.old_reed","ConfigHandler.ExtraPlantGen","ConfigHandler.ExtraJuices","ConfigHandler.berryPlantRarity"};
        Object[] saved=java.util.Arrays.stream(keys).map(LegacyConfig::value).toArray();
        var level=helper.getLevel();
        var origin=helper.absolutePos(new BlockPos(240,0,240));
        var min=new BlockPos(origin.getX(),94,origin.getZ());
        var max=new BlockPos(origin.getX()+24,94,origin.getZ()+24);
        try {
            set(keys[0],false); set(keys[1],true); set(keys[2],true); set(keys[3],10);
            for(var pos:BlockPos.betweenClosed(min,max)) level.setBlock(pos,Blocks.GRASS_BLOCK.defaultBlockState(),2);
            var context=new FeaturePlaceContext<>(java.util.Optional.empty(),level,level.getChunkSource().getGenerator(),RandomSource.create(551),origin,NoneFeatureConfiguration.INSTANCE);
            helper.assertTrue(ModWorldgen.PLANTS.get().place(context),"Berry generation must place a plant on a supported surface");
            set(keys[1],false);
            helper.assertTrue(!ModWorldgen.PLANTS.get().place(context),"Disabled plant generation must not place a berry");
        } finally {
            for(int i=0;i<keys.length;i++) set(keys[i],saved[i]);
            for(var pos:BlockPos.betweenClosed(min,max.above())) level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        }
        helper.succeed();
    }
}
