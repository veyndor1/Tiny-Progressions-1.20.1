package com.kashdeya.tinyprogressions.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.*;

/** Place the floating block beside the player's bounds, through Forge's normal placement path. */
public final class AngelBlockItem extends BlockItem {
    public AngelBlockItem(Block block, Properties properties) { super(block, properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var look = player.getLookAngle();
        Direction side = Direction.getNearest(look.x, look.y, look.z);
        AABB bounds = player.getBoundingBox();
        BlockPos target = BlockPos.containing(player.getEyePosition());
        target = switch (side) {
            case DOWN -> new BlockPos(target.getX(), (int)Math.floor(bounds.minY) - 1, target.getZ());
            case UP -> new BlockPos(target.getX(), (int)Math.floor(bounds.maxY) + 1, target.getZ());
            case NORTH -> new BlockPos(target.getX(), target.getY(), (int)Math.floor(bounds.minZ) - 1);
            case SOUTH -> new BlockPos(target.getX(), target.getY(), (int)Math.floor(bounds.maxZ) + 1);
            case WEST -> new BlockPos((int)Math.floor(bounds.minX) - 1, target.getY(), target.getZ());
            case EAST -> new BlockPos((int)Math.floor(bounds.maxX) + 1, target.getY(), target.getZ());
        };
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isLoaded(target) || !level.isEmptyBlock(target) || !level.mayInteract(player, target)
                || !player.mayUseItemAt(target, side, stack)) return InteractionResultHolder.fail(stack);
        var hit = new BlockHitResult(Vec3.atCenterOf(target), side, target, false);
        var result = stack.useOn(new UseOnContext(player, hand, hit));
        return new InteractionResultHolder<>(result, stack);
    }
}
