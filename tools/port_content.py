"""Generate registrations and convert legacy assets without editing the source archive.

Generated registrations provide identifiers and assets; gameplay behavior is
implemented in the mod's Java classes.
"""
from pathlib import Path
import json
import re
import shutil
from inventory import ROOT, LEGACY, JAVA, collect, split_args, strip_comments

RESOURCE = ROOT / 'src/main/resources'
ASSETS = RESOURCE / 'assets/tp'
SOURCE_ASSETS = LEGACY / 'src/main/resources/assets/tp'
PACKAGE = ROOT / 'src/main/java/com/kashdeya/tinyprogressions'
catalog = collect()
entries = catalog['entries']
defaults = catalog['config_defaults']


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def value(expression):
    if expression in defaults:
        expression = defaults[expression]['default']
    expression = re.sub(r'[FfLl]$', '', expression)
    try:
        return float(expression) if '.' in expression else int(expression)
    except ValueError:
        raise ValueError(f'Unresolved numeric expression: {expression}')


def q(text):
    return json.dumps(text)


def legacy_class(entry):
    cls = entry['assignments'][0]['class']
    paths = list(JAVA.rglob(cls + '.java'))
    return strip_comments(paths[0].read_text(encoding='utf-8-sig')) if paths else ''


def expand_metadata():
    expanded = []
    mappings = {}
    names = {'colored_glowstone': 'EnumGlowstoneColor', 'colored_lamp': 'EnumLampColor', 'colored_dust': 'EnumDustColor'}
    for entry in entries:
        if entry['id'] not in names:
            expanded.append(entry)
            continue
        enum = (JAVA / f'properties/{names[entry["id"]]}.java').read_text(encoding='utf-8-sig')
        for meta, name in re.findall(r'\w+\((\d+),\s*"([^"]+)"', enum):
            copy = dict(entry, id=name, legacy_id=entry['id'], metadata=int(meta))
            expanded.append(copy)
            mappings[f'tp:{entry["id"]}:{meta}'] = f'tp:{name}'
    return expanded, mappings


entries, metadata_map = expand_metadata()


def block_properties(entry):
    source = legacy_class(entry)
    if re.search(r'extends\s+BlockBerryBush\b', source):
        source += '\n' + strip_comments((JAVA / 'blocks/bushes/BlockBerryBush.java').read_text(encoding='utf-8-sig'))
    expression = entry['assignments'][0]['expression'] + '\n' + source
    hardness = re.findall(r'setHardness\(([-\d.]+)[Ff]?\)', expression)
    resistance = re.findall(r'setResistance\(([-\d.]+)[Ff]?\)', expression)
    light = re.findall(r'setLightLevel\(([\d.]+)[Ff]?\)', expression)
    colors = re.findall(r'MapColor\.([A-Z_]+)\b', expression)
    map_color = {'AIR':'NONE','GREEN':'COLOR_GREEN','BLACK':'COLOR_BLACK'}.get(colors[0], colors[0]) if colors else 'STONE'
    p = f'BlockBehaviour.Properties.of().mapColor(MapColor.{map_color})'
    p += f'.strength({hardness[0] if hardness else "1.5"}F, {resistance[0] if resistance else "6.0"}F)'
    if light:
        p += f'.lightLevel(state -> {int(float(light[0]) * 15)})'
    if 'setBlockUnbreakable' in expression:
        p += '.strength(-1F, 6000000F)'
    if re.search(r'setHarvest(?:Level|Lvl)\("pickaxe"',expression):
        p += '.requiresCorrectToolForDrops()'
    if re.search(r'canEntitySpawn[\s\S]*?return entityIn\.isImmuneToFire\(\)',expression):
        p += '.isValidSpawn((state, level, pos, entityType) -> entityType.fireImmune())'
    sounds = re.findall(r'SoundType\.(\w+)', expression)
    sound = {'PLANT':'GRASS', 'GROUND':'GRAVEL'}.get(sounds[0], sounds[0]) if sounds else 'STONE'
    p += f'.sound(SoundType.{sound})'
    if 'glass' in entry['id'] or 'Lamp' in entry['assignments'][0]['class'] or re.search(r'isOpaqueCube\([^)]*\)\s*\{\s*return false', expression):
        p += '.noOcclusion()'
    if 'bush' in entry['id'] or entry['id'] == 'old_reed':
        p += '.noCollission().noOcclusion()'
    return p


