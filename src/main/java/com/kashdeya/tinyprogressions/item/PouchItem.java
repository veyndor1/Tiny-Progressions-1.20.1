package com.kashdeya.tinyprogressions.item;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public final class PouchItem extends Item {
    public PouchItem(Properties properties) { super(properties); }
    @Override public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag tag) {
        return new Storage();
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            var capability = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                    () -> new IllegalStateException("Pouch storage missing"));
            NetworkHooks.openScreen(server,new SimpleMenuProvider(
                    (id, inventory, user) -> new PouchMenu(id,inventory,stack,slot,(ItemStackHandler)capability),
                    Component.translatable("item.tp.pouch")));
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    public static final class Storage implements ICapabilitySerializable<CompoundTag> {
        private final ItemStackHandler inventory = new ItemStackHandler(54) {
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return !(stack.getItem() instanceof PouchItem);
            }
        };
        private final LazyOptional<IItemHandler> capability = LazyOptional.of(() -> inventory);
        @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            return cap == ForgeCapabilities.ITEM_HANDLER ? capability.cast() : LazyOptional.empty();
        }
        @Override public CompoundTag serializeNBT() { return inventory.serializeNBT(); }
        @Override public void deserializeNBT(CompoundTag tag) { inventory.deserializeNBT(tag); }
    }
}
