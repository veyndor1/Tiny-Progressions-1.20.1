package com.kashdeya.tinyprogressions.gametest;

import com.google.gson.JsonParser;
import com.kashdeya.tinyprogressions.Content;
import com.kashdeya.tinyprogressions.FeatureCondition;
import com.kashdeya.tinyprogressions.ModFluids;
import com.kashdeya.tinyprogressions.item.PouchItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("tp")
@PrefixGameTestTemplate(false)
public final class ContentTests {
    @GameTest(template="empty") public static void infiniteBucketSuppliesWaterThroughForgeCapability(GameTestHelper helper) {
        var stack=Content.item("infin_bucket").getDefaultInstance();
        var fluid=stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(() -> new AssertionError("Missing infinite water capability"));
        for(int i=0;i<5;i++) helper.assertTrue(fluid.drain(1000,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).getAmount()==1000,"Each actual machine drain must receive another bucket of water");
        helper.assertTrue(fluid.drain(125,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE).getAmount()==125,"Partial simulated drain must preserve its requested amount");
        helper.assertTrue(fluid.getFluidInTank(0).getAmount()==1000 && fluid.getContainer()==stack,"Draining must preserve both the infinite source and item identity");
        helper.assertTrue(fluid.drain(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,1000),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).isEmpty(),"Water source must not supply lava");
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void everyItemBuildsItsTooltipWithoutCrashing(GameTestHelper helper) {
        for(var entry:Content.ITEMS.getEntries()) {
            var stack=entry.get().getDefaultInstance();
            var tooltip=new java.util.ArrayList<net.minecraft.network.chat.Component>();
            entry.get().appendHoverText(stack,helper.getLevel(),tooltip,net.minecraft.world.item.TooltipFlag.NORMAL);
            Content.appendLegacyTooltip(stack,tooltip);
        }
        var growthTooltip=new java.util.ArrayList<net.minecraft.network.chat.Component>();
        Content.appendLegacyTooltip(Content.item("growth_block").getDefaultInstance(),growthTooltip);
        helper.assertTrue(growthTooltip.size()==3,"Growth crystal must retain its three explanation lines");
        var appleTooltip=new java.util.ArrayList<net.minecraft.network.chat.Component>();
        Content.appendLegacyTooltip(Content.item("diamond_apple").getDefaultInstance(),appleTooltip);
        helper.assertTrue(appleTooltip.size()==4,"Diamond apple must retain its four effect explanation lines");
        helper.assertTrue(FeatureCondition.evaluate("true && (false || true) && !false"),"Feature conditions must retain nested boolean precedence");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void everyEnabledRecipeLoadsWithUsableIngredients(GameTestHelper helper) throws Exception {
        var resource = helper.getLevel().getServer().getResourceManager()
                .getResource(ResourceLocation.tryBuild("tp", "port_audit/recipes.json")).orElseThrow();
        try (var reader = resource.openAsReader()) {
            for (var element : JsonParser.parseReader(reader).getAsJsonArray()) {
                var definition = element.getAsJsonObject();
                boolean enabled = true;
                for (var condition : definition.getAsJsonArray("conditions")) {
                    enabled &= FeatureCondition.evaluate(condition.getAsString());
                }
                if (definition.has("tags")) for (var tag : definition.getAsJsonArray("tags")) {
                    var key = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.tryParse(tag.getAsString()));
                    enabled &= net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(key).map(items -> items.size() > 0).orElse(false);
                }
                var id = ResourceLocation.tryBuild(definition.has("namespace") ? definition.get("namespace").getAsString() : "tp",definition.get("name").getAsString());
                var recipe = helper.getLevel().getRecipeManager().byKey(id);
                helper.assertTrue(recipe.isPresent() == enabled,"Incorrect recipe load state for " + id);
                if (!enabled) continue;
                for (Ingredient ingredient : recipe.orElseThrow().getIngredients()) {
                    if (ingredient != Ingredient.EMPTY) helper.assertTrue(ingredient.getItems().length > 0,
                            "Unusable or empty ingredient tag in " + id);
                }
            }
        }
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void pouchPersistsAndRejectsNestedPouches(GameTestHelper helper) {
        ItemStack pouch = Content.item("pouch").getDefaultInstance();
        var inventory = pouch.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                () -> new AssertionError("Missing pouch inventory"));
        helper.assertTrue(inventory.getSlots() == 54,"Pouch must retain its original 54 slots");
        helper.assertTrue(inventory.insertItem(0,new ItemStack(Items.DIAMOND,12),false).isEmpty(),"Pouch must accept items");
        helper.assertTrue(!inventory.insertItem(1,Content.item("pouch").getDefaultInstance(),false).isEmpty(),
                "Pouch must reject nested pouches");
        ItemStack restored = ItemStack.of(pouch.save(new CompoundTag()));
        var restoredInventory = restored.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                () -> new AssertionError("Missing restored inventory"));
        helper.assertTrue(restoredInventory.getStackInSlot(0).is(Items.DIAMOND)
                && restoredInventory.getStackInSlot(0).getCount() == 12,"Pouch contents must survive item serialization");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void craftingToolsRemainInGrid(GameTestHelper helper) {
        for (String name : new String[]{"stone_hammer","juicer","flint_knife","quartz_knife","infin_bucket"}) {
            ItemStack stack = Content.item(name).getDefaultInstance();
            helper.assertTrue(stack.hasCraftingRemainingItem(),name + " must have a crafting remainder");
            helper.assertTrue(stack.getCraftingRemainingItem().is(stack.getItem()),name + " must return itself");
        }
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void vasholineUsesItsCustomSourceAndBucket(GameTestHelper helper) {
        helper.assertTrue(ModFluids.SOURCE.get().getBucket() == ModFluids.BUCKET.get(),"Fluid must use custom bucket");
        helper.assertTrue(ModFluids.FLOWING.get().getFluidType() == ModFluids.TYPE.get(),"Flowing and source fluid type must match");
        helper.succeed();
    }
}