def register_blocks():
    lines = []
    for entry in entries:
        if entry['group'] != 'TechBlocks':
            continue
        name = entry['id']
        p = block_properties(entry)
        if 'cobblegen' in name:
            interval = {'cobblegen_block':40, 'iron_cobblegen_block':20, 'diamond_cobblegen_block':10,
                        'blaze_cobblegen_block':5, 'emerald_cobblegen_block':1}[name]
            body = f'new CobbleGeneratorBlock({p}, {interval}, {32 if interval == 40 else 64})'
        elif name == 'vasholine':
            body = 'new VasholineBlock(BlockBehaviour.Properties.copy(Blocks.WATER).lightLevel(state -> 15))'
        elif name == 'stone_torch':
            body = f'new TorchBlock({p}.noCollission(), net.minecraft.core.particles.ParticleTypes.FLAME)'
        elif name == 'iron_furnace_block':
            body = f'new IronFurnaceBlock({p}.lightLevel(state -> state.getValue(IronFurnaceBlock.LIT) ? 15 : 0))'
        elif name == 'old_reed':
            body = f'new OldReedBlock({p}.replaceable())'
        elif name == 'ender_ore':
            body = f'new EnderOreBlock({p})'
        elif name in ('wub_ore','nether_wub_ore'):
            body = f'new DropExperienceBlock({p}, net.minecraft.util.valueproviders.UniformInt.of(1,5))'
        elif name in ('lava_block','nether_lava_block','water_block'):
            body = f'new SourceStoneBlock({p}, {str(name != "water_block").lower()})'
        elif name == 'lava_infused_stone':
            body = f'new InfusedStoneBlock({p})'
        elif name in ('asphalt_block','unhardened_stone','withered_block','reinforced_glass','reinforced_obsidian','quick_sand'):
            if name == 'quick_sand': p += '.noCollission().sound(SoundType.SAND)'
            body = f'new SpecialBlock({p}, {q(name)})'
        elif name == 'ghost_block':
            body = f'new Block({p}.noCollission().noOcclusion())'
        elif name.startswith('growth_'):
            factor = {'growth_block':40, 'growth_upgrade':20, 'growth_upgrade_two':10}[name]
            config_name = {'growth_block':'BlockGrowthTicks', 'growth_upgrade':'BlockGrowthUpgradeTicks',
                           'growth_upgrade_two':'BlockGrowthUpgradeTwoTicks'}[name]
            body = f'new GrowthCrystalBlock({p}, 4, () -> Math.max(1, LegacyConfig.integer({q("ConfigHandler." + config_name)}) * {factor}))'
        elif name.endswith('_stairs'):
            base = name.removesuffix('_stairs')
            body = f'new StairBlock(() -> block({q(base)}).defaultBlockState(), {p})'
        elif '_slab_' in name:
            body = f'new LegacySlabBlock({p}, {str(name.endswith("_double")).lower()})'
        elif name.endswith('_bush'):
            body = f'new BerryBushBlock({p}.replaceable(), {q(name.replace("_bush", "_berry"))})'
        else:
            source = legacy_class(entry)
            explosion_drops = not bool(re.search(r'canDropFromExplosion[\s\S]*?return false', source))
            explosion_immune = bool(re.search(r'onBlockExploded\([^)]*\)\s*\{\s*\}', source))
            triple_fall = 'onFallenUpon' in source and '3.0F' in source
            glass = entry['assignments'][0]['class'] in ('DirtyGlass','GlowstoneGlass')
            if not explosion_drops or explosion_immune or triple_fall or glass:
                body = f'new LegacyDecorativeBlock({p}, {str(explosion_drops).lower()}, {str(explosion_immune).lower()}, {str(triple_fall).lower()}, {str(glass).lower()})'
            else:
                body = f'new Block({p})'
        lines.append(f'        addBlock({q(name)}, () -> {body});')
    lines.append('        BLOCK_MAP.put("stone_wall_torch", BLOCKS.register("stone_wall_torch", () -> new WallTorchBlock(BlockBehaviour.Properties.copy(Blocks.WALL_TORCH), net.minecraft.core.particles.ParticleTypes.FLAME)));')
    return lines


def food(entry):
    assignment = entry['assignments'][0]
    args = assignment['args']
    builder = f'new FoodProperties.Builder().nutrition({value(args[0])}).saturationMod({value(args[1])}F)'
    if args[2] == 'true':
        builder += '.meat()'
    expression = assignment['expression'] + legacy_class(entry)
    if 'setAlwaysEdible' in expression:
        builder += '.alwaysEat()'
    for effect, duration, amplifier in re.findall(r'new PotionEffect\(MobEffects\.(\w+),\s*(\d+),\s*(\d+)', expression):
        effect = {'STRENGTH':'DAMAGE_BOOST', 'RESISTANCE':'DAMAGE_RESISTANCE', 'HASTE':'DIG_SPEED', 'SPEED':'MOVEMENT_SPEED'}.get(effect, effect)
        builder += f'.effect(() -> new MobEffectInstance(MobEffects.{effect}, {duration}, {amplifier}), 1.0F)'
    builder += '.build()'
    properties = f'new Item.Properties().food({builder})'
    dynamic = builder
    for argument in args[:2]:
        if argument.startswith('ConfigHandler.'):
            method = 'integer' if argument == args[0] else 'decimal'
            literal = str(value(argument)) + ('F' if method == 'decimal' else '')
            attribute = 'nutrition' if method == 'integer' else 'saturationMod'
            dynamic = dynamic.replace(f'.{attribute}({literal})', f'.{attribute}(LegacyConfig.{method}({q(argument)}))')
    if assignment['class'].endswith('Apple'):
        return f'new GlintFoodItem({properties}.rarity(Rarity.EPIC), () -> {dynamic})'
    if assignment['class'] in ['DrinkBase','SuccBase']:
        succ = assignment['class'] == 'SuccBase'
        bottle_setting = 'ConfigHandler.succ_juice_bottle' if succ else 'ConfigHandler.JuiceBottles'
        return f'new DrinkItem({properties}, () -> {dynamic}, {q(bottle_setting)}, {str(succ).lower()})'
    return f'new ConfiguredFoodItem({properties}, () -> {dynamic})'


