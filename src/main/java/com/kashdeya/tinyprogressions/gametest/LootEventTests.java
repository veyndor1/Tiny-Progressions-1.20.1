package com.kashdeya.tinyprogressions.gametest;

import com.kashdeya.tinyprogressions.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.*;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.gametest.*;

@GameTestHolder("tp")
@PrefixGameTestTemplate(false)
public final class LootEventTests {
    @SuppressWarnings({"unchecked","rawtypes"}) private static void set(String key,Object value) {
        ((ForgeConfigSpec.ConfigValue)LegacyConfig.SPEC.getValues().get(java.util.Arrays.asList(key.split("\\.")))).set(value);
    }
    @GameTest(template="empty") public static void globalDropsHonorSettingsAndSplitLargeStacks(GameTestHelper helper) {
        String[] keys={"extra_drops","extra_drops_vanilla","pearDropsChance","peachDropsChance","appleDropsChance","stickDropsChance","pearDropsAmount","BoneDrops","BoneDropsChance","BoneAmount"};
        Object[] previous=java.util.Arrays.stream(keys).map(k -> LegacyConfig.value("ConfigHandler."+k)).toArray();
        try {
            set("ConfigHandler.extra_drops",true); set("ConfigHandler.extra_drops_vanilla",false);
            for(String chance:new String[]{"pearDropsChance","peachDropsChance","appleDropsChance","stickDropsChance","BoneDropsChance"}) set("ConfigHandler."+chance,1000);
            set("ConfigHandler.pearDropsAmount",130); set("ConfigHandler.BoneDrops",true); set("ConfigHandler.BoneAmount",70);
            var pos=helper.absolutePos(new BlockPos(2,2,2));
            var leaves=Block.getDrops(Blocks.OAK_LEAVES.defaultBlockState(),helper.getLevel(),pos,null);
            helper.assertTrue(leaves.stream().filter(s -> s.is(Content.item("plump_pear"))).mapToInt(ItemStack::getCount).sum()==130,"Fruit setting must control actual global loot count");
            helper.assertTrue(leaves.stream().allMatch(s -> s.getCount()<=s.getMaxStackSize()),"Large configured drops must split into legal stacks");
            set("ConfigHandler.extra_drops",false);
            var disabled=Block.getDrops(Blocks.OAK_LEAVES.defaultBlockState(),helper.getLevel(),pos,null);
            helper.assertTrue(disabled.stream().noneMatch(s -> s.is(Content.item("plump_pear")) || s.is(Content.item("plump_peach"))),"Disabled extra fruit must not appear");
            var dirt=Block.getDrops(Blocks.DIRT.defaultBlockState(),helper.getLevel(),pos,null);
            helper.assertTrue(dirt.stream().filter(s -> s.is(Items.BONE)).mapToInt(ItemStack::getCount).sum()==70,"Dirt bone setting must affect runtime loot");
            helper.succeed();
        } finally { for(int i=0;i<keys.length;i++) set("ConfigHandler."+keys[i],previous[i]); }
    }
    @GameTest(template="empty") public static void bossDropsRespectFlagsAndOriginalCountRanges(GameTestHelper helper) {
        Object dragonSetting=LegacyConfig.value("ArmorHandler.dragon_armor"), witherSetting=LegacyConfig.value("ConfigHandler.wither_rib");
        try {
            set("ArmorHandler.dragon_armor",true); set("ConfigHandler.wither_rib",true);
            var dragon=EntityType.ENDER_DRAGON.create(helper.getLevel());
            var wither=EntityType.WITHER.create(helper.getLevel());
            for(var mob:new net.minecraft.world.entity.LivingEntity[]{dragon,wither}) {
                int total=0;
                for(int i=0;i<32;i++) {
                    var drops=new java.util.ArrayList<ItemEntity>();
                    MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(mob,helper.getLevel().damageSources().generic(),drops,0,true));
                    int count=drops.stream().mapToInt(e -> e.getItem().getCount()).sum(); total+=count;
                    helper.assertTrue(count>=0 && count<=(mob==dragon ? 15 : 5),"Boss drops must retain legacy random bounds");
                }
                helper.assertTrue(total>0,"Enabled boss drops must produce their resource");
            }
            set("ArmorHandler.dragon_armor",false); set("ConfigHandler.wither_rib",false);
            for(var mob:new net.minecraft.world.entity.LivingEntity[]{dragon,wither}) {
                var drops=new java.util.ArrayList<ItemEntity>();
                MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(mob,helper.getLevel().damageSources().generic(),drops,0,true));
                helper.assertTrue(drops.isEmpty(),"Disabled boss drop must not be emitted");
            }
            helper.succeed();
        } finally { set("ArmorHandler.dragon_armor",dragonSetting); set("ConfigHandler.wither_rib",witherSetting); }
    }
    @GameTest(template="empty") public static void hardenedBlocksSurviveExplosionFinalization(GameTestHelper helper) {
        var position=helper.absolutePos(new BlockPos(3,2,3));
        var explosion=new Explosion(helper.getLevel(),null,position.getX(),position.getY(),position.getZ(),4,false,Explosion.BlockInteraction.DESTROY);
        String[] names={"hardened_stone","reinforced_glass","reinforced_obsidian"};
        for(int i=0;i<names.length;i++) {
            var pos=position.offset(i,0,0); helper.getLevel().setBlockAndUpdate(pos,Content.block(names[i]).defaultBlockState());
            explosion.getToBlow().add(pos);
        }
        explosion.finalizeExplosion(false);
        for(int i=0;i<names.length;i++) helper.assertTrue(helper.getLevel().getBlockState(position.offset(i,0,0)).is(Content.block(names[i])),"Explosion must preserve protected block "+names[i]);
        helper.succeed();
    }
}
