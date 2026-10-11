package com.kashdeya.tinyprogressions.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.kashdeya.tinyprogressions.LegacyConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraftforge.common.ForgeMod;
import java.util.UUID;

/** Forge's server-validated reach attribute replaces the legacy client attack packet. */
public final class SpearItem extends SwordItem implements ConfiguredTools.Durable {
    private static final UUID REACH_ID = UUID.fromString("b58a14b6-bbf2-4d3c-a560-716c649be6b1");
    public SpearItem(Tier tier, Properties properties) { super(tier,3,-2.4F,properties); }
    @Override public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) return super.getAttributeModifiers(slot,stack);
        return ImmutableMultimap.<Attribute,AttributeModifier>builder()
                .putAll(super.getAttributeModifiers(slot,stack))
                .put(ForgeMod.ENTITY_REACH.get(),new AttributeModifier(REACH_ID,"Tiny Progressions spear reach",
                        Math.max(0,LegacyConfig.decimal("ConfigHandler.spear_reach")-3.0),AttributeModifier.Operation.ADDITION))
                .build();
    }
}