def tier_name(entry):
    arg = entry['assignments'][0]['args'][0]
    return arg.rsplit('.', 1)[-1]


def tooltip_conditions(source, position):
    result = []
    for match in re.finditer(r'\bif\s*\(', source[:position]):
        start = match.end()
        depth, end = 1, start
        while depth and end < len(source):
            depth += (source[end] == '(') - (source[end] == ')')
            end += 1
        condition = source[start:end-1].strip()
        body = end
        while body < len(source) and source[body].isspace(): body += 1
        if body >= len(source) or source[body] != '{': continue
        depth, end = 1, body + 1
        while depth and end < len(source):
            depth += (source[end] == '{') - (source[end] == '}')
            end += 1
        if body <= position < end and condition.startswith(('ArmorHandler.', 'ConfigHandler.')):
            result.append(condition)
    return ' && '.join(result)


def register_items():
    lines = []
    for entry in entries:
        if entry['group'] == 'TechBlocks':
            continue
        name = entry['id']
        assignment = entry['assignments'][0]
        cls = assignment['class']
        if entry['group'] == 'TechArmor':
            material = assignment['args'][0].rsplit('.',1)[-1]
            slot = assignment['args'][2].rsplit('.',1)[-1]
            slot = {'HEAD':'HELMET','CHEST':'CHESTPLATE','LEGS':'LEGGINGS','FEET':'BOOTS'}[slot]
            source = legacy_class(entry)
            matches = list(re.finditer(r'new TextComponentTranslation\("([^"]+)"\)',source))
            keys = [m[1] for m in matches]
            conditions = [tooltip_conditions(source, m.start()) for m in matches]
            body = (f'new LegacyArmorItem(Materials.armor({q(material)}), ArmorItem.Type.{slot}, new Item.Properties(), '
                    'new String[] {' + ','.join(q(k) for k in keys) + '}, new String[] {' +
                    ','.join(q(c) for c in conditions) + '})')
        elif entry['group'] == 'TechFoods' and cls != 'ItemJuicer' or cls == 'FoodBase':
            body = food(entry)
        elif entry['group'] == 'TechTools':
            if cls == 'ShearsBase':
                durability = re.search(r'setMaxDamage\((\d+)\)', assignment['expression'])[1]
                body = f'new ShearsItem(new Item.Properties().durability({durability}))'
            else:
                tier = f'Materials.tier({q(tier_name(entry))})'
                kind = 'sword' if any(w in name for w in ('sword', 'spear', 'battle')) else 'pickaxe' if any(w in name for w in ('pickaxe', 'pick', 'multi', 'paxel', 'hammer')) else 'axe' if name.endswith('_axe') else 'hoe' if 'hoe' in name or 'scythe' in name else 'shovel'
                type_name, damage, speed = {'sword':('SwordItem',3,-2.4), 'pickaxe':('PickaxeItem',1,-2.8),
                                          'axe':('AxeItem',6.0,-3.1), 'hoe':('HoeItem',-1,-2.0),
                                          'shovel':('ShovelItem',1.5,-3.0)}[kind]
                damage_arg = str(damage) + ('F' if type_name in ('AxeItem','ShovelItem') else '')
                properties = 'new Item.Properties()'
                override = re.search(r'setMaxDamage\(([^)]+)\)',assignment['expression'])
                if override:
                    properties += f'.durability({value(override[1])})'
                if name == 'birthday_pickaxe':
                    body = f'new BirthdayPickaxeItem({tier}, {properties})'
                elif name == 'kappa_pick':
                    body = f'new KappaPickaxeItem({tier}, {properties})'
                elif name == 'c_axe':
                    body = f'new SeaAxeItem({tier}, {properties})'
                elif name == 'wub_hammer':
                    body = f'new WubHammerItem({tier}, {properties})'
                elif name.endswith('_spear'):
                    body = f'new SpearItem({tier}, {properties})'
                elif name.endswith('_scythe'):
                    body = f'new ScytheItem({tier}, {properties})'
                elif name.endswith(('_multi','_paxel')):
                    body = f'new PaxelItem({tier}, {properties})'
                elif name.endswith('_battle') or type_name == 'AxeItem':
                    args = assignment['args']
                    damage = args[1] if len(args) > 1 else '6.0F'
                    speed_arg = args[2] if len(args) > 2 else '-3.1F'
                    axe_type = 'BattleAxeItem' if name.endswith('_battle') else 'AxeItem'
                    body = f'new {axe_type}({tier}, {damage} - {tier}.getAttackDamageBonus(), {speed_arg}, {properties})'
                else:
                    body = f'new {type_name}({tier}, {damage_arg}, {speed}F, {properties})'
        elif name in ('watering_can', 'watering_can_upgrade'):
            body = f'new WateringCanItem(new Item.Properties().stacksTo(1), {1 if name == "watering_can" else 2})'
        elif name in ('small_bandage','large_bandage','small_med_kit','large_med_kit'):
            setting = {'small_bandage':'smallBandageHealStack','large_bandage':'largeBandageStack',
                       'small_med_kit':'smallMedHealStack','large_med_kit':'LargeMedHealStack'}[name]
            size = value('ConfigHandler.' + setting)
            body = f'new MedicalItem(new Item.Properties().stacksTo({size}), {q(name)})'
        elif name == 'repair_tablet':
            body = 'new RepairTabletItem(new Item.Properties().stacksTo(1))'
        elif name == 'infin_bucket':
            body = 'new InfiniteBucketItem(new Item.Properties().stacksTo(1))'
        elif name == 'mycelium_seeds':
            body = 'new MyceliumSeedsItem(new Item.Properties())'
        elif name == 'pouch':
            body = 'new PouchItem(new Item.Properties().stacksTo(1))'
        elif name in ('quartz_knife','flint_knife'):
            setting = 'QuartzKnifeDamage' if name == 'quartz_knife' else 'FlintKnifeDamage'
            body = f'new CraftingToolItem(new Item.Properties().durability({value("ConfigHandler." + setting)}))'
        else:
            properties = 'new Item.Properties()'
            if cls in ('ItemJuicer','ItemStay'):
                body = f'new CraftingToolItem({properties}.stacksTo(1))'
            else:
                body = f'new Item({properties})'
        for vanilla, configured in [('PickaxeItem','Pickaxe'),('SwordItem','Sword'),('AxeItem','Axe'),('ShovelItem','Shovel'),('HoeItem','Hoe'),('ShearsItem','Shears')]:
            body = body.replace('new '+vanilla+'(', 'new ConfiguredTools.'+configured+'(')
        lines.append(f'        addItem({q(name)}, () -> {body});')
    return lines


