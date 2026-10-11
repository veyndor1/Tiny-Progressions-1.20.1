package com.kashdeya.tinyprogressions.gametest;

import com.kashdeya.tinyprogressions.Content;
import com.kashdeya.tinyprogressions.block.GrowthCrystalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("tp")
@PrefixGameTestTemplate(false)
public final class GrowthTests {
    @GameTest(template="empty",timeoutTicks=100) public static void growthCrystalGrowsNetherWartAndFruitFromMatureStems(GameTestHelper helper) {
        var center=new BlockPos(4,2,4);
        var wart=new BlockPos(2,2,2);
        var stem=new BlockPos(5,2,5);
        helper.setBlock(center,Content.block("growth_block"));
        helper.setBlock(wart.below(),Blocks.SOUL_SAND); helper.setBlock(wart,Blocks.NETHER_WART);
        for(var pos:BlockPos.betweenClosed(stem.offset(-1,-1,-1),stem.offset(1,-1,1))) helper.setBlock(pos,Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,7));
        helper.setBlock(stem,Blocks.MELON_STEM.defaultBlockState().setValue(StemBlock.AGE,7));
        helper.setBlock(stem.above(2),Blocks.GLOWSTONE);
        helper.runAtTickTime(5,() -> {
            var crystal=(GrowthCrystalBlock)Content.block("growth_block");
            var random=RandomSource.create(1220201);
            for(int i=0;i<100;i++) crystal.tick(helper.getBlockState(center),helper.getLevel(),helper.absolutePos(center),random);
            helper.assertTrue(helper.getBlockState(wart).getValue(NetherWartBlock.AGE)==3,"Growth crystal must support Nether wart even though it cannot be bonemealed");
            boolean melon=false;
            for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL) melon |= helper.getBlockState(stem.relative(direction)).is(Blocks.MELON);
            helper.assertTrue(melon,"Mature stems must receive growth ticks and produce fruit");
            helper.succeed();
        });
    }
}
