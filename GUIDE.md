# TotemAltars

Hunt rare mob drops, bring them to sacred altars, and craft powerful totems — each with a unique ability that triggers when you're about to die.

---

## Features

- **5 unique custom totems**, each with a different survival ability
- **Rare ingredients** dropped by specific mobs (player kills only)
- **Totem Altars** — special crafting stations set up by admins
- **Guardian Totem** pairing system — link two totems together for a rescue ability
- Post-pop **cooldown** displayed in the action bar

---

## The Totems

### 💥 Blast Totem
When it pops, every nearby living entity is **blasted away** from you in a shockwave. Particles and an explosion sound accompany the burst.

*Best for:* Escaping mobs that have surrounded you.

---

### 👁 Shadow Totem
When it pops, you turn **completely invisible** for a short duration. Your armor is hidden too — no silhouette, no fire overlay, nothing.

*Best for:* Stealth, PvP escapes, buying time.

---

### ⚡ Storm Totem
When it pops, you're **randomly teleported** to a safe surface location nearby. Thunder sounds, portal particles — you're just gone.

*Best for:* Escaping a dangerous spot instantly.

---

### 🔀 Swap Totem
When it pops, your position **instantly swaps** with a random nearby player. They go where you were; you go where they were.

*Best for:* PvP chaos. The target is chosen randomly — no predictable counters.

---

### 🛡 Guardian Totem
A **paired totem** system. Two Guardian Totems are linked when crafted together. When yours pops, your linked ally is **pulled to your location** — their linked totem is consumed in the process.

*Best for:* Co-op rescue. If you're about to die, your partner gets pulled to you instantly.

> Linked totems can be traded to any player. The link follows the item, not the person.

---

## Ingredients

Each totem requires a specific ingredient dropped by a specific mob. Only **player kills** trigger drops — mob grinders don't work.

| Ingredient | Source Mob | Notes |
|---|---|---|
| **Blast Eye** | Wither Skeleton | Standard drop chance |
| **Shadow Eye** | Creaking | 1.21.4+ mob |
| **Storm Eye** | Enderman | Must be in a **Warped Forest** or killed during **thunder/rain** |
| **Guardian Eye** | Guardian | Standard drop chance |
| **Swap Eye** | Shulker | Standard drop chance |

---

## Crafting at an Altar

1. Find a **Totem Altar** (an admin-designated anvil — ask your admins where they are)
2. Right-click the altar to open the crafting GUI
3. Place your ingredients in the correct pattern
4. Click the result to receive your totem

---

## Cooldown

After a totem pops, there is a **global cooldown** before any totem can activate again. The remaining time is shown in your action bar. You cannot activate another totem until it expires.

---

## Commands

| Command | Permission | Description |
|---|---|---|
| `/totemaltars altar` | All players | Open a nearby Totem Altar |
| `/totemaltars createaltar` | Admin | Designate the nearest anvil as an altar |
| `/totemaltars removealtar` | Admin | Remove the nearest altar |
| `/totemaltars giveingredient <player> <type>` | Admin | Give a player an ingredient |
| `/totemaltars givetotem <player> <type>` | Admin | Give a player a totem |
| `/totemaltars reload` | Admin | Reload config |

---

## Tips

- **Storm Eyes** only drop under specific conditions — hunt Endermen in the Warped Forest or wait for a storm
- **Swap Totem** picks a random nearby player, so you can't guarantee who you swap with in a crowd
- **Guardian Totems** must both be present for the pull to work — if your partner doesn't have their linked totem, nothing happens
- Ingredients look like Eye of Ender but have unique names and lore — check your item before assuming
- Totems stack in inventory — stock up

---

## Changelog Summary

**v1.0.20** — Swap Totem now picks a random nearby player instead of always the nearest. Shadow Totem invisibility fully fixed (armor silhouette, particles, overlays all hidden).

**v1.0.17** — Config migration system added: updates automatically on startup without overwriting your settings. Item migration: ingredient materials that changed between versions are upgraded in existing inventories on login.

**v1.0.15–16** — Initial release: 5 totems, 5 ingredients, altar system, Guardian link system, cooldown display.
