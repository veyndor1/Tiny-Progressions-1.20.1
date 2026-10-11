package com.kashdeya.tinyprogressions.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class MyceliumSeedsItem extends Item {
    public MyceliumSeedsItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        if (!level.getBlockState(pos).is(Blocks.DIRT) || player == null || !level.mayInteract(player,pos)
                || !player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand())) return InteractionResult.PASS;
        if (!level.isClientSide) {
            level.setBlock(pos,Blocks.MYCELIUM.defaultBlockState(),3);
            level.playSound(null,pos,SoundEvents.GRASS_HIT,SoundSource.BLOCKS,0.3F,1.5F);
            if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
