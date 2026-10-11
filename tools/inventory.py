"""Extract a reviewable source-of-truth catalog from the preserved 1.12.2 sources.

No Minecraft dependency is needed. This is an inventory, not a Java compiler.
Unresolved expressions are retained rather than silently approximated.
"""
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]
LEGACY = ROOT.parent / 'Tiny-Progressions-1.12.2'
JAVA = LEGACY / 'src/main/java/com/kashdeya/tinyprogressions'


def strip_comments(text):
    pattern = r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/'
    return re.sub(pattern, lambda m: ' ' if m[0].startswith(('/',)) else m[0], text)


def split_args(text):
    args, start, depth, quote, escape = [], 0, 0, None, False
    for i, c in enumerate(text):
        if quote:
            if escape:
                escape = False
            elif c == '\\':
                escape = True
            elif c == quote:
                quote = None
        elif c in '\'"':
            quote = c
        elif c in '([{':
            depth += 1
        elif c in ')]}':
            depth -= 1
        elif c == ',' and depth == 0:
            args.append(text[start:i].strip())
            start = i + 1
    if text[start:].strip():
        args.append(text[start:].strip())
    return args


def constructor(expression):
    m = re.search(r'new\s+(\w+)\s*\(', expression)
    if not m:
        return None, []
    depth, quote, escape = 1, None, False
    for end in range(m.end(), len(expression)):
        c = expression[end]
        if quote:
            if escape:
                escape = False
            elif c == '\\':
                escape = True
            elif c == quote:
                quote = None
        elif c in '\'"':
            quote = c
        elif c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0:
                return m[1], split_args(expression[m.end():end])
    raise ValueError(expression)


def enclosing_conditions(text, position):
    conditions = []
    for match in re.finditer(r'\bif\s*\(([^()]*)\)\s*', text[:position]):
        start = match.end()
        if start >= len(text): continue
        if text[start] == '{':
            depth = 1
            end = start + 1
            while depth and end < len(text):
                if text[end] == '{': depth += 1
                elif text[end] == '}': depth -= 1
                end += 1
        else:
            end = text.find(';',start) + 1
        if start <= position < end:
            conditions.append(match[1].strip())
    return conditions


def collect():
    entries = []
    for group in ['TechBlocks', 'TechItems', 'TechTools', 'TechArmor', 'TechFoods']:
        path = JAVA / 'inits' / f'{group}.java'
        raw = path.read_text(encoding='utf-8-sig')
        text = strip_comments(raw)
        fields = re.findall(r'public static (?:Block|Item|ItemArmor)\s+(\w+)\s*;', text)
        for field in fields:
            matches = list(re.finditer(r'\b' + field + r'\s*=\s*(?:\(ItemArmor\)\s*)?(new\s+[\s\S]*?);', text))
            assignments = []
            for match in matches:
                cls, args = constructor(match[1])
                line = text[:match.start()].count('\n') + 1
                assignments.append({'class': cls, 'args': args, 'expression': match[1], 'line': line,
                                    'conditions': enclosing_conditions(text,match.start())})
            entries.append({'id': field.lower(), 'field': field, 'group': group,
                            'source': str(path.relative_to(LEGACY)).replace('\\', '/'),
                            'assignments': assignments})
    defaults = {}
    for handler in ('ConfigHandler','ArmorHandler','DamageHandler','FuelHandler'):
        raw_handler = strip_comments((JAVA / f'handlers/{handler}.java').read_text(encoding='utf-8-sig'))
        for m in re.finditer(r'public static (boolean|int|float)\s+(\w+)\s*=\s*(true|false|[-\d.]+[Ff]?)\s*;',raw_handler):
            name, default = f'{handler}.{m[2]}', m[3]
            args = [json.dumps(m[2]),json.dumps('Legacy defaults'),default]
            if m[1] != 'boolean': args += ['0','Integer.MAX_VALUE' if m[1] == 'int' else '3.4028235E38F']
            args += [json.dumps('Default retained from the original ' + handler + '.')]
            defaults[name] = {'name':args[0],'category':args[1],'default':default,'args':args}
    config = strip_comments((JAVA / 'config/TinyConfig.java').read_text(encoding='utf-8-sig'))
    for m in re.finditer(r'(\w+\.\w+)\s*=\s*config\.get(?:Boolean|Int|Float)\(([^;]+)\);', config):
        args = split_args(m[2])
        defaults[m[1]] = {'name': args[0], 'category': args[1], 'default': args[2], 'args': args}
    recipes = []
    for path in (JAVA / 'recipes').glob('*.java'):
        if path.stem == 'OreDictRecipes':
            continue  # never registered by the original entry point
        text = strip_comments(path.read_text(encoding='utf-8-sig'))
        for m in re.finditer(r'(\w+(?:\[\w+\])?)\s*=\s*new\s+(ShapedRecipe|ShapelessRecipe)\(([\s\S]*?)\);', text):
            recipes.append({'name': m[1].lower(), 'type': m[2], 'args': split_args(m[3]),
                            'source': path.name, 'line': text[:m.start()].count('\n') + 1,
                            'conditions':enclosing_conditions(text,m.start())})
    return {'source_minecraft': '1.12.2', 'target_minecraft': '1.20.1',
            'entries': entries, 'config_defaults': defaults, 'recipes': recipes}


if __name__ == '__main__':
    catalog = collect()
    target = ROOT / 'migration/catalog.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    counts = {g: sum(e['group'] == g for e in catalog['entries']) for g in sorted({e['group'] for e in catalog['entries']})}
    print(json.dumps({'catalog': str(target), 'entries': counts, 'recipes': len(catalog['recipes']),
                      'unassigned': [e['id'] for e in catalog['entries'] if not e['assignments']]}, indent=2))
