package com.kashdeya.tinyprogressions;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.function.Supplier;
import java.util.HashMap;
import java.util.Map;

/** Numeric material values retained from the 1.12.2 MaterialHandler. */
public final class Materials {
    private static final Map<String, Tier> TIERS = new HashMap<>();
    private static final Map<String, ArmorMaterial> ARMOR = new HashMap<>();
    private static Tier sorted(String name, int level, int uses, float speed, float damage, int enchantment, Supplier<Ingredient> repair) {
        var id = ResourceLocation.tryBuild("tp", name);
        var tag = TagKey.create(Registries.BLOCK, ResourceLocation.tryBuild("tp", "needs_" + name + "_tool"));
        Tier result = new ForgeTier(level,uses,speed,damage,enchantment,tag,repair);
        Tier[] vanilla = {Tiers.WOOD,Tiers.STONE,Tiers.IRON,Tiers.DIAMOND,Tiers.NETHERITE};
        int index = Math.min(level,4);
        return TierSortingRegistry.registerTier(result,id,List.of(vanilla[index]),index < 4 ? List.of(vanilla[index+1]) : List.of());
    }
    public static Tier tier(String name) {
        return TIERS.computeIfAbsent(name, key -> switch (key) {
            case "WOOD" -> Tiers.WOOD;
            case "STONE" -> Tiers.STONE;
            case "IRON" -> Tiers.IRON;
            case "GOLD" -> Tiers.GOLD;
            case "DIAMOND" -> Tiers.DIAMOND;
            case "BONE" -> sorted("bone", 0, 49, 1.0F, 0.25F, 15, () -> Ingredient.of(Items.BONE));
            case "FLINT" -> sorted("flint", 0, 89, 3.0F, 0.5F, 15, () -> Ingredient.of(Items.FLINT));
            case "BIRTHDAY" -> sorted("birthday", 4, 3061, 100.0F, 10.0F, 22, () -> Ingredient.of(net.minecraft.world.level.block.Blocks.OBSIDIAN));
            case "OBSIDIAN" -> sorted("obsidian", 4, 1961, 9.0F, 4.0F, 10, () -> Ingredient.of(net.minecraft.world.level.block.Blocks.OBSIDIAN));
            case "EMERALD" -> sorted("emerald", 4, 2961, 10.0F, 5.0F, 22, () -> Ingredient.of(Items.EMERALD));
            case "KAPPA" -> sorted("kappa", 1, 31513, 5.0F, 2.0F, 22, () -> Ingredient.EMPTY);
            case "BSCIRON" -> sorted("bsciron", 3, 2750, 12.0F, 8.0F, 14, () -> Ingredient.of(Items.IRON_INGOT));
            case "BSCDIAMOND" -> sorted("bscdiamond", 3, 4549, 16.0F, 5.0F, 10, () -> Ingredient.of(Items.DIAMOND));
            case "BSCGOLD" -> sorted("bscgold", 0, 788, 16.0F, 2.0F, 22, () -> Ingredient.of(Items.GOLD_INGOT));
            case "WUBWUB" -> sorted("wubwub", 4, 3261, 12.0F, 7.0F, 22, () -> Ingredient.of(Content.item("wub_ingot")));
            case "PWOOD" -> sorted("pwood", 0, 59, 2.0F, 5.0F, 15, () -> Ingredient.EMPTY);
            case "PSTONE" -> sorted("pstone", 1, 131, 4.0F, 7.0F, 5, () -> Ingredient.EMPTY);
            case "PIRON" -> sorted("piron", 2, 250, 6.0F, 8.0F, 14, () -> Ingredient.EMPTY);
            case "PDIAMOND" -> sorted("pdiamond", 3, 1561, 8.0F, 10.0F, 10, () -> Ingredient.EMPTY);
            case "PGOLD" -> sorted("pgold", 0, 32, 5.0F, 1.2F, 22, () -> Ingredient.EMPTY);
            case "POBSIDIAN" -> sorted("pobsidian", 4, 3000, 9.0F, 11.0F, 10, () -> Ingredient.of(net.minecraft.world.level.block.Blocks.OBSIDIAN));
            case "PEMERALD" -> sorted("pemerald", 4, 4000, 10.0F, 12.0F, 22, () -> Ingredient.of(Items.EMERALD));
            case "PFLINT" -> sorted("pflint", 0, 89, 4.0F, 5.5F, 15, () -> Ingredient.of(Items.FLINT));
            case "PWUBWUB" -> sorted("pwubwub", 4, 4250, 12.0F, 14.0F, 22, () -> Ingredient.of(Content.item("wub_ingot")));
            case "SEAIRON" -> sorted("seairon", 3, 2750, 10.0F, 10.0F, 22, () -> Ingredient.of(Items.IRON_INGOT));
            default -> throw new IllegalArgumentException("Unknown tier " + key);
        });
    }
    public static ArmorMaterial armor(String name) {
        return ARMOR.computeIfAbsent(name, key -> switch (key) {
            case "stoneArmourMaterial" -> new LegacyArmor("tp:stone", 15, new int[] {1, 3, 3, 2}, 10, 0.05F, SoundEvents.ARMOR_EQUIP_GENERIC, () -> Ingredient.of(net.minecraft.world.level.block.Blocks.STONE));
            case "flintArmourMaterial" -> new LegacyArmor("tp:flint", 12, new int[] {1, 3, 3, 1}, 10, 0.01F, SoundEvents.ARMOR_EQUIP_GENERIC, () -> Ingredient.of(Items.FLINT));
            case "boneArmourMaterial" -> new LegacyArmor("tp:bone", 8, new int[] {1, 3, 2, 1}, 8, 0.02F, SoundEvents.ARMOR_EQUIP_GENERIC, () -> Ingredient.of(Items.BONE));
            case "woodArmourMaterial" -> new LegacyArmor("tp:wood", 5, new int[] {1, 2, 1, 1}, 5, 0.01F, SoundEvents.ARMOR_EQUIP_GENERIC, () -> Ingredient.of(net.minecraft.world.level.block.Blocks.OAK_PLANKS));
            case "lavaArmorMaterial" -> new LegacyArmor("tp:lava", 50, new int[] {5, 8, 10, 5}, 35, 5.0F, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(Items.MAGMA_CREAM));
            case "witherArmorMaterial" -> new LegacyArmor("tp:wither", 100, new int[] {6, 9, 11, 6}, 40, 15.0F, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(Items.NETHER_STAR));
            case "dragonArmorMaterial" -> new LegacyArmor("tp:dragon", 200, new int[] {7, 10, 12, 7}, 75, 20.0F, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(Items.END_CRYSTAL));
            case "obsidianArmorMaterial" -> new LegacyArmor("tp:obsidian", 33, new int[] {3, 6, 8, 3}, 10, 3.0F, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(net.minecraft.world.level.block.Blocks.OBSIDIAN));
            case "redstoneArmourMaterial" -> new LegacyArmor("tp:redstone", 20, new int[] {1, 4, 5, 1}, 20, 2.25F, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(Items.REDSTONE));
            case "lapisArmourMaterial" -> new LegacyArmor("tp:lapis", 20, new int[] {1, 4, 5, 3}, 20, 1.5F, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(Items.LAPIS_LAZULI));
            case "quartzArmourMaterial" -> new LegacyArmor("tp:quartz", 20, new int[] {1, 4, 5, 2}, 12, 1.2F, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(Items.QUARTZ));
            case "emeraldArmourMaterial" -> new LegacyArmor("tp:emerald", 43, new int[] {4, 7, 9, 4}, 25, 4.0F, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(Items.EMERALD));
            case "bamArmourMaterial" -> new LegacyArmor("tp:bam_tshirt", 43, new int[] {4, 7, 9, 4}, 25, 4.5F, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(Items.EMERALD));
            default -> throw new IllegalArgumentException("Unknown armor material " + key);
        });
    }
    private record LegacyArmor(String name, int multiplier, int[] defense, int enchantment, float toughness, SoundEvent sound, Supplier<Ingredient> repair)
            implements ArmorMaterial {
        @Override public int getDurabilityForType(ArmorItem.Type type) {
            return switch(type) {
                case BOOTS -> 13 * multiplier;
                case LEGGINGS -> 15 * multiplier;
                case CHESTPLATE -> 16 * multiplier;
                case HELMET -> 11 * multiplier;
            };
        }
        @Override public int getDefenseForType(ArmorItem.Type type) {
            return defense[switch(type) {case BOOTS -> 0; case LEGGINGS -> 1; case CHESTPLATE -> 2; case HELMET -> 3;}];
        }
        @Override public int getEnchantmentValue() { return enchantment; }
        @Override public SoundEvent getEquipSound() { return sound; }
        @Override public Ingredient getRepairIngredient() { return repair.get(); }
        @Override public String getName() { return name; }
        @Override public float getToughness() { return toughness; }
        @Override public float getKnockbackResistance() { return 0; }
    }
}
