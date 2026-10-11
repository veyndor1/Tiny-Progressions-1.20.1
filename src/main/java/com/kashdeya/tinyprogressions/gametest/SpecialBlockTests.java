package com.kashdeya.tinyprogressions.gametest;

import com.kashdeya.tinyprogressions.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("tp")
@PrefixGameTestTemplate(false)
public final class SpecialBlockTests {
    @GameTest(template="empty") public static void doubleSlabsDropTwoMatchingHalfSlabs(GameTestHelper helper) {
        var pos=helper.absolutePos(new BlockPos(2,2,2));
        for(String family:new String[]{"hardened_stone","hardened_stone_bricks","hardened_stone_smallbricks"}) {
            for(String suffix:new String[]{"_half","_double"}) {
                var state=Content.block(family+"_slab"+suffix).defaultBlockState().setValue(SlabBlock.TYPE,net.minecraft.world.level.block.state.properties.SlabType.DOUBLE);
                var loot=Block.getDrops(state,helper.getLevel(),pos,null,null,new ItemStack(Items.DIAMOND_PICKAXE));
                helper.assertTrue(loot.size()==1 && loot.get(0).is(Content.item(family+"_slab_half")) && loot.get(0).getCount()==2,"Double slab must return two halves: "+family+suffix);
            }
        }
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=140) public static void netherStarBlocksPowerAnActualBeacon(GameTestHelper helper) {
        var pos=new BlockPos(3,3,3);
        for(var base:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,-1,1))) helper.setBlock(base,Content.block("netherstar_block"));
        helper.setBlock(pos,Blocks.BEACON);
        var absolute=helper.absolutePos(pos);
        for(int y=absolute.getY()+1;y<helper.getLevel().getMaxBuildHeight();y++) helper.getLevel().setBlockAndUpdate(new BlockPos(absolute.getX(),y,absolute.getZ()),Blocks.AIR.defaultBlockState());
        helper.assertTrue(Content.block("netherstar_block").defaultBlockState().is(net.minecraft.tags.BlockTags.BEACON_BASE_BLOCKS),"Nether star block must be present in the loaded beacon base tag");
        helper.runAfterDelay(100,() -> {
            var beacon=helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
            var menu=beacon instanceof net.minecraft.world.level.block.entity.BeaconBlockEntity active ? active.createMenu(1,player.getInventory(),player) : null;
            helper.assertTrue(menu instanceof net.minecraft.world.inventory.BeaconMenu active && active.getLevels()==1,"Original nether star blocks must activate a one-level beacon pyramid; pos="+absolute+", base="+helper.getBlockState(pos.below())+", tile="+beacon);
            helper.succeed();
        });
    }
    @GameTest(template="empty") public static void oreLootRespectsSilkTouchAndFortuneLimit(GameTestHelper helper) {
        var tool=new ItemStack(Items.DIAMOND_PICKAXE);
        tool.enchant(Enchantments.SILK_TOUCH,1);
        var pos=helper.absolutePos(new BlockPos(2,2,2));
        for(String name:new String[]{"ender_ore","wub_ore","nether_wub_ore"}) {
            var state=Content.block(name).defaultBlockState();
            var silk=Block.getDrops(state,helper.getLevel(),pos,null,null,tool);
            helper.assertTrue(silk.size()==1 && silk.get(0).is(Content.item(name)),"Silk Touch must return the ore itself: "+name);
            var fortune=new ItemStack(Items.DIAMOND_PICKAXE); fortune.enchant(Enchantments.BLOCK_FORTUNE,10);
            for(int repeat=0;repeat<20;repeat++) {
                var drops=Block.getDrops(state,helper.getLevel(),pos,null,null,fortune);
                helper.assertTrue(drops.size()==1 && drops.get(0).getCount()>=1 && drops.get(0).getCount()<=6,"Legacy Fortune ore limit must stay between one and six");
                helper.assertTrue(drops.get(0).is(Content.item(name.equals("ender_ore") ? "ender_dust" : "wub_gem")),"Ordinary ore must give its mineral");
            }
        }
        helper.succeed();
    }
    @GameTest(template="empty") public static void infusedStoneConvertsWaterWithoutDestroyingWaterloggedBlocks(GameTestHelper helper) {
        BlockPos pos=new BlockPos(3,2,3);
        helper.setBlock(pos.east(),Blocks.WATER);
        helper.setBlock(pos,Content.block("lava_infused_stone"));
        helper.assertBlockPresent(Blocks.OBSIDIAN,pos.east());
        var slab=Blocks.OAK_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,true);
        helper.setBlock(pos.west(),slab);
        helper.assertTrue(helper.getBlockState(pos.west()).is(Blocks.OAK_SLAB),"A waterlogged block must retain its own block and contents");
        helper.succeed();
    }
    @GameTest(template="empty") public static void oldPlantsKeepTheirDropAndSupportRules(GameTestHelper helper) {
        BlockPos pos=new BlockPos(2,2,2);
        helper.setBlock(pos.below(),Blocks.SAND);
        var reed=Content.block("old_reed").defaultBlockState();
        helper.assertTrue(reed.canSurvive(helper.getLevel(),helper.absolutePos(pos)),"Old reed must grow on sand");
        var drops=Block.getDrops(reed,helper.getLevel(),helper.absolutePos(pos),null);
        helper.assertTrue(drops.size()==1 && drops.get(0).is(Content.item("dead_reed")) && drops.get(0).getCount()==2,"Old reed must drop two dead reeds");
        helper.setBlock(pos.below(),Blocks.DIRT);
        helper.assertTrue(!reed.canSurvive(helper.getLevel(),helper.absolutePos(pos)),"Old reed must reject dirt");
        var player=helper.makeMockPlayer();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        for(String name:new String[]{"blueberry","blackberry","maloberry","raspberry"}) {
            var bush=Content.block(name+"_bush").defaultBlockState();
            helper.setBlock(pos,bush);
            helper.assertTrue(bush.canSurvive(helper.getLevel(),helper.absolutePos(pos)),"Berry bush must grow on dirt: "+name);
            helper.assertTrue(bush.getDestroyProgress(player,helper.getLevel(),helper.absolutePos(pos))>=1.0F,"Berry bush must break in one hit with an empty hand: "+name);
            var sound=bush.getSoundType(helper.getLevel(),helper.absolutePos(pos),player);
            helper.assertTrue(sound.getBreakSound()==SoundType.GRASS.getBreakSound(),"Berry bush must use the grass breaking sound: "+name);
            var berries=Block.getDrops(bush,helper.getLevel(),helper.absolutePos(pos),null);
            helper.assertTrue(berries.size()==1 && berries.get(0).is(Content.item(name+"_berry")) && berries.get(0).getCount()>=1 && berries.get(0).getCount()<=5,"Bush must drop berries, with the original count range: "+name);
        }
        helper.succeed();
    }
}
