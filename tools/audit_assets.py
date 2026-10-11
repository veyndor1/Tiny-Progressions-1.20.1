"""Check referenced models and textures against the actual 1.20.1 client resources."""
from pathlib import Path
import json
import re
import zipfile
from inventory import ROOT

resources = ROOT / 'src/main/resources'
client = Path.home() / '.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar'
vanilla = zipfile.ZipFile(client)
vanilla_paths = set(vanilla.namelist())
forge_path=next((Path.home()/'.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge').glob('*/forge-*_mapped_official_1.20.1.jar'))
forge=zipfile.ZipFile(forge_path)
forge_paths=set(forge.namelist())
errors, visited = [], set()
effective_cache = {}


def resource_path(reference, kind, suffix):
    namespace, name = reference.split(':',1) if ':' in reference else ('minecraft',reference)
    return f'assets/{namespace}/{kind}/{name}{suffix}'


def exists(path):
    return (resources / path).exists() or path in vanilla_paths or path in forge_paths

def read_resource(path):
    if (resources/path).exists(): return (resources/path).read_text(encoding='utf-8')
    return (vanilla if path in vanilla_paths else forge).read(path).decode()


def model(reference, validate=True):
    if (reference in visited and not validate) or reference.startswith('builtin/') or reference in ('minecraft:builtin/generated', 'minecraft:builtin/entity'):
        return
    visited.add(reference)
    path = resource_path(reference,'models','.json')
    if not exists(path):
        errors.append({'type':'model','reference':reference}); return
    raw = read_resource(path)
    data = json.loads(raw)
    if 'parent' in data: model(data['parent'],False)
    for texture in data.get('textures',{}).values():
        if texture.startswith('#'): continue
        if not exists(resource_path(texture,'textures','.png')):
            errors.append({'type':'texture','reference':texture,'model':reference})
    if not validate: return
    effective = effective_model(reference)
    bindings = effective.get('textures',{})
    for element in effective.get('elements',[]):
        for face in element.get('faces',{}).values():
            texture=face.get('texture','')
            seen=set()
            while texture.startswith('#'):
                key=texture[1:]
                if key in seen or key not in bindings:
                    errors.append({'type':'texture_binding','reference':texture,'model':reference})
                    break
                seen.add(key)
                texture=bindings[key]


def effective_model(reference):
    if reference in effective_cache: return effective_cache[reference]
    path=resource_path(reference,'models','.json')
    if not exists(path): return {}
    data=json.loads(read_resource(path))
    parent=effective_model(data['parent']) if data.get('parent') and 'builtin/' not in data['parent'] else {}
    result=dict(parent,**data)
    result['textures']=dict(parent.get('textures',{}),**data.get('textures',{}))
    effective_cache[reference]=result
    return result


def references(node):
    if isinstance(node,dict):
        for key,val in node.items():
            if key == 'model' and isinstance(val,str): model(val)
            else: references(val)
    elif isinstance(node,list):
        for val in node: references(val)


content = (ROOT / 'src/main/java/com/kashdeya/tinyprogressions/Content.java').read_text()
for name in re.findall(r'add(?:Item|Block)\("([^"]+)"',content):
    model('tp:item/' + name)
for state in (resources / 'assets/tp/blockstates').glob('*.json'):
    data=json.loads(state.read_text())
    for key,variant in data.get('variants',{}).items():
        if key and any('=' not in part for part in key.split(',')):
            errors.append({'type':'variant_key','reference':key,'blockstate':state.name})
        for choice in variant if isinstance(variant,list) else [variant]:
            if 'model' not in choice:
                errors.append({'type':'variant_model','reference':key,'blockstate':state.name})
    references(data)
report = {'models_checked':len(visited),'errors':errors}
(ROOT / 'migration/asset-audit.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
raise SystemExit(bool(errors))
