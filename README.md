# LegendarySwords 0.5.1

**55 legendary weapons** for Paper 1.21.11, with detailed original voxel art inspired by Zelda swords, animated material atlases, ability effects and staged ultimates. The latest 26 additions have freshly authored silhouettes and thematic textures, with rings, mines, moving hazards and persistent domains; no game assets are ripped. The previous 16 additions also receive separate engraved texture patterns.

Right-click uses an ability. Sneak + right-click uses a fully charged ultimate. Ultimates play a shared reveal, then release their attack, damage and a short character follow-through. Observers see the cast body with vanilla armor hidden; the caster keeps normal first-person/F5 rendering and inventory. Ordinary walking/running/swings remain vanilla.

## Install

1. Stop your server, replace the old LegendarySwords JAR with **LegendarySwords-0.5.1.jar**, and restart. Keep your configuration/data and the separate LegendaryAdmin JAR.
2. The plugin installs/updates **115 BetterModel files** automatically, backing up known originals and preserving edits. Wait for imports, then check `/ls status`; use `/bm reload` if needed. BetterModel 3.1 requires Java 25 on the server.
3. **Replace the old resource pack with the new 0.5.1 combined pack.** It preserves the previous supplied BetterModel pack and adds all 55 item definitions plus official-compiler geometry/textures for the new named slash aliases used by the reported BetterModel 3.1-SNAPSHOT-526 setup. Different BetterModel namespaces/builds or other changed server models need a fresh `build.zip` merged with this release's sword pack.
4. Use `/ls give all`, `/ls cinematics on`, `/ls charge 100`, and `/ls resetcooldowns` to test. PvP remains off by default; enable `pvp-enabled` if player targets should take damage.

The standalone sword pack contains all 55 item models. The combined pack is the single ZIP to enable in Minecraft; keep it zipped. No resource pack is needed for LegendaryAdmin.

## Boss weapons

Five implements have separate item art, physical attack meshes and original monster rigs. Version 0.5.1 rebuilds all five boss bodies with closed rounded contours, individual 128×128 material atlases, layered angel feathers, carved stone plates, long serpent forms, dragon membranes, and crystal armor. Dragon and leviathan jaws have separate animated controls. The uploaded dragon was used only as a visual reference; its geometry, textures and animations are not included.

| Weapon | ID | Attack | Ultimate form / power |
| --- | --- | --- | --- |
| Angel Sovereign | `angel_sovereign` | Feathered wing cleave and radiant impacts | Winged angel; temporary flight and sanctuary healing |
| Granite Colossus Fists | `granite_colossus` | Animated boulder fists and rubble shockwaves | Rock monster with oversized fists and boulder hurl |
| Sky Leviathan Fang | `sky_leviathan` | Snapping fanged jaws and a directional bite | Flying sea serpent; bite and tidal roar |
| Infernal Dragon Claw | `infernal_dragon` | Animated claws/jaws and aimed fire breath | Winged dragon; fire breath and claw rend |
| Frost Titan Hammer | `frost_titan` | Ice hammer slam and rising ice shards | Giant with an animated hammer and ice prison |

Boss forms begin when the ultimate releases and last five minutes by default. Left-click or right-click during the form triggers its primary attack; sneak + right-click triggers the secondary; two form abilities have separate configurable cooldowns. The reveal triggers an initial attack; further attacks are controlled by the player. A countdown shows remaining form time and the two ability names. Normal holding and walking remain vanilla. Flight ends with the form; prior flight is restored and survival flyers receive a short slow-falling landing window. Forms stop on death, quit, teleport, world change, reload and shutdown. Armor still reduces damage and PvP/protection rules apply.

`bosses.duration-seconds` controls duration (3–600); `bosses.flight-enabled` controls flight; `bosses.smash-interval-ticks` controls the primary interval; `bosses.special-interval-ticks` controls the secondary interval (40–1200 ticks; primary 12–200). Existing `ultimates.speed` and per-weapon `swords.<id>.ultimate-speed` control reveal speed. Charge and normal ultimate cooldown are required to enter a form; its two skills then use their own cooldowns.

