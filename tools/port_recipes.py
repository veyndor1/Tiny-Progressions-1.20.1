"""Convert static legacy recipes. Unsupported constructs are reported, never guessed."""
import json
import re
import zipfile
from pathlib import Path
from inventory import ROOT, JAVA, collect, constructor, split_args, strip_comments, enclosing_conditions

RESOURCE = ROOT / 'src/main/resources/data'
CATALOG = collect()
METADATA = json.loads((ROOT / 'migration/metadata-map.json').read_text(encoding='utf-8'))
ALIASES = {'reeds':'sugar_cane','melon':'melon_slice','speckled_melon':'glistering_melon_slice',
           'slime_ball':'slime_ball','dye':'dye', 'grass':'grass_block','stonebrick':'stone_bricks',
           'end_bricks':'end_stone_bricks', 'magma':'magma_block', 'nether_brick':'nether_bricks',
           'netherbrick':'nether_brick', 'red_nether_brick':'red_nether_bricks',
           'hardened_clay':'terracotta','silver':'light_gray', 'fish':'cod','cooked_fish':'cooked_cod',
           'wooden_door':'oak_door','boat':'oak_boat','sign':'oak_sign'}
COLORS = ['black','red','green','brown','blue','purple','cyan','light_gray',
          'gray','pink','lime','yellow','light_blue','magenta','orange','white']
TAG_EXCEPTIONS = {'stickWood':'forge:rods/wooden','plankWood':'minecraft:planks',
                  'logWood':'minecraft:logs','treeLeaves':'minecraft:leaves',
                  'blockGlass':'forge:glass','paneGlass':'forge:glass_panes',
                  'stone':'forge:stone','cobblestone':'forge:cobblestone',
                  'sugarcane':'forge:sugar_cane','chainmail':'tp:chainmail',
                  'dragonScale':'tp:dragon_scale','witherRib':'tp:wither_rib',
                  'blockGlassColorless':'forge:glass/colorless', 'juicer':'tp:juicer',
                  'bone':'forge:bones','feather':'forge:feathers','flint':'forge:flint',
                  'leather':'forge:leather','netherStar':'forge:nether_stars',
                  'netherrack':'forge:netherrack','obsidian':'forge:obsidian','torch':'tp:torches',
                  'stoneAndesitePolished':'tp:polished_andesite',
                  'stoneDioritePolished':'tp:polished_diorite', 'stoneGranitePolished':'tp:polished_granite'}
TAG_EXCEPTIONS.update({'glassBottle':'forge:glass_bottles','string':'forge:string'})


def snake(name):
    return re.sub(r'(?<!^)(?=[A-Z])','_',name).lower()


def tag(name):
    if name in TAG_EXCEPTIONS:
        return TAG_EXCEPTIONS[name]
    for prefix, folder in [('ingot','ingots'),('nugget','nuggets'),('dust','dusts'),('gem','gems'),
                           ('block','storage_blocks'),('ore','ores'),('dye','dyes'),('crop','crops'),('stick','rods')]:
        if name.startswith(prefix):
            suffix = snake(name[len(prefix):]).replace('silver','light_gray')
            return f'forge:{folder}/{suffix}'
    raise ValueError('Unknown ore dictionary tag: ' + name)


