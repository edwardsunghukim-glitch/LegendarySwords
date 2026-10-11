1.1.2 stable rendering update: floating accessory displays are disabled by default and on the first upgrade (saved as `rendering-version: 2`). Native wearable textures, armor stats, passives and abilities remain. Extra 3D shoulders and horns are not displayed in this mode. Manually setting `accessories-enabled: true` restores the old attachments and their latency limitations. Existing resource pack stays compatible.

1.1.1 redesign follows the horned green knight reference: no wings/halos/fins, dark plates, green scale insets and curved horns on each greathelm. The plugin removes accessory interpolation and updates every tick, reducing trailing. This does not eliminate network latency or make rigid pauldrons follow vanilla arm animations. Install the new JAR and matching combined pack.

Dark fantasy knight redesign: scarred metal, enclosed greathelms, horned variants, shoulder spikes, leather straps and cloth panels. Install the 1.1.0 JAR and the latest combined pack together. A third cosmetic display follows head position and pitch; it is hidden from its wearer to prevent first-person obstruction, while other players see it. The wearer still sees wearable helmet textures and existing crests. Accessories now use normal world lighting.

# LegendaryArmor 1.1.0

Five original fantasy armor sets for Paper **1.21.11**, Java **21+**. The plugin is independent of LegendarySwords and BetterModel. Each set has four real netherite armor pieces with separate custom item icons, painted 256×128 wearable textures, three cosmetic 3D attachments and an active ability. Protection uses native netherite armor, armor toughness and knockback resistance. No player disguise is used.

| Set ID | Full-set passive | Sneak + F ability | Accessories |
| --- | --- | --- | --- |
| `angel` | Restores 1 HP every 4 seconds | Radiant Sanctuary: heals 6 HP and strikes enemies in a golden circle | Horned helmet and spiked plates |
| `dragon` | Immune to fire, lava and hot-floor damage | Wyrmfire Breath: a fire cone follows your aim, including vertical aim | Green scale panels and curved horns |
| `leviathan` | Water breathing underwater and 8% movement-speed bonus | Tidal Dominion: a traveling wave damages and pushes enemies | Abyssal plates and horned helmet |
| `stone` | 12% extra damage reduction and added knockback resistance | Earthshatter: an expanding shockwave with stone spikes | Heavy pauldrons and horned helmet |
| `frost` | Successful melee attackers receive a brief slow | Glacial Domain: ice crystals and a snow ring repeatedly damage and slow enemies | Cold steel plates and horned helmet |

Wear **all four matching pieces** to activate passives, accessories and abilities. Sneak + swap-hand key (default **F**) activates the ability; `/armor ability` also works. Ordinary swap-hand behavior remains when you are not sneaking or not wearing a full set. Each player has one shared armor-ability cooldown, so switching sets does not bypass it. Taking armor off, dying, disconnecting or changing worlds ends an active scene; all scene entities are removed after two seconds. Accessories follow the player and respect invisibility and Bukkit player visibility. These are cosmetic attachments, not a new player hitbox or limb animation.

`/armor` (or `/armors`, `/larmor`, `/legendaryarmor`) opens a 3-row gold menu. Everyone with `legendaryarmor.use` can browse; only `legendaryarmor.admin` can click to receive armor. `/armor give <online-player> <angel|dragon|leviathan|stone|frost>` gives all four pieces and requires four empty storage slots. Admin permission defaults to operators; use permission defaults to everyone. `/armor reload` reloads settings and removes existing scenes/accessories before rebuilding them.

## Installation

Stop the server, add `LegendaryArmor-1.1.0.jar` to `plugins`, and restart. Keep your existing plugins. Install **LegendaryArmor-Swords-combined-resource-pack.zip** as your single client resource pack, replacing the previous combined sword pack. Keep the resource pack zipped. This preserves the known 0.5.3 sword/BetterModel compilation; a differently configured BetterModel build or additional models still need a fresh merge. No model files need to be added to BetterModel for this plugin.

The supplied bundle also includes the standalone armor pack and an honest front-view authoring preview. The preview uses the actual textures and accessory boxes; it does not simulate Minecraft lighting, player skin or animation. Custom wearable armor retains the vanilla humanoid shape; extra volume comes from the accessory models. Worn armor and accessories use normal Minecraft lighting; attack effect meshes use bright display lighting. There is no shader-dependent bloom.

## Configuration

Edit `plugins/LegendaryArmor/config.yml`, then `/armor reload`. Configure per-set damage, cooldowns, healing, movement speed, stone reduction and knockback resistance, fire duration and slow duration. `pvp` and `friendly-fire` control player ability targets, with world PvP and scoreboard-team checks. Native damage events are used for armor/protection and combat-tag compatibility; secondary fire/knockback/slow effects apply only when damage is accepted. Dragon breath uses sight checks and stops particles at solid blocks; it does not break blocks. Other attacks also leave terrain unchanged.

`accessories-enabled` toggles all cosmetic entities. `accessories-visible-to-owner: false` hides your own attachments if they obstruct your view with a camera/client mod. Accessories have static geometry that follows position/yaw; they do not provide custom walking/body animations. Armor item names and enchantments are preserved as normal items; newly generated pieces use the current `unbreakable` setting.

## Development and validation

Run `mvn verify` in this directory. Generate assets with `python scripts/generate_armor_pack.py` from the repository root (requires Pillow), then `python scripts/test_armor_pack.py`. Tests cover set/slot validation, input trigger behavior, aimed cone targeting, PvP/team eligibility, passive modifier cleanup, all pack references, geometry bounds and preservation of existing combined assets. No live Minecraft server/client test has been performed.
