# Validate on a real server before public play

Automated Java tests exercise mechanics, protection/event handling, and animation/effect lifecycle with mocked Bukkit objects. Resource-pack checks validate file structure and references. They do not start Paper or render a Minecraft client. Run the following checks on **Paper 1.21.11, Java 25 with BetterModel 3.1, and a real 1.21.11 client**. Core combat without BetterModel can also be checked on Java 21. Record the server and plugin versions and the final pack SHA-1 with the results.

The 0.2.3 hotfix responds to a reported `clientbound/minecraft:set_equipment` decoder disconnect with 557 unread bytes on BetterModel `3.1-SNAPSHOT-526`. The exact packet cause has not been proven. **No live-client test has been performed.** Validate the default body-animation pause first; only re-enable body animations in a controlled test after collecting evidence for the reported failure.

## Equipment-packet hotfix and default behavior

- Install `LegendarySwords-0.2.3.jar` while the server is stopped, keep only one plugin JAR, and restart.
- Test a fresh configuration and an existing configuration without `bettermodel.player-body-animations`. Both should use false. If a configuration explicitly has true, set it to false before restarting.
- Keep `bettermodel.enabled: true`. `/ls status` should report that player body animations are paused while BetterModel remains available for slash models; ultimate presentation remains available.
- With two connected clients, give all eight swords and repeatedly switch hotbar slots, swing, walk, sprint, change to an empty slot, swap hands, move items in inventory, and drop/pick up swords. Check for equipment-packet decoder errors and disconnects on both clients, including the observer.
- Verify hotbar/inventory icons, first-person swords, and ordinary third-person swords remain visible. Custom standing, walking, running, pose, and player action body animations should remain paused. `/ls pose` should explain that body animation is unavailable.
- Cast every ability and ultimate with `/ls charge 100` and `/ls resetcooldowns` between attempts. Sword textures, particles/displays, BetterModel slash effects, and ultimate presentation should remain available. Neither a player-attached body tracker nor its animated held-item selector should activate in this default mode.
- Test disconnect/reconnect, death/respawn, world change, `/ls reload`, `/bm reload`, and a full restart while swords are held. Check cleanup, ordinary item visibility, and the continued body-animation pause.
- Record the duration, number of players, number of attacks, exact BetterModel build, and both client/server logs. Passing automated tests alone cannot establish that the reported disconnect is resolved.

## Upgrade and assets

- Stop the server and replace the old JAR with `LegendarySwords-0.2.3.jar`. Confirm only one LegendarySwords JAR exists.
- Preserve an old configuration and check that the original swords retain intended values and the five new swords receive distinct defaults. Also test a fresh configuration.
- Test with BetterModel installed and absent. Combat and particle/display effects should work either way; `/ls status` should explain unavailable animation support.
- Confirm all eight item models load with the 0.2.2 sword pack. Check first-person, third-person, inventory, dropped-item, and item-frame positions, textures, and texture animation.
- Test an upgrade from the six original 0.1 model files. Known originals should be backed up and updated; deliberately edited files should be preserved and reported.
- Upgrade original 0.2.0 and 0.2.1 player files to the unchanged bundled 0.2.2 clips. Confirm they update automatically with backups after restart. If edited files are preserved, back them up before manual replacement from the sixteen-model ZIP.
- Run `/bm reload`. Confirm all sixteen model imports succeed. Regenerate and merge `build.zip` only if other BetterModel models/configuration changed; the 0.2.3 hotfix itself needs no new generated client assets.
- Keep `LegendarySwords-combined-resource-pack-0.2.2.zip` when upgrading from 0.2.2. Replace packs from before 0.2.2 with that ZIP. Verify all eight swords, slash effects, and ultimate displays render while player body animations remain paused.
- Test pack decline/download failure. Gameplay should continue; ordinary item appearances are expected without the pack.

## Eight distinct attacks

Use `/ls give all`, `/ls charge 100`, and `/ls resetcooldowns` as an operator. Try every sword against separate groups of mobs in an open test area.

| Sword | Ability to inspect | Ultimate to inspect |
| --- | --- | --- |
| Master Sword | Sustained luminous laser | Sacred beam attack |
| Flameblade | Clearly visible ring of fire | Expanding fire columns/vortex |
| Fierce Deity Sword | Traveling lunar crescent | Falling lunar meteors |
| Frostblade | Rising ice-spike line | Swirling blizzard |
| Thunderblade | Chained lightning segments | Overhead storm strikes |
| Royal Claymore | Ground fissure | Large blade slam |
| Eightfold Blade | Traveling wind ribbons | Multiple angled cuts |
| Biggoron Sword | Large spinning blade | Radial earth eruption |

- Verify the effects are visible from first person, F5, and another player's view. Test different particle settings and daylight/night. Confirm displays disappear when each effect expires.
- Ensure repeated clicks obey cooldowns, switching swords cannot bypass their own cooldowns, and another player has independent cooldowns.
- Check walls and solid blocks stop directed attacks as intended. A target outside range or behind an obstructing wall must not take unintended hits.
- Verify effects do not permanently change terrain or leave fire blocks behind.
- Watch server performance while several players use sustained effects together.

