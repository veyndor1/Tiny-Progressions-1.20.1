package com.kashdeya.tinyprogressions.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class ScytheItem extends SwordItem implements ConfiguredTools.Durable {
    public ScytheItem(Tier tier, Properties properties) { super(tier,3,-2.4F,properties); }
    @Override public boolean canPerformAction(ItemStack stack,ToolAction action) { return action==ToolActions.HOE_TILL || super.canPerformAction(stack,action); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (player == null || !level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand())) return InteractionResult.PASS;
        BlockState current = level.getBlockState(pos);
        BlockState replacement = null;
        if (context.getClickedFace()!=net.minecraft.core.Direction.DOWN && level.isEmptyBlock(pos.above())) {
            if (current.is(Blocks.GRASS_BLOCK) || current.is(Blocks.DIRT_PATH) || current.is(Blocks.COARSE_DIRT)) replacement = Blocks.DIRT.defaultBlockState();
            else if (current.is(Blocks.MYCELIUM) || current.is(Blocks.PODZOL)) replacement = Blocks.COARSE_DIRT.defaultBlockState();
            else if (current.is(Blocks.DIRT)) replacement = Blocks.FARMLAND.defaultBlockState();
        }
        if (replacement != null) {
            var event=new net.minecraftforge.event.level.BlockEvent.BlockToolModificationEvent(current,context,ToolActions.HOE_TILL,false);
            event.setFinalState(replacement);
            if(net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event) || event.getFinalState()==null) return InteractionResult.FAIL;
            replacement=event.getFinalState();
            if (!level.isClientSide) {
                level.setBlock(pos,replacement,3);
                level.playSound(null,pos,net.minecraft.sounds.SoundEvents.HOE_TILL,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
                context.getItemInHand().hurtAndBreak(1,player,p -> p.broadcastBreakEvent(context.getHand()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        boolean harvested = false;
        if(level.isClientSide) {
            for(BlockPos target:BlockPos.betweenClosed(pos.offset(-1,0,-1),pos.offset(1,0,1))) {
                BlockState state=level.getBlockState(target);
                if(state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)
                        || state.getBlock() instanceof StemBlock && state.getValue(StemBlock.AGE)==7
                        || state.getBlock() instanceof CocoaBlock && state.getValue(CocoaBlock.AGE)==2) return InteractionResult.SUCCESS;
            }
        }
        if (!level.isClientSide) for (BlockPos target : BlockPos.betweenClosed(pos.offset(-1,0,-1),pos.offset(1,0,1))) {
            if (context.getItemInHand().isEmpty()) break;
            if (!level.hasChunkAt(target) || !level.mayInteract(player,target) || !player.mayUseItemAt(target,context.getClickedFace(),context.getItemInHand())) continue;
            BlockState state = level.getBlockState(target);
            BlockState replanted=null;
            if(state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) replanted=crop.getStateForAge(0);
            else if(state.getBlock() instanceof StemBlock && state.getValue(StemBlock.AGE)==7) replanted=state.setValue(StemBlock.AGE,0);
            else if(state.getBlock() instanceof CocoaBlock && state.getValue(CocoaBlock.AGE)==2) replanted=state.setValue(CocoaBlock.AGE,0);
            if (replanted!=null) {
                if(player instanceof ServerPlayer server && ForgeHooks.onBlockBreakEvent(level,server.gameMode.getGameModeForPlayer(),server,target)<0) continue;
                // Vanilla loot receives the tool and player; replacing the plant keeps the original replant behavior.
                net.minecraft.world.level.block.Block.dropResources(state,level,target,null,player,context.getItemInHand());
                level.setBlock(target,replanted,3);
                context.getItemInHand().hurtAndBreak(1,player,p -> p.broadcastBreakEvent(context.getHand()));
                harvested = true;
            }
        }
        return harvested ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
