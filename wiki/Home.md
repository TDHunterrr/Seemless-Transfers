# ⚡ Welcome to Seemless Transfers Wiki

Welcome to the official documentation for **Seemless Transfers**, created by **TD_Hunterrr**.

**Seemless Transfers** is an ultra-fast, proxy-free, cross-server teleportation mod and plugin designed for **Fabric**, **Paper/Purpur**, and **NeoForge** (Minecraft 1.20.5+ and 1.21+).

---

## 🚀 Why Seemless Transfers?

Traditional Minecraft multi-server networks rely on centralized proxies like BungeeCord or Velocity. While proxies serve their purpose for complex routing, they add latency, require dedicated infrastructure, and route all player traffic through an intermediary bottleneck.

With Minecraft 1.20.5+, Mojang introduced native client transfer packets (`ClientboundTransferPacket`). **Seemless Transfers** leverages this protocol combined with **client-side cryptographic cookie synchronization** and background **peer chunk preloading**, enabling:

* 🎯 **Direct Server-to-Server Hops** — Teleport players straight between separate server instances without any proxy software.
* 🌐 **100% Primary Port Transit** — Works seamlessly on shared hosting providers (Pterodactyl panels, PlayWithBao, Falix, etc.) where only a single game port (e.g. `25565` or `47466`) is open.
* 📦 **Instant Arrival without Void Falling** — Destination chunks are preloaded into server memory before the player finishes handshaking, eliminating terrain-loading freezes.
* 📐 **Pinpoint Decimal Coordinates & Rotation** — Control exact X, Y, Z coordinates plus pitch and yaw facing angles.
* ⚡ **Zero-Throttle Engine** — Automatically resets and disables Paper's `settings.connection-throttle: 0`, preventing reconnection timeout kicks during fast hops.
* 🛡️ **Cryptographic HMAC Security** — Transfer tokens are signed with HMAC-SHA256 to guarantee security, anti-tampering, and replay protection.

---

## 📑 Wiki Contents

Explore the detailed sections of this documentation:

| Section | Description |
| :--- | :--- |
| **[Architecture & Protocol](Architecture)** | How client cookies, Netty interceptors, and PeerBridge work internally. |
| **[Commands & Permissions](Commands-and-Permissions)** | Command syntax, argument constraints, permissions, and command block usage. |
| **[Hosting & Ports Setup](Hosting-and-Ports)** | Hosting guide: single-port shared hosts vs multi-node dedicated setups. |
| **[Connection Throttle Bypass](Connection-Throttle)** | How Paper/Spigot throttle kicks are solved automatically. |
| **[FAQ & Troubleshooting](FAQ-and-Troubleshooting)** | Solutions to common setup questions and edge cases. |
| **[License & Modpack Policy](License-and-Modpack-Policy)** | Official terms, author attribution, and modpack restrictions. |

---

## ⚡ Quick Start

1. Install **Seemless Transfers** on all servers in your network (Fabric, Paper, or NeoForge).
2. Start the servers. No configuration files required out of the box!
3. Teleport a player to another server:
   ```text
   /sl Steve lobby.mynetwork.com 25565 0.5 64.0 0.5 0.0 0.0
   ```
