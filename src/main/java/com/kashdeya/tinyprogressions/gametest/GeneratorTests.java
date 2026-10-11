package com.kashdeya.tinyprogressions.gametest;

import com.kashdeya.tinyprogressions.Content;
import com.kashdeya.tinyprogressions.block.CobbleGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("tp")
@PrefixGameTestTemplate(false)
public final class GeneratorTests {
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void basicGeneratorKeepsOriginalRate(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, Content.block("cobblegen_block"));
        helper.runAtTickTime(39, () -> helper.assertTrue(generator(helper,pos).contents().isEmpty(),
                "Basic tier produced before its 40 tick cycle"));
        helper.runAtTickTime(41, () -> {
            helper.assertTrue(generator(helper,pos).contents().getCount() == 1,
                    "Basic tier must produce one cobblestone per 40 ticks");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void exportsIntoChestAbove(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos.above(), Blocks.CHEST);
        helper.setBlock(pos, Content.block("emerald_cobblegen_block"));
        helper.runAtTickTime(5, () -> {
            ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(pos.above());
            helper.assertTrue(chest.getItem(0).is(Items.COBBLESTONE) && chest.getItem(0).getCount() >= 4,
                    "Emerald tier must export cobblestone to the chest above");
            helper.assertTrue(generator(helper,pos).contents().isEmpty(), "Accepted output must leave the buffer");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void fullDestinationRetainsOutputAndRejectsInput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos.above(), Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(pos.above());
        for (int slot = 0; slot < chest.getContainerSize(); slot++) chest.setItem(slot, new ItemStack(Items.DIRT,64));
        helper.setBlock(pos, Content.block("emerald_cobblegen_block"));
        helper.runAtTickTime(70, () -> {
            CobbleGeneratorBlockEntity generator = generator(helper,pos);
            helper.assertTrue(generator.contents().getCount() == 64, "Blocked output must fill to capacity without loss");
            var inventory = generator.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                    () -> new AssertionError("Missing inventory capability"));
            helper.assertTrue(inventory.insertItem(0,new ItemStack(Items.DIRT),false).getCount() == 1,
                    "Generator must reject external insertion");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void inventorySurvivesSerialization(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, Content.block("emerald_cobblegen_block"));
        helper.runAtTickTime(8, () -> {
            CobbleGeneratorBlockEntity original = generator(helper,pos);
            int count = original.contents().getCount();
            CompoundTag saved = original.saveWithoutMetadata();
            CobbleGeneratorBlockEntity restored = new CobbleGeneratorBlockEntity(helper.absolutePos(pos), original.getBlockState());
            restored.load(saved);
            helper.assertTrue(count > 0 && restored.contents().getCount() == count,
                    "Generated cobblestone must survive a save/load round trip");
            helper.succeed();
        });
    }

    private static CobbleGeneratorBlockEntity generator(GameTestHelper helper, BlockPos pos) {
        return (CobbleGeneratorBlockEntity) helper.getBlockEntity(pos);
    }
}
