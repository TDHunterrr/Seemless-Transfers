# Seemless Transfers — Official Wiki & Technical Documentation

Welcome to the official technical wiki for **Seemless Transfers**, created by **TD_Hunterrr**. This document provides an in-depth breakdown of the mod's architecture, setup requirements, networking protocol, command mechanics, and security features.

---

## 📑 Table of Contents
1. [Architecture & How It Works](#1-architecture--how-it-works)
   - [Direct Peer Socket Bridge (`PeerBridge`)](#direct-peer-socket-bridge-peerbridge)
   - [Native Client Transfer Protocol (Minecraft 1.20.5+)](#native-client-transfer-protocol-minecraft-1205)
   - [Cryptographic Client Cookie Fallback](#cryptographic-client-cookie-fallback)
2. [Network & Port Configuration](#2-network--port-configuration)
   - [Dynamic Port Binding](#dynamic-port-binding)
   - [Firewall & Port Forwarding Requirements](#firewall--port-forwarding-requirements)
3. [Command Deep-Dive & Usage](#3-command-deep-dive--usage)
   - [Syntax & Argument Breakdown](#syntax--argument-breakdown)
   - [Practical Command Examples](#practical-command-examples)
   - [Command Blocks & Console Usage](#command-blocks--console-usage)
4. [Permissions & Security](#4-permissions--security)
   - [Permission Nodes](#permission-nodes)
   - [HMAC-SHA256 Token Validation](#hmac-sha256-token-validation)
   - [World Boundary Verification](#world-boundary-verification)
5. [Connection Throttling & Fast Transfers](#5-connection-throttling--fast-transfers)
6. [Cross-Platform Compatibility Matrix](#6-cross-platform-compatibility-matrix)
7. [Troubleshooting & Frequently Asked Questions (FAQ)](#7-troubleshooting--frequently-asked-questions-faq)

---

## 1. Architecture & How It Works

Traditional proxy-based setups (BungeeCord, Velocity) route all player traffic through a central proxy instance. **Seemless Transfers** utilizes modern Minecraft 1.20.5+ native client transfers (`ClientboundTransferPacket`) combined with a two-tier synchronization model:

```
[Origin Server] --(1. TCP PeerBridge: Preload Request)--> [Destination Server]
        |                                                        |
        |--(2. ClientboundStoreCookiePacket: HMAC Token)         |
        |                                                        |
        v                                                        v
[Player Client] ---------(3. ClientboundTransferPacket)-------> [Connects & Lands Instantly]
```

### Direct Peer Socket Bridge (`PeerBridge`)
When `/sl` is executed, the origin server immediately opens an asynchronous background socket connection to the destination server:
* **Payload Structure:** `PRELOAD <UUID>:<PlayerName>:<X>:<Y>:<Z>:<Yaw>:<Pitch>`
* **Chunk Pre-Generation:** Upon receiving the packet, the destination server asynchronously loads the chunk at `(X >> 4, Z >> 4)` into server memory before the client arrives.
* **Instant Handshake:** When the client connects, the destination server immediately consumes the pre-cached target from memory, teleporting the player upon join without waiting for disk I/O.

### Native Client Transfer Protocol (Minecraft 1.20.5+)
Minecraft 1.20.5 introduced direct client transfer packets. Rather than disconnecting to the main menu or going through an intermediary proxy tunnel, the vanilla client cleanly transitions TCP connections directly from Server A to Server B.

### Cryptographic Client Cookie Fallback
In the event of a restrictive firewall blocking inter-server TCP socket bridges:
1. The origin server signs the target coordinates, destination IP, player UUID, and epoch timestamp with an HMAC-SHA256 secret.
2. The payload is stored directly in the player's client memory via `ClientboundStoreCookiePacket` (`seemless_transfers:fast_auth`).
3. On Paper servers, this cookie is retrieved on join (`player.retrieveCookie(...)`), verified cryptographically, and used to teleport the player even if the direct socket bridge was blocked.

---

## 2. Network & Port Configuration

### Dynamic Port Binding
To prevent port collisions when hosting multiple servers on the same physical machine or IP address, `PeerBridge` uses an intelligent candidate scanning system:
* **Preferred Port Calculation:** `GamePort + 10` (e.g. if Minecraft runs on `25565`, PeerBridge attempts to bind `25575`).
* **Fallback Ports:** If the primary port is occupied, it automatically attempts `GamePort + 1000`, `25585`, `25595`, `25586`, `25587`, and `GamePort`.
* **Sender Candidate Scanning:** When `/sl` transfers to `target.server.com:25565`, the sender automatically checks `25575`, `26565`, `25585`, `25595`, etc., with a 250ms timeout per socket until a connection is confirmed.

### Firewall & Port Forwarding Requirements
If your servers reside on separate physical machines across the internet:
* Ensure the **Game Port** (e.g. `25565`) is port-forwarded and accessible to clients.
* Ensure the **PeerBridge Port** (default `GamePort + 10` or `25585`) is open between the servers for instant chunk preloading.
* *Note: Even if the peer socket is completely blocked by firewalls, transfers will still succeed via the client-side cryptographic cookie fallback.*

---

## 3. Command Deep-Dive & Usage

### Syntax & Argument Breakdown

```
/sl <player> <ip> <port> <X> <Y> <Z> [yaw] [pitch]
```

- `<player>`: The online player name or selector (e.g. `@p`, `@s`).
- `<ip>`: Target server hostname or IP address (e.g. `na.mycluster.net`, `192.168.1.50`).
- `<port>`: Target server port (must be an integer between `1` and `65535`).
- `<X>`: Double coordinate between `-29,999,984.0` and `29,999,984.0`.
- `<Y>`: Double coordinate between `-64.0` and `320.0`.
- `<Z>`: Double coordinate between `-29,999,984.0` and `29,999,984.0`.
- `<yaw>`: *(Optional)* Horizontal rotation angle in degrees (`-180.0` to `180.0`). Default: `0.0`.
- `<pitch>`: *(Optional)* Vertical rotation angle in degrees (`-90.0` to `90.0`). Default: `0.0`.

### Practical Command Examples

1. **Transfer player to lobby spawn facing South:**
   ```
   /sl Steve lobby.myserver.net 25565 0.5 64.0 0.5 0.0 0.0
   ```

2. **Transfer self to dungeon arena facing North looking slightly down:**
   ```
   /sl @s arena.myserver.net 25566 128.5 72.0 -350.5 180.0 15.0
   ```

3. **Quick transfer without rotation:**
   ```
   /sl Alex 127.0.0.1 25567 100.0 65.0 200.0
   ```

### Command Blocks & Console Usage
- Executable via Command Blocks: **Yes** (using selectors `@p`, `@a`, `@r`).
- Executable via Server Console: **Yes** (specify exact player name).

---

## 4. Permissions & Security

### Permission Nodes
- **Fabric & NeoForge:** Requires Minecraft permission level 2 (`LEVEL_GAMEMASTERS` / standard OP). Non-operators cannot view, auto-complete, or execute the command.
- **Paper:** Requires operator status (`isOp() == true`) or the explicit permission node:
  ```yaml
  seemless.admin
  ```

### HMAC-SHA256 Token Validation
To protect against forged client packets or unauthorized coordinate tampering:
- Tokens are encrypted with a 256-bit hash message authentication code (HMAC-SHA256).
- Every token includes the player UUID, target IP, exact destination coordinates, rotation, and timestamp.
- Tokens expire after **5 minutes** (300,000ms) to prevent replay attacks.
- Tokens are strictly rejected if the UUID in the token does not match the UUID of the connecting player.

### World Boundary Verification
Every coordinate is strictly validated against Minecraft world boundaries:
- `X` and `Z` must fall within `±29,999,984.0`.
- `Y` must fall within `-64.0` to `320.0`.
Any value outside this range triggers an immediate validation failure and aborts the transfer.

---

## 5. Connection Throttling & Fast Transfers

In vanilla Bukkit/Paper, `bukkit.yml` enforces `connection-throttle: 4000` (4-second delay required between connections from the same IP address). When testing or fast-hopping players between servers, this causes players to be kicked with:
> *"Connection throttled! Please wait before reconnecting."*

**How Seemless Transfers Solves This:**
- **In-Memory Bypass:** Automatically reflects into the server's runtime configuration on startup and resets the connection throttle to `0`.
- **Permanent Config Synchronization:** Detects `bukkit.yml` and updates `settings.connection-throttle: 0` so the throttle is permanently removed across restarts.
- **On-Demand Reset:** Every execution of `/sl` and every incoming preload notification resets the throttle tracker, ensuring seamless back-and-forth hops.

---

## 6. Cross-Platform Compatibility Matrix

| Feature | Fabric | Paper | NeoForge |
| :--- | :---: | :---: | :---: |
| Native Transfer Packet | ✅ | ✅ | ✅ |
| PeerBridge Preloading | ✅ | ✅ | ✅ |
| Exact X, Y, Z Positioning | ✅ | ✅ | ✅ |
| Exact Yaw & Pitch Angles | ✅ | ✅ | ✅ |
| Multi-Color Brigadier Highlighting | ✅ | ✅ | ✅ |
| Auto Throttle Removal (`0ms`) | N/A *(Not Throttled)* | ✅ | N/A *(Not Throttled)* |
| Cookie Cryptographic Fallback | ✅ *(Sender)* | ✅ *(Sender & Receiver)* | ✅ *(Sender)* |

---

## 7. Troubleshooting & Frequently Asked Questions (FAQ)

#### Q: The player arrives at the target server but spawns at the default world spawn instead of the target coordinates.
**A:** Ensure that both servers have **Seemless Transfers** installed! If only the origin server has the mod, the destination server will not know where to teleport the player. Also verify that the destination server is reachable via the PeerBridge port (or that Paper can retrieve client cookies).

#### Q: Does Seemless Transfers require BungeeCord or Velocity?
**A:** **No.** Seemless Transfers uses direct Minecraft 1.20.5+ client-to-server transfers. It works directly between standalone servers without requiring any proxy software.

#### Q: Can players use `/sl`?
**A:** No. `/sl` is strictly restricted to Server Operators (Permission Level 2) or staff with `seemless.admin`. Regular players will receive an "Unknown command" error and cannot see command suggestions.

#### Q: Can I include this mod in my modpack?
**A:** **No.** Under the [All Rights Reserved License](LICENSE), Seemless Transfers may **only** be distributed by the original creator (**TD_Hunterrr**). Redistribution or inclusion in modpacks is strictly prohibited without explicit written permission.
