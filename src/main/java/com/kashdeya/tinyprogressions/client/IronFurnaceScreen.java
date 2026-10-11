package com.kashdeya.tinyprogressions.client;

import com.kashdeya.tinyprogressions.menu.IronFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class IronFurnaceScreen extends AbstractContainerScreen<IronFurnaceMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.tryBuild("tp","textures/gui/container/furnace_inventory.png");
    public IronFurnaceScreen(IronFurnaceMenu menu,Inventory inventory,Component title) { super(menu,inventory,title); }
    @Override protected void renderBg(GuiGraphics graphics,float partialTick,int mouseX,int mouseY) {
        graphics.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        if(menu.burnTime()>0) { int height=(int)Math.min(13,(long)menu.burnTime()*13/Math.max(1,menu.fuelTime())); graphics.blit(TEXTURE,leftPos+56,topPos+48-height,176,12-height,14,height+1); }
        int progress=(int)Math.min(24,(long)menu.cookTime()*24/Math.max(1,menu.cookTotal()));
        graphics.blit(TEXTURE,leftPos+79,topPos+34,176,14,progress+1,16);
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) { renderBackground(graphics); super.render(graphics,mouseX,mouseY,partialTick); renderTooltip(graphics,mouseX,mouseY); }
}
