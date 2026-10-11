# Installation

## Requirements

| Component | Version |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.4.10 or later within 47.x |
| Java | 17 |
| Player mod file | `tp-4.0.0-alpha.2.jar` |

Download the player jar and its SHA-256 checksum from [GitHub Releases](https://github.com/veyndor1/Tiny-Progressions-1.20.1/releases/tag/v4.0.0-alpha.2). GitHub's automatically generated source archives contain source code; they are not installable mod jars.

## Single-player and client

1. Create a Minecraft 1.20.1 instance with Forge 47.x.
2. Configure the launcher to use Java 17.
3. Stop the game and open the instance directory.
4. Copy the player jar into `mods/`. Remove older copies of this port from that directory.
5. Start the game and confirm that **Tiny Progressions** appears in the Mods list.

## Dedicated server

1. Prepare a Minecraft 1.20.1 Forge server using Java 17.
2. Complete the server's normal first-run setup and review the Minecraft EULA.
3. Stop the server and copy the player jar into its `mods/` directory.
4. Install the same player jar on every connecting client.
5. Start the server, then review the generated world configuration.

See [configuration](CONFIGURATION.md) for world-specific settings. Use the server operator's existing launch command; this repository does not include a preconfigured public server.

## Verify a download

Compare the output with the checksum included in the same release.

Windows PowerShell:

```powershell
Get-FileHash .\tp-4.0.0-alpha.2.jar -Algorithm SHA256
```

Linux:

```shell
sha256sum tp-4.0.0-alpha.2.jar
```

macOS:

```shell
shasum -a 256 tp-4.0.0-alpha.2.jar
```

## Updating and troubleshooting

- Back up the world before updating. Stop the game or server before replacing the jar.
- Keep the client and server mod versions identical.
- A Java class-version error usually indicates that the instance is using an incompatible Java runtime.
- A loader mismatch requires checking both Minecraft and Forge versions; Fabric cannot load this Forge jar.
- If startup fails, reproduce the issue in a separate instance and report the relevant, sanitized error excerpt through this repository's issue form.

Do not install the developer `-audit.jar` companion for ordinary gameplay. Existing 1.12.2 worlds are not automatically converted.

[Return to the README](../README.md)
