package com.kashdeya.tinyprogressions.loot;

import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.*;

public final class ModLoot {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> MODIFIERS=DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,"tp");
    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> BLOCK_DROPS=MODIFIERS.register("legacy_block_drops",() -> LegacyBlockDrops.CODEC);
}
