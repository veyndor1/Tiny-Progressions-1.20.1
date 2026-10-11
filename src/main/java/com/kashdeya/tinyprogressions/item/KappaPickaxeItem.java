package com.kashdeya.tinyprogressions.item;

import com.kashdeya.tinyprogressions.LegacyConfig;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.TierSortingRegistry;

public final class KappaPickaxeItem extends PickaxeItem {
    public KappaPickaxeItem(Tier tier, Properties properties) { super(tier, 1, -2.8F, properties); }
    @Override public int getMaxDamage(ItemStack stack) { return LegacyConfig.integer("ConfigHandler.kappa_damage"); }
    @Override public boolean isCorrectToolForDrops(BlockState state) {
        Tier[] tiers = {Tiers.WOOD, Tiers.STONE, Tiers.IRON, Tiers.DIAMOND};
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                && TierSortingRegistry.isCorrectTierForDrops(tiers[LegacyConfig.integer("ConfigHandler.kappa_level")], state);
    }
    @Override public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) { return isCorrectToolForDrops(state); }
}
