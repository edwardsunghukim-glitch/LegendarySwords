# BackroomsHorror 1.0.0

Original Paper 1.21.11 plugin inspired by Backrooms horror. This is not the
Backrooms: Found Footage mod or a reproduction of its proprietary entity assets.
CurseForge blocked access to the supplied page during development, so its exact
entities/levels could not be verified. No client mod/resource pack is required.

## Install and summon

Put BackroomsHorror-1.0.0.jar in plugins and restart. Admin permission:
backrooms.admin (operators by default). Regular players have backrooms.use.

- /br summon stalker — zombie-based charger, darkness pulse, temporary invisibility.
- /br summon smiler — wither-skeleton-based screecher, blinding pulse, speed burst.
- /br summon hound — hostile wolf with leap, slowing howl and mauling bite.
- /br summon watcher — husk-based entity that freezes when its target watches it;
  while unseen it can blink forward, emit darkness and deliver a slowing touch.
- /br status
- /br remove — removes only entities spawned by this plugin.
- /br reload

Tab completes commands and entities. Entities appear three blocks in front of you,
pursue Survival/Adventure players within 32 blocks and despawn after five minutes
by default. They use existing Minecraft mob appearances with colored names and
custom behaviors, not imported mod models. Player armor applies to damage.
No drops/XP; unload/shutdown removes NPCs. Per-world maximum defaults to eight.
Auto-spawning defaults OFF; no entities are automatically added to your normal worlds.

WorldGuard region `spawn` and a 16-block radius around native world spawns are
protected from NPC targeting/damage. Edit spawn-protection for your region names.
NPC damage goes through Bukkit so other plugins can cancel it. Generated entry
rooms additionally have a safe-entry-radius. Protection applies to operators too.

## Included optional map

/br map 0 creates and opens a separate generated Level 0 map (yellow rooms).
/br map 1 creates Level 1 (concrete corridors).
/br map 2 creates Level 2 (dark pipe corridors).

These are original connected room grids with different materials/lighting, not
maps exported from the mod. World size defaults to 256 x 256; chunks generate on
demand. Generation creates new world folders, not modifications to your main world.
Save generated world folders if you want to move the maps to another server.
Actual prebuilt Minecraft world ZIPs are supplied for all three levels, each
256 x 256 blocks with 256 pregenerated chunks, level.dat and region files.
Extract each ZIP into its own server-root folder named backrooms_horror_0,
backrooms_horror_1 or backrooms_horror_2, alongside the normal world folder.
Back up existing same-name folders first; do not overwrite a live world.
Restart and run /br map 0, 1 or 2 to load it. No WorldEdit is needed. World
data uses Minecraft Java 1.21 format and can be upgraded by Paper 1.21.11.
Standalone singleplayer use is supported; plugin-specific entities and escape
logic require BackroomsHorror on a server. Minecraft rendering/server loading
still need an in-game check.

/br enter <0|1|2> lets a player enter a generated level; /br leave returns them.
The gold pressure plate on the emerald floor block at x=52,z=52 advances 0 -> 1
-> 2 -> escape. /br setportal saves the block you look at; right-click that block
to enter. No entry portal is created unless you set one. Return positions are
saved; deaths in a map respawn outside it. Inventories/held items are untouched;
normal server death drops and kits remain in effect. KillTokens combat-tagged
players cannot enter or leave with these travel commands/plates.

If desired, set auto-spawn: true to add entities only in the generated maps.
Normal players cannot build/break in map worlds by default; admins can. Vanilla
explosions/other plugins may change blocks; use WorldGuard if you need complete
map protection. The maze does not include found-footage camera UI, shaders,
custom NPC meshes, exact mod levels or mod-specific mechanics.

## Configure and validate

plugins/BackroomsHorror/config.yml sets each NPC's hearts, attack damage (health
points), movement speed, cooldown and shared cap/lifetime. World names/material
layout settings apply to newly created worlds. Do not reuse an existing unrelated
world folder with the configured prefix. Existing maps are loaded with their
saved chunks; /br map does not regenerate them.

Maven verify checks doorway connectivity, a reachable escape route, watcher view
angles and admin autocomplete. Rendering, NPC AI, actual world generation and
WorldGuard integration still need a test on your live Paper server.
