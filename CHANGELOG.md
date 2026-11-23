# Changelog

## 2.0.0

### Added
- Ported the mod to a Fabric + NeoForge multiloader layout.
- Added shared command and leak logic.
- Added server plugins for Bukkit, Spigot, Paper, Purpur, Folia, Velocity, BungeeCord, Waterfall, and SpongeAPI 17.


## 1.0.0

### Added
- Initial release
- `/leak start <MB> <seconds>` command to start gradual memory leak
- `/leak stop` command to stop active leak
- `/leak clear` command to free leaked memory
- `/leak status` command to check leak progress
- Boss bar progress indicator for admins
- Permission-based access (operator level 4)