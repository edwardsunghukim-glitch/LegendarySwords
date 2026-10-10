# BetterModel animations

The optional integration targets **BetterModel 1.15.2**. Use a build compatible with your Paper 1.21.11 server. Later major BetterModel versions have a different API and are not claimed compatible with this release.

The supplied assets include original swing, ability, and ultimate player animations for all three swords, plus animated slash crescents. They are a starting set to test and customize in Blockbench.

## Set up the assets

1. Install BetterModel and confirm it starts normally.
2. Install LegendarySwords and restart the server. By default it copies the bundled assets into BetterModel's folders when the destination files do not already exist:

   ```text
   plugins/BetterModel/players/legendaryswords_master_sword.bbmodel
   plugins/BetterModel/players/legendaryswords_flameblade.bbmodel
   plugins/BetterModel/players/legendaryswords_fierce_deity_sword.bbmodel
   plugins/BetterModel/models/legendaryswords_master_sword_slash.bbmodel
   plugins/BetterModel/models/legendaryswords_flameblade_slash.bbmodel
   plugins/BetterModel/models/legendaryswords_fierce_deity_sword_slash.bbmodel
   ```

3. In BetterModel's configuration, enable `module.player-animation` for player limb animations and `module.model` for slash crescents.
4. Run `/bm reload` (also available as `/bettermodel reload`) to import the models and regenerate BetterModel's resource pack. In BetterModel 1.15.2's default ZIP configuration, the generated file is `plugins/BetterModel/build.zip`. If the models were just installed, a full server restart lets both plugins import them together.
5. Distribute both BetterModel's generated pack and the LegendarySwords sword assets. If BetterModel already sends its own pack, LegendarySwords adds its sword pack to the client's pack stack. You can also combine the packs and host one final ZIP. Follow [the resource pack guide](../resource-pack/README.md).
6. Run `/ls status`, give yourself a sword, and ask a second player to watch your swing, ability, and ultimate. Test in third-person too.

Existing BetterModel source files are preserved. If you edit these `.bbmodel` files, a LegendarySwords restart will not replace your work. The plugin's `bettermodel.enabled` option controls integration; `bettermodel.install-bundled-models` controls automatic asset copying. Both default to true.

## What players will see

The body animations move third-person player limbs and render the sword held in the player's hand. During an ultimate camera, an animated player-skin model stays at the casting position so the scene can show the hero while the camera moves. Short-lived slash models accompany abilities and ultimates. The slash models use full model brightness, while sword glint, animated textures, and particles add visual glow.

The standalone sword resource pack contains sword assets. BetterModel's generated pack contains the player skin, shader, and generated model assets that its animation rendering needs. Installing only the sword pack will not provide the complete BetterModel presentation. Players need both packs accepted, or a merged pack containing both sets of assets.

These animations do not replace the vanilla first-person arm swing. Ultimate cameras temporarily show the scene from outside the player. World lighting and emissive shader effects depend on the client's shader or mod support.

## Customize and check

Open the copied `.bbmodel` files in Blockbench to edit animations or the slash models. Keep the player limb structure and the `swing`, `ability`, and `ultimate` animation names used by the plugin. Back up your work, regenerate BetterModel's pack, then update the hosted final ZIP and SHA-1.

If an animation is missing, inspect `/ls status` and BetterModel's startup/reload logs, confirm the files above exist, confirm the required modules are enabled, and verify the client accepted the combined resource pack. Complete [the multiplayer and camera recovery checks](TESTING.md) before using the animations publicly.
