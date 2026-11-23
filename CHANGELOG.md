# Changelog

## Unreleased

- Build all supported Minecraft and loader combinations from one Stonecutter project.
- Share the command and allocation logic across Fabric, Quilt, NeoForge, and Forge.
- Add a dedicated-server smoke test for the built mod jars.

## 2.0.0

### Added
- Ported the mod to a Fabric + NeoForge multiloader layout.
- Added shared command and leak logic.


## 1.0.0

### Added
- Initial release
- `/leak start <MB> <seconds>` command to start gradual memory leak
- `/leak stop` command to stop active leak
- `/leak clear` command to free leaked memory
- `/leak status` command to check leak progress
- Boss bar progress indicator for admins
- Permission-based access (operator level 4)