**Viewing:** other players see the custom boss body with the vanilla body hidden. The owner keeps normal first-person/F5 rendering to avoid the previous equipment packet and camera issues, and sees the shared attack meshes and effects. Boss shapes do not enlarge the player's physical hitbox. BetterModel is required for custom meshes, and the matching combined pack is required on clients. Without BetterModel, attack particles, protected damage and temporary powers remain available. This release has automated validation, but requires an in-game check on your snapshot build.

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
| Ember Halo | `ember_halo` | Burning Circlet | Domain: Furnace Crown |
| Void Reaver | `void_reaver` | Collapsing Ring | Domain: Event Horizon |
| Astral Edge | `astral_edge` | Orbiting Stars | Domain: Astral Observatory |
| Tidebreaker | `tidebreaker` | Tidal Wall | Domain: Drowned Court |
| Storm Cage | `storm_cage` | Conductive Prison | Domain: Storm Tribunal |
| Chrono Saber | `chrono_saber` | Clockwork Seal | Domain: Broken Hourglass |
| Eclipse Fang | `eclipse_fang` | Twin Eclipse | Domain: Black Sun |
| Crystal Oath | `crystal_oath` | Prism Mines | Domain: Shattered Cathedral |
| Bloodthorn | `bloodthorn` | Thorn Procession | Domain: Crimson Garden |
| Sunspire | `sunspire` | Solar Descent | Domain: Noonstar Sanctuary |
| Moonveil | `moonveil` | Returning Crescent | Domain: Lunar Tides |
| Runic Judgment | `runic_judgment` | Cross Seal | Domain: Rune Labyrinth |
| Nebula Blade | `nebula_blade` | Star Serpent | Domain: Nebula Vault |
| Obsidian Maw | `obsidian_maw` | Magma Teeth | Domain: Volcanic Throne |
| Seraph Wing | `seraph_wing` | Wingbreak | Domain: Seraph Ascension |
| Abyssal Trident | `abyssal_trident` | Depth Charge | Domain: Abyssal Whirlpool |
| Mirage Saber | `mirage_saber` | Echo Traps | Domain: Hall of Mirages |
| Meteor Brand | `meteor_brand` | Meteor Mark | Domain: Falling Firmament |
| Glacial Crown | `glacial_crown` | Frost Coronation | Domain: Winter Palace |
| Tempest Rapier | `tempest_rapier` | Storm Lance | Domain: Eye of the Tempest |
| Gravity Cleaver | `gravity_cleaver` | Graviton Well | Domain: Singularity |
| Prism Sword | `prism_sword` | Triangular Verdict | Domain: Crystal Prism |
| Thunder Drum | `thunder_drum` | Resonance Wave | Domain: Thunder Chamber |
| Wraith Scythe | `wraith_scythe` | Reaping Arc | Domain: Soul Harvest |
| Verdant Oath | `verdant_oath` | Seedburst | Domain: Ancient Grove |
| Dragonheart | `dragonheart` | Dragon Breath | Domain: Dragon Furnace |

## Visual polish in 0.4.1

All 50 weapons have separate atlas patterns and continuous blade UVs to avoid repeating the same engraving on each cube. Metal shading is quieter, new blades taper in finer connected steps, fittings sit flush, and first-person/inventory sizes are normalized by model height. All 26 second-collection meshes are checked for disconnected cubes. The new collection also has larger attack areas (5-block ability fields and 8-block ultimate fields where applicable) and themed manifestations: jagged lightning, clock seals, portal rings, orbital blades, tidal walls, crystal spikes, feathers, meteors, ice crowns, roots and dragon fire. Attack range, shape, statuses and names stay matched; armor and hit caps remain enforced.

## Sword browser

`/swords` and `/legendaryswords` open the full collection in two pages. Every player can browse descriptions. Only players with `legendaryswords.admin` (operators by default) can click to receive a weapon. The permission is checked on every click; display items cannot be moved, dragged or cloned out of the menu. `/ls help` and existing subcommands still work.

## Rings and domains

The second collection uses 26 separate attack branches: hollow expanding rims, inward collapse/pull, orbiting satellites, travelling walls, conductive cages, rotating clock hands, converging orbs, proximity mines, thorn perimeters, descending columns, returning crescents, cross/grid seals, aimed corkscrews, magma eruptions, wing sweeps, three-pronged strikes/whirlpools, delayed echoes, falling meteors, inward ice crowns, aimed lances, gravity wells, contracting triangles, resonance shells, reaping arcs, root labyrinths and fire cones. Abilities last up to four seconds; ultimate fields last up to eight seconds after the reveal. Some leave a safe center or gaps: visible geometry defines their hit area. Domains are animated temporary hazards, not solid barriers, and they never alter world blocks or teleport players. At most one ability hit or three ultimate hits per victim; armor and protection rules still apply.

`/ls give all` gives 55 weapons; inventory overflow drops at your feet. Use `/ls give <weapon_id>` for individual testing.

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

There are 55 player rigs, 55 signature effect models, and five ordinary monster rigs. Player files include `idle`, `walk`, `run`, `pose`, `swing`, `ability`, `ultimate`, plus `draw`, `charge`, `release`, `recover`. Runtime cast-only mode uses ability/ultimate and release; the extra authoring clips are included for editing, with ordinary movement/poses disabled. New casts use forward guards checked against actual arms, torso, head and held-item geometry. Optional OptiFine-compatible emissive textures brighten luminous materials; vanilla Minecraft does not make swords emit real world lighting.

`bettermodel.cast-body-animations: false` disables character disguises if needed while preserving attack effects, cinematics and damage. The previous equipment disconnect still needs live-client verification. See [BetterModel setup](docs/BETTERMODEL.md) and [testing](docs/TESTING.md).

## Build

Use JDK 25+ and Maven 3.9+: `mvn verify`. Core classes target Java 21. Validate assets with `python3 scripts/test_resource_pack.py` and `python3 scripts/test_bettermodel_assets.py`. Asset authoring scripts are explicit source changes; ordinary builds package committed art.

[LegendaryAdmin](admin-plugin/README.md) is a separate plugin with direct commands (`/heal`, `/fly`, `/vanish`, `/invsee`, `/adminmenu`), moderation, vanish and read-only inventory viewing. Build it with `mvn -f admin-plugin/pom.xml verify`.
