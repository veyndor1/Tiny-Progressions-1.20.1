"""Report source config references still missing from active handwritten consumers."""
import json
import re
from inventory import ROOT, JAVA, collect, strip_comments

defaults = collect()['config_defaults']
modern = '\n'.join(p.read_text(encoding='utf-8') for p in (ROOT/'src/main/java').rglob('*.java')
                   if p.name != 'LegacyConfig.java' and 'gametest' not in p.parts)
recipes = '\n'.join(p.read_text(encoding='utf-8') for p in (ROOT/'src/main/resources/data').rglob('*.json'))
dynamic_consumers = {
    'ConfigHandler.extra_drops_vanilla':'loot/LegacyBlockDrops.java',
    **{'ConfigHandler.'+key:'loot/LegacyBlockDrops.java' for key in ('pearDropsChance','pearDropsAmount','peachDropsChance','peachDropsAmount','BoneDrops','BoneDropsChance','BoneAmount','SkullDrops','SkullDropsChance','SkullAmount','stickDropsChance','stickDropsAmount','appleDropsChance','appleDropsAmount')},
    **{'ConfigHandler.'+key:'block/GrowthCrystalBlock.java' for key in ('GrowthParticalTicks','GrowthUpgradeParticalTicks','GrowthUpgradeTwoParticalTicks')},
    **{'ConfigHandler.'+key:'item/MedicalItem.java' for key in ('smallBandageHealStack','largeBandageStack','largeBandageDuration','largeBandageBoostTime','smallMedHealStack','smallMedDuration','smallMedBoostTime','LargeMedHealStack','LargeMedDuration','largeMedBoostTime')},
    **{'ArmorHandler.'+key:'ArmorEffects.java' for key in ('wither_resistance_lvl','wither_fire_lvl','wither_strength_lvl','dragon_resistance_lvl','dragon_fire_lvl','dragon_strength_lvl','obsidian_armor_resistance_lvl','lapis_armor_water_lvl','quartz_armor_strength_lvl','redstone_armor_speed_lvl','lava_armor_resistance_lvl','lava_armor_fire_lvl')},
    **{'ConfigHandler.'+key:'worldgen/LegacyOreFeature.java' for key in ('overworld_wub','nether_wub','CharcoalWorldgen')},
}
for prefix, size in [('wub_block','count'),('nether_wub_block','count'),('charcoal','size'),('ender_ore','size'),('nether_lava_block','size'),('lava_block','size'),('water_block','size')]:
    for suffix in ('frequency','min','max',size): dynamic_consumers['ConfigHandler.'+prefix+'_'+suffix]='worldgen/LegacyOreFeature.java'
missing = []
for key in defaults:
    if key in modern or key in recipes or key in dynamic_consumers:
        continue
    files = [str(p.relative_to(JAVA)) for p in JAVA.rglob('*.java')
             if key in strip_comments(p.read_text(encoding='utf-8-sig')) and p.stem not in ('ConfigHandler','ArmorHandler','DamageHandler')]
    missing.append({'key':key,'default':defaults[key]['default'],'legacy_consumers':files})
report={'total_settings':len(defaults),'known_dynamic_consumers':dynamic_consumers,'settings_without_consumer':missing,
        'note':'Unused legacy declarations are preserved, not assigned invented behavior. Dynamic consumer mappings are source-reviewed.'}
(ROOT/'migration/config-usage-audit.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(missing,indent=2))
