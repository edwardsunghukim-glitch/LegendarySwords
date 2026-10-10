# BetterModel animations and one combined pack

The integration uses **BetterModel 3.1's API** on Paper 1.21.11. **BetterModel 3.1 requires Java 25 on the server.** The reported server uses `3.1-SNAPSHOT-526`. Version **0.2.3** responds to a Minecraft 1.21.11 equipment-packet decoder disconnect with 557 unread bytes. It copies item metadata before replacing inventory items and supplies cached server-thread item snapshots to the BetterModel mapper. The exact packet cause has not been proven, and compilation does not prove snapshot runtime behavior.

**Player body animations are temporarily paused by default.** Keep `bettermodel.player-body-animations: false` in LegendarySwords' configuration to avoid player-attached BetterModel trackers and their equipment/disguise path. This pauses custom standing, walking, running, pose, and player action body animations. Keep `bettermodel.enabled: true` so BetterModel slash models and ultimate presentation remain available, along with sword textures, combat effects, abilities, and ultimates. `/ls status` reports that body animations are paused.

There are **sixteen `.bbmodel` files**: eight player-animation models and eight slash-effect models. The 0.2.3 JAR retains the 0.2.2 `idle`, `walk`, `run`, `pose`, `swing`, `ability`, and `ultimate` clips without changes. These include the carry/pose adjustments for all eight swords and action adjustments for Master Sword, Fierce Deity Sword, Thunderblade, Eightfold Blade, and Biggoron Sword. No model geometry, textures, sword artwork, slash assets, or resource-pack assets change in 0.2.3.

**Reuse `LegendarySwords-combined-resource-pack-0.2.2.zip` when upgrading from 0.2.2.** The hotfix does not require a new pack or a new `build.zip`. For an upgrade from 0.2.0 or 0.2.1, install that 0.2.2 pack because it includes the item selector those earlier packs lack. When body animations are enabled for a controlled test, the selector hides only the ordinary `THIRD_PERSON` held copy while the animated bone's `FIXED` item, inventory icon, and first-person item stay visible. Cleanup restores the base item model.

## Install or upgrade the models

1. Stop the server, replace the old LegendarySwords JAR with **`LegendarySwords-0.2.3.jar`**, and keep only one plugin JAR. Leave `bettermodel.player-body-animations: false` before restarting with BetterModel installed. Existing configurations without this key use the false default; change an explicit true value to false.
2. The plugin installs missing bundled models automatically. On upgrade it recognizes its original old files, backs them up, and updates them. Files you edited yourself are preserved and reported rather than silently replaced.
3. For manual installation, extract **`LegendarySwords-BetterModel-models.zip`**. Upload the files inside its `players` folder to **`plugins/BetterModel/players`** and those inside `models` to **`plugins/BetterModel/models`**. Back up your own edits before deliberately replacing a file.
4. Enable `module.player-animation` and `module.model` in BetterModel's configuration.
5. Run **`/bm reload`** to import the models and regenerate BetterModel's resource pack. A full restart may help if either plugin was installed or upgraded while the server was running.
6. Keep the **0.2.2 combined pack** if it is already installed. Its BetterModel portion reuses `build (12).zip`; 0.2.3 changes no client assets. For first installation or upgrades from earlier packs, install the 0.2.2 combined pack. Only if your other BetterModel models or configuration changed, download the **new** `plugins/BetterModel/build.zip` from the server file manager and merge it with the sword pack. This path is on your server, not in the release folder on your computer; different output settings may use another path.

The exact filenames follow these patterns for every sword ID:

```text
plugins/BetterModel/players/legendaryswords_<sword_id>.bbmodel
plugins/BetterModel/models/legendaryswords_<sword_id>_slash.bbmodel
```

Sword IDs: `master_sword`, `flameblade`, `fierce_deity_sword`, `frostblade`, `thunderblade`, `royal_claymore`, `eightfold_blade`, and `biggoron_sword`. Keep the filenames and animation clip names unchanged.

