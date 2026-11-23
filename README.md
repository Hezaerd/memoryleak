# 🧠 Memory Leak

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.11-green?style=for-the-badge" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Mod%20Loader-Fabric-blue?style=for-the-badge" alt="Fabric">
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" alt="License">
</p>

A Fabric mod for **server administrators** to intentionally cause controlled memory leaks. Perfect for stress testing servers, testing memory monitoring tools, or simulating memory pressure scenarios.

## ⚠️ Warning

This mod is designed for **testing and development purposes only**. Using it on production servers without proper understanding can cause crashes and data loss.

## ✨ Features

- **Gradual Memory Leak** - Leak memory over a configurable time period
- **Boss Bar Progress** - Visual feedback showing leak progress (admin-only)
- **Full Control** - Start, stop, clear, and check status at any time
- **Permission-Based** - Only server operators can use leak commands

## 📦 Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) (0.18.4+)
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download the latest release from [Modrinth](https://modrinth.com/mod/memory-leak)
4. Place the `.jar` file in your `mods` folder

## 🎮 Commands

All commands require **operator permissions** (level 4).

| Command | Description |
|---------|-------------|
| `/leak start <MB> <seconds>` | Start leaking `<MB>` megabytes over `<seconds>` seconds |
| `/leak stop` | Stop the current leak (keeps leaked memory) |
| `/leak clear` | Stop leak and free all leaked memory |
| `/leak status` | Check current leak status |

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

## 📜 License

This project is licensed under the [MIT License](LICENSE).

## 🤝 Contributing

Contributions are welcome! Feel free to open issues or submit pull requests.

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/Hezaerd">Hezaerd</a>
</p>
