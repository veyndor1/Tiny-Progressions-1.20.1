# Development

## Build prerequisites

- Java 17 JDK, configured through `JAVA_HOME` or the Gradle Java toolchain.
- Network access for the first dependency download.
- The included Gradle wrapper; a separate Gradle installation is unnecessary.

Build from the repository root:

```shell
./gradlew build
```

Windows:

```powershell
.\build.ps1 build
```

The player artifact is `build/libs/tp-4.0.0-alpha.2.jar`. A separately named audit companion may also be built; the player artifact excludes developer gallery drivers and GameTest fixtures.

## Run the mod

| Task | Purpose |
| --- | --- |
| `runClient` | Launch a development client |
| `runServer` | Launch a development server |
| `runGameTestServer` | Execute the registered Minecraft GameTests |
| `genIntellijRuns` | Generate IntelliJ run configurations |
| `genEclipseRuns` | Generate Eclipse run configurations |

```shell
./gradlew runGameTestServer
```

Inspect the GameTest results, including failures and the count of tests executed. A successful build with no tests executed does not establish gameplay correctness. Dedicated-server setup requires the operator to review and accept the Minecraft EULA.

## Source layout

```text
src/main/java/          Mod implementation and development fixtures
src/main/resources/     Models, textures, recipes, translations, and world generation
src/productionAudit/    Optional companion for testing the packaged player jar
tools/                  Python conversion and validation tools
migration/              Content catalog and legacy metadata mapping
docs/                   User and developer documentation
```

The mod retains the upstream `com.kashdeya.tinyprogressions` package and `tp` identifiers. Preserve stable registry identifiers when modifying existing content.

## Optional conversion tools

The Python conversion tools expect the original 1.12.2 source tree in a sibling directory named `Tiny-Progressions-1.12.2`. Normal builds use the committed 1.20.1 sources and require no legacy checkout.

Some Java registration and configuration files are generated. Review the corresponding generator before regenerating them. Conversion tools are maintenance utilities and are not a substitute for checking runtime behavior.

## Visual review

```powershell
.\build.ps1 runClient '-PmodelAudit=true' '-PvisualAudit=true'
```

This opt-in task creates a disposable development world, renders a content gallery, captures interfaces and armor, and exits. Output remains in ignored local directories. These drivers do not run during ordinary gameplay.

The production audit tools additionally require separate Forge client/server installations under `migration/production-client` and `migration/production-server`. Configure Java 17 through `JAVA_HOME` or `PATH`. These installations and their local data are not distributed with the repository.

## Continuous integration

The repository workflow builds with Java 17 and uploads only the player jar. Minecraft GameTests and visual checks can be run locally; the workflow does not claim to execute them.

[Return to the README](../README.md)
