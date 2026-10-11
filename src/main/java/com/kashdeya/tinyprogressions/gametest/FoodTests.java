package com.kashdeya.tinyprogressions.gametest;

import com.kashdeya.tinyprogressions.*;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder("tp")
@PrefixGameTestTemplate(false)
public final class FoodTests {
    @GameTest(template="empty") public static void medicalKitHealsOnlyAfterCompletedUse(GameTestHelper helper) {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.fromString("d9bdf701-1000-4000-8000-000000000007"),"TPMedicalTest"));
        player.setGameMode(GameType.SURVIVAL); player.setHealth(5);
        var kit=Content.item("small_med_kit").getDefaultInstance(); kit.setCount(2);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,kit);
        helper.assertTrue(kit.getItem().use(helper.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND).getResult().consumesAction(),"Kit must start use");
        helper.assertTrue(player.getHealth()==5 && kit.getCount()==2,"Starting treatment must consume nothing and apply no healing");
        player.stopUsingItem();
        helper.assertTrue(!player.hasEffect(net.minecraft.world.effect.MobEffects.HEALTH_BOOST),"Interrupted treatment must apply no effect");
        kit.finishUsingItem(helper.getLevel(),player);
        var healing=player.getEffect(net.minecraft.world.effect.MobEffects.HEAL);
        helper.assertTrue(healing!=null && healing.getDuration()==20 && kit.getCount()==1 && player.hasEffect(net.minecraft.world.effect.MobEffects.HEALTH_BOOST),"Completed treatment must start the original twenty healing ticks and consume exactly one kit");
        for(int i=0;i<20;i++) healing.tick(player,() -> {});
        helper.assertTrue(player.getHealth()==player.getMaxHealth(),"The original medical treatment must heal up to the boosted maximum");
        helper.succeed();
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void set(String key,Object value) { ((ForgeConfigSpec.ConfigValue)LegacyConfig.SPEC.getValues().get(java.util.Arrays.asList(key.split("\\.")))).set(value); }
    @GameTest(template="empty") public static void nutritionAndBottleSettingsApplyToActualConsumption(GameTestHelper helper) {
        String[] keys={"ConfigHandler.AppleAmount","ConfigHandler.AppleSaturation","ConfigHandler.JuiceBottles","ConfigHandler.succ_juice_bottle"};
        Object[] saved=java.util.Arrays.stream(keys).map(LegacyConfig::value).toArray();
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.fromString("d9bdf701-1000-4000-8000-000000000006"),"TPFoodTest"));
        player.setGameMode(GameType.SURVIVAL);
        try {
            set(keys[0],7); set(keys[1],.4); set(keys[2],false); set(keys[3],true);
            player.getFoodData().setFoodLevel(0);
            var juice=Content.item("apple_juice").getDefaultInstance();
            helper.assertTrue(juice.finishUsingItem(helper.getLevel(),player).isEmpty(),"Disabled bottles must not return a glass bottle");
            helper.assertTrue(player.getFoodData().getFoodLevel()==7,"Consumption must use configured nutrition rather than registry defaults");
            var succ=Content.item("succ_pearls").getDefaultInstance();
            helper.assertTrue(succ.finishUsingItem(helper.getLevel(),player).is(Items.GLASS_BOTTLE),"Succ bottle setting must work independently of ordinary juice bottles");
            helper.assertTrue(ModSounds.SUCC.get().getLocation().toString().equals("tp:succ.the.pearls"),"Original supporter sound must be registered");
        } finally { for(int i=0;i<keys.length;i++) set(keys[i],saved[i]); }
        helper.succeed();
    }
}
