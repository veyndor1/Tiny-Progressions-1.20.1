package com.kashdeya.tinyprogressions.client;

import com.kashdeya.tinyprogressions.Content;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Actual worn armor meshes, including the distinct leggings layer. */
public final class ArmorGalleryScreen extends Screen {
    private static final String[] FAMILIES={"wooden","stone","flint","bone","emerald","obsidian","lapis","quartz","redstone","lava","wither","dragon"};
    private int frames;
    public ArmorGalleryScreen() { super(Component.literal("Tiny Progressions armor review")); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        graphics.fillGradient(0,0,width,height,0xff171c27,0xff252c3b);
        graphics.drawString(font,"Tiny Progressions · Worn armor · Minecraft 1.20.1",12,12,0xffeef5ff,false);
        var player=minecraft.player;
        EquipmentSlot[] slots={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
        String[] parts={"helmet","chestplate","leggings","boots"};
        ItemStack[] saved=java.util.Arrays.stream(slots).map(player::getItemBySlot).toArray(ItemStack[]::new);
        try {
            for(int i=0;i<FAMILIES.length;i++) {
                for(int j=0;j<slots.length;j++) player.setItemSlot(slots[j],Content.item(FAMILIES[i]+"_"+parts[j]).getDefaultInstance());
                int x=width/12+(i%6)*width/6,y=180+(i/6)*180;
                InventoryScreen.renderEntityInInventoryFollowsAngle(graphics,x,y,60,-10,0,player);
                graphics.drawCenteredString(font,FAMILIES[i],x,y+8,0xffd8e4f3);
            }
        } finally { for(int i=0;i<slots.length;i++) player.setItemSlot(slots[i],saved[i]); }
        if(++frames==25) { graphics.flush(); GameplayAudit.capture("worn-armor.png"); }
        if(frames>=35) GameplayAudit.armorFinished();
    }
}
