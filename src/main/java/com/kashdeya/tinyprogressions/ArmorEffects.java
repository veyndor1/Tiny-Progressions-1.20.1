package com.kashdeya.tinyprogressions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="tp")
public final class ArmorEffects {
    private static final String OWNED_FLIGHT="tp:dragon_flight_owned";
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if(event.phase==TickEvent.Phase.END && !event.player.level().isClientSide) update(event.player);
    }
    @SubscribeEvent public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) { update(event.getEntity()); }
    @SubscribeEvent public static void respawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) { update(event.getEntity()); }
    @SubscribeEvent public static void dimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) { update(event.getEntity()); }
    public static boolean fullSet(Player player,String family) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(Content.item(family+"_helmet"))
                && player.getItemBySlot(EquipmentSlot.CHEST).is(Content.item(family+"_chestplate"))
                && player.getItemBySlot(EquipmentSlot.LEGS).is(Content.item(family+"_leggings"))
                && player.getItemBySlot(EquipmentSlot.FEET).is(Content.item(family+"_boots"));
    }
    private static boolean hasPiece(Player player,String family) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(Content.item(family+"_helmet"))
                || player.getItemBySlot(EquipmentSlot.CHEST).is(Content.item(family+"_chestplate"))
                || player.getItemBySlot(EquipmentSlot.LEGS).is(Content.item(family+"_leggings"))
                || player.getItemBySlot(EquipmentSlot.FEET).is(Content.item(family+"_boots"));
    }
    private static boolean active(Player player,String family,String config) {
        return LegacyConfig.enabled("ArmorHandler."+config) && (fullSet(player,family)
                || (player.isCreative() || player.isSpectator()) && hasPiece(player,family));
    }
    private static void effect(Player player,MobEffect effect,String setting) {
        if(LegacyConfig.enabled("ArmorHandler."+setting))
            player.addEffect(new MobEffectInstance(effect,180,LegacyConfig.integer("ArmorHandler."+setting+"_lvl"),false,false));
    }
    public static void update(Player player) {
        if(player.level().isClientSide) return;
        if(hasPiece(player,"stone")) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,180,0,false,false));
        if(active(player,"lapis","lapis_armor")) effect(player,MobEffects.WATER_BREATHING,"lapis_armor_water");
        if(active(player,"quartz","quartz_armor")) effect(player,MobEffects.DAMAGE_BOOST,"quartz_armor_strength");
        if(active(player,"redstone","redstone_armor")) effect(player,MobEffects.MOVEMENT_SPEED,"redstone_armor_speed");
        if(active(player,"obsidian","obsidian_armor")) effect(player,MobEffects.DAMAGE_RESISTANCE,"obsidian_armor_resistance");
        if(active(player,"lava","lava_armor")) {
            effect(player,MobEffects.FIRE_RESISTANCE,"lava_armor_fire");
            effect(player,MobEffects.DAMAGE_RESISTANCE,"lava_armor_resistance");
        }
        if(active(player,"wither","wither_armor")) {
            effect(player,MobEffects.DAMAGE_BOOST,"wither_strength");
            effect(player,MobEffects.FIRE_RESISTANCE,"wither_fire");
            effect(player,MobEffects.DAMAGE_RESISTANCE,"wither_resistance");
        }
        boolean dragon=active(player,"dragon","dragon_armor");
        if(dragon) {
            effect(player,MobEffects.DAMAGE_BOOST,"dragon_strength");
            effect(player,MobEffects.FIRE_RESISTANCE,"dragon_fire");
            effect(player,MobEffects.DAMAGE_RESISTANCE,"dragon_resistance");
        }
        boolean flight=player.isAlive() && dragon && LegacyConfig.enabled("ArmorHandler.dragon_fly");
        var abilities=player.getAbilities();
        var data=player.getPersistentData();
        if(flight && !abilities.mayfly) {
            // Remember only permissions this mod introduced; never claim pre-existing flight.
            data.putBoolean(OWNED_FLIGHT,true);
            abilities.mayfly=true;
            player.onUpdateAbilities();
        } else if(!flight && data.getBoolean(OWNED_FLIGHT)) {
            data.remove(OWNED_FLIGHT);
            if(!player.isCreative() && !player.isSpectator()) {
                abilities.mayfly=false;
                abilities.flying=false;
                player.onUpdateAbilities();
            }
        }
    }
}
