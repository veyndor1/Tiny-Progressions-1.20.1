package com.kashdeya.tinyprogressions.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

public final class InfiniteBucketItem extends BucketItem {
    public InfiniteBucketItem(Properties properties) { super(() -> Fluids.WATER, properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack original = player.getItemInHand(hand).copy();
        var result = super.use(level,player,hand);
        return result.getObject().is(Items.BUCKET) ? new InteractionResultHolder<>(result.getResult(),original) : result;
    }
    @Override public boolean hasCraftingRemainingItem(ItemStack stack) { return true; }
    @Override public ItemStack getCraftingRemainingItem(ItemStack stack) { return stack.copyWithCount(1); }
    @Override public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack,net.minecraft.nbt.CompoundTag tag) {
        class InfiniteWater implements net.minecraftforge.fluids.capability.IFluidHandlerItem,net.minecraftforge.common.capabilities.ICapabilityProvider {
            private final net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandlerItem> capability=net.minecraftforge.common.util.LazyOptional.of(() -> this);
            @Override public ItemStack getContainer() { return stack; }
            @Override public int getTanks() { return 1; }
            @Override public int getTankCapacity(int tank) { return tank==0 ? 1000 : 0; }
            @Override public net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) { return tank==0 ? new net.minecraftforge.fluids.FluidStack(Fluids.WATER,1000) : net.minecraftforge.fluids.FluidStack.EMPTY; }
            @Override public boolean isFluidValid(int tank,net.minecraftforge.fluids.FluidStack fluid) { return tank==0 && fluid.getFluid()==Fluids.WATER; }
            @Override public int fill(net.minecraftforge.fluids.FluidStack fluid,FluidAction action) { return 0; }
            @Override public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack fluid,FluidAction action) {
                return fluid.getFluid()==Fluids.WATER ? drain(fluid.getAmount(),action) : net.minecraftforge.fluids.FluidStack.EMPTY;
            }
            @Override public net.minecraftforge.fluids.FluidStack drain(int amount,FluidAction action) {
                return amount>0 && stack.getCount()==1 ? new net.minecraftforge.fluids.FluidStack(Fluids.WATER,Math.min(1000,amount)) : net.minecraftforge.fluids.FluidStack.EMPTY;
            }
            @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> requested,net.minecraft.core.Direction side) {
                return requested==net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM ? capability.cast() : net.minecraftforge.common.util.LazyOptional.empty();
            }
        }
        return new InfiniteWater();
    }
}
