# ❓ FAQ & Troubleshooting

### Q1: The player connects to the destination server, but lands at world spawn instead of the target coordinates.
**Solutions:**
1. **Ensure the mod is installed on BOTH servers.** If Seemless Transfers is only present on the origin server, the destination server has no code to read the cookie or teleport the player.
2. **Ensure both servers are running compatible versions.** Both servers must run Minecraft 1.20.5 or newer with Seemless Transfers installed.

---

### Q2: I see `PeerBridge direct socket unavailable for host:port; transferring seamlessly via primary-port client cookie.` in the console. Is this an error?
**Answer:**
**No.** This is an informational notice. It simply means your destination server host (e.g. PlayWithBao, Falix, or Pterodactyl) has closed secondary ports. Seemless Transfers automatically falls back to sending the player data directly through the primary game port via encrypted client cookies.

---

### Q3: My Paper server runs in offline-mode, but my Fabric server runs in online-mode (or via proxy). Will transfers work?
**Answer:**
**Yes!** Seemless Transfers validates tokens using an HMAC-SHA256 signature and player name fallback. Even if the player's UUID differs between online and offline modes across your servers, the player is recognized and teleported accurately.

---

### Q4: Do players need to install any client-side mods?
**Answer:**
**No!** Seemless Transfers is 100% server-side. Vanilla Minecraft 1.20.5+ clients natively support transfer packets and cookie storage without any mods or resource packs.

---

### Q5: Does Seemless Transfers require BungeeCord or Velocity?
**Answer:**
**No.** Seemless Transfers is a proxy-free system. Servers communicate directly with each other and transfer players directly from one server to another. You do not need any proxy instances.

---

### Q6: Can regular players use the `/sl` command?
**Answer:**
**No.** `/sl` is strictly restricted to Server Operators (Permission Level 2) on Fabric/NeoForge, or staff with `seemless.admin` on Paper. Regular players cannot view, auto-complete, or execute the command.