def tooltip_data():
    lines = []
    for entry in entries:
        if entry['group'] == 'TechArmor': continue
        source = legacy_class(entry)
        start = source.find('void addInformation(')
        if start < 0: continue
        body = source.find('{', start)
        depth, end = 1, body + 1
        while depth and end < len(source):
            depth += (source[end] == '{') - (source[end] == '}')
            end += 1
        additions = []
        for match in re.finditer(r'tooltip\.add\(TextFormatting\.(\w+)\s*\+\s*new TextComponentTranslation\("([^"]+)"\)', source[body:end]):
            color, key = match.groups()
            condition = tooltip_conditions(source, body + match.start())
            additions.append(f'                Tooltips.add(tooltip, {q(key)}, net.minecraft.ChatFormatting.{color}, {q(condition)});')
        if additions:
            lines.append(f'            case {q(entry["id"])} -> {{\n'+'\n'.join(additions)+'\n            }')
    return '\n'.join(lines)


def materials():
    raw = (JAVA / 'handlers/MaterialHandler.java').read_text(encoding='utf-8-sig')
    tiers, armors = [], []
    def repair_ingredient(selected):
        results=[]
        for entry in selected:
            source=legacy_class(entry)
            match=re.search(r'ItemStack\s+mat\s*=\s*new ItemStack\(([^;]+)\);',source)
            if not match: continue
            args=split_args(match[1])
            owner,name=args[0].split('.')
            if name == 'DYE': expr='Items.LAPIS_LAZULI'
            elif name == 'PLANKS': expr='net.minecraft.world.level.block.Blocks.OAK_PLANKS'
            elif owner == 'TechItems': expr=f'Content.item({q(name.lower())})'
            elif owner == 'Blocks': expr='net.minecraft.world.level.block.Blocks.'+name
            else: expr='Items.'+name
            if expr not in results: results.append(expr)
        return 'Ingredient.of('+','.join(results)+')' if results else 'Ingredient.EMPTY'
    for m in re.finditer(r'ToolMaterial\s+(\w+)\s*=\s*EnumHelper.addToolMaterial\(([^;]+)\);', raw):
        args = split_args(m[2])
        repair=repair_ingredient(e for e in entries if e['group']=='TechTools' and e['assignments'][0]['args'] and tier_name(e)==m[1])
        tiers.append(f'            case {q(m[1])} -> sorted({q(m[1].lower())}, {args[1]}, {args[2]}, {args[3]}, {args[4]}, {args[5]}, () -> {repair});')
    for m in re.finditer(r'ArmorMaterial\s+(\w+)\s*=\s*EnumHelper.addArmorMaterial\(([^;]+)\);', raw):
        args = split_args(m[2])
        defense = re.search(r'\{([^}]+)\}', args[3])[1]
        repair=repair_ingredient(e for e in entries if e['group']=='TechArmor' and e['assignments'][0]['args'][0].rsplit('.',1)[-1]==m[1])
        equip=args[5].replace('ITEM_ARMOR_EQUIP_','ARMOR_EQUIP_')
        armors.append(f'            case {q(m[1])} -> new LegacyArmor({args[1]}, {args[2]}, new int[] {{{defense}}}, {args[4]}, {args[6]}, {equip}, () -> {repair});')
    source = '''package com.kashdeya.tinyprogressions;

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
@TIERS@
            default -> throw new IllegalArgumentException("Unknown tier " + key);
        });
    }
    public static ArmorMaterial armor(String name) {
        return ARMOR.computeIfAbsent(name, key -> switch (key) {
@ARMORS@
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
'''.replace('@TIERS@','\n'.join(tiers)).replace('@ARMORS@','\n'.join(armors))
    (PACKAGE / 'Materials.java').write_text(source, encoding='utf-8')


