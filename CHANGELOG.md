# TotemAltars Changelog

## v1.0.20 (current)

- **Swap Totem** — now picks a random nearby player instead of always targeting the nearest. Prevents predictable counters in PvP.
- **Shadow Totem** — fixed invisibility. Fire overlay, arrow shafts, and stinger particles are now suppressed via metadata packet interception during the invisible window. Armor silhouette hidden via ProtocolLib.

---

## v1.0.18 – v1.0.19

- Refactored command handling; removed dead/unreachable code paths.
- Expanded PlaceholderAPI placeholders for cooldown tracking.

---

## v1.0.17

- Config migration system added (`ConfigMigrator`) — on startup, detects the stored config version and adds any missing keys with safe defaults. Existing values are never overwritten.
- Item migration on player join (`ItemMigrationListener`) — scans inventory for ingredients whose material changed between versions and upgrades them in place.

---

## v1.0.16 – v1.0.15

- Initial public release baseline.
- Five custom totems: Blast, Shadow, Storm, Swap, Guardian.
- Five ingredients (Eye of Ender variants) dropped by specific mobs.
- Altar system: admin-designated anvils, GUI-based crafting, TextDisplay holograms.
- Guardian Totem link system: right-click a player to pair; on pop, linked ally is pulled to your location.
- Post-pop cooldown with action bar countdown display.
- `/totemaltars` command suite: createaltar, removealtar, giveingredient, givetotem, reload, altar.
