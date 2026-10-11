package com.kashdeya.tinyprogressions.client;

import com.google.gson.GsonBuilder;
import com.kashdeya.tinyprogressions.Content;
import com.kashdeya.tinyprogressions.TinyProgressions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Development-only audit exercises the real client model loader, including every block state. */
@Mod.EventBusSubscriber(modid="tp",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ModelAudit {
    private static volatile boolean completed;
    @SubscribeEvent public static void baked(ModelEvent.BakingCompleted event) {
        if(!Boolean.getBoolean("tp.auditModels")) return;
        var missing=event.getModelManager().getMissingModel();
        List<String> errors=new ArrayList<>(),textures=new ArrayList<>(); int blocks=0,items=0;
        for(var entry:Content.BLOCKS.getEntries()) for(var state:entry.get().getStateDefinition().getPossibleStates()) {
            var key=BlockModelShaper.stateToModelLocation(state);
            var model=event.getModels().get(key);
            if(model==null || model==missing) errors.add(key.toString());
            else if(model.getParticleIcon().contents().name().equals(MissingTextureAtlasSprite.getLocation())) textures.add(key.toString());
            blocks++;
        }
        for(var entry:Content.ITEMS.getEntries()) {
            var key=new ModelResourceLocation(entry.getId(),"inventory");
            var model=event.getModels().get(key);
            if(model==null || model==missing) errors.add(key.toString());
            else if(model.getParticleIcon().contents().name().equals(MissingTextureAtlasSprite.getLocation())) textures.add(key.toString());
            items++;
        }
        Map<String,Object> report=new LinkedHashMap<>();
        report.put("block_states_checked",blocks); report.put("items_checked",items); report.put("missing_models",errors);
        report.put("missing_particle_textures",textures);
        try {
            Files.writeString(Path.of("../migration/client-model-audit.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
            TinyProgressions.LOGGER.info("TP client model audit: {} block states, {} items, {} missing models, {} missing particle textures",blocks,items,errors.size(),textures.size());
        } catch(Exception exception) { TinyProgressions.LOGGER.error("Could not save model audit",exception); }
        completed=true;
    }
    @Mod.EventBusSubscriber(modid="tp",value=Dist.CLIENT)
    public static final class Finish {
        private static int readyTicks;
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            if(event.phase==TickEvent.Phase.END && completed && Minecraft.getInstance().getOverlay()==null && Minecraft.getInstance().screen instanceof TitleScreen && ++readyTicks>30) {
                var client=Minecraft.getInstance();
                client.getWindow().setWindowed(1280,800);
                client.options.guiScale().set(2);
                client.resizeDisplay();
                client.setScreen(new ContentGalleryScreen());
            }
        }
    }
}
