package com.kashdeya.tinyprogressions.item;

import java.util.function.Supplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;

/** Read nutrition from the loaded configuration rather than freezing registry-time defaults. */
public class ConfiguredFoodItem extends Item {
    private final Supplier<FoodProperties> food;
    public ConfiguredFoodItem(Properties properties, Supplier<FoodProperties> food) { super(properties); this.food=food; }
    @Override public FoodProperties getFoodProperties(ItemStack stack, LivingEntity consumer) { return food.get(); }
    @Override public int getUseDuration(ItemStack stack) { return com.kashdeya.tinyprogressions.LegacyConfig.integer("ConfigHandler.eat_timer"); }
}
