package com.kashdeya.tinyprogressions.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import com.kashdeya.tinyprogressions.LegacyConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class DrinkItem extends ConfiguredFoodItem {
    private final String bottleSetting;
    private final boolean succ;
    public DrinkItem(Properties properties, java.util.function.Supplier<net.minecraft.world.food.FoodProperties> food, String bottleSetting, boolean succ) {
        super(properties,food); this.bottleSetting=bottleSetting; this.succ=succ;
    }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public int getUseDuration(ItemStack stack) { return LegacyConfig.integer("ConfigHandler.eat_timer"); }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        ItemStack result = super.finishUsingItem(stack, level, consumer);
        if (succ && !level.isClientSide) level.playSound(null,consumer.blockPosition(),com.kashdeya.tinyprogressions.ModSounds.SUCC.get(),net.minecraft.sounds.SoundSource.PLAYERS,.5F,.9F+level.random.nextFloat()*.1F);
        if (LegacyConfig.enabled(bottleSetting) && consumer instanceof Player player
                && !player.getAbilities().instabuild) {
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (result.isEmpty()) return bottle;
            if (!level.isClientSide && !player.getInventory().add(bottle)) player.drop(bottle,false);
        }
        return result;
    }
}
