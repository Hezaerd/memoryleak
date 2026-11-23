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

Set `JAVA_HOME` to a JDK 25 installation and run `./gradlew buildMods` from the repository root. On Windows use `.\gradlew.bat buildMods`. Gradle finds or downloads the JDKs required by the individual nodes. The task runs the core tests and writes each distributable jar to `versions/<node>/build/libs/`.

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

## License

Memory Leak is licensed under the [MIT License](LICENSE).
