# ⚡ Seemless Transfers

> **Zero-delay, proxy-free, cross-server teleportation with chunk preloading for Fabric, Paper, and NeoForge.**

---

## 🌐 What is Seemless Transfers?

**Seemless Transfers** eliminates the need for heavyweight proxy layers like BungeeCord or Velocity when connecting independent Minecraft servers. Leveraging Minecraft 1.20.5+ native client transfer protocol alongside a custom background socket bridge, Seemless Transfers lets administrators teleport players between different physical servers or containers **instantly**, landing at exact coordinates and view angles without chunk loading lag or connection throttles.

Whether you run a massive minigame hub, a multi-world SMP network, or a private cluster across Fabric, Paper, and NeoForge, **Seemless Transfers** handles cross-server transit in the blink of an eye.

---

## ✨ Key Features

* 🚀 **Zero-Proxy Architecture** — Direct server-to-server client transfers using vanilla Minecraft 1.20.5+ packets (`ClientboundTransferPacket`). No BungeeCord, Waterfall, or Velocity required.
* 📦 **`PeerBridge` Chunk Preloading** — Origin servers notify destination servers over an ultra-fast background socket. The destination server pre-generates and loads the arrival chunk **before** the client arrives, eliminating the infamous "falling through the void" chunk loading freeze.
* 🎯 **Precision Coordinates & Angles** — Transfer players to exact decimal coordinates (`X`, `Y`, `Z`) with custom pitch and yaw facing angles.
* 🛡️ **Cryptographic Cookie Fallback** — Armed with HMAC-SHA256 signed client cookies (`seemless_transfers:fast_auth`) to preserve transfer data even if strict external firewalls block peer sockets.
* ⚡ **Zero-Throttle Engine** — Automatically detects and removes Paper/Spigot's `connection-throttle: 4000` limit, preventing the notorious *"Connection throttled! Please wait before reconnecting"* error during rapid server hops.
* 🔄 **True Cross-Platform Ecosystem** — Seamlessly transfer players across any combination of **Fabric**, **Paper/Purpur**, and **NeoForge**.
* 🎨 **Brigadier Syntax Highlighting** — Fully colored command arguments and real-time validation across all server loaders.

---

## 🎮 Command Usage

Seemless Transfers provides a single, unified administration command:

```
/sl <player> <ip> <port> <X> <Y> <Z> [yaw] [pitch]
```

### Arguments:
| Argument | Type | Description |
| :--- | :--- | :--- |
| `<player>` | `Player` | The target player name or selector (`@p`, `@a`, `@s`, `@r`). |
| `<ip>` | `String` | Target server hostname or IP address (e.g. `hub.mynetwork.com`, `192.168.1.100`). |
| `<port>` | `Integer` | Target server port (`1` - `65535`). |
| `<X> <Y> <Z>` | `Double` | Target world coordinates (clamped to vanilla world bounds). |
| `[yaw]` | `Float` | *(Optional)* Horizontal rotation angle in degrees (`-180.0` to `180.0`). Default: `0.0`. |
| `[pitch]` | `Float` | *(Optional)* Vertical tilt angle in degrees (`-90.0` to `90.0`). Default: `0.0`. |

### Practical Examples:
```
# Teleport Steve to lobby spawn facing South
/sl Steve lobby.mynetwork.com 25565 0.5 64.0 0.5 0.0 0.0

# Teleport triggering player into an arena facing North looking slightly down
/sl @s arena.mynetwork.com 25566 128.5 72.0 -350.5 180.0 15.0

# Quick transfer without custom rotation
/sl Alex 10.0.0.5 25567 100.0 65.0 200.0
```

> **Note:** Command Blocks and Server Console can also trigger `/sl`.

---

## 🔐 Permissions & Security

To prevent unauthorized access, `/sl` is strictly restricted to operators:
- **Fabric & NeoForge:** Requires Operator Permission Level 2 (`LEVEL_GAMEMASTERS`).
- **Paper / Bukkit:** Requires Server Operator status (`isOp() == true`) or the explicit permission node `seemless.admin`.
- Non-operators cannot see suggestions, auto-complete, or execute the command.
- All transfer tokens are protected by **HMAC-SHA256 signatures** and a 5-minute replay-attack expiration window.

---

## 🔌 Port & Network Setup

* **Game Port:** Ensure the client can connect to the target server's main port (e.g. `25565`).
* **PeerBridge Port:** By default, Seemless Transfers listens on `GamePort + 10` (e.g. `25575`). For instant chunk preloading between distinct physical machines, ensure this port is open between your servers. If blocked by a firewall, the mod automatically falls back to client cookie authentication.

---

## 📋 Compatibility

| Platform | Minimum Minecraft Version | Java Runtime |
| :--- | :--- | :--- |
| **Fabric** | 1.20.5+ (including 1.20.6 & 1.21+) | Java 21 |
| **Paper / Purpur** | 1.20.5+ (including 1.20.6 & 1.21+) | Java 21 |
| **NeoForge** | 1.20.5+ (including 1.20.6 & 1.21+) | Java 21 |

---

## 📜 License & Distribution Rights

**Copyright © TD_Hunterrr. All Rights Reserved (ARR).**

* **Official Distribution:** This mod is created and exclusively distributed by **TD_Hunterrr**.
* **Redistribution Prohibited:** You may **NOT** re-upload, mirror, or distribute this mod on any other platform, website, or hosting provider.
* **Modpack Policy:** You may **NOT** include this mod in any public or third-party modpack without explicit, prior written permission from **TD_Hunterrr**.
