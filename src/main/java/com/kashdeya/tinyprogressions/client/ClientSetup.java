package com.kashdeya.tinyprogressions.client;

import com.kashdeya.tinyprogressions.menu.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid="tp",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) { event.enqueueWork(() -> MenuScreens.register(ModMenus.IRON_FURNACE.get(),IronFurnaceScreen::new)); }
}
