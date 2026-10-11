"""Inspect the built/reobfuscated jar's metadata, Java target and legacy binary references."""
import json
import struct
import sys
import tomllib
import zipfile
from pathlib import Path
from inventory import ROOT

properties = dict(line.split('=',1) for line in (ROOT/'gradle.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
jar = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT/'build/libs'/f"{properties['mod_id']}-{properties['mod_version']}.jar"
errors = []
classes = resources = 0
with zipfile.ZipFile(jar) as archive:
    metadata = tomllib.loads(archive.read('META-INF/mods.toml').decode('utf-8'))
    if metadata['mods'][0]['modId'] != 'tp': errors.append('Incorrect mod ID')
    if metadata['mods'][0]['version'] != properties['mod_version']: errors.append('Inconsistent mod version')
    dependencies = {entry['modId']: entry['versionRange'] for entry in metadata['dependencies']['tp']}
    if dependencies.get('minecraft') != '[1.20.1]': errors.append('Minecraft dependency must match 1.20.1 exactly')
    if dependencies.get('forge') != properties['forge_version_range']: errors.append('Inconsistent Forge range')
    if json.loads(archive.read('pack.mcmeta'))['pack']['pack_format'] != 15: errors.append('Incorrect resource pack format')
    if 'mcmod.info' in archive.namelist(): errors.append('Legacy mod metadata packaged')
    for name in archive.namelist():
        if '/gametest/' in name or '/port_audit/' in name or any(driver in name for driver in ('ModelAudit','GameplayAudit','ArmorGalleryScreen','ContentGalleryScreen')):
            errors.append('Developer-only audit packaged: '+name)
        data = archive.read(name)
        if name.endswith('.json'):
            json.loads(data); resources += 1
        if name.endswith('.class'):
            classes += 1
            if struct.unpack('>H',data[6:8])[0] != 61: errors.append('Not Java 17: '+name)
            for reference in (b'net/minecraft/block/', b'net/minecraft/item/', b'net/minecraft/entity/', b'net/minecraft/util/math/', b'net/minecraftforge/fml/relauncher/'):
                if reference in data: errors.append('Legacy binary reference in '+name+': '+reference.decode())
report={'jar':str(jar),'version':metadata['mods'][0]['version'],'classes_checked':classes,'json_resources_checked':resources,'errors':errors}
(ROOT/'migration/jar-audit.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,indent=2))
sys.exit(bool(errors))
