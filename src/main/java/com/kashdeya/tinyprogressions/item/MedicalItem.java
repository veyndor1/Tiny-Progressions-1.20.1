package com.kashdeya.tinyprogressions.item;

import com.kashdeya.tinyprogressions.LegacyConfig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public final class MedicalItem extends Item {
    private final String kind;
    public MedicalItem(Properties properties, String kind) { super(properties); this.kind = kind; }
    @Override public int getMaxStackSize(ItemStack stack) {
        String setting=switch(kind) { case "small_bandage" -> "smallBandageHealStack"; case "large_bandage" -> "largeBandageStack"; case "small_med_kit" -> "smallMedHealStack"; default -> "LargeMedHealStack"; };
        return LegacyConfig.integer("ConfigHandler."+setting);
    }
    @Override public int getUseDuration(ItemStack stack) {
        if(kind.equals("small_bandage")) return 4;
        String setting=switch(kind) { case "large_bandage" -> "largeBandageDuration"; case "small_med_kit" -> "smallMedDuration"; default -> "LargeMedDuration"; };
        return LegacyConfig.integer("ConfigHandler."+setting);
    }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return kind.equals("small_bandage") ? UseAnim.NONE : UseAnim.BOW; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getHealth() >= player.getMaxHealth()) return InteractionResultHolder.fail(stack);
        if (!kind.equals("small_bandage")) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if(player.getAbilities().instabuild) return InteractionResultHolder.fail(stack);
        apply(stack,level,player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        if(consumer instanceof Player player && player.getHealth()<player.getMaxHealth()) apply(stack,level,player);
        return stack;
    }
    private void apply(ItemStack stack, Level level, Player player) {
        if (!level.isClientSide) {
            if (kind.equals("small_bandage")) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                        LegacyConfig.integer("ConfigHandler.smallBandageRegen") * 20, 0,false,false));
            } else {
                String setting = switch(kind) {
                    case "large_bandage" -> "largeBandageBoostTime";
                    case "small_med_kit" -> "smallMedBoostTime";
                    default -> "largeMedBoostTime";
                };
                int amplifier = kind.equals("large_med_kit") ? 2 : kind.equals("small_med_kit") ? 1 : 0;
                player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST,
                        LegacyConfig.integer("ConfigHandler." + setting) * 20, amplifier,false,false));
                player.addEffect(new MobEffectInstance(MobEffects.HEAL,20,0,false,false));
                level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC,net.minecraft.sounds.SoundSource.PLAYERS,1,.1F);
                if(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) net.minecraft.advancements.CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer,stack);
            }
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<net.minecraft.network.chat.Component> tooltip,TooltipFlag flag) {
        String[] keys=switch(kind) {
            case "small_bandage" -> new String[]{"tooltip.bandage_1","tooltip.bandage_2"};
            case "large_bandage" -> new String[]{"tooltip.medkit_1","tooltip.largebandage_1"};
            case "small_med_kit" -> new String[]{"tooltip.medkit_1","tooltip.smallmedkit_2"};
            default -> new String[]{"tooltip.medkit_1","tooltip.largemedkit_2"};
        };
        for(String key:keys) tooltip.add(net.minecraft.network.chat.Component.translatable(key).withStyle(net.minecraft.ChatFormatting.YELLOW));
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.medkits").withStyle(net.minecraft.ChatFormatting.RED));
    }
}
