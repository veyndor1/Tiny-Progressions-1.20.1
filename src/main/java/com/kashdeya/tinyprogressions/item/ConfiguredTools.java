package com.kashdeya.tinyprogressions.item;

import com.kashdeya.tinyprogressions.Content;
import net.minecraft.world.item.*;
import net.minecraftforge.common.extensions.IForgeItem;

/** Loaded server settings override registry-time tool durability through stack-aware Forge hooks. */
public final class ConfiguredTools {
    public interface Durable extends IForgeItem {
        @Override default int getMaxDamage(ItemStack stack) { return Content.configuredDurability(stack,((Item)this).getMaxDamage()); }
    }
    public static final class Pickaxe extends PickaxeItem implements Durable {
        public Pickaxe(Tier tier,int damage,float speed,Item.Properties properties) { super(tier,damage,speed,properties); }
    }
    public static final class Sword extends SwordItem implements Durable {
        public Sword(Tier tier,int damage,float speed,Item.Properties properties) { super(tier,damage,speed,properties); }
    }
    public static final class Axe extends AxeItem implements Durable {
        public Axe(Tier tier,float damage,float speed,Item.Properties properties) { super(tier,damage,speed,properties); }
    }
    public static final class Shovel extends ShovelItem implements Durable {
        public Shovel(Tier tier,float damage,float speed,Item.Properties properties) { super(tier,damage,speed,properties); }
    }
    public static final class Hoe extends HoeItem implements Durable {
        public Hoe(Tier tier,int damage,float speed,Item.Properties properties) { super(tier,damage,speed,properties); }
        @Override public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot slot,ItemStack stack) {
            if(slot!=net.minecraft.world.entity.EquipmentSlot.MAINHAND) return super.getAttributeModifiers(slot,stack);
            // ItemHoe in 1.12.2 supplied zero damage and material damage minus three for attack speed.
            return com.google.common.collect.ImmutableMultimap.<net.minecraft.world.entity.ai.attributes.Attribute,net.minecraft.world.entity.ai.attributes.AttributeModifier>builder()
                    .put(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_UUID,"Tool modifier",0,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION))
                    .put(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_SPEED_UUID,"Tool modifier",getTier().getAttackDamageBonus()-3.0,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION))
                    .build();
        }
    }
    public static final class Shears extends ShearsItem implements Durable {
        public Shears(Item.Properties properties) { super(properties); }
    }
}
