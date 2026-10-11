package com.kashdeya.tinyprogressions.gametest;

import com.kashdeya.tinyprogressions.ArmorEffects;
import com.kashdeya.tinyprogressions.Content;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder("tp")
@PrefixGameTestTemplate(false)
public final class ArmorTests {
    @GameTest(template="empty") public static void flightOwnershipSurvivesLoginAndClearsOnDeath(GameTestHelper helper) {
        var original=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.fromString("d9bdf701-1000-4000-8000-000000000010"),"TPFlightSave"));
        original.setGameMode(GameType.SURVIVAL); original.setHealth(20); original.getAbilities().mayfly=false;
        equip(original,"dragon"); ArmorEffects.update(original);
        var saved=original.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        var restored=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.fromString("d9bdf701-1000-4000-8000-000000000011"),"TPFlightLogin"));
        restored.load(saved);
        // FakePlayer has no network channel; invoke our login listener without Forge's packet-sync listeners.
        ArmorEffects.login(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(restored));
        helper.assertTrue(restored.getAbilities().mayfly,"Saved armor and flight must survive login");
        restored.setHealth(0); ArmorEffects.update(restored);
        helper.assertTrue(!restored.getAbilities().mayfly,"A dead survival player must release armor flight");
        restored.setHealth(20); equip(restored,"dragon");
        ArmorEffects.respawn(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent(restored,false));
        helper.assertTrue(restored.getAbilities().mayfly,"Respawning with a complete set must restore armor flight");
        var other=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.fromString("d9bdf701-1000-4000-8000-000000000012"),"TPFlightOther"));
        other.setGameMode(GameType.SURVIVAL); other.getAbilities().mayfly=false;
        helper.assertTrue(!other.getAbilities().mayfly,"Flight ownership must not leak to another player");
        restored.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY); ArmorEffects.update(restored);
        helper.assertTrue(!restored.getAbilities().mayfly,"Restored flight ownership must be revoked when armor is removed");
        helper.succeed();
    }
    private static void equip(net.minecraft.world.entity.player.Player player,String family) {
        player.setItemSlot(EquipmentSlot.HEAD,Content.item(family+"_helmet").getDefaultInstance());
        player.setItemSlot(EquipmentSlot.CHEST,Content.item(family+"_chestplate").getDefaultInstance());
        player.setItemSlot(EquipmentSlot.LEGS,Content.item(family+"_leggings").getDefaultInstance());
        player.setItemSlot(EquipmentSlot.FEET,Content.item(family+"_boots").getDefaultInstance());
    }
    @GameTest(template="empty") public static void dragonFlightRequiresCompleteSetAndReleasesOwnPermission(GameTestHelper helper) {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.fromString("d9bdf701-1000-4000-8000-000000000001"),"TPDragonTest"));
        player.setGameMode(GameType.SURVIVAL); player.getAbilities().mayfly=false;
        equip(player,"dragon"); ArmorEffects.update(player);
        helper.assertTrue(player.getAbilities().mayfly,"Full dragon set must grant flight");
        helper.assertTrue(player.hasEffect(MobEffects.FIRE_RESISTANCE) && player.hasEffect(MobEffects.DAMAGE_BOOST),"Dragon set must apply configured effects");
        player.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY); ArmorEffects.update(player);
        helper.assertTrue(!player.getAbilities().mayfly && !player.getAbilities().flying,"Taking off a piece must revoke flight introduced by this set");
        helper.succeed();
    }
    @GameTest(template="empty") public static void removingArmorPreservesPreexistingFlight(GameTestHelper helper) {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.fromString("d9bdf701-1000-4000-8000-000000000002"),"TPFlightTest"));
        player.setGameMode(GameType.SURVIVAL); player.getAbilities().mayfly=true;
        equip(player,"dragon"); ArmorEffects.update(player);
        player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY); ArmorEffects.update(player);
        helper.assertTrue(player.getAbilities().mayfly,"Preexisting flight must remain when dragon armor is removed");
        player.setGameMode(GameType.CREATIVE); ArmorEffects.update(player);
        helper.assertTrue(player.getAbilities().mayfly,"Creative flight must be preserved");
        helper.succeed();
    }
    @GameTest(template="empty") public static void incompleteLapisSetGivesNoWaterBreathing(GameTestHelper helper) {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.fromString("d9bdf701-1000-4000-8000-000000000003"),"TPLapisTest"));
        player.setGameMode(GameType.SURVIVAL); player.removeAllEffects();
        equip(player,"lapis"); player.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY); ArmorEffects.update(player);
        helper.assertTrue(!player.hasEffect(MobEffects.WATER_BREATHING),"Incomplete lapis set must not apply its full-set effect");
        player.setItemSlot(EquipmentSlot.FEET,Content.item("lapis_boots").getDefaultInstance()); ArmorEffects.update(player);
        helper.assertTrue(player.hasEffect(MobEffects.WATER_BREATHING),"Complete lapis set must give water breathing");
        helper.succeed();
    }
}
