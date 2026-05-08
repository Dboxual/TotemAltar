# TotemAltars

> A PaperMC plugin that replaces the vanilla Totem of Undying with five unique custom totems, each granting a powerful active ability when it pops on death.

Totems are not found — they are **crafted**. Rare mob drops are brought to admin-placed **Totem Altars** and combined with a vanilla Totem of Undying to forge a custom one. Every totem has its own identity, ingredient, and ability, making them a meaningful part of both survival and PvP gameplay.

---

## ✨ Features

- **5 Unique Custom Totems** — each with its own ability, lore, ingredient, and fantasy aesthetic
- **Totem Altar Crafting System** — admin-placed anvil altars with a custom GUI for forging totems
- **Hologram Altars** — floating TextDisplay holograms mark each altar in the world
- **Rare Mob Drops** — each totem's ingredient drops from a specific mob at a configurable rate
- **PvP-Focused Abilities** — knockback blasts, position swaps, invisibility, and ally teleportation
- **Guardian Link System** — pair two players together; on pop, pull your ally to your location
- **Global Cooldown** — a shared cooldown prevents chaining totems back-to-back
- **PlaceholderAPI Support** — expose cooldown timers and progress bars on scoreboard/TAB
- **Admin Commands** — give ingredients or finished totems directly, reload config live

---

## 🪄 Totems

All five totems share the same base item (`Totem of Undying`) and are distinguished by their custom name, lore, and embedded metadata. They activate on death just like a vanilla totem.

---

### 🔴 Blast Totem

| Field | Value |
|---|---|
| **Display Name** | `Blast Totem` |
| **Ingredient** | Volatile Bone |
| **Ingredient Drop** | Wither Skeleton — 10% chance (player kill) |
| **Cooldown** | 15s global cooldown |

**Ability:** On pop, launches every nearby living entity away from you in a powerful explosion burst. Affects all entities within a configurable radius. No block damage.

**Lore preview:**
```
The bone still crackles with unstable fury.
On pop: blast all nearby entities away.
Radius: 8 blocks | No block damage.
Forged with Volatile Bone
```

**Crafting:** Bring a **Volatile Bone** (dropped by Wither Skeletons at a 10% chance) to a Totem Altar. Place the bone in the ingredient slot alongside a vanilla Totem of Undying to receive a Blast Totem.

---

### ⚫ Shadow Totem

| Field | Value |
|---|---|
| **Display Name** | `Shadow Totem` |
| **Ingredient** | Shadow Heartwood |
| **Ingredient Drop** | Creaking — 15% chance (player kill) |
| **Cooldown** | 15s global cooldown |

**Ability:** On pop, grants complete invisibility for 15 seconds. Armor is fully hidden from other players during the duration using packet-level suppression (requires ProtocolLib). Armor visibility is automatically restored when the effect ends.

**Lore preview:**
```
Some things move better unseen.
On pop: vanish completely for 15 seconds.
Armor hidden during invisibility.
Forged with Shadow Heartwood
```

**Crafting:** Bring a **Shadow Heartwood** (dropped by Creakings at a 15% chance) to a Totem Altar.

---

### 🟣 Storm Totem

| Field | Value |
|---|---|
| **Display Name** | `Storm Totem` |
| **Ingredient** | Void Chorus |
| **Ingredient Drop** | Enderman — 10% chance, only in a Warped Forest biome OR during active thunder/rain |
| **Cooldown** | 15s global cooldown |

**Ability:** On pop, teleports you to a random safe location 25–35 blocks away from your current position. The plugin attempts up to 50 positions and guarantees a safe landing (no lava, water, or block collision). If no safe location is found, the ability fails gracefully with a message.

**Lore preview:**
```
Space bends wherever chaos wills.
On pop: teleport 25–35 blocks away.
Safe landing guaranteed.
Bound with Void Chorus
```

**Crafting:** Bring a **Void Chorus** (dropped by Endermen under specific conditions at a 10% chance) to a Totem Altar.

---

### 🟡 Swap Totem

| Field | Value |
|---|---|
| **Display Name** | `Swap Totem` |
| **Ingredient** | Fractured Echo |
| **Ingredient Drop** | Shulker — 15% chance (player kill) |
| **Cooldown** | 15s global cooldown |

**Ability:** On pop, swaps your position with the nearest non-spectator player within range. Both players keep their original yaw and pitch after the swap. If no player is within range, the ability fails with a message.

**Lore preview:**
```
The echo knows where you should be standing.
On pop: swap positions with the nearest player.
Range: 25 blocks.
Crafted using Fractured Echo
```

