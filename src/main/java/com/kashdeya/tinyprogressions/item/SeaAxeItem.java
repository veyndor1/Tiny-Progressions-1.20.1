package com.kashdeya.tinyprogressions.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.kashdeya.tinyprogressions.LegacyConfig;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.*;

/** Pickaxe mode takes precedence, as in the legacy two conditional assignments. */
public final class SeaAxeItem extends AxeItem {
    public SeaAxeItem(Tier tier, Properties properties) { super(tier, 10 - tier.getAttackDamageBonus(), -1.2F, properties); }
    private boolean pickaxe() { return LegacyConfig.enabled("ConfigHandler.sea_pickaxe"); }
    @Override public boolean canPerformAction(ItemStack stack, ToolAction action) {
        return ToolActions.DEFAULT_SWORD_ACTIONS.contains(action)
                || (pickaxe() ? ToolActions.DEFAULT_PICKAXE_ACTIONS : ToolActions.DEFAULT_AXE_ACTIONS).contains(action);
    }
    @Override public boolean isCorrectToolForDrops(BlockState state) {
        if (state.is(Blocks.COBWEB)) return true;
        return pickaxe() ? state.is(BlockTags.MINEABLE_WITH_PICKAXE) && TierSortingRegistry.isCorrectTierForDrops(Tiers.IRON, state)
                : super.isCorrectToolForDrops(state);
    }
    @Override public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) { return isCorrectToolForDrops(state); }
    @Override public float getDestroySpeed(ItemStack stack, BlockState state) {
        return state.is(Blocks.COBWEB) || state.is(pickaxe() ? BlockTags.MINEABLE_WITH_PICKAXE : BlockTags.MINEABLE_WITH_AXE)
                ? getTier().getSpeed() : 1;
    }
    @Override public InteractionResult useOn(UseOnContext context) { return pickaxe() ? InteractionResult.PASS : super.useOn(context); }
    @Override public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (!pickaxe() || slot != EquipmentSlot.MAINHAND) return super.getAttributeModifiers(slot, stack);
        return ImmutableMultimap.<Attribute, AttributeModifier>builder()
                .put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", 1 + getTier().getAttackDamageBonus(), AttributeModifier.Operation.ADDITION))
                .put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", -2.8, AttributeModifier.Operation.ADDITION)).build();
    }
}
