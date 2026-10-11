package com.kashdeya.tinyprogressions.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class GlintFoodItem extends ConfiguredFoodItem {
    public GlintFoodItem(Properties properties, java.util.function.Supplier<net.minecraft.world.food.FoodProperties> food) { super(properties,food); }
    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public int getUseDuration(ItemStack stack) { return 32; }
}
