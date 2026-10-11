package com.kashdeya.tinyprogressions.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

public final class PaxelItem extends PickaxeItem implements ConfiguredTools.Durable {
    public PaxelItem(Tier tier, Properties properties) { super(tier,1,-2.8F,properties); }
    private static boolean plantOrWeb(BlockState state) {
        return state.is(net.minecraft.world.level.block.Blocks.COBWEB) || state.is(BlockTags.CLIMBABLE) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS);
    }
    @Override public float getDestroySpeed(ItemStack stack, BlockState state) {
        return plantOrWeb(state) || state.is(BlockTags.MINEABLE_WITH_AXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                ? getTier().getSpeed() : super.getDestroySpeed(stack,state);
    }
    @Override public boolean isCorrectToolForDrops(BlockState state) {
        if (plantOrWeb(state) || state.is(BlockTags.MINEABLE_WITH_AXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return net.minecraftforge.common.TierSortingRegistry.isCorrectTierForDrops(getTier(),state);
        }
        return super.isCorrectToolForDrops(state);
    }
    @Override public boolean isCorrectToolForDrops(ItemStack stack,BlockState state) { return isCorrectToolForDrops(state); }
    @Override public boolean canPerformAction(ItemStack stack, ToolAction action) {
        return ToolActions.DEFAULT_PICKAXE_ACTIONS.contains(action) || ToolActions.DEFAULT_AXE_ACTIONS.contains(action)
                || ToolActions.DEFAULT_SHOVEL_ACTIONS.contains(action);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (player == null || !level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand())) return InteractionResult.PASS;
        BlockState state = level.getBlockState(pos);
        for (ToolAction action : new ToolAction[]{ToolActions.AXE_STRIP,ToolActions.AXE_SCRAPE,
                                                 ToolActions.AXE_WAX_OFF,ToolActions.SHOVEL_FLATTEN}) {
            if (action == ToolActions.SHOVEL_FLATTEN && !level.isEmptyBlock(pos.above())) continue;
            BlockState modified = state.getToolModifiedState(context,action,false);
            if (modified == null) continue;
            if (!level.isClientSide) {
                level.setBlock(pos,modified,3);
                context.getItemInHand().hurtAndBreak(1,player,p -> p.broadcastBreakEvent(context.getHand()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
