"""Preserve all configuration defaults and ranges in a ForgeConfigSpec."""
import json
from inventory import ROOT, collect

defaults = collect()['config_defaults']
lines = []
for key, data in defaults.items():
    args = data['args']
    default = data['default']
    description = json.loads(args[-1])
    path = ', '.join(json.dumps(part) for part in key.split('.'))
    if default in ('true','false'):
        definition = f'builder.define(List.of({path}), {default})'
    else:
        default = default.rstrip('Ff')
        low, high = args[3].rstrip('Ff'), args[4].rstrip('Ff')
        if key.endswith(('_min','_max')):
            low,high='-64','319'
            description += ' Minecraft 1.20.1 accepts heights from -64 to 319; placement is clamped to the dimension build height.'
        if '.' in default:
            definition = f'builder.defineInRange(List.of({path}), (double) {default}, (double) {low}, (double) {high})'
        else:
            definition = f'builder.defineInRange(List.of({path}), {default}, {low}, {high})'
    lines.append(f'        VALUES.put({json.dumps(key)}, builder.comment({json.dumps(description)}).{definition.removeprefix("builder.")});')
source = '''package com.kashdeya.tinyprogressions;

import java.util.*;
import net.minecraftforge.common.ForgeConfigSpec;

/** Generated configuration names, defaults and ranges from the preserved source. */
public final class LegacyConfig {
    private static final Map<String, ForgeConfigSpec.ConfigValue<?>> VALUES = new HashMap<>();
    public static final ForgeConfigSpec SPEC;
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
@VALUES@
        SPEC = builder.build();
    }
    public static Object value(String name) {
        ForgeConfigSpec.ConfigValue<?> setting = Objects.requireNonNull(VALUES.get(name), "Unknown config " + name);
        return SPEC.isLoaded() ? setting.get() : setting.getDefault();
    }
    public static boolean enabled(String name) { return (Boolean) value(name); }
    public static int integer(String name) { return ((Number) value(name)).intValue(); }
    public static float decimal(String name) { return ((Number) value(name)).floatValue(); }
}
'''.replace('@VALUES@','\n'.join(lines))
path = ROOT / 'src/main/java/com/kashdeya/tinyprogressions/LegacyConfig.java'
path.write_text(source,encoding='utf-8')
print(f'{len(lines)} configuration options written')
