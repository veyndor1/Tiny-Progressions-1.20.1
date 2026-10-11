package com.kashdeya.tinyprogressions;

import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "tp")
public final class FuelEvents {
    @SubscribeEvent public static void fuel(FurnaceFuelBurnTimeEvent event) {
        var id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id == null || !id.getNamespace().equals("tp")) return;
        switch (id.getPath()) {
            case "charcoal_block" -> event.setBurnTime(LegacyConfig.integer("FuelHandler.CharcoalBlockBurn"));
            case "tiny_coal" -> event.setBurnTime(LegacyConfig.integer("FuelHandler.tiny_coal_burntime"));
            case "tiny_charcoal" -> event.setBurnTime(LegacyConfig.integer("FuelHandler.tiny_charcoal_burntime"));
            case "stone_torch" -> event.setBurnTime(150);
        }
    }
}
