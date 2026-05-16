# TotemAltars — Claude Context

## Project purpose

TotemAltars is the production PaperMC plugin for a custom SMP. Players collect rare Eye-based ingredients from specific hostile mobs, bring them to an admin-placed Totem Altar (anvil), and combine them with a vanilla Totem of Undying to craft a custom totem with a unique death ability.

This is the **stable production repo**. Experimental features are developed in the separate `TotemAltarDev` repo first.

---

## Build

```bash
./gradlew build
```

Output: `build/libs/TotemAltars-{version}.jar`

Version is tracked in `version.properties` and stamped into `plugin.yml` at build time. The build task auto-increments the patch version on every run.

---

## Java / Paper target

- Java: 21
- Paper API: 1.21.11
- `api-version: '1.21'` in plugin.yml

---

## Dependencies

- **ProtocolLib** — hard dependency. Plugin will not load without it. Required for Shadow Totem armor hiding.
- **PlaceholderAPI** — soft dependency. Enables cooldown placeholders.

---

## Key design rules

- **Ingredient identity is PDC-only.** All ingredients use Eye of Ender as material. Identity key: `ingredient_type` (string). Never use display name or material for item checks.
- **Totem identity is PDC-only.** Key: `totem_type` (string). Guardian Totem also carries `guardian_link_id` (UUID as string).
- **Config keys are never renamed or removed** without a `ConfigMigrator` migration entry. Existing server configs must never break on update.
- **No GUI spam.** The altar GUI exists for crafting only. Do not add more GUIs.
- **ProtocolLib packet interception** is used in `ShadowArmorManager` to hide armor during Shadow invisibility. Changes here require understanding of the metadata packet format.
- **Distance checks** always use `distanceSquared()` and check world equality first.
- `Particle.FLASH` in 1.21.4+ requires `DustOptions` data — plain call crashes.

---

## Current version

1.0.20

---

## Key files

| File | Role |
|---|---|
| `TotemAltars.java` | Main class, wires all managers and listeners |
| `utils/ItemUtil.java` | Creates ingredient and totem items; all PDC keys live here |
| `listeners/MobDropListener.java` | Ingredient drops from mobs; drop chances from config |
| `listeners/TotemActivationListener.java` | EntityResurrectEvent handler; dispatches totem abilities |
| `listeners/AltarListener.java` | Right-click opens altar GUI; handles input/output slots |
| `managers/AltarManager.java` | Loads/saves altars.yml; manages hologram TextDisplay entities |
| `managers/ConfigManager.java` | Typed config value getters |
| `managers/ConfigMigrator.java` | Version-aware config migration on startup |
| `managers/CooldownManager.java` | Per-player cooldown tracking after totem pop |
| `managers/ShadowArmorManager.java` | ProtocolLib packet intercept for Shadow invisibility |
| `gui/AltarGUI.java` | 27-slot altar GUI builder |
| `listeners/GuardianListener.java` | Guardian Totem right-click-player linking |

---

## Known issues / next TODOs

- No known bugs in v1.0.20.
- Swap Totem now picks a random nearby player instead of the nearest — intentional design choice.
- Shadow invisibility is stable; fire/arrow/stinger suppression handled via metadata packets.
- Dev branch (TotemAltarDev) is building the Eye observation system and awakening ritual — these will eventually be merged here.
