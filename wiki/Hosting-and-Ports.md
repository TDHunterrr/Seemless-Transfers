# 🌐 Hosting & Ports Setup

One of the greatest challenges of running multi-server Minecraft networks is configuring firewalls and ports across different hosting providers.

**Seemless Transfers** is engineered specifically to eliminate these networking headaches.

---

## 1. Shared Hosting & Panels (Single-Port Hosts)

Most commercial Minecraft game server providers (e.g. **PlayWithBao**, **Pterodactyl Panels**, **PebbleHost**, **FalixNodes**, **Aternos**, **BisectHosting**) operate inside containerized environments (Docker) where:
- You are allocated **only one port**: the primary game port (e.g. `25565`, `47466`, or `31245`).
- Any attempt to open a secondary socket (like `25585` or `47476`) is blocked by the host's firewall.

### How Seemless Transfers Handles This:
**Zero extra ports are required!**
When transferring to a shared host:
1. Specify the server's primary game port in `/sl`:
   ```text
   /sl Steve lifesteal123456.playwithbao.com 47466 100.5 70.0 100.5
   ```
2. The coordinates and HMAC authentication travel directly inside the player client's connection over port `47466`.
3. If the background `PeerBridge` socket fails to connect due to host firewalls, the server logs:
   ```text
   [SeemlessTransfers] PeerBridge direct socket unavailable for host:port; transferring seamlessly via primary-port client cookie.
   ```
4. The destination server seamlessly retrieves the cookie over port `47466` and teleports the player on arrival.

---

## 2. Dedicated Servers & Local Networks (Multi-Port Setups)

If you are running on dedicated virtual servers (VPS), bare-metal hardware, or a local network (LAN):
- You have the option to allow inter-server background sockets (`PeerBridge`).
- **Default PeerBridge Port:** `GamePort + 10` (e.g. if Minecraft runs on `25565`, PeerBridge attempts to bind `25575`).
- **Candidate Fallbacks:** Automatically attempts `GamePort + 1000`, `25585`, `25595`, `25586`, and `25587`.
- **Preload Benefit:** Origin servers notify destination servers before the client arrives, loading chunks into RAM asynchronously.

### Summary Table:
| Environment | Required Ports | Transfer Mechanism |
| :--- | :--- | :--- |
| **Shared Game Hosts** *(Pterodactyl, PlayWithBao, Falix)* | **Game Port Only** (`25565` or custom) | Primary-Port Client Cookie |
| **VPS / Dedicated Servers** *(Ubuntu, Debian, Windows Server)* | Game Port + optional PeerPort (`GamePort + 10`) | Primary Cookie + `PeerBridge` Socket |
| **Local Network / LAN** | Game Port + optional PeerPort | Primary Cookie + `PeerBridge` Socket |
