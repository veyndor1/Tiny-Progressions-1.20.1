package com.kashdeya.tinyprogressions;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.*;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,"tp");
    public static final RegistryObject<SoundEvent> SUCC=SOUNDS.register("succ.the.pearls",() -> SoundEvent.createVariableRangeEvent(ResourceLocation.tryBuild("tp","succ.the.pearls")));
}