`bettermodel.enabled` controls all BetterModel integration. `bettermodel.install-bundled-models` controls asset installation. Both default to true. `bettermodel.player-body-animations` defaults to false, including when an existing configuration omits it. This separate switch retains slash models and ultimate presentation. If the installation option is off, update model files yourself when needed.

Only set `bettermodel.player-body-animations: true` for a controlled test after gathering evidence for the reported disconnect. Restart after changing this setting, and follow [the controlled test checklist](TESTING.md#controlled-player-body-animation-test). No new command is required; edit `plugins/LegendarySwords/config.yml` through your server's file manager.

## Combine the packs

If your server accepts one pack URL, use the published **0.2.2 combined pack**, or merge a compatible BetterModel pack with the eight-sword resource pack. Existing `build (12).zip` BetterModel assets can be reused when other models/configuration stay unchanged. Packs from before 0.2.2 lack its item selectors and must be replaced; the 0.2.3 hotfix itself needs no new merge.

With Python 3 installed, extract `LegendarySwords-source.zip`, put both ZIPs in the project folder, and run from that folder:

```sh
python3 scripts/merge_resource_packs.py build.zip LegendarySwords-resource-pack.zip LegendarySwords-combined-resource-pack.zip
```

On Windows, `python` may be the command instead of `python3`. If the downloaded BetterModel filename is `build (12).zip`, either rename it to `build.zip` or put the filename in quotes in the command.

The script keeps BetterModel's generated assets and metadata extras, adds the sword assets, uses the sword pack's Minecraft 1.21.11 format declaration, and writes a SHA-1 file. It stops if two different assets use the same path, so it cannot silently replace a conflicting model or shader. It cannot generate missing BetterModel assets: `build.zip` must already contain compatible generated geometry and IDs.

To merge manually:

1. Extract the **fresh BetterModel ZIP** into an empty folder.
2. Copy the new sword pack's `assets/legendaryswords` contents into the matching namespace. Merge subfolders without deleting BetterModel's files.
3. Copy any other sword assets, including its optional emissive configuration, without replacing different files at the same path.
4. Use the sword pack's `pack` version declaration in `pack.mcmeta` while preserving BetterModel's other top-level metadata, such as overlays.
5. ZIP the folder's **contents**, so `pack.mcmeta` and `assets` are at the ZIP root.
6. Compute the final merged ZIP's SHA-1.

Install the combined ZIP locally in **Options → Resource Packs** or host it and configure your server's one pack URL. Use its hash, disable obsolete separate pack offers, and reconnect. When generated BetterModel assets change, regenerate its pack before merging. When sword-pack assets change, repeat the merge using the new sword pack and update the hash even if BetterModel's generated files stay the same.

## Check missing animations

- Run `/ls status`. With the 0.2.3 default, a body-animation pause is expected. Confirm BetterModel remains available for slash models; ultimate presentation remains available.
- Check the server's console after `/bm reload`. A failed import cannot be fixed by uploading a pack made before the failure.
- Confirm all sixteen files exist and the player/model modules are enabled.
- If an old edited file was preserved, back it up and manually install the new bundled file, or add all seven expected animation names to your custom file.
- Confirm Minecraft accepted the **0.2.2 combined pack**. No custom player body movement or pose is expected while `player-body-animations` is false; this is the temporary hotfix behavior.
- Hold every sword, walk, sprint, swing, cast abilities, and use ultimates. Check the selected hotbar icon, inventory icon, first-person sword, and normal third-person sword stay visible, with no decoder disconnect.
- For an explicitly enabled controlled body-animation test, inspect carry/action transitions and the animated grip using [the real-server checklist](TESTING.md#controlled-player-body-animation-test). A standalone sword pack lacks BetterModel's generated player/shader assets.
- Check another plugin is not continually replacing the player's BetterModel animation/model.

The vanilla first-person arm swing stays unchanged. The in-place ultimate display does not switch game mode or move the player. Full-bright displays and energy textures add visibility; emitted world light and shader bloom depend on client support.

Complete [the real-server checklist](TESTING.md) before public deployment. No live-client validation is implied by compiling these assets.