def stack(expression):
    if expression.startswith('FluidUtil.getFilledBucket') and 'vasholine' in expression:
        return {'item':'tp:vasholine_bucket','count':1}
    if expression == 'BirthdayPickaxe':
        return {'item':'tp:birthday_pickaxe','count':1,'nbt':'{Enchantments:[{id:"minecraft:mending",lvl:1s}]}'}
    if re.fullmatch(r'(?:Items|Blocks|Tech\w+)\.\w+',expression):
        expression = f'new ItemStack({expression})'
    cls, args = constructor(expression)
    if cls != 'ItemStack':
        raise ValueError('Unsupported stack: ' + expression)
    ref = args[0]
    count = int(args[1]) if len(args) > 1 and args[1].isdigit() else 1
    meta = args[2] if len(args) > 2 else '0'
    if ref.startswith(('TechBlocks.','TechItems.','TechFoods.','TechTools.','TechArmor.')):
        name = ref.split('.')[1].lower()
        if name in ('colored_glowstone','colored_lamp','colored_dust'):
            if not meta.isdigit():
                raise ValueError('Unresolved metadata: ' + expression)
            name = METADATA[f'tp:{name}:{meta}'].split(':')[1]
        return {'item':'tp:' + name, 'count':count}
    if ref.startswith(('Items.','Blocks.')):
        name = ref.split('.')[1].lower()
        if name in ('log','log2','planks'):
            if not meta.isdigit(): raise ValueError('Wood wildcard must be converted to a tag')
            woods = ['acacia','dark_oak'] if name == 'log2' else ['oak','spruce','birch','jungle','acacia','dark_oak']
            name = woods[int(meta)] + ('_planks' if name == 'planks' else '_log')
        if name == 'dye':
            if not meta.isdigit(): raise ValueError('Unresolved dye: ' + expression)
            dye = {0:'ink_sac',3:'cocoa_beans',4:'lapis_lazuli',15:'bone_meal'}.get(int(meta),COLORS[int(meta)] + '_dye')
            return {'item':'minecraft:' + dye,'count':count}
        if name == 'coal' and meta == '1': name = 'charcoal'
        if name == 'golden_apple' and meta == '1': name = 'enchanted_golden_apple'
        if name == 'stone' and meta != '0':
            if not meta.isdigit(): raise ValueError('Unresolved stone variant')
            name = ['stone','granite','polished_granite','diorite','polished_diorite','andesite','polished_andesite'][int(meta)]
        if name in ('wool','stained_hardened_clay','stained_glass','stained_glass_pane'):
            if not meta.isdigit(): raise ValueError('Unresolved color')
            name = COLORS[15-int(meta)] + '_' + {'wool':'wool','stained_hardened_clay':'terracotta',
                                               'stained_glass':'stained_glass','stained_glass_pane':'stained_glass_pane'}[name]
        return {'item':'minecraft:' + ALIASES.get(name,name),'count':count}
    raise ValueError('Unsupported reference: ' + ref)


def ingredient(expression):
    expression = expression.strip()
    if 'OreDictionary.WILDCARD_VALUE' in expression:
        if 'Blocks.LOG2' in expression: return {'tag':'tp:logs2'}
        if 'Blocks.LOG' in expression: return {'tag':'tp:logs1'}
        if 'Blocks.PLANKS' in expression: return {'tag':'minecraft:planks'}
    potion = re.search(r'PotionTypes\.(\w+)', expression)
    if potion:
        potion_name = {'SWIFTNESS':'swiftness','LONG_SWIFTNESS':'long_swiftness','STRONG_SWIFTNESS':'strong_swiftness'}[potion[1]]
        return {'type':'forge:nbt','item':'minecraft:potion','nbt':'{Potion:"minecraft:' + potion_name + '"}'}
    if expression.startswith('"'):
        return {'tag':tag(json.loads(expression))}
    result = stack(expression)
    result.pop('count',None)
    return result


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')


