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

Each totem is crafted at a Totem Altar using a vanilla Totem of Undying and a specific ingredient dropped by a mob. When the totem activates (on death), its unique ability triggers instead of the vanilla resurrection effect — you still survive, but the outcome is very different.

---

### 💥 Blast Totem
**Ingredient:** Volatile Bone *(rare drop from Wither Skeletons)*

On pop, releases a shockwave that launches all nearby living entities away from you. No block damage — purely a knockback burst. Useful for escaping a crowd or punishing melee rushers.

---

### 🌑 Shadow Totem
**Ingredient:** Shadow Heartwood *(rare drop from Creakings)*

On pop, makes you fully invisible for a configurable duration. Your equipped armor is also hidden from other players during this window, so you cannot be spotted by silhouette. Useful for repositioning or escaping pursuit after a fight goes wrong.

---

### 🌩 Storm Totem
**Ingredient:** Void Chorus *(rare drop from Endermen)*

On pop, teleports you to a random safe location 25–35 blocks away. The plugin guarantees you land on solid ground with two clear blocks above — no landing in lava or inside walls. Useful for creating instant distance without a predictable direction.

---

### 🔀 Swap Totem
**Ingredient:** Fractured Echo *(rare drop from Shulkers)*

On pop, instantly swaps your position with the nearest player within range. Both players keep their original look direction. Useful for forcing a pursuer into a bad position or pulling a fleeing target back into range.

---

### 👁 Guardian Totem
**Ingredient:** Sentinel Eye *(rare drop from Guardians)*

Unlike other totems, this one must be linked first. Right-click another player while holding an unlinked Guardian Totem to pair both of you — each player receives a linked totem showing the partner’s name. On pop, your linked ally is teleported directly to you and their linked totem is consumed. Works even if the totem was traded to a third player. Useful for duo play and rescue pulls.

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

All commands use `/totemaltars` (or `/ta` if aliased).

| Command                                    | Permission           | Description                                    |
| ------------------------------------------ | -------------------- | ---------------------------------------------- |
| `/totemaltars createaltar`                 | `totemaltars.admin`  | Designate the targeted anvil as a Totem Altar  |
| `/totemaltars removealtar`                 | `totemaltars.admin`  | Remove the nearest Totem Altar                 |
| `/totemaltars giveingredient <type> [player]` | `totemaltars.admin` | Give a crafting ingredient to a player        |
| `/totemaltars givetotem <type> [player]`   | `totemaltars.admin`  | Give a finished totem to a player              |
| `/totemaltars reload`                      | `totemaltars.admin`  | Reload config.yml without restarting           |
| `/totemaltars altar`                       | `totemaltars.use`    | Show the location of the nearest Totem Altar   |

---

## 🔑 Permissions

| Permission           | Default | Description                                                     |
| -------------------- | ------- | --------------------------------------------------------------- |
| `totemaltars.admin`  | op      | Access to all admin commands (createaltar, removealtar, give*, reload) |
| `totemaltars.use`    | true    | Interact with Totem Altars and use `/totemaltars altar`         |

---

## 📦 Installation

**Required dependencies (must be installed first):**
- [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) — required; the plugin will not load without it

**Optional dependencies:**
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) — enables cooldown placeholders for scoreboards and HUDs

**Steps:**
1. Install ProtocolLib into `/plugins`
2. Download the TotemAltars jar and place it inside `/plugins`
3. Restart the server
4. Configure `plugins/TotemAltars/config.yml` as needed

---

## ✅ Compatibility

* PaperMC 1.21.11
* ProtocolLib (required — hard dependency)
* PlaceholderAPI (optional)

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
