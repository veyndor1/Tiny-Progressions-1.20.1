package com.kashdeya.tinyprogressions.client;

import com.kashdeya.tinyprogressions.Content;
import com.kashdeya.tinyprogressions.TinyProgressions;
import com.kashdeya.tinyprogressions.block.IronFurnaceBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import java.nio.file.*;

/** Opt-in developer QA uses a disposable world and real client/server menu synchronization. */
@Mod.EventBusSubscriber(modid="tp", value=Dist.CLIENT)
public final class GameplayAudit {
    private static int stage=-1, ticks, screenFrames;
    public static void begin() {
        stage=0; ticks=0;
        var client=Minecraft.getInstance();
        client.options.pauseOnLostFocus=false;
        var rules=new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
        var settings=new LevelSettings("Tiny Progressions visual QA",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT);
        client.createWorldOpenFlows().createFreshLevel("tp-visual-audit-"+System.currentTimeMillis(),settings,new WorldOptions(1220201L,false,false),
                registry -> registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || stage<0) return;
        var client=Minecraft.getInstance();
        if(++ticks>2400) { TinyProgressions.LOGGER.error("Gameplay QA timed out in stage {} on {}",stage,client.screen); client.stop(); stage=-1; return; }
        if(stage==0 && client.level!=null && client.player!=null && client.getOverlay()==null && ticks>100) {
            stage=1; ticks=screenFrames=0;
            client.getSingleplayerServer().execute(() -> {
                var server=client.getSingleplayerServer();
                var player=server.getPlayerList().getPlayers().get(0);
                var level=player.serverLevel();
                level.setDayTime(6000);
                player.teleportTo(level,-2.5,83,-1.5,-30,18);
                player.getAbilities().flying=true; player.onUpdateAbilities();
                for(var pos:BlockPos.betweenClosed(new BlockPos(-8,80,-8),new BlockPos(18,80,16))) level.setBlock(pos,Blocks.STONE.defaultBlockState(),3);
                String[] blocks={"growth_block","growth_upgrade","growth_upgrade_two","cobblegen_block","iron_cobblegen_block","diamond_cobblegen_block","blaze_cobblegen_block","emerald_cobblegen_block",
                        "iron_furnace_block","ender_ore","wub_ore","nether_wub_ore","asphalt_block","reinforced_glass","fmf_block","lava_infused_stone"};
                for(int i=0;i<blocks.length;i++) level.setBlock(new BlockPos((i%8)*2,81,(i/8)*3),Content.block(blocks[i]).defaultBlockState(),3);
                for(var pos:BlockPos.betweenClosed(new BlockPos(0,81,7),new BlockPos(4,81,11))) {
                    boolean edge=pos.getX()==0 || pos.getX()==4 || pos.getZ()==7 || pos.getZ()==11;
                    level.setBlock(pos,edge ? Blocks.QUARTZ_BLOCK.defaultBlockState() : Content.block("vasholine").defaultBlockState(),3);
                }
                var furnace=(IronFurnaceBlockEntity)level.getBlockEntity(new BlockPos(0,81,3));
                furnace.inventory().setStackInSlot(0,new ItemStack(Items.IRON_ORE,64));
                furnace.inventory().setStackInSlot(1,new ItemStack(Items.COAL,32));
                NetworkHooks.openScreen(player,furnace,furnace.getBlockPos());
            });
        } else if(stage==3 && ticks>40) {
            stage=4; ticks=screenFrames=0; client.setScreen(new ArmorGalleryScreen());
        } else if(stage==5 && ticks>60) {
            capture("world-blocks-and-fluid.png");
            stage=6; ticks=0;
            client.getSingleplayerServer().execute(() -> {
                var player=client.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                player.teleportTo(player.serverLevel(),2.5,84,5.5,0,50);
                player.getAbilities().flying=true; player.onUpdateAbilities();
            });
        } else if(stage==6 && ticks>=60 && ticks%5==0) {
            capture("fluid-surface-"+ticks+".png");
            if(ticks>=80) { capture("fluid-surface.png"); stage=-1; client.stop(); }
        }
    }
    @SubscribeEvent public static void screenRendered(ScreenEvent.Render.Post event) {
        var client=Minecraft.getInstance();
        if(stage==1 && event.getScreen() instanceof IronFurnaceScreen && ++screenFrames>=45) {
            event.getGuiGraphics().flush();
            capture("iron-furnace.png"); stage=2; ticks=screenFrames=0;
            client.getSingleplayerServer().execute(() -> {
                var player=client.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                player.closeContainer();
                var pouch=Content.item("pouch").getDefaultInstance();
                var storage=pouch.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).orElseThrow(() -> new IllegalStateException("Pouch capability missing"));
                storage.insertItem(0,new ItemStack(Items.DIAMOND,12),false);
                storage.insertItem(10,Content.item("wub_hammer").getDefaultInstance(),false);
                player.setItemInHand(InteractionHand.MAIN_HAND,pouch);
                pouch.getItem().use(player.level(),player,InteractionHand.MAIN_HAND);
            });
        } else if(stage==2 && event.getScreen() instanceof ContainerScreen && ++screenFrames>=45) {
            event.getGuiGraphics().flush();
            capture("pouch.png"); stage=3; ticks=screenFrames=0;
            client.getSingleplayerServer().execute(() -> client.getSingleplayerServer().getPlayerList().getPlayers().get(0).closeContainer());
        }
    }
    public static void armorFinished() {
        var client=Minecraft.getInstance();
        stage=5; ticks=0; client.setScreen(null); client.options.hideGui=true;
        client.getSingleplayerServer().execute(() -> {
            var player=client.getSingleplayerServer().getPlayerList().getPlayers().get(0);
            player.teleportTo(player.serverLevel(),-2.5,83,-5.5,-30,18);
        });
    }
    public static void capture(String name) {
        var client=Minecraft.getInstance();
        try(var screenshot=Screenshot.takeScreenshot(client.getMainRenderTarget())) {
            Path directory=Path.of("../migration/client-qa"); Files.createDirectories(directory);
            screenshot.writeToFile(directory.resolve(name));
            TinyProgressions.LOGGER.info("Saved gameplay QA {}",name);
        } catch(Exception error) { TinyProgressions.LOGGER.error("Could not capture gameplay QA {}",name,error); }
    }
}
