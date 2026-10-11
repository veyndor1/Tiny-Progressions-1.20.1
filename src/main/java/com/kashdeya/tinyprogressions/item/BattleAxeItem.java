package com.kashdeya.tinyprogressions.item;

import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

/** The original battle axe is an axe with sword tool actions and fast web harvesting. */
public final class BattleAxeItem extends AxeItem implements ConfiguredTools.Durable {
    public BattleAxeItem(Tier tier,float damage,float speed,Properties properties) { super(tier,damage,speed,properties); }
    @Override public boolean canPerformAction(ItemStack stack,ToolAction action) {
        return ToolActions.DEFAULT_AXE_ACTIONS.contains(action) || ToolActions.DEFAULT_SWORD_ACTIONS.contains(action);
    }
    @Override public float getDestroySpeed(ItemStack stack,BlockState state) {
        return state.is(Blocks.COBWEB) ? getTier().getSpeed() : super.getDestroySpeed(stack,state);
    }
    @Override public boolean isCorrectToolForDrops(BlockState state) { return state.is(Blocks.COBWEB) || super.isCorrectToolForDrops(state); }
    @Override public boolean isCorrectToolForDrops(ItemStack stack,BlockState state) { return isCorrectToolForDrops(state); }
}
