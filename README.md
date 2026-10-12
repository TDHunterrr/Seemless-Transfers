<div align="center">

  <img src="fabric/src/main/resources/assets/seemless-transfers/icon.png" alt="Seemless Transfers Logo" width="160" height="160" />

  # Seemless Transfers

  **High-speed, zero-lag server transfers with precise coordinate teleportation across Fabric, Paper, Folia, and NeoForge.**

  [![Minecraft](https://img.shields.io/badge/Minecraft-1.20.5%20--%2026.3-brightgreen.svg)](https://www.minecraft.net/)
  [![Platform](https://img.shields.io/badge/Platform-Fabric%20%7C%20Paper%20%7C%20Folia%20%7C%20NeoForge-blue.svg)](#features)
  [![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red.svg)](LICENSE)
  [![Author](https://img.shields.io/badge/Author-TD__Hunterrr-orange.svg)](https://github.com/TDHunterrr)

</div>

---

## 🚀 Overview

**Seemless Transfers** is an ultra-fast, multi-loader Minecraft server mod and plugin designed for network administrators and server clusters. Leveraging Minecraft 1.20.5+'s native client transfer protocol combined with a high-speed direct peer communication socket, it allows administrators to transfer players between different servers instantaneously and place them precisely at designated coordinates and view angles—with zero chunk-loading lag and zero connection cooldowns.

---

## ✨ Features

- ⚡ **Zero-Lag Instant Transfers:** Direct socket preloading (`PeerBridge`) alerts the destination server before the player arrives, asynchronously pre-loading target chunks so players appear instantly without loading screens or falling through the world.
- 🎯 **Precise Coordinate & Rotation Teleportation:** Supports full 3D coordinates plus `<yaw>` and `<pitch>` angles for pinpoint arrival accuracy.
- 🎨 **Multi-Colored Brigadier Syntax Highlighting:** Native Brigadier command nodes registered across all platforms (Paper, Fabric, NeoForge). Displays rich multi-colored parameter hints in chat (yellow for player, green for IP, aqua for port, gold for coordinates and rotation).
- 🍪 **Cryptographic Transfer Cookies:** Automatically encodes and verifies HMAC-SHA256 client cookies as an instant fallback mechanism.
- ⏱️ **Zero Connection Throttling:** Automatically overrides Bukkit/Paper's legacy connection throttling (`0ms`), allowing players to hop between servers repeatedly without triggering `"Connection throttled!"` kicks.
- 🌐 **Cross-Platform Compatibility:** Operates seamlessly between **Fabric**, **Paper**, **Folia**, and **NeoForge** servers.

---

## 📜 Command Reference

### `/sl <player> <ip> <port> <X> <Y> <Z> <yaw> <pitch>`

Fast-transfers the specified player to a target server and positions them at the given coordinates upon arrival.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `<player>` | Player / Selector | Target online player to transfer (e.g. `Cosmic`, `@p`) |
| `<ip>` | String | Destination server IP address or hostname (e.g. `127.0.0.1`, `play.example.com`) |
| `<port>` | Integer `[1-65535]` | Destination server game port (e.g. `25565`) |
| `<X>` | Double `[-30M to +30M]` | Target X coordinate |
| `<Y>` | Double `[-64 to 320]` | Target Y coordinate |
| `<Z>` | Double `[-30M to +30M]` | Target Z coordinate |
| `<yaw>` | Float `[-180.0 to 180.0]` | *(Optional)* Target horizontal view rotation |
| `<pitch>` | Float `[-90.0 to 90.0]` | *(Optional)* Target vertical view pitch angle |

### Permissions
- **Fabric / NeoForge:** Requires Operator Level 2 (`LEVEL_GAMEMASTERS`).
- **Paper / Folia:** Requires Operator status (`op`) or the `seemless.admin` permission node.

---

## 📥 Installation

Place the compiled universal jar into the corresponding folder of each server:

- **Fabric:** Drop into the `mods/` folder (requires [Fabric Loader](https://fabricmc.net/) and [Fabric API](https://modrinth.com/mod/fabric-api)).
- **Paper / Folia:** Drop into the `plugins/` folder (Paper / Folia 1.20.6+).
- **NeoForge:** Drop into the `mods/` folder.

---

## 🛠️ Building from Source

To compile the universal hybrid jar containing Fabric, Paper, and NeoForge implementations:

```powershell
./gradlew clean build --no-daemon
```

Compiled jars will be located in:
- Root universal package: `build/libs/seemless-transfers-0.0.1.jar`
- Fabric loader mod: `fabric/build/libs/`
- Paper server plugin: `paper/build/libs/`
- NeoForge mod: `neoforge/build/libs/`

---

## ⚖️ Author & License

Developed and created by **TD_Hunterrr**.

Copyright (c) 2026 **TD_Hunterrr**. All Rights Reserved.

- **Distribution:** This mod/plugin may **ONLY** be distributed, hosted, and made available for download by the original creator, **TD_Hunterrr**. Unauthorized mirroring, re-uploading, or redistribution is strictly prohibited.
- **Modpacks:** Inclusion in public, private, or curated modpacks (CurseForge, Modrinth, FTB, Technic, etc.) without explicit prior written consent from the author is **forbidden**.
- For full legal terms and conditions, consult the [LICENSE](LICENSE) file.
