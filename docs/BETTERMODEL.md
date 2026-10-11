# BetterModel 50-weapon integration

Use BetterModel 3.1 on Paper 1.21.11 with server Java 25. The reported version is 3.1-SNAPSHOT-526. Keep `module.player-animation` and `module.model` enabled for cast bodies and slash models.

The plugin installs 115 models with backups, then imports them. If `/ls status` reports preserved edited files or missing clips, update those edits or install the supplied model ZIP manually. Cast rigs require `ability`, `ultimate`, and `release`. `draw`, `charge`, `recover`, and legacy movement clips are also supplied for authoring; the runtime keeps normal movement outside casts.

Observers receive cast bodies with vanilla equipment hidden; the owner is excluded from that body/equipment packet path and keeps their normal character and inventory. Cast items are immutable snapshots captured on the server thread. Ultimate speed scales the windup/body clip; a short release clip plays with the attack afterward. Cleanup covers clip expiry, death, quit, teleport, world change and plugin shutdown.

Use the new 0.5.2 combined pack. It retains the previously supplied generated BetterModel assets and adds officially compiled model geometry and textures for all expansion attack meshes and five new boss bodies, with named `bettermodel:c/legendaryswords_<id>_slash_<bone>` aliases. This is tailored to the reported snapshot, not a substitute for a fresh server build when BetterModel's namespace/export mode or unrelated models change. In those cases download the new `build.zip` and use the merge command in [installation](INSTALLATION.md).

No live Minecraft-client test was performed. If an equipment decoder disconnect recurs, set `bettermodel.cast-body-animations: false` and restart; attacks and world effects continue. BetterModel cannot replace vanilla first-person arm animation without client support.

## Boss art in 0.5.2

All five boss bodies are original rebuilds with painted 128×128 material atlases. The dragon and leviathan have linked spine bones and separate jaw controls; other forms have connected body/limb controls. Only the root scales during transformation, so a long bone chain does not collapse from repeated scale multiplication. The official pack exporter traverses nested bones to include every drawable group. Uploaded reference assets are excluded from the plugin and source bundle.

Optional visual review with Blender: `blender -b -t 4 --python scripts/render_boss_form.py -- src/main/resources/bettermodel/models/legendaryswords_infernal_dragon_boss.bbmodel /tmp/dragon.png`. This renders the real model and texture at an idle pose; it does not verify in-game viewer handling.

## Colossal forms in 0.5.2

All five bundled boss meshes use larger authored geometry and joint origins. The angel is roughly nine blocks tall; the colossus and frost titan roughly ten. The winged serpentine dragon is about nineteen blocks long and the aquatic leviathan about twenty-four. Extents vary with animation. The dragon has a curved spine, horned snout, whiskers, fangs, wing membranes and small claw limbs; the leviathan has a broad jaw, gill frills, pectoral fins, a ribbon sail and crescent tail. The owner still keeps normal first-person rendering; observers see the giant form. The normal player collision box and combat rules remain unchanged.

Use the updated JAR and matching 0.5.2 combined pack together. Automatically installed unedited models update using the saved installer hashes; edited server models remain preserved. Add `--player-scale` to the Blender render command for a neutral 1.8-block mannequin beside the real model.

## Boss import compatibility in 0.5.3

Boss outliner nodes now carry complete names, pivots, rotations and visibility as well as the Blockbench 5 group table. Official compiler checks load every boss both normally and with the group table removed, comparing every bone name, origin, rotation and geometry scale. This validates the inline fallback; it does not claim testing the reported snapshot on a live client.
