# 🎮 Commands & Permissions

**Seemless Transfers** provides a single, unified administration command: `/sl`.

---

## 1. Command Syntax

```text
/sl <player> <ip> <port> <X> <Y> <Z> [yaw] [pitch]
```

### Argument Reference:

| Argument | Type | Bounds / Format | Description |
| :--- | :--- | :--- | :--- |
| `<player>` | `Player` | Online player name or selector | The target player to transfer (e.g. `Steve`, `@p`, `@s`). |
| `<ip>` | `String` | Domain or IP address | The destination server address (e.g. `hub.network.com`, `192.168.1.50`). |
| `<port>` | `Integer` | `1` to `65535` | The destination server primary game port (e.g. `25565` or `47466`). |
| `<X>` | `Double` | `-29,999,984.0` to `29,999,984.0` | Target world X coordinate. |
| `<Y>` | `Double` | `-64.0` to `320.0` | Target world Y coordinate. |
| `<Z>` | `Double` | `-29,999,984.0` to `29,999,984.0` | Target world Z coordinate. |
| `[yaw]` | `Float` | `-180.0` to `180.0` *(Optional)* | Horizontal rotation angle in degrees. Default: `0.0`. |
| `[pitch]` | `Float` | `-90.0` to `90.0` *(Optional)* | Vertical look angle in degrees. Default: `0.0`. |

---

## 2. Command Examples

### Example 1: Transfer Player to Spawn Facing South
```text
/sl Steve lobby.mynetwork.com 25565 0.5 64.0 0.5 0.0 0.0
```

### Example 2: Teleport Yourself into an Arena Facing North, Looking Down
```text
/sl @s arena.mynetwork.com 25566 128.5 72.0 -350.5 180.0 15.0
```

### Example 3: Quick Transfer Without Custom Rotation
```text
/sl Alex 10.0.0.5 25567 100.0 65.0 200.0
```

---

## 3. Automation: Command Blocks & Server Console

### Command Blocks:
You can automate transfers in minigame portals or pressure plates using Command Blocks:
```text
/sl @p dungeon.mynetwork.com 25565 0.5 100.0 0.5 90.0 0.0
```
Command blocks support all standard vanilla target selectors (`@p`, `@a`, `@r`).

### Server Console:
Console administrators can execute `/sl` directly from the server terminal:
```text
sl Notch minigames.myserver.org 25565 500.5 80.0 -200.5
```

---

## 4. Permissions

To protect servers from unauthorized transfer exploits, `/sl` is strictly restricted to authorized staff:

### Fabric & NeoForge:
* Requires Minecraft Permission Level 2 (`LEVEL_GAMEMASTERS` / standard OP).
* Non-operators cannot execute the command or see auto-complete tab suggestions.

### Paper / Purpur:
* Requires Server Operator status (`isOp() == true`) or the explicit permission node:
  ```yaml
  seemless.admin
  ```
