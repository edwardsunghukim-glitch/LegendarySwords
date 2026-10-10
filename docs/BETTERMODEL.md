# BetterModel 50-weapon integration

Use BetterModel 3.1 on Paper 1.21.11 with server Java 25. The reported version is 3.1-SNAPSHOT-526. Keep `module.player-animation` and `module.model` enabled for cast bodies and slash models.

The plugin installs 115 models with backups, then imports them. If `/ls status` reports preserved edited files or missing clips, update those edits or install the supplied model ZIP manually. Cast rigs require `ability`, `ultimate`, and `release`. `draw`, `charge`, `recover`, and legacy movement clips are also supplied for authoring; the runtime keeps normal movement outside casts.

Observers receive cast bodies with vanilla equipment hidden; the owner is excluded from that body/equipment packet path and keeps their normal character and inventory. Cast items are immutable snapshots captured on the server thread. Ultimate speed scales the windup/body clip; a short release clip plays with the attack afterward. Cleanup covers clip expiry, death, quit, teleport, world change and plugin shutdown.

Use the new 0.5.0 combined pack. It retains the previously supplied generated BetterModel assets and adds officially compiled model geometry and textures for all expansion attack meshes and five new boss bodies, with named `bettermodel:c/legendaryswords_<id>_slash_<bone>` aliases. This is tailored to the reported snapshot, not a substitute for a fresh server build when BetterModel's namespace/export mode or unrelated models change. In those cases download the new `build.zip` and use the merge command in [installation](INSTALLATION.md).

No live Minecraft-client test was performed. If an equipment decoder disconnect recurs, set `bettermodel.cast-body-animations: false` and restart; attacks and world effects continue. BetterModel cannot replace vanilla first-person arm animation without client support.
