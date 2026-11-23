# Memory Leak

Memory Leak is a Minecraft server mod for generating controlled memory pressure. It allocates memory over a set number of server ticks and shows progress in a boss bar for administrators.

Use it on test servers. Excessive allocations can crash a server.

## Supported Minecraft versions

| Minecraft | Fabric and Quilt | NeoForge | Forge | Java |
|---|---|---|---|---|
| 1.20.1 | Yes | | Yes | 17 |
| 1.21.1 | Yes | Yes | | 21 |
| 1.21.10 | Yes | Yes | | 21 |
| 1.21.11 | Yes | Yes | | 21 |
| 26.1.1, 26.1.2, 26.2 | Yes | Yes | | 25 |
| 26.3 | Yes | Yes | | 25 |

Download the jar for your Minecraft version and loader from [Modrinth](https://modrinth.com/mod/memory-leak) and place it in the server's `mods` directory. On Fabric and Quilt, also install [Fabric API](https://modrinth.com/mod/fabric-api) for your Minecraft version. Clients can join without Memory Leak installed. For Minecraft 26.1.1 and 26.2, use the 26.1.2 jar.

## Commands

The commands require level 4 operator access or Minecraft's equivalent administrator permission.

| Command | Description |
|---|---|
| `/leak start <MB> <seconds>` | Allocate 1 to 10,000 MB over 1 to 3,600 seconds. |
| `/leak stop` | Stop allocating and keep the leaked memory. |
| `/leak clear` | Stop allocating and release all leaked memory. |
| `/leak status` | Show the current progress or total leaked memory. |

## Build the mods

Set `JAVA_HOME` to a JDK 25 installation and run `./gradlew buildAll` from the repository root. On Windows use `.\gradlew.bat buildAll`. Gradle finds or downloads the JDKs required by the individual nodes. The task runs the core tests and writes each mod jar to `versions/<node>/build/libs/` and each plugin jar to `<platform>/build/libs/`.

To build one jar, run `./gradlew :<node>:assemble`, for example `./gradlew :1.20.1-forge:assemble`.

## Add a Minecraft version

1. Copy an existing loader's `versions/<minecraft>-<loader>/gradle.properties` into a new node directory. Set the Minecraft, Java, loader, and API versions.
2. Set `minecraft_versions` to the comma-separated Minecraft releases you plan to verify. This list controls loader metadata and Modrinth game versions. Supply `fabric_api_version_<minecraft>` or `loader_version_<minecraft>` if a second release uses a different runtime dependency.
3. Make Minecraft or loader API changes in `src/main/java/com/hezaerd/memoryleak/MemoryLeak.java` using Stonecutter conditions. Keep allocation and command replies in `core/`.
4. Build the matrix and smoke-test the new node and every release in its list.

## Run dedicated server smoke tests

Set `JAVA_HOME_17`, `JAVA_HOME_21`, and `JAVA_HOME_25` to the corresponding JDK directories and build the jars. Run the script with Python 3.10 or later. Passing `--accept-eula` accepts the [Minecraft EULA](https://aka.ms/MinecraftEULA) for the test servers.

```sh
python scripts/smoke-mods.py --accept-eula
python scripts/smoke-mods.py --accept-eula 1.20.1-forge
```

The script uses throwaway server directories under `.smoke/`. It leaves the console and installer logs there for inspection.

## Server plugins

Build once per platform. Copy the matching jar to the server's plugin directory.

| Platform | Jar in the module's `build/libs` directory | Compatibility | Install in |
|---|---|---|---|
| Bukkit, Spigot, Paper, Purpur, Folia | `memory-leak-bukkit-<version>.jar` | Minecraft 1.13.2 through 26.3, where the platform has a release | `plugins/` |
| Velocity | `memory-leak-velocity-<version>.jar` | Velocity 3.5.1 and 4.3.1 | `plugins/` |
| BungeeCord, Waterfall | `memory-leak-bungee-<version>.jar` | BungeeCord build 2111 and Waterfall final build 615 | `plugins/` |
| Sponge | `memory-leak-sponge-<version>.jar` | SpongeAPI 17 on Minecraft 1.21.10 | `mods/` |

The command and boss bar use the `memoryleak.admin` permission. Bukkit and Sponge grant access to operators by default. Proxies have no Minecraft operator list. The proxy console has access by default, and player access requires the proxy permission. BungeeCord and Waterfall have no boss bar API.

The Bukkit jar uses the Bukkit scheduler on Bukkit-derived servers and selects Folia's global region scheduler at runtime. Reflection keeps the jar compatible with the Spigot 1.13.2 API. The smoke test covers Paper at both ends of the range and recent Folia and Purpur. Spigot and CraftBukkit require BuildTools, so their compatibility is based on the Bukkit API exercised on Paper and Purpur. Waterfall is end-of-life. Proxy Modrinth game tags describe the tested builds' client protocol support. The BungeeCord and Waterfall listing uses their shared range through 26.2.

### Build and smoke-test the plugins

Set `JAVA_HOME` to JDK 25. Use Python 3.8 or newer. Run these commands in Windows PowerShell.

```powershell
.\gradlew.bat --configure-on-demand :core:test :bukkit:build :velocity:build :bungee:build :sponge:build
$java8 = .\gradlew.bat --configure-on-demand -q :bukkit:printJava8
python scripts/smoke-plugins.py --java8 $java8 --java21 "$env:JAVA21_HOME/bin/java.exe" --java25 "$env:JAVA_HOME/bin/java.exe"
```

Set `JAVA21_HOME` to a Java 21 installation before running the smoke test. The Gradle task provisions Java 8 through Foojay for Paper 1.13.2. Pass server names to run a subset, or use `--help` to list the names. Each run accepts the EULA for its fresh server directory under `build/smoke-plugins/` and saves the console log there.

## License

Memory Leak is licensed under the [MIT License](LICENSE).
