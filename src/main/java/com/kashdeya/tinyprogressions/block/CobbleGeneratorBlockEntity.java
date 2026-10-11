package com.kashdeya.tinyprogressions.block;

import com.kashdeya.tinyprogressions.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

public final class CobbleGeneratorBlockEntity extends BlockEntity {
    private int cycle;
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
        @Override public int getSlotLimit(int slot) { return ((CobbleGeneratorBlock) getBlockState().getBlock()).capacity(); }
    };
    private LazyOptional<IItemHandler> capability = LazyOptional.of(() -> inventory);

    public CobbleGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(Content.COBBLE_GENERATOR.get(), pos, state);
    }
    public ItemStack extract(int count) { return inventory.extractItem(0, count, false); }
    public ItemStack contents() { return inventory.getStackInSlot(0).copy(); }

    public static void tick(Level level, BlockPos pos, BlockState state, CobbleGeneratorBlockEntity entity) {
        if (level.isClientSide || !(state.getBlock() instanceof CobbleGeneratorBlock block)) return;
        if (++entity.cycle < block.interval()) return;
        entity.cycle = 0;
        ItemStack stack = entity.inventory.getStackInSlot(0);
        if (stack.isEmpty()) entity.inventory.setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        else if (stack.getCount() < block.capacity()) {
            stack.grow(1);
            entity.setChanged();
        }
        BlockEntity above = level.getBlockEntity(pos.above());
        if (above != null && !entity.inventory.getStackInSlot(0).isEmpty()) {
            above.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).ifPresent(destination -> {
                ItemStack offered = new ItemStack(Items.COBBLESTONE);
                // Remove exactly what the destination actually accepted; rejected output remains stored.
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(destination, offered, false);
                if (remainder.isEmpty()) entity.extract(1);
            });
        }
        entity.setChanged();
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("Cycle", cycle);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        cycle = Math.max(0, tag.getInt("Cycle"));
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        return cap == ForgeCapabilities.ITEM_HANDLER ? capability.cast() : super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); capability.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); capability = LazyOptional.of(() -> inventory); }
}
