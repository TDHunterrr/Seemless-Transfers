# 🏛️ Architecture & Networking Protocol

**Seemless Transfers** utilizes a two-tier synchronization model designed to deliver zero-lag, instant teleportation between Minecraft servers without requiring proxy software.

---

## 1. The Dual-Channel Transfer Model

When a player is transferred, Seemless Transfers coordinates two communication channels:

```
[Origin Server (Paper / Fabric)]
       │
       ├─► (Channel A: Primary Game Port)
       │    Stores HMAC token into Player Client (`ClientboundStoreCookiePacket`)
       │    Sends `ClientboundTransferPacket` to Target IP:Port
       │
       └─► (Channel B: PeerBridge Background Socket - Optional LAN/Dedicated)
            Sends `PRELOAD UUID:Name:X:Y:Z:Yaw:Pitch` directly to Destination Server
                                      │
                                      ▼
                      [Destination Server (Fabric / Paper)]
                      ┌────────────────────────────────────────────────────────┐
                      │ 1. Receives Player via primary game port (e.g. 25565)  │
                      │ 2. Intercepts Cookie response on primary Netty channel │
                      │ 3. Preloads chunk at (X >> 4, Z >> 4) into memory       │
                      │ 4. Teleports player to exact (X, Y, Z, yaw, pitch)     │
                      └────────────────────────────────────────────────────────┘
```

---

## 2. Channel A: Primary Port Client Cookie Transit

### Why Client Cookies?
On shared hosting panels (such as Pterodactyl, PlayWithBao, Falix, PebbleHost, etc.), host firewalls only allow traffic over the **single assigned primary game port** (e.g. `25565` or `47466`). External sockets to secondary ports are strictly blocked.

Minecraft 1.20.5+ introduced native client cookies (`ClientboundStoreCookiePacket` and `ServerboundCookieResponsePacket`). These cookies:
1. Live in the player's client memory.
2. Travel with the player directly to the target server.
3. Travel **100% over the primary game port** connection.

### How It Works:
1. **At Origin:** Before transferring the player, the origin server signs a payload containing `(UUID, TargetHost, X, Y, Z, Yaw, Pitch, Timestamp)` using an HMAC-SHA256 key and stores it on the client under `seemless_transfers:fast_auth`.
2. **Transfer:** The origin server dispatches `ClientboundTransferPacket(host, port)`.
3. **At Destination:** 
   - On **Paper:** Handled via Paper's native `player.retrieveCookie(key)`.
   - On **Fabric:** Handled via `CookieAuthReceiver`. Fabric sends `ClientboundCookieRequestPacket`. When the client responds, a custom Netty inbound interceptor catches `ServerboundCookieResponsePacket`, extracts and verifies the HMAC token, and teleports the player immediately, dropping the packet so vanilla Minecraft never triggers an "unexpected query" disconnect.

---

## 3. Channel B: Direct `PeerBridge` Socket (LAN / Dedicated)

In local network environments, VPNs, or unfirewalled dedicated server clusters:
* The origin server connects via a background TCP socket to the destination server's `PeerBridge` listener.
* **Payload:** `PRELOAD <UUID>:<PlayerName>:<X>:<Y>:<Z>:<Yaw>:<Pitch>`
* **ACK Handshake:** Destination responds with `ACK` before the connection terminates.
* **Instant Chunk Pre-Generation:** Destination server immediately loads the arrival chunk before the player's TCP connection even finishes handshaking.

---

## 4. Cryptographic Security & HMAC-SHA256

To guarantee that clients cannot fabricate coordinates or spoof teleport locations:
- Every token is encrypted with a 256-bit hash message authentication code (HMAC-SHA256).
- The signature validates all parameters: UUID, Host, X, Y, Z, Yaw, Pitch, and Timestamp.
- Tokens expire after **5 minutes** (300,000ms), preventing replay attacks.
- Coordinates are strictly validated against vanilla world boundaries ($\pm 29,999,984.0$, $Y \in [-64, 320]$).
