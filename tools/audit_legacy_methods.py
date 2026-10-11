"""Inventory source methods beyond registry boilerplate for the feature parity review."""
import json
import re
from inventory import ROOT, JAVA, collect, strip_comments
boilerplate={'getOreDictName','getIsRepairable','setUnlocalName','setHarvestLvl','setRenderLayer','getToolClasses','addInformation','getArmorTexture','getItemAttributeModifiers'}
classes={a['class'] for e in collect()['entries'] for a in e['assignments']}
rows=[]
for path in JAVA.rglob('*.java'):
    source=strip_comments(path.read_text(encoding='utf-8-sig'))
    # Include bases inherited by registered classes as well as those classes themselves.
    if path.stem not in classes and not any(re.search(r'extends\s+'+re.escape(path.stem)+r'\b',strip_comments(p.read_text(encoding='utf-8-sig'))) for p in JAVA.rglob('*.java')): continue
    methods=re.findall(r'(?:public|protected)\s+(?:static\s+)?(?:final\s+)?[\w<>?,\[\]]+\s+(\w+)\s*\(',source)
    methods=sorted(set(methods)-boilerplate)
    if methods: rows.append({'class':path.stem,'source':str(path.relative_to(JAVA)),'methods':methods})
(ROOT/'migration/legacy-methods-audit.json').write_text(json.dumps(rows,indent=2)+'\n',encoding='utf-8')
print(json.dumps(rows,indent=2))
