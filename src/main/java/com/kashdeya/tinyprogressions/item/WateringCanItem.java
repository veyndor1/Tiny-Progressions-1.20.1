package com.kashdeya.tinyprogressions.item;

import com.kashdeya.tinyprogressions.LegacyConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class WateringCanItem extends Item {
    private final int radius;
    public WateringCanItem(Properties properties, int radius) { super(properties); this.radius = radius; }
    @Override public boolean isFoil(ItemStack stack) { return stack.hasTag() && stack.getTag().getBoolean("Active"); }
    @Override public net.minecraft.world.item.Rarity getRarity(ItemStack stack) { return isFoil(stack) ? net.minecraft.world.item.Rarity.UNCOMMON : net.minecraft.world.item.Rarity.COMMON; }
    @Override public int getUseDuration(ItemStack stack) { return 72000; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            if (!context.getLevel().isClientSide) {
                var tag = context.getItemInHand().getOrCreateTag();
                tag.putBoolean("Active", !tag.getBoolean("Active"));
            }
        } else {
            player.startUsingItem(context.getHand());
            water(context.getLevel(), player, context.getItemInHand(), context.getClickedPos());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (entity instanceof Player player) waterLookingAt(level, player, stack);
    }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (entity instanceof Player player && isFoil(stack)
                && (selected || player.getOffhandItem() == stack)) waterLookingAt(level, player, stack);
    }
    private void waterLookingAt(Level level, Player player, ItemStack stack) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() == HitResult.Type.BLOCK) water(level, player, stack, hit.getBlockPos());
    }
    private void water(Level level, Player player, ItemStack stack, BlockPos center) {
        if (!(level instanceof ServerLevel server)) return;
        long now = level.getGameTime();
        var tag = stack.getOrCreateTag();
        if (tag.contains("LastWatered") && now >= tag.getLong("LastWatered")
                && now - tag.getLong("LastWatered") < 4) return;
        tag.putLong("LastWatered", now);
        if (level.random.nextInt(100) >= LegacyConfig.integer(radius == 1
                ? "ConfigHandler.WateringCanChance" : "ConfigHandler.WateringCanUpgradeChance")) return;
        for (BlockPos target : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                                                     center.offset(radius, radius, radius))) {
            if (!level.hasChunkAt(target) || !level.mayInteract(player, target)) continue;
            BlockState state = level.getBlockState(target);
            if (com.kashdeya.tinyprogressions.block.GrowthSupport.grows(state)) {
                state.randomTick(server, target.immutable(), level.random);
            }
        }
        server.sendParticles(ParticleTypes.SPLASH, center.getX() + 0.5, center.getY() + 1,
                center.getZ() + 0.5, 6, radius * 0.5, 0.2, radius * 0.5, 0);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<net.minecraft.network.chat.Component> tooltip,net.minecraft.world.item.TooltipFlag flag) {
        String prefix=radius==1 ? "tooltip.can_" : "tooltip.canupgrade_";
        for(int i=1;i<=3;i++) tooltip.add(net.minecraft.network.chat.Component.translatable(prefix+i).withStyle(net.minecraft.ChatFormatting.YELLOW));
    }
}
