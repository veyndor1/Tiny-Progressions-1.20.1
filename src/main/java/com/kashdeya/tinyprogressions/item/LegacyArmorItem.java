package com.kashdeya.tinyprogressions.item;

import com.kashdeya.tinyprogressions.LegacyConfig;
import com.kashdeya.tinyprogressions.FeatureCondition;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class LegacyArmorItem extends ArmorItem {
    private final String[] tooltipKeys,conditions;
    public LegacyArmorItem(ArmorMaterial material,Type type,Properties properties,String[] tooltipKeys,String[] conditions) {
        super(material,type,properties);
        if (tooltipKeys.length != conditions.length) throw new IllegalArgumentException("Armor tooltip conditions must match their descriptions");
        this.tooltipKeys=tooltipKeys; this.conditions=conditions;
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flags) {
        super.appendHoverText(stack,level,tooltip,flags);
        for(int i=0;i<tooltipKeys.length;i++) {
            if(conditions[i].isEmpty() || FeatureCondition.evaluate(conditions[i]))
                tooltip.add(Component.translatable(tooltipKeys[i]).withStyle(ChatFormatting.YELLOW));
        }
    }
    @Override public String getArmorTexture(ItemStack stack, net.minecraft.world.entity.Entity entity, net.minecraft.world.entity.EquipmentSlot slot, String type) {
        String material=getMaterial().getName();
        if (material.equals("tp:dragon") || material.equals("tp:wither"))
            return "tp:textures/models/armor/"+material.substring(3)+"_layer_1.png";
        return null;
    }
}
