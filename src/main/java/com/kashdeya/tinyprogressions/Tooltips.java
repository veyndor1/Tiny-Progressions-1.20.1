package com.kashdeya.tinyprogressions;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="tp")
public final class Tooltips {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        Content.appendLegacyTooltip(event.getItemStack(),event.getToolTip());
    }
    public static void add(List<Component> tooltip,String key,ChatFormatting color,String condition) {
        if(!condition.isEmpty() && !FeatureCondition.evaluate(condition)) return;
        Component component=Component.translatable(key).withStyle(color);
        if(tooltip.stream().noneMatch(existing -> existing.getString().equals(component.getString()))) tooltip.add(component);
    }
}
