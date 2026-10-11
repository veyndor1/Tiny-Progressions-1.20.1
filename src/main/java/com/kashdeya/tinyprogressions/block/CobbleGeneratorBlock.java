package com.kashdeya.tinyprogressions.block;

import com.kashdeya.tinyprogressions.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class CobbleGeneratorBlock extends BaseEntityBlock {
    private final int interval;
    private final int capacity;

    public CobbleGeneratorBlock(Properties properties, int interval, int capacity) {
        super(properties);
        this.interval = interval;
        this.capacity = capacity;
    }

    public int interval() { return interval; }
    public int capacity() { return capacity; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CobbleGeneratorBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Content.COBBLE_GENERATOR.get(),
                CobbleGeneratorBlockEntity::tick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof CobbleGeneratorBlockEntity generator) {
            if (!level.isClientSide) {
                ItemStack extracted = generator.extract(player.isShiftKeyDown() ? 64 : 1);
                if (!player.getInventory().add(extracted)) player.drop(extracted, false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof CobbleGeneratorBlockEntity generator) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), generator.extract(64));
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
