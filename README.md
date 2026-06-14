# TotemAltars

A Paper 1.21 plugin adding custom Totem Shards dropped by hostile mobs, with crafting altar mechanics for the shard system.

## Features
- Custom Totem Shards dropped by hostile mobs
- Bedrock Relic system with placement mechanics
- Altar crafting GUI for assembling totems
- Affinity listener and ritual mechanics
- PlaceholderAPI expansion support
- Legacy item migration and cleanup

## Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/totemaltars giveshard` | Give a shard | `totemaltars.admin` |
| `/totemaltars givealtar` | Give an altar block | `totemaltars.admin` |
| `/totemaltars givefinishedaltar` | Give a finished altar | `totemaltars.admin` |
| `/totemaltars givebedrockrelic` | Give a bedrock relic | `totemaltars.admin` |
| `/totemaltars givetotem` | Give a totem | `totemaltars.admin` |
| `/totemaltars purgelegacy` | Remove legacy items | `totemaltars.admin` |
| `/totemaltars reload` | Reload configuration | `totemaltars.admin` |

## Build
```bash
./gradlew build
```
