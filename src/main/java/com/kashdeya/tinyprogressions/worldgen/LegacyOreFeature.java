package com.kashdeya.tinyprogressions.worldgen;

import com.kashdeya.tinyprogressions.Content;
import com.kashdeya.tinyprogressions.LegacyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.level.block.Blocks;
import java.util.List;

/** Configurable legacy attempts and random vein sizes, using modern vanilla ore placement. */
public final class LegacyOreFeature extends Feature<ModWorldgen.OreGroup> {
    public LegacyOreFeature() { super(ModWorldgen.OreGroup.CODEC); }
    private record Settings(String block,String prefix,String size,String[] flags,boolean nether,boolean magma) {}
    private Settings settings(String name) {
        return switch(name) {
            case "wub" -> new Settings("wub_ore","wub_block","count",new String[]{"vasholine","overworld_wub"},false,false);
            case "ender" -> new Settings("ender_ore","ender_ore","size",new String[]{"ender_ore"},false,false);
            case "lava" -> new Settings("lava_block","lava_block","size",new String[]{"lava_block"},false,false);
            case "water" -> new Settings("water_block","water_block","size",new String[]{"water_block"},false,false);
            case "charcoal" -> new Settings("charcoal_block","charcoal","size",new String[]{"CharcoalWorldgen","CharcoalBlock"},true,true);
            case "nether_wub" -> new Settings("nether_wub_ore","nether_wub_block","count",new String[]{"vasholine","nether_wub"},true,false);
            case "nether_lava" -> new Settings("nether_lava_block","nether_lava_block","size",new String[]{"nether_lava_block"},true,false);
            default -> throw new IllegalArgumentException("Unknown legacy ore group "+name);
        };
    }
    private static int integer(String key) { return LegacyConfig.integer("ConfigHandler."+key); }
    @Override public boolean place(FeaturePlaceContext<ModWorldgen.OreGroup> context) {
        Settings settings=settings(context.config().group());
        var level=context.level();
        if (!level.getLevel().dimension().equals(settings.nether ? Level.NETHER : Level.OVERWORLD)) return false;
        for(String flag:settings.flags) if(!LegacyConfig.enabled("ConfigHandler."+flag)) return false;
        int min=Math.max(level.getMinBuildHeight(),integer(settings.prefix+"_min"));
        // The old active generator uses an exclusive upper bound. Equal bounds are handled safely.
        int max=Math.min(level.getMaxBuildHeight(),integer(settings.prefix+"_max"));
        if(max<=min) return false;
        int attempts=integer(settings.prefix+"_frequency");
        int sizeLimit=Math.max(1,integer(settings.prefix+"_"+settings.size));
        var state=Content.block(settings.block).defaultBlockState();
        List<OreConfiguration.TargetBlockState> targets;
        if(settings.nether) targets=List.of(OreConfiguration.target(new BlockMatchTest(settings.magma ? Blocks.MAGMA_BLOCK : Blocks.NETHERRACK),state));
        else targets=List.of(OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES),state),
                OreConfiguration.target(new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES),state));
        var random=context.random();
        int size=1+random.nextInt(sizeLimit);
        boolean generated=false;
        for(int attempt=0;attempt<attempts;attempt++) {
            BlockPos pos=new BlockPos(context.origin().getX()+random.nextInt(16),min+random.nextInt(max-min),context.origin().getZ()+random.nextInt(16));
            var config=new OreConfiguration(targets,size);
            generated |= Feature.ORE.place(config,level,context.chunkGenerator(),random,pos);
        }
        return generated;
    }
}