def main():
    converted, pending, names = [], [], set()
    expectations = []
    expanded_recipes = []
    dust_enum = (JAVA / 'properties/EnumDustColor.java').read_text(encoding='utf-8-sig')
    dusts = re.findall(r'\w+\((\d+),\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)"', dust_enum)
    for original in CATALOG['recipes']:
        if '[i]' not in original['name']:
            expanded_recipes.append(original)
            continue
        for meta, name, ore, dye in dusts:
            args = [a.replace(', i)', ', ' + meta + ')')
                    .replace('EnumDustColor.values()[i].getOredict()', json.dumps(ore))
                    .replace('EnumDustColor.values()[i].getCraftingOredict()', json.dumps(dye))
                    for a in original['args']]
            expanded_recipes.append(dict(original, name=original['name'].replace('[i]', '_' + meta), args=args))
    for recipe in expanded_recipes:
        try:
            args = recipe['args']
            output = stack(args[0])
            name = recipe['name']
            if '[' in name: raise ValueError('Array recipe needs metadata expansion')
            # Same field names in different legacy recipe classes are disambiguated deterministically.
            if name in names: name = recipe['source'].removesuffix('.java').lower() + '_' + name
            names.add(name)
            if recipe['type'] == 'ShapedRecipe':
                pattern = []
                i = 1
                while i < len(args) and args[i].startswith('"'):
                    pattern.append(''.join(json.loads(s) for s in re.findall(r'"(?:\\.|[^"\\])*"', args[i]))); i += 1
                if not pattern or len(pattern) > 3: raise ValueError('Invalid pattern')
                key = {}
                while i < len(args):
                    char = args[i].strip("'")
                    if len(char) != 1: raise ValueError('Invalid ingredient key')
                    key[char] = ingredient(args[i+1]); i += 2
                used = set(''.join(pattern)) - {' '}
                if used != set(key): raise ValueError('Pattern keys do not match ingredients')
                result = {'type':'minecraft:crafting_shaped','pattern':pattern,'key':key,'result':output}
            else:
                result = {'type':'minecraft:crafting_shapeless','ingredients':[ingredient(x) for x in args[1:]],'result':output}
                if len(result['ingredients']) > 9: raise ValueError('Too many ingredients')
            if recipe.get('conditions'):
                result['conditions'] = [{'type':'tp:feature','expression':c} for c in recipe['conditions']]
            required_tags = ['forge:ingots/copper'] if recipe['source'].endswith('OreDictRecipes.java') else []
            for required_tag in required_tags:
                result.setdefault('conditions', []).append({'type':'forge:not', 'value':{'type':'forge:tag_empty','tag':required_tag}})
            write(RESOURCE / 'tp/recipes' / f'{name}.json', result)
            converted.append(name)
            expectations.append({'name':name,'conditions':recipe.get('conditions',[]),'tags':required_tags})
        except (ValueError,KeyError,IndexError) as error:
            pending.append(dict(recipe, reason=str(error)))
    # The original ore dictionary declarations become shared item tags.
    tags = {}
    for entry in CATALOG['entries']:
        for assignment in entry['assignments']:
            match = re.search(r'setOreDictName\("([^\"]+)"\)', assignment['expression'])
            if match:
                tags.setdefault(tag(match[1]),[]).append('tp:' + entry['id'])
    tags['forge:sugar_cane'] = ['minecraft:sugar_cane', 'tp:dead_reed']
    tags.update({'tp:juicer':['tp:juicer'], 'tp:torches':['minecraft:torch','tp:stone_torch'],
                 'tp:logs1':['minecraft:oak_log','minecraft:spruce_log','minecraft:birch_log','minecraft:jungle_log'],
                 'tp:logs2':['minecraft:acacia_log','minecraft:dark_oak_log'],
                 'tp:polished_andesite':['minecraft:polished_andesite'],
                 'tp:polished_diorite':['minecraft:polished_diorite'],
                 'tp:polished_granite':['minecraft:polished_granite']})
    for _, name, ore, dye in dusts:
        tags[tag(ore)] = ['tp:' + name]
    for key, items in tags.items():
        namespace, name = key.split(':')
        write(RESOURCE / namespace / 'tags/items' / (name + '.json'), {'replace':False,'values':sorted(set(items))})
    # Furnace registrations were outside the recipe registry in 1.12.2.
    smelting = []
    for path in (JAVA / 'recipes').glob('*.java'):
        if path.stem == 'OreDictRecipes': continue
        raw = strip_comments(path.read_text(encoding='utf-8-sig'))
        for m in re.finditer(r'GameRegistry.addSmelting\(([\s\S]*?)\);',raw):
            args = split_args(m[1])
            try:
                output = stack(args[1])
                source = ingredient(args[0])
                name = 'smelting_' + output['item'].split(':')[1]
                conditions=enclosing_conditions(raw,m.start())
                result={'type':'minecraft:smelting','ingredient':source,
                      'result':output['item'],'experience':float(args[2].rstrip('Ff')),'cookingtime':200}
                if conditions: result['conditions']=[{'type':'tp:feature','expression':c} for c in conditions]
                write(RESOURCE / 'tp/recipes' / (name + '.json'),result)
                smelting.append(name)
                expectations.append({'name':name,'conditions':conditions})
            except ValueError as error:
                pending.append({'source':path.name,'reason':str(error),'args':args})
    # Preserve the vanilla recipe when Apple Pro is off; replace its former runtime removal with a data condition.
    client = Path.home()/'.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar'
    with zipfile.ZipFile(client) as archive:
        golden_apple = json.loads(archive.read('data/minecraft/recipes/golden_apple.json'))
    golden_apple['conditions'] = [{'type':'tp:feature','expression':'!ConfigHandler.ApplePro'}]
    write(RESOURCE/'minecraft/recipes/golden_apple.json', golden_apple)
    expectations.append({'namespace':'minecraft','name':'golden_apple','conditions':['!ConfigHandler.ApplePro']})
    write(ROOT / 'migration/recipe-report.json', {'converted':converted,'smelting':smelting,'pending':pending})
    write(RESOURCE / 'tp/port_audit/recipes.json',expectations)
    print(json.dumps({'converted':len(converted),'pending':len(pending),
                      'reasons': sorted({r['reason'] for r in pending})}, indent=2))


if __name__ == '__main__':
    main()
