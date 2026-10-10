# BetterModel animations and one combined pack

The integration uses **BetterModel 3.1's API** on Paper 1.21.11. **BetterModel 3.1 requires Java 25 on the server.** The server reported `3.1-SNAPSHOT-526`; compilation against the 3.1 API does not prove that snapshot's runtime behavior. `/ls status` reports the installed engine, missing models, and missing animation clips. The earlier 0.1 integration used the older BetterModel 1.15 API and must be replaced with this plugin JAR.

There are **sixteen `.bbmodel` files**: eight player-animation models and eight slash-effect models. Each player model has its own `idle`, `walk`, `run`, `pose`, `swing`, `ability`, and `ultimate` animations. Hold a sword to use its idle/walk/sprint clips; run `/ls pose` for its pose. Switching away from the sword clears the movement animation. Use F5 or another player's view to see body movement.

## Install or upgrade the models

1. Install the new LegendarySwords JAR and restart the server with BetterModel installed.
2. The plugin installs missing bundled models automatically. On upgrade it recognizes its original old files, backs them up, and updates them. Files you edited yourself are preserved and reported rather than silently replaced.
3. For manual installation, extract **`LegendarySwords-BetterModel-models.zip`**. Upload the files inside its `players` folder to **`plugins/BetterModel/players`** and those inside `models` to **`plugins/BetterModel/models`**. Back up your own edits before deliberately replacing a file.
4. Enable `module.player-animation` and `module.model` in BetterModel's configuration.
5. Run **`/bm reload`** to import the models and regenerate BetterModel's resource pack. A full restart may help if either plugin was installed or upgraded while the server was running.
6. Download the **new** `plugins/BetterModel/build.zip` from the server file manager. This path is on your server, not in the release folder on your computer. Different BetterModel output settings may use a different path.

The exact filenames follow these patterns for every sword ID:

```text
plugins/BetterModel/players/legendaryswords_<sword_id>.bbmodel
plugins/BetterModel/models/legendaryswords_<sword_id>_slash.bbmodel
```

Sword IDs: `master_sword`, `flameblade`, `fierce_deity_sword`, `frostblade`, `thunderblade`, `royal_claymore`, `eightfold_blade`, and `biggoron_sword`. Keep the filenames and animation clip names unchanged.

`bettermodel.enabled` controls integration. `bettermodel.install-bundled-models` controls asset installation. Both default to true. If that installation option is off, update the model files yourself before regenerating the pack.

## Combine the packs

If your server accepts one pack URL, merge the newly generated BetterModel pack with the **0.2.0 sword pack**. Your old `build.zip` or old combined pack does not contain the updated sixteen models.

With Python 3 installed, extract `LegendarySwords-source.zip`, put both ZIPs in the project folder, and run from that folder:

```sh
python3 scripts/merge_resource_packs.py build.zip LegendarySwords-resource-pack.zip LegendarySwords-combined-resource-pack.zip
```

On Windows, `python` may be the command instead of `python3`. If the downloaded BetterModel filename is `build (12).zip`, either rename it to `build.zip` or put the filename in quotes in the command.

The script keeps BetterModel's generated assets and metadata extras, adds the sword assets, uses the sword pack's Minecraft 1.21.11 format declaration, and writes a SHA-1 file. It stops if two different assets use the same path, so it cannot silently replace a conflicting model or shader. It cannot generate missing BetterModel assets: `build.zip` must already come from the updated models.

To merge manually:

1. Extract the **fresh BetterModel ZIP** into an empty folder.
2. Copy the new sword pack's `assets/legendaryswords` contents into the matching namespace. Merge subfolders without deleting BetterModel's files.
3. Copy any other sword assets, including its optional emissive configuration, without replacing different files at the same path.
4. Use the sword pack's `pack` version declaration in `pack.mcmeta` while preserving BetterModel's other top-level metadata, such as overlays.
5. ZIP the folder's **contents**, so `pack.mcmeta` and `assets` are at the ZIP root.
6. Compute the final merged ZIP's SHA-1.

Install the combined ZIP locally in **Options → Resource Packs** or host it and configure your server's one pack URL. Use its hash, disable obsolete separate pack offers, and reconnect. When models change, regenerate BetterModel's pack, repeat the merge, and update the hash.

## Check missing animations

- Run `/ls status`. Confirm the reported BetterModel version and all eight sword models/clips.
- Check the server's console after `/bm reload`. A failed import cannot be fixed by uploading a pack made before the failure.
- Confirm all sixteen files exist and the player/model modules are enabled.
- If an old edited file was preserved, back it up and manually install the new bundled file, or add all seven expected animation names to your custom file.
- Confirm Minecraft accepted the **new** combined pack. If the swords appear but body animations do not, the standalone sword pack may have been installed without BetterModel's generated assets.
- Hold a sword, use F5, walk, sprint, run `/ls pose`, swing, and use its ability. Ask a second player to watch as well.
- Check another plugin is not continually replacing the player's BetterModel animation/model.

The vanilla first-person arm swing stays unchanged. The in-place ultimate display does not switch game mode or move the player. Full-bright displays and energy textures add visibility; emitted world light and shader bloom depend on client support.

Complete [the real-server checklist](TESTING.md) before public deployment. No live-client validation is implied by compiling these assets.
