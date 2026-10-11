package com.kashdeya.tinyprogressions.audit.client;

import com.google.gson.GsonBuilder;
import com.kashdeya.tinyprogressions.Content;
import com.kashdeya.tinyprogressions.LegacyConfig;
import com.kashdeya.tinyprogressions.TinyProgressions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/** Client-only companion checks a real TCP connection to the isolated dedicated server. */
@Mod.EventBusSubscriber(modid="tp_audit", value=Dist.CLIENT)
public final class NetworkAudit {
    private static int stage, readyTicks, joinedTicks, totalTicks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("tp.auditNetwork") || event.phase!=TickEvent.Phase.END) return;
        var client=Minecraft.getInstance();
        if(++totalTicks>2400) { fail(client,"Dedicated connection timed out"); return; }
        if(stage==0 && client.getOverlay()==null && client.screen instanceof TitleScreen && ++readyTicks>30) {
            stage=1;
            var address=System.getProperty("tp.auditServer","127.0.0.1:25575");
            client.options.pauseOnLostFocus=false;
            ConnectScreen.startConnecting(client.screen,client,ServerAddress.parseString(address),
                    new ServerData("Tiny Progressions dedicated QA",address,false),false);
        } else if(stage==1 && client.screen instanceof DisconnectedScreen) {
            fail(client,"Server rejected or closed the connection");
        } else if(stage==1 && client.level!=null && client.player!=null && client.screen==null && client.getOverlay()==null && ++joinedTicks>=100) {
            var errors=new ArrayList<String>();
            var connection=client.getConnection();
            if(client.hasSingleplayerServer()) errors.add("Expected a dedicated server, found an integrated server");
            if(connection==null || !connection.getConnection().isConnected()) errors.add("TCP connection missing");
            if(!LegacyConfig.SPEC.isLoaded()) errors.add("Server configuration was not loaded");
            int expected=Integer.getInteger("tp.auditExpectedGrowthTicks",37);
            int received=LegacyConfig.integer("ConfigHandler.BlockGrowthTicks");
            if(received!=expected) errors.add("Server config mismatch: expected "+expected+", received "+received);
            var report=new LinkedHashMap<String,Object>();
            report.put("dedicated_connection",!client.hasSingleplayerServer());
            report.put("remote_address",connection==null ? "" : connection.getConnection().getRemoteAddress().toString());
            report.put("synchronized_setting","ConfigHandler.BlockGrowthTicks");
            report.put("expected_setting",expected); report.put("received_setting",received);
            report.put("registered_items",Content.ITEMS.getEntries().size());
            report.put("errors",errors);
            try {
                Files.writeString(Path.of("../migration/client-network-audit.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
                GameplayAudit.capture("dedicated-world.png");
                TinyProgressions.LOGGER.info("TP dedicated client audit: config {}, {} items, {} errors",received,Content.ITEMS.getEntries().size(),errors.size());
            } catch(Exception error) { fail(client,"Could not save dedicated audit: "+error); return; }
            stage=2; readyTicks=0;
            client.level.disconnect();
            client.clearLevel(new TitleScreen());
        } else if(stage==2 && ++readyTicks>=30) { stage=3; client.stop(); }
    }
    private static void fail(Minecraft client,String reason) {
        stage=3; TinyProgressions.LOGGER.error("TP dedicated client audit failed: {}",reason); client.stop();
    }
}
