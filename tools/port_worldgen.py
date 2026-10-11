"""Write dynamic-config features and modern biome modifiers for the active legacy generators."""
from inventory import ROOT
import json

data=ROOT/'src/main/resources/data/tp'
def write(path,value):
    target=data/path
    target.parent.mkdir(parents=True,exist_ok=True)
    target.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')

groups={'wub':False,'ender':False,'lava':False,'water':False,'charcoal':True,'nether_wub':True,'nether_lava':True}
for name,nether in groups.items():
    feature='ore_'+name
    write(f'worldgen/configured_feature/{feature}.json',{'type':'tp:legacy_ore','config':{'group':name}})
    write(f'worldgen/placed_feature/{feature}.json',{'feature':'tp:'+feature,'placement':[{'type':'minecraft:biome'}]})
    write(f'forge/biome_modifier/{feature}.json',{'type':'forge:add_features',
          'biomes':'#minecraft:is_nether' if nether else '#minecraft:is_overworld','features':'tp:'+feature,'step':'underground_ores'})
write('worldgen/configured_feature/legacy_plants.json',{'type':'tp:legacy_plants','config':{}})
write('worldgen/placed_feature/legacy_plants.json',{'feature':'tp:legacy_plants','placement':[{'type':'minecraft:biome'}]})
write('forge/biome_modifier/legacy_plants.json',{'type':'forge:add_features','biomes':'#minecraft:is_overworld','features':'tp:legacy_plants','step':'vegetal_decoration'})
print('7 ore groups and legacy plants converted to modern features and biome modifiers')
