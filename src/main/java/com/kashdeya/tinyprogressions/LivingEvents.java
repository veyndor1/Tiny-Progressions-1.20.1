package com.kashdeya.tinyprogressions;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="tp")
public final class LivingEvents {
    @SubscribeEvent public static void beforeHammerBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        var player=event.getPlayer(); var stack=player.getMainHandItem();
        if(!(stack.getItem() instanceof com.kashdeya.tinyprogressions.item.WubHammerItem) || com.kashdeya.tinyprogressions.item.WubHammerItem.areaMining() || player.isCreative()) return;
        var hit=player.pick(player.getAttributeValue(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get()),1,false);
        if(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit && blockHit.getBlockPos().equals(event.getPos()))
            com.kashdeya.tinyprogressions.item.WubHammerItem.recordFace(stack,event.getPos(),blockHit.getDirection());
    }
    @SubscribeEvent public static void drops(LivingDropsEvent event) {
        if(event.getEntity().level().isClientSide) return;
        ItemStack drop=ItemStack.EMPTY;
        var entity=event.getEntity();
        if(entity instanceof EnderDragon && LegacyConfig.enabled("ArmorHandler.dragon_armor")) drop=new ItemStack(Content.item("dragon_scale"),entity.getRandom().nextInt(16));
        else if(entity instanceof WitherBoss && LegacyConfig.enabled("ConfigHandler.wither_rib")) drop=new ItemStack(Content.item("wither_rib"),entity.getRandom().nextInt(6));
        if(!drop.isEmpty()) event.getDrops().add(new ItemEntity(entity.level(),entity.getX(),entity.getY(),entity.getZ(),drop));
    }
    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent event) {
        var entity=event.getEntity(); var level=entity.level(); var pos=entity.blockPosition();
        if(level.getBlockState(pos).is(Content.block("quick_sand")) || level.getBlockState(pos.below()).is(Content.block("quick_sand"))) {
            var movement=entity.getDeltaMovement(); entity.setDeltaMovement(movement.x,0,movement.z);
        }
    }
}
