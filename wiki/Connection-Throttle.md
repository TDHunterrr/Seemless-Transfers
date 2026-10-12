# ⚡ Connection Throttle Bypass

## The Problem: "Connection Throttled!"

In standard Bukkit, Spigot, Paper, and Purpur servers, `bukkit.yml` includes an anti-bot feature enabled by default:
```yaml
settings:
  connection-throttle: 4000
```
This forces all connections from the same IP address to wait at least **4000 milliseconds (4 seconds)** before reconnecting.

### Why This Breaks Fast Cross-Server Transfers:
When players transfer back and forth between servers rapidly (such as entering a lobby, hopping into a minigame, or testing transfers), their IP address triggers this threshold.

Players are abruptly kicked with:
> *"Connection throttled! Please wait before reconnecting."*

---

## How Seemless Transfers Eliminates This

**Seemless Transfers** provides an automated three-layer solution:

### 1. In-Memory Runtime Bypass
On plugin initialization, Seemless Transfers uses reflection into the server's runtime `CraftServer` instance to set:
* `connectionThrottle = 0`
* `settings.connection-throttle = 0`
This takes effect instantly without requiring a server reboot.

### 2. Disk Configuration Synchronization
Seemless Transfers detects `bukkit.yml` in the server root. If `connection-throttle` is not `0`, it safely updates:
```yaml
settings:
  connection-throttle: 0
```
This guarantees that even across server restarts and updates, the throttle remains disabled.

### 3. On-Demand Transfer Reset
Whenever `/sl` is executed, the server's throttle tracker is cleared immediately, ensuring zero-delay transit regardless of how quickly players hop.
