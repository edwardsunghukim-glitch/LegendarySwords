# LegendarySwords 0.3.0

**24 legendary weapons** for Paper 1.21.11, with detailed original voxel art inspired by Zelda swords, animated material atlases, ability effects and staged ultimates. The sixteen additions have separate attack patterns and themed colors/geometry; no game assets are ripped.

Right-click uses an ability. Sneak + right-click uses a fully charged ultimate. Ultimates play a shared reveal, then release their attack, damage and a short character follow-through. Observers see the cast body with vanilla armor hidden; the caster keeps normal first-person/F5 rendering and inventory. Ordinary walking/running/swings remain vanilla.

## Install

1. Stop your server, replace the old LegendarySwords JAR with **LegendarySwords-0.3.0.jar**, and restart. Keep your configuration/data and the separate LegendaryAdmin JAR.
2. The plugin installs/updates **48 BetterModel files** automatically, backing up known originals and preserving edits. Wait for imports, then check `/ls status`; use `/bm reload` if needed. BetterModel 3.1 requires Java 25 on the server.
3. **Replace the old resource pack with the new 0.3.0 combined pack.** It preserves the previous supplied BetterModel pack and adds all 24 item definitions plus official-compiler geometry/textures for the new named slash aliases used by the reported BetterModel 3.1-SNAPSHOT-526 setup. Different BetterModel namespaces/builds or other changed server models need a fresh `build.zip` merged with this release's sword pack.
4. Use `/ls give all`, `/ls cinematics on`, `/ls charge 100`, and `/ls resetcooldowns` to test. PvP remains off by default; enable `pvp-enabled` if player targets should take damage.

The standalone sword pack contains all 24 item models. The combined pack is the single ZIP to enable in Minecraft; keep it zipped. No resource pack is needed for LegendaryAdmin.

## Weapons

| Sword | ID | Ability | Ultimate |
| --- | --- | --- | --- |
| Master Sword | `master_sword` | Skyward Laser | Triforce Judgment |
| Flameblade | `flameblade` | Ring of Fire | Inferno Vortex |
| Fierce Deity Sword | `fierce_deity_sword` | Lunar Crescent | Falling Moon |
| Frostblade | `frostblade` | Ice Fang | Frozen Sanctuary |
| Thunderblade | `thunderblade` | Chain Lightning | Thunderstorm |
| Royal Claymore | `royal_claymore` | Royal Faultline | King's Verdict |
| Eightfold Blade | `eightfold_blade` | Gale Step | Eightfold Tempest |
| Biggoron Sword | `biggoron_sword` | Goron Spin | Mountain Breaker |
| White Sword | `white_sword` | Radiant Fan | Dawn Barrage |
| Goddess Sword | `goddess_sword` | Spiral Light | Sacred Helix |
| Goddess Longsword | `goddess_longsword` | Falling Star | Celestial Rain |
| Goddess White Sword | `goddess_white_sword` | Light Bloom | Heaven's Garden |
| True Master Sword | `true_master_sword` | Triad Beam | Golden Trinity |
| Phantom Sword | `phantom_sword` | Spirit Blades | Phantom Legion |
| Lokomo Sword | `lokomo_sword` | Spirit Rail | Divine Tracks |
| Razor Sword | `razor_sword` | Razor Wheel | Steel Cyclone |
| Gilded Sword | `gilded_sword` | Gilded Petals | Golden Tempest |
| Great Fairy Sword | `great_fairy_sword` | Blossom Strike | Fairy's Awakening |
| Sword of the Six Sages | `sword_of_six_sages` | Sage Pillars | Sixfold Seal |
| Guardian Sword | `guardian_sword` | Pulse Burst | Guardian Overdrive |
| Ancient Short Sword | `ancient_short_sword` | Hex Blades | Ancient Array |
| Scimitar of the Seven | `scimitar_of_the_seven` | Sand Spiral | Desert Sovereign |
| Forest Dweller's Sword | `forest_dweller_sword` | Vine Snare | Forest Dominion |
| Dusk Claymore | `dusk_claymore` | Shadow Ridge | Twilight Rupture |

## Tuning

Edit `plugins/LegendarySwords/config.yml`, then `/ls reload`:

```yaml
ultimates:
  speed: 2.0
damage:
  ability-multiplier: 1.5
  ultimate-multiplier: 2.0
```

Speed 1.0 gives the default 1.2-second windup, 2.0 gives 0.6 seconds, and 0.5 gives 2.4 seconds. `swords.<id>.ultimate-speed` overrides one weapon. This controls windup/character clip speed, not cooldowns or projectile travel. Individual sword damage/cooldowns remain editable; new swords get defaults even in old configs. Damage goes through Paper's armor, toughness, enchantments and resistance rules.

## Models and animations

There are 24 player rigs and 24 signature effect models. Player files include `idle`, `walk`, `run`, `pose`, `swing`, `ability`, `ultimate`, plus `draw`, `charge`, `release`, `recover`. Runtime cast-only mode uses ability/ultimate and release; the extra authoring clips are included for editing, with ordinary movement/poses disabled. New casts use forward guards checked against actual arms, torso, head and held-item geometry. Optional OptiFine-compatible emissive textures brighten luminous materials; vanilla Minecraft does not make swords emit real world lighting.

`bettermodel.cast-body-animations: false` disables character disguises if needed while preserving attack effects, cinematics and damage. The previous equipment disconnect still needs live-client verification. See [BetterModel setup](docs/BETTERMODEL.md) and [testing](docs/TESTING.md).

## Build

Use JDK 25+ and Maven 3.9+: `mvn verify`. Core classes target Java 21. Validate assets with `python3 scripts/test_resource_pack.py` and `python3 scripts/test_bettermodel_assets.py`. Asset authoring scripts are explicit source changes; ordinary builds package committed art.

[LegendaryAdmin](admin-plugin/README.md) is a separate plugin with `/la`, moderation, vanish and read-only inventory viewing. Build it with `mvn -f admin-plugin/pom.xml verify`.
