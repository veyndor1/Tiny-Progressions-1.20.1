package com.kashdeya.tinyprogressions.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class CraftingToolItem extends Item implements ConfiguredTools.Durable {
    public CraftingToolItem(Properties properties) { super(properties); }
    @Override public boolean hasCraftingRemainingItem(ItemStack stack) { return true; }
    @Override public ItemStack getCraftingRemainingItem(ItemStack stack) { return stack.copyWithCount(1); }
}
