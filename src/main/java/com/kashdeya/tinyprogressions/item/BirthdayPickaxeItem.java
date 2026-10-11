package com.kashdeya.tinyprogressions.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** The original cake-producing supporter pickaxe, using ordinary placement validation. */
public final class BirthdayPickaxeItem extends PickaxeItem {
    public BirthdayPickaxeItem(Tier tier, Properties properties) {
        super(tier, 0, -2.8F, properties.durability(6521));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        var target = context.getClickedPos().relative(context.getClickedFace());
        if (!context.getLevel().isEmptyBlock(target)) return InteractionResult.PASS;
        if (!context.getLevel().mayInteract(player, target)
                || !player.mayUseItemAt(target, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        var hit = new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside());
        var cakeContext = new UseOnContext(context.getLevel(), player, context.getHand(), new ItemStack(Items.CAKE), hit);
        InteractionResult result = Items.CAKE.useOn(cakeContext);
        if (result.consumesAction() && !context.getLevel().isClientSide) {
            context.getItemInHand().hurtAndBreak(854, player, entity -> entity.broadcastBreakEvent(context.getHand()));
            if (player.getGameProfile().getName().equalsIgnoreCase("darkosto")) {
                player.displayClientMessage(Component.literal("HAPPY BIRTHDAY DARKOSTO").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), false);
                context.getLevel().addFreshEntity(new FireworkRocketEntity(context.getLevel(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.FIREWORK_ROCKET)));
            }
        }
        return result;
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.birthday_1").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.birthday_2").withStyle(ChatFormatting.YELLOW));
    }
}
