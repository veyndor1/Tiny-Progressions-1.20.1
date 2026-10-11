package com.kashdeya.tinyprogressions.loot;

import com.kashdeya.tinyprogressions.Content;
import com.kashdeya.tinyprogressions.LegacyConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.*;

public final class LegacyBlockDrops extends LootModifier {
    public static final Codec<LegacyBlockDrops> CODEC=RecordCodecBuilder.create(instance -> codecStart(instance).apply(instance,LegacyBlockDrops::new));
    public LegacyBlockDrops(LootItemCondition[] conditions) { super(conditions); }
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context) {
        var state=context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if(state==null) return loot;
        int trigger=context.getRandom().nextInt(1000);
        if(state.is(BlockTags.LEAVES)) {
            add(loot,trigger,"extra_drops","peachDropsChance","peachDropsAmount",Content.item("plump_peach"));
            add(loot,trigger,"extra_drops","pearDropsChance","pearDropsAmount",Content.item("plump_pear"));
            add(loot,trigger,"extra_drops_vanilla","appleDropsChance","appleDropsAmount",Items.APPLE);
            add(loot,trigger,"extra_drops_vanilla","stickDropsChance","stickDropsAmount",Items.STICK);
        }
        if(state.is(BlockTags.DIRT)) {
            add(loot,trigger,"BoneDrops","BoneDropsChance","BoneAmount",Items.BONE);
            add(loot,trigger,"SkullDrops","SkullDropsChance","SkullAmount",Items.SKELETON_SKULL);
        }
        return loot;
    }
    private static void add(ObjectArrayList<ItemStack> loot,int trigger,String enabled,String chance,String amount,Item item) {
        if(LegacyConfig.enabled("ConfigHandler."+enabled) && trigger<LegacyConfig.integer("ConfigHandler."+chance)) {
            int remaining=LegacyConfig.integer("ConfigHandler."+amount);
            while(remaining>0) { int count=Math.min(remaining,item.getMaxStackSize()); loot.add(new ItemStack(item,count)); remaining-=count; }
        }
    }
    @Override public Codec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