def registrations():
    source = '''package com.kashdeya.tinyprogressions;

import com.kashdeya.tinyprogressions.block.*;
import com.kashdeya.tinyprogressions.item.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.*;
import java.util.*;
import java.util.function.Supplier;

/** Generated by tools/port_content.py from migration/catalog.json. */
public final class Content {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "tp");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "tp");
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "tp");
    private static final Map<String, RegistryObject<Block>> BLOCK_MAP = new LinkedHashMap<>();
    private static final Map<String, RegistryObject<Item>> ITEM_MAP = new LinkedHashMap<>();
    public static final RegistryObject<BlockEntityType<CobbleGeneratorBlockEntity>> COBBLE_GENERATOR =
            BLOCK_ENTITIES.register("cobble_generator", () -> BlockEntityType.Builder.of(CobbleGeneratorBlockEntity::new,
                    block("cobblegen_block"), block("iron_cobblegen_block"), block("diamond_cobblegen_block"),
                    block("blaze_cobblegen_block"), block("emerald_cobblegen_block")).build(null));
    public static final RegistryObject<BlockEntityType<IronFurnaceBlockEntity>> IRON_FURNACE =
            BLOCK_ENTITIES.register("iron_furnace", () -> BlockEntityType.Builder.of(IronFurnaceBlockEntity::new,
                    block("iron_furnace_block")).build(null));
    public static Block block(String name) { return Objects.requireNonNull(BLOCK_MAP.get(name), name).get(); }
    public static Item item(String name) { return Objects.requireNonNull(ITEM_MAP.get(name), name).get(); }
    public static int configuredDurability(ItemStack stack, int fallback) {
        var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !id.getNamespace().equals("tp")) return fallback;
        return switch (id.getPath()) {
@DURABILITY@
            default -> fallback;
        };
    }
    public static boolean enabled(String name) {
        return switch (name) {
@ENABLED@
            case "stone_wall_torch" -> enabled("stone_torch");
            case "vasholine_bucket" -> enabled("vasholine");
            default -> true;
        };
    }
    public static void appendLegacyTooltip(ItemStack stack, java.util.List<net.minecraft.network.chat.Component> tooltip) {
        var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !id.getNamespace().equals("tp")) return;
        switch (id.getPath()) {
@TOOLTIPS@
            default -> { }
        }
    }
    public static void initialize() {
@BLOCKS@
@ITEMS@
        ITEM_MAP.put("vasholine_bucket", ModFluids.BUCKET);
    }
    private static void addBlock(String name, Supplier<Block> factory) {
        RegistryObject<Block> block = BLOCKS.register(name, factory);
        BLOCK_MAP.put(name, block);
        if (name.equals("stone_torch")) addItem(name, () -> new StandingAndWallBlockItem(block.get(), block("stone_wall_torch"), new Item.Properties(), net.minecraft.core.Direction.DOWN));
        else if (name.equals("fmf_block")) addItem(name, () -> new AngelBlockItem(block.get(), new Item.Properties()));
        else if (!name.equals("vasholine")) addItem(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
    private static void addItem(String name, Supplier<Item> factory) {
        ITEM_MAP.put(name, ITEMS.register(name, factory));
    }
}
'''.replace('@BLOCKS@', '\n'.join(register_blocks())).replace('@ITEMS@', '\n'.join(register_items())).replace('@TOOLTIPS@', tooltip_data())
    enabled = []
    for entry in entries:
        branches = [a.get('conditions', []) for a in entry['assignments']]
        if any(not b for b in branches): continue
        expression = ' || '.join('FeatureCondition.evaluate(' + q(' && '.join(b)) + ')' for b in branches)
        enabled.append(f'            case {q(entry["id"])} -> {expression};')
    source = source.replace('@ENABLED@', '\n'.join(enabled))
    durability = []
    for entry in entries:
        override = re.search(r'setMaxDamage\(((?:DamageHandler|ConfigHandler)\.\w+)\)',entry['assignments'][0]['expression'])
        if override:
            durability.append(f'            case {q(entry["id"])} -> LegacyConfig.integer({q(override[1])});')
    durability.extend([f'            case {q(name)} -> LegacyConfig.integer({q("ConfigHandler."+setting)});' for name,setting in [('quartz_knife','QuartzKnifeDamage'),('flint_knife','FlintKnifeDamage')]])
    source = source.replace('@DURABILITY@', '\n'.join(durability))
    (PACKAGE / 'Content.java').write_text(source, encoding='utf-8')


