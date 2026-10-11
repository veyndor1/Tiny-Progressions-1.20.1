# Compatibility

| Environment | Status |
| --- | --- |
| Minecraft 1.20.1 with Forge 47.4.10 | Reference build and runtime configuration |
| Later Forge 47.x versions | Accepted by mod metadata; validate the chosen modpack |
| Client and dedicated server | Install matching mod versions on both sides |
| Java 17 | Required |
| Fabric, Quilt, or NeoForge | Not supported by this Forge artifact |
| Minecraft versions other than 1.20.1 | Outside the declared version range |
| Existing Minecraft 1.12.2 worlds | No automatic conversion provided |

## Alpha release

Version `4.0.0-alpha.1` is a prerelease. Keep world backups and evaluate the mod in a separate instance before adding it to an established server or modpack.

## Modpacks

Compatibility depends on the complete mod list, configuration, and recipe changes. Forge version acceptance does not establish compatibility with every Forge version or modpack. Report reproducible interactions through this repository's issue form.

## Port behavior

The port retains original content identifiers and artwork where applicable. Minecraft and Forge APIs differ between 1.12.2 and 1.20.1; implementation details and some integration behavior may differ. The repository does not offer a general save, metadata, or third-party integration migration tool.

[Return to the README](../README.md)