## Charge, ultimate reliability, and commands

- Normal successful melee grants 10 charge; a melee kill adds 20, with a 100 maximum. Cancelled hits and ability damage must not generate charge.
- At 99 charge, sneak + right-click rejects the ultimate without spending charge. At 100, it activates once, spends charge once, and starts its cooldown.
- Test ultimates with cosmetic cinematics enabled and disabled. Effects/damage must start independently of the presentation; the player must not teleport or change game mode.
- Walk and turn during the in-place presentation. Confirm ordinary control remains and the effect's targets/anchor behave as intended.
- Disconnect/reconnect during a cooldown and confirm it remains active. `/ls resetcooldowns` should explicitly clear it for testing.
- Test `/ls give`, `/ls charge`, and `/ls resetcooldowns` from the console with a named player. Invalid IDs, invalid charge values, and offline players should get useful messages.
- Non-operators must be denied admin commands. Denying `legendaryswords.use` should block abilities and poses.

## Controlled player body animation test

These checks apply only after gathering equipment-packet evidence and deliberately setting `bettermodel.player-body-animations: true` on a test server, then restarting. The setting is false for normal 0.2.3 use. Keep the 0.2.2 combined pack; the retained clips and geometry are unchanged. Use two players: one holds a sword, and one watches. Test every sword in F5 and from the observer's view. Capture logs from both clients and the server; if the decoder failure returns, set the option to false and restart.

- Standing, walking, and sprinting should select distinct `idle`, `walk`, and `run` clips belonging to that sword. Confirm the player model follows the player rather than remaining at an old location.
- In F5, walk forward on level ground: both legs should cycle continuously, knees should bend backward during recovery, and the pelvis should stay connected. Stop, sprint, and switch to an empty slot; check that the correct motion resumes or the vanilla body returns.
- Inspect all eight swords' full `idle`, `walk`, `run`, and `pose` cycles from front and side views. Arms and grips should stay outside the torso; blades should avoid the torso and head. Confirm Royal Claymore and Biggoron Sword use their forward braces. Include animation loop boundaries and transitions when checking clearance.
- Inspect the retained 0.2.2 `swing`, `ability`, and `ultimate` clips for Master Sword, Fierce Deity Sword, Thunderblade, Eightfold Blade, and Biggoron Sword. Check each clip's entry/exit and interruption transitions for remaining clipping; automated samples cannot establish every live-client transition.
- With a tracker active, verify the selected hotbar/inventory icon and first-person sword stay visible, while only the animated grip appears in third person. Switch slots, swap to the offhand, move/drop the item, change worlds, die, reconnect, reload, and disable/restart the plugin. Check that the base item model is restored and no stale `_animated` item becomes invisible or renders twice.
- Run `/ls pose`, then swing, cast an ability, and use an ultimate. One-shot clips should play and return to movement rather than being immediately overwritten.
- Switch to another sword, empty hand, and ordinary tools. The previous sword's body animation must clear without affecting other plugins' models.
- Test walking backwards, strafing, sneaking, jumping, swimming, mounting, and different skins. Check the body orientation and held sword attachment.
- Run `/bm reload` while a sword is held. Confirm animation support recovers after the reload.
- Repeat interruption checks during movement, one-shot animation, ability, and ultimate: disconnect, die, change world, external teleport, plugin disable, server restart. Temporary displays and this plugin's trackers must clean up; legitimate game mode, teleport destination, and respawn location must remain.
- Test a protection plugin cancelling a cosmetic entity spawn. Combat should still cast; failed presentation must not leave the player frozen, moved, or in another mode.

After the controlled test, restore `bettermodel.player-body-animations: false` and restart. Report body-animation runtime validation separately from validation of the default mitigation.

The vanilla first-person arm animation is expected to remain standard. Emissive texture overlays and dynamic world lighting depend on supported clients; assess those separately from ordinary particles and display effects.

## Multiplayer, protection, and configuration

- With PvP disabled, verify direct, chained, and area attacks do not damage players.
- In a separate PvP test world, verify world PvP settings, friendly fire, spectator/creative exclusions, protected regions, invulnerability, and cancelled damage events remain respected.
- Confirm owner pets and armor stands are excluded as intended. Decide how passive animals and other protected entities should be treated for your server.
- Let damage through while cancelling entity ignition. Flame attacks must respect the cancelled combustion event. Modified burn duration should be honored without shortening an existing longer burn.
- Verify cancelled damage does not apply associated knockback/fire or create recursive melee charge.
- Edit damage/cooldown/PvP settings and run `/ls reload`. Give fresh swords after changing base attack attributes.
- Replace the hosted final ZIP and update its SHA-1. Reconnect with a clean client cache and verify the updated pack is accepted.
- Finish with a full restart and a non-operator player receiving all eight swords from an operator and trying every ability and ultimate. With the default false body-animation setting, a pose should remain unavailable.

Report any failed step with the exact sword ID, `/ls status` output, the body-animation setting, server console exception, full client decoder error if present, BetterModel version, and a short recording. Claim runtime validation only after it was actually performed.
