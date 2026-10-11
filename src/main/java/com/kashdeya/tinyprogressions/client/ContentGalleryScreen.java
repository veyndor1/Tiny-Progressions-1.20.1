package com.kashdeya.tinyprogressions.client;

import com.kashdeya.tinyprogressions.Content;
import com.kashdeya.tinyprogressions.TinyProgressions;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.nio.file.*;
import java.util.List;

/** Developer review gallery: screenshots show real baked item models and original textures. */
public final class ContentGalleryScreen extends Screen {
    private final List<net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item>> items=List.copyOf(Content.ITEMS.getEntries());
    private int page,frames;
    public ContentGalleryScreen() { super(Component.literal("Tiny Progressions · Content review")); }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        graphics.fillGradient(0,0,width,height,0xff171c27,0xff252c3b);
        graphics.drawString(font,"Tiny Progressions · Minecraft 1.20.1",12,10,0xffeef5ff,false);
        graphics.drawString(font,"Content review "+(page+1)+" / "+((items.size()+63)/64),12,22,0xff94adc9,false);
        int cell=(width-24)/8;
        for(int i=0;i<64 && page*64+i<items.size();i++) {
            var entry=items.get(page*64+i);
            int x=12+(i%8)*cell,y=38+(i/8)*42;
            graphics.fill(x,y,x+cell-3,y+39,0xff30394a);
            ItemStack stack=entry.get().getDefaultInstance();
            graphics.renderItem(stack,x+(cell-16)/2,y+3);
            String name=font.plainSubstrByWidth(entry.getId().getPath(),cell-8);
            graphics.drawString(font,name,x+3,y+25,0xffd8e4f3,false);
        }
        graphics.drawString(font,"Original content · Development build 4.0.0-alpha.2",12,height-14,0xff94adc9,false);
        if(++frames==15) {
            graphics.flush();
            try(var screenshot=Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
                Path directory=Path.of("../migration/client-qa"); Files.createDirectories(directory);
                screenshot.writeToFile(directory.resolve(String.format("content-%02d.png",page+1)));
                TinyProgressions.LOGGER.info("Saved content gallery page {}",page+1);
            } catch(Exception exception) { TinyProgressions.LOGGER.error("Could not save content gallery",exception); }
        }
        if(frames>=25) {
            frames=0;
            if(++page*64>=items.size()) {
                if (Boolean.getBoolean("tp.auditGameplay")) GameplayAudit.begin();
                else minecraft.stop();
            }
        }
    }
}
