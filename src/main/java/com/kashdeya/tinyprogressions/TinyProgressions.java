package com.kashdeya.tinyprogressions;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.core.registries.Registries;
import org.slf4j.Logger;

@Mod(TinyProgressions.MOD_ID)
public final class TinyProgressions {
    public static final String MOD_ID = "tp";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("tiny_progressions", () ->
            CreativeModeTab.builder().title(Component.translatable("itemGroup.tp"))
                    .icon(() -> Content.item("growth_block").getDefaultInstance())
                    .displayItems((parameters, output) -> Content.ITEMS.getEntries().stream()
                            .filter(item -> Content.enabled(item.getId().getPath()))
                            .forEach(item -> output.accept(item.get())))
                    .build());

    public TinyProgressions(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        context.registerConfig(ModConfig.Type.SERVER, LegacyConfig.SPEC, "tinyprogressions-server.toml");
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> CraftingHelper.register(new FeatureCondition.Serializer())));
        ModFluids.initialize();
        Content.initialize();
        Content.BLOCKS.register(bus);
        Content.ITEMS.register(bus);
        Content.BLOCK_ENTITIES.register(bus);
        com.kashdeya.tinyprogressions.menu.ModMenus.MENUS.register(bus);
        com.kashdeya.tinyprogressions.worldgen.ModWorldgen.FEATURES.register(bus);
        com.kashdeya.tinyprogressions.loot.ModLoot.MODIFIERS.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModParticles.PARTICLES.register(bus);
        ModFluids.TYPES.register(bus);
        ModFluids.FLUIDS.register(bus);
        TABS.register(bus);
    }
}
