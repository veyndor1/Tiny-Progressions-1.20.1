package com.kashdeya.tinyprogressions.block;

import com.kashdeya.tinyprogressions.Content;
import com.kashdeya.tinyprogressions.menu.IronFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import net.minecraft.world.Containers;
import org.jetbrains.annotations.Nullable;

/** Six original slots: input, fuel, and four results. A smelt commits only when its entire result fits. */
public final class IronFurnaceBlockEntity extends BlockEntity implements MenuProvider {
    private int burnTime, fuelTime, cookTime;
    private String cookingRecipe = "";
    public static final int COOK_TICKS = 100;
    private final ItemStackHandler inventory = new ItemStackHandler(6) {
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 || slot == 1 && (stack.is(Items.BUCKET) || ForgeHooks.getBurnTime(stack,RecipeType.SMELTING) > 0); }
        @Override protected int getStackLimit(int slot, ItemStack stack) { return slot == 1 && stack.is(Items.BUCKET) ? 1 : super.getStackLimit(slot,stack); }
        @Override protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null) level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());
        }
    };
    private LazyOptional<IItemHandler> all, input, fuel, output;
    public final ContainerData data = new ContainerData() {
        public int get(int index) { return switch(index) { case 0 -> burnTime & 65535; case 1 -> burnTime >>> 16; case 2 -> fuelTime & 65535; case 3 -> fuelTime >>> 16; case 4 -> cookTime; case 5 -> COOK_TICKS; default -> 0; }; }
        public void set(int index,int value) { /* Authoritative values only change on the server. */ }
        public int getCount() { return 6; }
    };
    public IronFurnaceBlockEntity(BlockPos pos, BlockState state) { super(Content.IRON_FURNACE.get(),pos,state); createCaps(); }
    public ItemStackHandler inventory() { return inventory; }
    private void createCaps() {
        all = LazyOptional.of(() -> new SidedHandler(0,6,false));
        input = LazyOptional.of(() -> new SidedHandler(0,1,false));
        fuel = LazyOptional.of(() -> new SidedHandler(1,1,false));
        output = LazyOptional.of(() -> new SidedHandler(2,4,true));
    }
    private final class SidedHandler implements IItemHandler {
        private final int start,size; private final boolean extraction;
        SidedHandler(int start,int size,boolean extraction) { this.start=start; this.size=size; this.extraction=extraction; }
        private int actual(int slot) { if(slot<0 || slot>=size) throw new IndexOutOfBoundsException(slot); return start+slot; }
        public int getSlots() { return size; }
        public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(actual(slot)); }
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) { int target=actual(slot); return extraction || target>=2 ? stack : inventory.insertItem(target,stack,simulate); }
        public ItemStack extractItem(int slot,int amount,boolean simulate) { int target=actual(slot); return extraction || target>=2 ? inventory.extractItem(target,amount,simulate) : ItemStack.EMPTY; }
        public int getSlotLimit(int slot) { return inventory.getSlotLimit(actual(slot)); }
        public boolean isItemValid(int slot,ItemStack stack) { return !extraction && inventory.isItemValid(actual(slot),stack); }
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,@Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return (side == null ? all : side == Direction.UP ? input : side == Direction.DOWN ? output : fuel).cast();
        return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); all.invalidate(); input.invalidate(); fuel.invalidate(); output.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); createCaps(); }
    public static void tick(Level level,BlockPos pos,BlockState state,IronFurnaceBlockEntity furnace) {
        if (level.isClientSide) return;
        boolean wasLit = furnace.burnTime > 0;
        if (furnace.burnTime > 0) --furnace.burnTime;
        var wrapper = new RecipeWrapper(furnace.inventory);
        SmeltingRecipe recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,wrapper,level).orElse(null);
        String recipeId = recipe == null ? "" : recipe.getId().toString();
        if (!furnace.cookingRecipe.equals(recipeId)) { furnace.cookingRecipe=recipeId; furnace.cookTime=0; }
        ItemStack result = recipe == null ? ItemStack.EMPTY : recipe.assemble(wrapper,level.registryAccess());
        boolean canSmelt = !result.isEmpty() && furnace.insertResult(result,true).isEmpty();
        if (furnace.burnTime == 0 && canSmelt) {
            ItemStack fuel = furnace.inventory.getStackInSlot(1);
            int duration = ForgeHooks.getBurnTime(fuel,RecipeType.SMELTING);
            if (duration > 0) {
                furnace.burnTime=furnace.fuelTime=duration;
                ItemStack remainder=fuel.getCraftingRemainingItem();
                fuel.shrink(1);
                if (fuel.isEmpty()) furnace.inventory.setStackInSlot(1,remainder);
            }
        }
        if (furnace.burnTime > 0 && canSmelt) {
            if (++furnace.cookTime >= COOK_TICKS) {
                ItemStack source=furnace.inventory.getStackInSlot(0);
                furnace.insertResult(result,false);
                if (source.is(Items.WET_SPONGE) && furnace.inventory.getStackInSlot(1).is(Items.BUCKET))
                    furnace.inventory.setStackInSlot(1,new ItemStack(Items.WATER_BUCKET));
                source.shrink(1);
                furnace.cookTime=0;
            }
        } else furnace.cookTime = canSmelt ? Math.max(0,furnace.cookTime-2) : 0;
        boolean lit=furnace.burnTime>0;
        if (wasLit != lit || state.getValue(IronFurnaceBlock.LIT) != lit)
            level.setBlock(pos,state.setValue(IronFurnaceBlock.LIT,lit),3);
        if (lit || wasLit) furnace.setChanged();
    }
    private ItemStack insertResult(ItemStack result, boolean simulate) {
        ItemStack remainder=result.copy();
        for (int slot=2;slot<6 && !remainder.isEmpty();slot++) {
            ItemStack current=inventory.getStackInSlot(slot);
            if (!current.isEmpty() && !ItemStack.isSameItemSameTags(current,remainder)) continue;
            int accepted=Math.min(remainder.getCount(),Math.min(inventory.getSlotLimit(slot),remainder.getMaxStackSize())-current.getCount());
            if (accepted<=0) continue;
            if (!simulate) { ItemStack replacement=current.isEmpty() ? remainder.copy() : current.copy(); replacement.setCount(current.getCount()+accepted); inventory.setStackInSlot(slot,replacement); }
            remainder.shrink(accepted);
        }
        return remainder;
    }
    public int comparatorSignal() {
        float fullness=0; boolean occupied=false;
        for(int i=0;i<6;i++) { ItemStack stack=inventory.getStackInSlot(i); if(!stack.isEmpty()) { occupied=true; fullness+=(float)stack.getCount()/Math.min(64,stack.getMaxStackSize()); } }
        return (int)(fullness/6*14)+(occupied ? 1 : 0);
    }
    public void dropContents() { if(level!=null && !level.isClientSide) for(int i=0;i<6;i++) Containers.dropItemStack(level,worldPosition.getX(),worldPosition.getY(),worldPosition.getZ(),inventory.extractItem(i,64,false)); }
    @Override public Component getDisplayName() { return Component.translatable("block.tp.iron_furnace_block"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) { return new IronFurnaceMenu(id,playerInventory,this); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.put("Inventory",inventory.serializeNBT()); tag.putInt("BurnTime",burnTime); tag.putInt("FuelTime",fuelTime); tag.putInt("CookTime",cookTime); tag.putString("CookingRecipe",cookingRecipe); }
    @Override public void load(CompoundTag tag) { super.load(tag); inventory.deserializeNBT(tag.getCompound("Inventory")); burnTime=Math.max(0,tag.getInt("BurnTime")); fuelTime=Math.max(0,tag.getInt("FuelTime")); cookTime=Math.max(0,Math.min(COOK_TICKS,tag.getInt("CookTime"))); cookingRecipe=tag.getString("CookingRecipe"); }
}
