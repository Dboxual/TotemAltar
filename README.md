# TotemAltar

A custom Minecraft Paper plugin focused on unique Totems, altar crafting, PvP mechanics, and survival progression.

Built for PaperMC 1.21.11.

---

## ✨ Features

* Custom Totems with special abilities
* Altar crafting system
* Hologram-based altar locations
* PvP & SMP focused mechanics
* Custom item lore and models
* Multiplayer utility & combat abilities

---

## 🧿 Totems

### 🌩 Storm Totem

**Item:** Chorus Fruit
Randomly teleports the player a short distance when activated.

### 👁 Guardian Totem

**Item:** Ender Pearl / Eye of Ender
Links two players together. If one player’s totem activates, the ally is teleported to them.

### 🌑 Shadow Totem

Temporarily hides the player when activated.

### 💥 Blast Totem

Pushes nearby enemies away with an explosion effect.

More totems may be added over time.

---

## ⚒ Altar System

Players craft special totems using custom altars around the world.

Admins can:

* Create altar locations
* Set altar holograms
* Manage altar recipes

Players can:

* Find altars
* Insert crafting materials
* Receive custom totems

---

## 📜 Commands

| Command         | Description          |
| --------------- | -------------------- |
| `/altar create` | Create an altar      |
| `/altar remove` | Remove an altar      |
| `/altar list`   | List altar locations |
| `/altar tp`     | Teleport to altar    |

---

## 🔑 Permissions

| Permission          | Description       |
| ------------------- | ----------------- |
| `totalaltar.admin`  | Full admin access |
| `totalaltar.use`    | Use altar systems |
| `totalaltar.create` | Create altars     |

---

## 📦 Installation

1. Download the plugin jar
2. Place it inside `/plugins`
3. Restart the server
4. Configure settings if needed

---

## ✅ Compatibility

* PaperMC 1.21.11
* ProtocolLib support
* PlaceholderAPI support (if enabled)

---

## 🔄 Updating

Updating TotemAltars **only requires replacing the jar**. No data or config deletion is needed.

**Standard update process:**
1. Stop the server.
2. Replace `plugins/TotemAltars-old.jar` with the new jar.
3. Start the server.

On startup the plugin will automatically:
- Detect your existing `config.yml` version and add any new keys with safe defaults (your values are never overwritten).
- Rename any config keys that changed between versions.
- Scan player inventories on login and migrate any ingredient items whose material changed in an update — they remain functional in the altar regardless.
- Preserve all altar locations (`altars.yml`) exactly as they were.

**You do NOT need to delete:**
- `plugins/TotemAltars/` (config, altars, data)
- `plugins/.paper-remapped/` (Paper's remap cache — only delete this as a last resort if the server refuses to load the jar after a major Paper update)

If the plugin logs migration messages on startup (e.g. `[Config v0→v1] Renamed keys: ...`), that is normal and expected.

---

## 🚧 Planned Features

* More Totems
* Better particle effects
* Additional altar types
* GUI systems
* Totem upgrades

---

## ❤️ Credits

Created for custom SMP gameplay experiences.
