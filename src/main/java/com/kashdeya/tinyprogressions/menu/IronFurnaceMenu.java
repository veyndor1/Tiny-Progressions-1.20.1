package com.kashdeya.tinyprogressions.menu;

import com.kashdeya.tinyprogressions.Content;
import com.kashdeya.tinyprogressions.block.IronFurnaceBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class IronFurnaceMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final ContainerLevelAccess access;
    public IronFurnaceMenu(int id,Inventory player,FriendlyByteBuf buffer) {
        this(id,player,new ItemStackHandler(6),new SimpleContainerData(6),ContainerLevelAccess.create(player.player.level(),buffer.readBlockPos()));
    }
    public IronFurnaceMenu(int id,Inventory player,IronFurnaceBlockEntity furnace) {
        this(id,player,furnace.inventory(),furnace.data,ContainerLevelAccess.create(furnace.getLevel(),furnace.getBlockPos()));
    }
    private IronFurnaceMenu(int id,Inventory player,ItemStackHandler inventory,ContainerData data,ContainerLevelAccess access) {
        super(ModMenus.IRON_FURNACE.get(),id); this.data=data; this.access=access;
        addSlot(new SlotItemHandler(inventory,0,56,17));
        addSlot(new SlotItemHandler(inventory,1,56,53) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(net.minecraft.world.item.Items.BUCKET) || ForgeHooks.getBurnTime(stack,RecipeType.SMELTING)>0; }
            @Override public int getMaxStackSize(ItemStack stack) { return stack.is(net.minecraft.world.item.Items.BUCKET) ? 1 : super.getMaxStackSize(stack); }
        });
        for(int i=0;i<4;i++) addSlot(new SlotItemHandler(inventory,i+2,106+(i%2)*19,25+(i/2)*19) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlot(new Slot(player,col+row*9+9,8+col*18,84+row*18));
        for(int col=0;col<9;col++) addSlot(new Slot(player,col,8+col*18,142));
        addDataSlots(data);
    }
    public int burnTime() { return (data.get(0)&65535) | ((data.get(1)&65535)<<16); }
    public int fuelTime() { return (data.get(2)&65535) | ((data.get(3)&65535)<<16); }
    public int cookTime() { return data.get(4); }
    public int cookTotal() { return data.get(5); }
    @Override public boolean stillValid(Player player) { return stillValid(access,player,Content.block("iron_furnace_block")); }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size()) return ItemStack.EMPTY;
        Slot slot=slots.get(index); if(!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),original=stack.copy();
        if(index<6) { if(!moveItemStackTo(stack,6,42,true)) return ItemStack.EMPTY; }
        else if(stack.is(net.minecraft.world.item.Items.BUCKET) || ForgeHooks.getBurnTime(stack,RecipeType.SMELTING)>0) { if(!moveItemStackTo(stack,1,2,false)) return ItemStack.EMPTY; }
        else { if(!moveItemStackTo(stack,0,1,false)) return ItemStack.EMPTY; }
        if(stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player,stack);
        return original;
    }
}
