# BackroomsHorror 1.0.2

Original Paper 1.21.11 plugin inspired by Backrooms horror. This is not the
Backrooms: Found Footage mod or a reproduction of its proprietary entity assets.
CurseForge blocked access to the supplied page during development, so its exact
entities/levels could not be verified. No client mod is required. The converted Bacteria model requires BetterModel
3.1 and its regenerated resource pack; the other NPCs use vanilla appearances.

## Install and summon

Put BackroomsHorror-1.0.2.jar in plugins and restart. Admin permission:
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
by default. Stalker/Smiler/Hound/Watcher use Minecraft mob appearances with colored names
and custom behaviors. Bacteria uses the uploaded Lifeform mesh converted for
BetterModel; it requires the matching generated resource pack. Player armor applies to damage.
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

## Uploaded Bacteria model (1.0.2)

The uploaded custom-bacteria.zip contained source/Lifeform.glb: a black-material
rigged mesh with 26,478 triangles, 23 rig bones and no animation clips or textures.
The supplied conversion preserves every triangle in the rest pose, scales height
to 3.2 blocks and supplies a near-black material texture. Faces are grouped by
their strongest joint influence for BetterModel's rigid bone animation; Blender
weighted skin deformation is approximated and may show seams during motion.
New clips: idle, walk, run, charge, scream and claw.

Install BackroomsHorror-1.0.2.jar and BetterModel 3.1. Restart: the plugin copies
backroomshorror_bacteria.bbmodel into plugins/BetterModel/models if missing.
It never overwrites an existing edited model. Run /bm reload and install the
newly generated plugins/BetterModel/build.zip as your client/server resource pack.
If you already use other packs, merge the updated BetterModel build into your
existing combined pack. Run /br status to check model readiness, then summon a
fresh entity with /br summon bacteria. Existing entities should be removed
with /br remove first. The model file is also bundled for manual installation.

Default bacteria-renderer is bettermodel, including on existing configs missing
this setting. Missing BetterModel/model reports an error instead of silently
showing the old body. To deliberately use the older block-display fallback, set
bacteria-renderer: blocks and restart. The other four NPCs still work without
BetterModel. Their appearances were not changed.

Bacteria retains 45 hearts, rush charge, darkness/scream pulse and slowing claw
attack. These defaults work on old configs lacking its section. NPC appearance
and animation are cleaned up on death/removal/shutdown. The underlying enlarged
central wither-skeleton hitbox is retained; individual mesh limb collision is
not supplied. Rendering, resource-pack export, animation seams, and alignment
still need an in-game check on your BetterModel snapshot.

The converted model was parsed and loaded in strict mode using BetterModel's
actual 3.1 model loader. Five Java checks passed including triangle preservation,
all six clips, rig count, map connectivity and autocomplete. The preview is an
offline render of the converted geometry, not a Minecraft screenshot.
