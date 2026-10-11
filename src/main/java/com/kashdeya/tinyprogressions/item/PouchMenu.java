package com.kashdeya.tinyprogressions.item;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;

/** Vanilla six-row screen with server-enforced pouch and nesting protections. */
public final class PouchMenu extends ChestMenu {
    private final ItemStack pouch;
    private final Inventory owner;
    private final int ownerSlot;
    public PouchMenu(int id, Inventory player, ItemStack pouch, int ownerSlot, ItemStackHandler storage) {
        super(MenuType.GENERIC_9x6,id,player,new RecipeWrapper(storage),6);
        this.pouch = pouch;
        this.owner = player;
        this.ownerSlot = ownerSlot;
        for (int i = 0; i < 54; i++) {
            Slot previous = slots.get(i);
            Slot replacement = new SlotItemHandler(storage,i,previous.x,previous.y) {
                @Override public boolean mayPlace(ItemStack stack) { return !(stack.getItem() instanceof PouchItem); }
            };
            replacement.index = previous.index;
            slots.set(i,replacement);
        }
        for (int i = 54; i < slots.size(); i++) {
            Slot previous = slots.get(i);
            if (previous.getSlotIndex() != ownerSlot) continue;
            Slot replacement = new Slot(player,ownerSlot,previous.x,previous.y) {
                @Override public boolean mayPickup(Player user) { return false; }
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            };
            replacement.index = previous.index;
            slots.set(i,replacement);
        }
    }
    @Override public boolean stillValid(Player player) { return owner.getItem(ownerSlot) == pouch; }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (type == ClickType.SWAP && button == ownerSlot) return;
        super.clicked(slot,button,type,player);
    }
}