def convert_assets():
    ASSETS.mkdir(parents=True, exist_ok=True)
    for directory in ['textures','sounds']:
        shutil.copytree(SOURCE_ASSETS / directory, ASSETS / directory, dirs_exist_ok=True)
    shutil.copyfile(SOURCE_ASSETS / 'sounds.json', ASSETS / 'sounds.json')
    # Model paths gained a required directory component after 1.12.
    block_names = {p.stem for p in (SOURCE_ASSETS / 'models/block').glob('*.json')}
    for path in (SOURCE_ASSETS / 'models').rglob('*.json'):
        data = json.loads(path.read_text(encoding='utf-8-sig'))
        parent = data.get('parent', '')
        data['parent'] = {'block/half_slab':'minecraft:block/slab',
                          'block/upper_slab':'minecraft:block/slab_top',
                          'block/torch_wall':'minecraft:block/wall_torch'}.get(parent, parent) if parent else data.get('parent','minecraft:block/block')
        if parent.startswith('tp:') and '/' not in parent.split(':')[1]:
            data['parent'] = 'tp:block/' + parent.split(':')[1]
        for key, texture in data.get('textures', {}).items():
            if ':' not in texture and texture.startswith('blocks/'):
                texture = texture.replace('blocks/', 'minecraft:block/', 1)
                texture = texture.replace('torch_on', 'torch')
                if 'glass_' in texture:
                    color = texture.split('glass_')[1].replace('silver','light_gray')
                    texture = 'minecraft:block/' + color + '_stained_glass'
                data['textures'][key] = texture
        if data.get('textures') and 'particle' not in data['textures']:
            data['textures']['particle']=next(iter(data['textures'].values()))
        if any(word in path.stem for word in ['glass','growth','lamp','cobblegen','bush','reed','torch','fmf']):
            data['render_type'] = 'minecraft:cutout'
        write_json(ASSETS / 'models' / path.relative_to(SOURCE_ASSETS / 'models'), data)
    all_languages = {}
    for path in (SOURCE_ASSETS / 'lang').glob('*.lang'):
        translations = {}
        for line in path.read_text(encoding='utf-8-sig').splitlines():
            if '=' in line and not line.startswith('#'):
                key, text = line.split('=',1)
                translations[key.strip()] = text.strip()
        all_languages[path.stem.lower()] = translations
    missing = []
    for entry in entries:
        name = entry['id']
        expression = entry['assignments'][0]['expression'] + legacy_class(entry)
        translation = re.search(r'set(?:TranslationKey|UnlocalName)\("([^"]+)"\)', expression)
        old_name = name if 'metadata' in entry else translation[1] if translation else name
        old_type = 'tile' if entry['group'] == 'TechBlocks' else 'item'
        new_type = 'block' if entry['group'] == 'TechBlocks' else 'item'
        for lang in all_languages.values():
            lang[f'{new_type}.tp.{name}'] = lang.get(f'{old_type}.{old_name}.name', name.replace('_',' ').title())
        if entry['group'] == 'TechBlocks':
            legacy_state = SOURCE_ASSETS / 'blockstates' / f'{entry.get("legacy_id", name)}.json'
            state = json.loads(legacy_state.read_text(encoding='utf-8-sig')) if legacy_state.exists() else {}
            variants = state.get('variants', {})
            if 'metadata' in entry:
                variants = {'': variants.get('color=' + name, {'model':'tp:' + name})}
            else:
                variants = {('' if k in ('normal','inventory') else k):v for k,v in variants.items() if k != 'inventory'}
            def model_paths(node):
                if isinstance(node, dict):
                    for key, val in node.items():
                        if key == 'model' and isinstance(val,str) and val.startswith('tp:') and '/' not in val.split(':')[1]:
                            node[key] = 'tp:block/' + val.split(':')[1]
                        else:
                            model_paths(val)
                elif isinstance(node,list):
                    for val in node: model_paths(val)
            model_paths(variants)
            if '_slab_' in name:
                base = name.split('_slab_')[0]
                variants = {'type=bottom':{'model':'tp:block/' + base + '_slab_bottom'},
                            'type=top':{'model':'tp:block/' + base + '_slab_top'},
                            'type=double':{'model':'tp:block/' + base}}
            if name == 'stone_torch':
                variants = {'':{'model':'tp:block/stone_torch'}}
            if name in ('growth_upgrade','growth_upgrade_two','lava_infused_stone'):
                variants = {'':{'model':'tp:block/'+name}}
            if name == 'iron_furnace_block':
                variants = {}
                for lit in (False,True):
                    model = 'iron_furnace_lit' if lit else 'iron_furnace'
                    write_json(ASSETS / 'models/block' / f'{model}.json', {
                        'parent':'minecraft:block/orientable', 'textures':{
                            'front':'tp:blocks/furnace_front_on' if lit else 'tp:blocks/furnace_off_front',
                            'side':'tp:blocks/furnace_side','top':'tp:blocks/furnace_top'}})
                    for face, rotation in [('north',0),('east',90),('south',180),('west',270)]:
                        variants[f'facing={face},lit={str(lit).lower()}'] = {'model':'tp:block/' + model,'y':rotation}
            if not variants:
                if name in ('water_harvester','bridge_builder','tower_builder'):
                    texture = {'water_harvester':'minecraft:block/iron_block',
                               'bridge_builder':'minecraft:block/oak_planks',
                               'tower_builder':'minecraft:block/stone_bricks'}[name]
                    write_json(ASSETS / 'models/block' / f'{name}.json', {
                        'parent':'minecraft:block/cube_all','textures':{'all':texture}})
                else:
                    missing.append(name)
                variants = {'': {'model':'tp:block/' + name}}
            write_json(ASSETS / 'blockstates' / f'{name}.json', {'variants':variants})
            if not (ASSETS / 'models/item' / f'{name}.json').exists():
                model = name if name in block_names else old_name
                write_json(ASSETS / 'models/item' / f'{name}.json', {'parent':'tp:block/' + model})
            write_json(RESOURCE / 'data/tp/loot_tables/blocks' / f'{name}.json', {
                'type':'minecraft:block', 'pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'tp:'+name}],
                'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
            if name == 'vasholine':
                write_json(RESOURCE / 'data/tp/loot_tables/blocks/vasholine.json', {'type':'minecraft:block','pools':[]})
            if '_slab_' in name:
                drop = name.replace('_double','_half')
                count = {'function':'minecraft:set_count','count':2}
                if name.endswith('_half'):
                    count['conditions']=[{'condition':'minecraft:block_state_property','block':'tp:'+name,'properties':{'type':'double'}}]
                write_json(RESOURCE / 'data/tp/loot_tables/blocks' / f'{name}.json', {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'tp:'+drop,'functions':[count,{'function':'minecraft:explosion_decay'}]}]}]})
            if name.endswith('_bush') or name == 'old_reed':
                drop=name.replace('_bush','_berry') if name.endswith('_bush') else 'dead_reed'
                count={'type':'minecraft:uniform','min':1,'max':5} if name.endswith('_bush') else 2
                write_json(RESOURCE / 'data/tp/loot_tables/blocks' / f'{name}.json', {
                    'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'tp:'+drop,
                    'functions':[{'function':'minecraft:set_count','count':count},{'function':'minecraft:explosion_decay'}]}]}]})
            if entry.get('legacy_id') == 'colored_glowstone':
                dust=metadata_map[f'tp:colored_dust:{entry["metadata"]}']
                write_json(RESOURCE / 'data/tp/loot_tables/blocks' / f'{name}.json',{
                    'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:alternatives','children':[
                    {'type':'minecraft:item','name':'tp:'+name,'conditions':[{'condition':'minecraft:match_tool','predicate':{'enchantments':[{'enchantment':'minecraft:silk_touch','levels':{'min':1}}]}}]},
                    {'type':'minecraft:item','name':dust,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':2,'max':4}}, {'function':'minecraft:explosion_decay'}]}]}]}]})
            if name in ('ender_ore','wub_ore','nether_wub_ore','lava_block','nether_lava_block','water_block','withered_block','ghost_block'):
                silk={'condition':'minecraft:match_tool','predicate':{'enchantments':[{'enchantment':'minecraft:silk_touch','levels':{'min':1}}]}}
                normal=[]
                if name.endswith('_ore'):
                    drop='ender_dust' if name=='ender_ore' else 'wub_gem'
                    normal=[{'type':'minecraft:item','name':'tp:'+drop,'functions':[
                        {'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':5 if name=='ender_ore' else 2}},
                        {'function':'minecraft:apply_bonus','enchantment':'minecraft:fortune','formula':'minecraft:uniform_bonus_count','parameters':{'bonusMultiplier':1}},
                        {'function':'minecraft:limit_count','limit':{'min':1,'max':6}},
                        {'function':'minecraft:explosion_decay'}]}]
                pools=[] if name in ('ghost_block','withered_block') else [{'rolls':1,'entries':[{'type':'minecraft:alternatives','children':[
                    {'type':'minecraft:item','name':'tp:'+name,'conditions':[silk]}]+normal}]}]
                write_json(RESOURCE / 'data/tp/loot_tables/blocks' / f'{name}.json',{'type':'minecraft:block','pools':pools})
        elif not (ASSETS / 'models/item' / f'{name}.json').exists():
            if (ASSETS / 'models/item' / f'{old_name}.json').exists():
                write_json(ASSETS / 'models/item' / f'{name}.json', {'parent':'tp:item/' + old_name})
            else:
                missing.append(name)
    for name, translations in all_languages.items():
        translations['itemGroup.tp'] = 'Tiny Progressions'
        write_json(ASSETS / 'lang' / f'{name}.json', translations)
    write_json(ASSETS / 'models/item/vasholine_bucket.json', {'parent':'forge:item/bucket',
        'loader':'forge:fluid_container','fluid':'tp:vasholine'})
    write_json(ASSETS / 'blockstates/vasholine.json', {'variants':{'':{'model':'minecraft:block/water'}}})
    write_json(ASSETS / 'blockstates/stone_wall_torch.json', {'variants':{
        f'facing={face}':dict(model='tp:block/stone_torch_wall', y=rotation)
        for face,rotation in [('east',0),('south',90),('west',180),('north',270)]}})
    write_json(RESOURCE / 'data/tp/loot_tables/blocks/stone_wall_torch.json', {
        'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'tp:stone_torch'}]}]})
    write_json(RESOURCE / 'assets/minecraft/atlases/blocks.json', {'sources':[
        {'type':'minecraft:directory','source':'blocks','prefix':'blocks/'},
        {'type':'minecraft:directory','source':'items','prefix':'items/'},
        {'type':'minecraft:single','resource':'tp:fluids/wub_juice_still','sprite':'tp:fluids/wub_juice_still'},
        {'type':'minecraft:single','resource':'tp:fluids/wub_juice_flow','sprite':'tp:fluids/wub_juice_flow'}]})
    # Tags replace the ore dictionary, and make modern mining tools recognize the blocks.
    beacon_bases = ['tp:'+e['id'] for e in entries if e['group']=='TechBlocks' and re.search(r'isBeaconBase\([^)]*\)\s*\{\s*return true',legacy_class(e))]
    write_json(RESOURCE / 'data/minecraft/tags/blocks/beacon_base_blocks.json', {'replace':False,'values':beacon_bases})
    mineable = ['tp:' + e['id'] for e in entries if e['group'] == 'TechBlocks' and not e['id'].endswith('_bush') and e['id'] != 'old_reed']
    write_json(RESOURCE / 'data/minecraft/tags/blocks/mineable/pickaxe.json', {'replace':False,'values':mineable})
    for level, tag_name in [(1,'needs_stone_tool'),(2,'needs_iron_tool'),(3,'needs_diamond_tool')]:
        required = []
        for entry in entries:
            if entry['group'] != 'TechBlocks': continue
            expression = entry['assignments'][0]['expression'] + legacy_class(entry)
            harvest = re.search(r'setHarvest(?:Level|Lvl)\("pickaxe",\s*(\d+)\)',expression)
            if harvest and int(harvest[1]) >= level: required.append('tp:' + entry['id'])
        write_json(RESOURCE / 'data/minecraft/tags/blocks' / (tag_name + '.json'), {'replace':False,'values':required})
    write_json(ROOT / 'migration/metadata-map.json', metadata_map)
    write_json(ROOT / 'migration/asset-gaps.json', missing)
    return missing


if __name__ == '__main__':
    PACKAGE.mkdir(parents=True, exist_ok=True)
    materials()
    registrations()
    missing = convert_assets()
    write_json(ROOT / 'migration/catalog.json', catalog)
    print(json.dumps({'registered_entries':len(entries), 'asset_gaps':missing}, indent=2))