**Crafting:** Bring a **Fractured Echo** (dropped by Shulkers at a 15% chance) to a Totem Altar.

---

### 🔵 Guardian Totem

| Field | Value |
|---|---|
| **Display Name** | `Guardian Totem` |
| **Ingredient** | Sentinel Eye |
| **Ingredient Drop** | Guardian — 15% chance (player kill) |
| **Cooldown** | 15s global cooldown |

**Ability:** On pop, teleports your linked ally directly to your location. When the ability fires, the partner's linked totem is automatically consumed from their inventory.

**Linking process:**
1. Hold an **unlinked** Guardian Totem.
2. Right-click another player.
3. Both players receive a new **linked** Guardian Totem showing the partner's name in the lore.
4. The unlinked totem is consumed from the initiator's hand.

**Rules:**
- A player can only hold one active Guardian link at a time.
- You cannot link to yourself.
- Both players must be unlinked before a new link can be formed.
- The linked totem can be traded — the link is embedded in the item, not the player.
- If the partner is offline when the totem pops, the ability fails with a message.

**Lore preview:**
```
Some bonds hold even across death.
Right-click a player to create a Guardian link.
On pop: pull your linked ally to your location.
Sealed with Sentinel Eye
```

**Crafting:** Bring a **Sentinel Eye** (dropped by Guardians at a 15% chance) to a Totem Altar.

---

## ⚒️ Altar System

Totem Altars are the only way to craft custom totems. They are placed and managed by server administrators.

### Creating an Altar (Admin)

1. Place any **anvil** block (`ANVIL`, `CHIPPED_ANVIL`, or `DAMAGED_ANVIL`) in the world.
2. Look directly at the anvil (within 5 blocks).
3. Run `/totemaltars createaltar`.

A hologram will appear above the anvil and its location will be saved to `altars.yml`. Altars survive server restarts.

### Removing an Altar (Admin)

1. Look directly at the altar anvil (within 5 blocks).
2. Run `/totemaltars removealtar`.

The hologram is removed and the entry is deleted from `altars.yml`.

### Holograms

Each altar displays two floating text lines using **TextDisplay entities** (no armor stands or external hologram plugins required):

```
✦ Totem Altar ✦       ← gold
Right-click to craft  ← gray
```

Holograms respawn automatically when their chunk is loaded.

### Using the Altar (Player)

**Right-click** an altar anvil to open the crafting GUI — a custom 27-slot inventory titled `Totem Altar`:

```
[ Slot 11 ]  →  Place a vanilla Totem of Undying here
[ Slot 15 ]  →  Place the ingredient (e.g. Volatile Bone) here
[ Slot 13 ]  →  Click to receive the crafted custom totem
```

All other slots are non-interactive gray glass panes. Items placed in the input slots are returned if you close the GUI without crafting.

Items placed in the ingredient slot must be valid plugin ingredients (plain Chorus Fruit from your inventory will not work — only the custom-tagged versions dropped by mobs).

### Finding the Nearest Altar

Players can run `/totemaltars altar` to see the coordinates and distance to the nearest Totem Altar.

---

## 📋 Commands

All commands use the root `/totemaltars`. The alias `/ta` can be configured if desired.

| Command | Description | Permission |
|---|---|---|
| `/totemaltars createaltar` | Register the anvil you're looking at as a Totem Altar | `totemaltars.admin` |
| `/totemaltars removealtar` | Unregister the altar anvil you're looking at | `totemaltars.admin` |
| `/totemaltars giveingredient <player> <type> [amount]` | Give a player a crafting ingredient | `totemaltars.admin` |
| `/totemaltars givetotem <player> <type> [amount]` | Give a player a finished custom totem | `totemaltars.admin` |
| `/totemaltars reload` | Reload `config.yml` without restarting | `totemaltars.admin` |
| `/totemaltars altar` | Show the nearest altar's coordinates and distance | `totemaltars.use` |

**Valid types** for `giveingredient` and `givetotem`: `blast`, `shadow`, `storm`, `swap`, `guardian`

**Amount** is optional and defaults to `1`. Maximum is `64`.

Tab completion is supported for all subcommands, player names, and totem types.

---

## 🔑 Permissions

| Permission | Description | Default |
|---|---|---|
| `totemaltars.admin` | Access to all admin commands (createaltar, removealtar, give\*, reload) | OP |
| `totemaltars.use` | Interact with Totem Altars and run `/totemaltars altar` | All players |

---

## ⚙️ Configuration

The plugin generates a `config.yml` in `plugins/TotemAltars/` on first run.

### Drop Chances

