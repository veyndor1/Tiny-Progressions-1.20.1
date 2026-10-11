package com.kashdeya.tinyprogressions.item;

import com.kashdeya.tinyprogressions.LegacyConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class RepairTabletItem extends Item {
    public RepairTabletItem(Properties properties) { super(properties); }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        var tag = stack.getOrCreateTag();
        int ticks = tag.getInt("Timer") + 1;
        int interval = Math.max(1, LegacyConfig.integer("ConfigHandler.repair_tablet_cooldown"));
        tag.putInt("Timer", ticks >= interval ? 0 : ticks);
        if (ticks < interval) return;
        for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
            ItemStack target = player.getInventory().getItem(index);
            if (target.isDamaged() && target.getItem().isRepairable(target)
                    && !(player.swinging && (target == player.getMainHandItem() || target == player.getOffhandItem()))) {
                target.setDamageValue(Math.max(0,target.getDamageValue()-1));
            }
        }
    }
}
