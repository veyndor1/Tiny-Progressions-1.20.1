package com.kashdeya.tinyprogressions.gametest;

import com.kashdeya.tinyprogressions.Content;
import com.kashdeya.tinyprogressions.block.IronFurnaceBlock;
import com.kashdeya.tinyprogressions.block.IronFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("tp")
@PrefixGameTestTemplate(false)
public final class FurnaceTests {
    @GameTest(template="empty",timeoutTicks=150)
    public static void dryingSpongeUsesOneBucketAndPreservesRemainingBuckets(GameTestHelper helper) {
        var furnace=create(helper);
        furnace.inventory().setStackInSlot(0,new ItemStack(Items.WET_SPONGE));
        furnace.inventory().setStackInSlot(1,new ItemStack(Items.COAL));
        helper.runAtTickTime(2,() -> {
            var side=furnace.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.EAST).orElseThrow(() -> new AssertionError("Missing side inventory"));
            var remainder=side.insertItem(0,new ItemStack(Items.BUCKET,2),false);
            helper.assertTrue(remainder.getCount()==1 && furnace.inventory().getStackInSlot(1).getCount()==1,"Fuel slot must accept one bucket and return additional buckets");
        });
        helper.runAtTickTime(101,() -> {
            helper.assertTrue(furnace.inventory().getStackInSlot(1).is(Items.WATER_BUCKET),"Sponge water must fill the waiting bucket");
            helper.assertTrue(furnace.inventory().getStackInSlot(2).is(Items.SPONGE),"Dry sponge must still appear in the output");
            helper.succeed();
        });
    }
    private static final BlockPos POS=new BlockPos(2,2,2);
    private static IronFurnaceBlockEntity create(GameTestHelper helper) {
        helper.setBlock(POS,Content.block("iron_furnace_block"));
        return (IronFurnaceBlockEntity)helper.getBlockEntity(POS);
    }
    @GameTest(template="empty",timeoutTicks=150)
    public static void smeltsAtOriginalSpeedAndKeepsFuelContainer(GameTestHelper helper) {
        var furnace=create(helper);
        furnace.inventory().setStackInSlot(0,new ItemStack(Items.RAW_IRON));
        furnace.inventory().setStackInSlot(1,new ItemStack(Items.LAVA_BUCKET));
        helper.runAtTickTime(2,() -> {
            helper.assertTrue(helper.getBlockState(POS).getValue(IronFurnaceBlock.LIT),"Lit state must reflect active fuel");
            helper.assertTrue(furnace.inventory().getStackInSlot(1).is(Items.BUCKET),"Lava fuel must leave its bucket");
        });
        helper.runAtTickTime(99,() -> helper.assertTrue(furnace.inventory().getStackInSlot(2).isEmpty(),"Smelting must wait 100 ticks"));
        helper.runAtTickTime(101,() -> {
            helper.assertTrue(furnace.inventory().getStackInSlot(0).isEmpty(),"Input must be consumed exactly once");
            helper.assertTrue(furnace.inventory().getStackInSlot(2).is(Items.IRON_INGOT) && furnace.inventory().getStackInSlot(2).getCount()==1,"One input must produce one smelted result");
            var saved=furnace.saveWithoutMetadata();
            var restored=new IronFurnaceBlockEntity(furnace.getBlockPos(),furnace.getBlockState());
            restored.load(saved);
            helper.assertTrue(restored.data.get(0)==furnace.data.get(0) && restored.inventory().getStackInSlot(2).is(Items.IRON_INGOT),"Inventory and fuel must survive serialization");
            helper.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=150)
    public static void blockedOutputConsumesNeitherFuelNorInput(GameTestHelper helper) {
        var furnace=create(helper);
        furnace.inventory().setStackInSlot(0,new ItemStack(Items.RAW_IRON));
        furnace.inventory().setStackInSlot(1,new ItemStack(Items.COAL));
        for(int i=2;i<6;i++) furnace.inventory().setStackInSlot(i,new ItemStack(Items.DIRT,64));
        helper.runAtTickTime(120,() -> {
            helper.assertTrue(furnace.inventory().getStackInSlot(0).getCount()==1,"Blocked furnace must preserve input");
            helper.assertTrue(furnace.inventory().getStackInSlot(1).getCount()==1,"Blocked furnace must preserve unused fuel");
            helper.assertTrue(!helper.getBlockState(POS).getValue(IronFurnaceBlock.LIT),"Blocked furnace must not ignite");
            helper.succeed();
        });
    }
    @GameTest(template="empty")
    public static void sidedAutomationUsesOriginalSlots(GameTestHelper helper) {
        var furnace=create(helper);
        var top=furnace.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).orElseThrow(() -> new AssertionError("Missing top handler"));
        var side=furnace.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.EAST).orElseThrow(() -> new AssertionError("Missing side handler"));
        var bottom=furnace.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).orElseThrow(() -> new AssertionError("Missing bottom handler"));
        helper.assertTrue(top.insertItem(0,new ItemStack(Items.RAW_IRON),false).isEmpty(),"Top must accept smelting input");
        helper.assertTrue(top.extractItem(0,1,false).isEmpty(),"Top must not remove input");
        helper.assertTrue(!side.insertItem(0,new ItemStack(Items.DIRT),false).isEmpty(),"Sides must reject nonfuel");
        helper.assertTrue(side.insertItem(0,new ItemStack(Items.COAL),false).isEmpty(),"Sides must accept fuel");
        helper.assertTrue(bottom.getSlots()==4 && !bottom.insertItem(0,new ItemStack(Items.DIRT),false).isEmpty(),"Bottom must expose four extraction-only results");
        furnace.inventory().setStackInSlot(5,new ItemStack(Items.IRON_INGOT,3));
        helper.assertTrue(bottom.extractItem(3,2,false).getCount()==2 && furnace.inventory().getStackInSlot(5).getCount()==1,"Bottom extraction must affect only the selected result slot");
        helper.succeed();
    }
}