```yaml
drop-chances:
  blast:    0.10   # Wither Skeleton → Volatile Bone
  shadow:   0.15   # Creaking        → Shadow Heartwood
  storm:    0.10   # Enderman        → Void Chorus
  swap:     0.15   # Shulker         → Fractured Echo
  guardian: 0.15   # Guardian        → Sentinel Eye
```

Values range from `0.0` (never) to `1.0` (always).

### Global Cooldown

```yaml
global-cooldown: 15
```

Seconds applied to all totem types after any totem pops. Prevents chaining totems back-to-back.

### Ability Tuning

```yaml
abilities:
  blast:
    radius: 8.0
    knockback-strength: 3.0
  shadow:
    duration-seconds: 15
  storm:
    min-distance: 25
    max-distance: 35
    max-attempts: 50
  swap:
    range: 25
```

### Item Names

All custom item display names support `&` color codes and can be overridden in `config.yml`:

```yaml
item-names:
  volatile-bone:    "&7Volatile Bone"
  shadow-heartwood: "&8Shadow Heartwood"
  void-chorus:      "&dVoid Chorus"
  fractured-echo:   "&eFractured Echo"
  sentinel-eye:     "&3Sentinel Eye"
  blast-totem:      "&cBlast Totem"
  shadow-totem:     "&8Shadow Totem"
  storm-totem:      "&dStorm Totem"
  swap-totem:       "&eSwap Totem"
  guardian-totem:   "&3Guardian Totem"
```

### Auto-Versioning

The plugin uses a `version.properties` file to track its build version. Every `gradle build` automatically increments the patch number and stamps the compiled JAR. This file is managed by the build system — do not edit it manually.

### Altar Locations

Altar locations are saved automatically to `plugins/TotemAltars/altars.yml` in the format `world:x:y:z`. This file is fully managed by the plugin.

---

## 📊 PlaceholderAPI

Requires [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) to be installed. The expansion registers automatically on startup — no manual installation needed.

| Placeholder | Description |
|---|---|
| `%totemaltars_cooldown%` | Seconds remaining on the totem held in hand |
| `%totemaltars_cooldown_bar%` | Progress bar for the totem held in hand |
| `%totemaltars_cooldown_global%` | Seconds remaining on the global cooldown |
| `%totemaltars_cooldown_bar_global%` | Progress bar for the global cooldown |
| `%totemaltars_cooldown_<type>%` | Effective seconds remaining for a specific type |
| `%totemaltars_cooldown_bar_<type>%` | Progress bar for a specific type |

Replace `<type>` with: `blast`, `shadow`, `storm`, `swap`, or `guardian`.

**Example TAB integration:**
```yaml
tablist-name: "%player_name% | %totemaltars_cooldown%s"
```

**Bar format:** `§a` (filled) + `§7` (empty), 10 segments total.
Example: `§a██████§7████` (60% recovered)

---

## 🚀 Installation

1. Download the latest `TotemAltars-x.x.x.jar` from [Releases](https://github.com/Dboxual/TotalAltar/releases).
2. Place the JAR in your server's `plugins/` folder.
3. Install [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) — **required** for the Shadow Totem's armor-hiding effect.
4. Optionally install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) for cooldown placeholders.
5. Restart the server.
6. Edit `plugins/TotemAltars/config.yml` to tune drop rates, cooldowns, and ability values.
7. Place anvils in the world and register them as altars with `/totemaltars createaltar`.

---

## 🔧 Compatibility

| Requirement | Version | Notes |
|---|---|---|
| **Server** | Paper 1.21.4+ | Tested on 1.21.11. Spigot is not supported. |
| **Java** | Java 21+ | Required by the Paper 1.21.x API |
| **ProtocolLib** | 5.3.0+ | **Required.** Used for packet-level armor suppression on Shadow Totem. |
| **PlaceholderAPI** | 2.11.6+ | Optional. Enables cooldown placeholders for scoreboard/TAB. |

> ⚠️ This plugin requires **ProtocolLib** to function. The server will refuse to load TotemAltars if ProtocolLib is not present.

---

## 🔮 Planned Features

- Per-type cooldowns on top of the global cooldown
- Additional totem types (freeze, lifesteal, decoy)
- Configurable per-world enable/disable
- Altar tiers with upgraded ingredient slots
- Totem rarity system with visual particle distinctions
- Public Modrinth / Hangar release with auto-updater

---

## 📄 Credits

**TotemAltars** was designed and built for a custom PaperMC server experience.

- Built for **Paper 1.21.11**
- Depends on [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) by dmulloy2
- Optional integration with [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)

---

*Issues and suggestions welcome via [GitHub Issues](https://github.com/Dboxual/TotalAltar/issues).*
