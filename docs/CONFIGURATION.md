# Configuration

Tiny Progressions uses Forge **server configuration**. The active world stores its settings at:

```text
<world>/serverconfig/tinyprogressions-server.toml
```

In single-player, this file is inside the selected save directory. On a dedicated server, it is inside the configured world directory. Server values are synchronized to clients when they connect.

## Edit an existing world

1. Start the world once so Forge creates the configuration file.
2. Stop the world or server and back up the file.
3. Edit the generated TOML, preserving section names, capitalization, and documented ranges.
4. Restart the world or server so settings and recipe conditions are loaded consistently.

## Set defaults for new worlds

Place a complete configuration template at:

```text
defaultconfigs/tinyprogressions-server.toml
```

Forge uses that template when creating new worlds. Updating the template does not replace the settings of an existing world; edit that world's `serverconfig` file separately.

## Example

This excerpt shows existing option names. Edit the matching section in the generated file rather than duplicating a TOML section.

```toml
[ConfigHandler]
Cobblegen = true
iron_furnace = true
pouch = true
infin_bucket = true
```

| Option | Purpose |
| --- | --- |
| `Cobblegen` | Enables the basic cobblestone generator feature |
| `iron_furnace` | Enables the Iron Furnace feature |
| `pouch` | Enables the portable pouch feature |
| `infin_bucket` | Enables the infinite water bucket feature |

The generated file includes additional material, armor, growth, durability, and world-generation options. Their definitions and allowed ranges are available in [LegacyConfig.java](../src/main/java/com/kashdeya/tinyprogressions/LegacyConfig.java).

Feature switches control recipes and creative entries while retaining registry identifiers. Disabling a feature is not a world-data conversion or a general-purpose removal of existing placed blocks. World-generation settings affect newly generated terrain; they do not rewrite already generated chunks.

[Return to the README](../README.md)
