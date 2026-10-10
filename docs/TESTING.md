# Validation before using the plugin publicly

The Maven build tests pure charge and cooldown rules, sword command IDs, beam/cone collision geometry, and combat and camera lifecycle behavior using mocked Bukkit objects. Combat tests check cancelled damage and ignition, adjusted burn duration, knockback, charge recursion, owned pet protection, PvP settings, friendly fire, and melee kill attribution. Camera tests check ordinary cleanup, death, world changes, external mode changes and teleports, exception cleanup, and saved spectator-state recovery. Those checks do not start a Minecraft server or render an actual camera. Complete the checks below on a separate Paper 1.21.11 test server with a real 1.21.11 client before installing on your public server.

## Startup and assets

- Start Paper using Java 21. Check that LegendarySwords enables without exceptions and `/ls status` reports its configuration.
- Test with BetterModel installed and with it absent. Core swords and abilities should work in both cases; animation status should explain when BetterModel is unavailable.
- Install the generated pack and accept it. Use `/ls give all`; verify that all three swords have the intended distinct models, correct first-person and third-person placement, and animated textures where supplied.
- Test pack decline and download failure. The server should remain playable. Ordinary item appearances are expected without the pack.
- With BetterModel installed, install its animation assets and generated pack. Have a second player watch each ability and ultimate to verify the third-person animations.
- Test the plugin pack alongside BetterModel's pack. Check that both model sets remain visible after requesting `/ls pack` and after reconnecting.

## Abilities and charge

- As an operator, run `/ls give all` and `/ls charge 100`. Repeat from the console using an online player's name. Invalid sword IDs, offline players, and invalid charge values should produce a useful message.
- As a non-operator, verify admin commands are denied. Verify abilities work with `legendaryswords.use`, then deny that permission and check that abilities are blocked.
- Right-click each sword. Confirm its expected effect and that rapid repeated clicks cannot bypass the cooldown. Test a different sword and a different player to confirm cooldowns are independent.
- Earn charge through normal melee: successful sword hits add 10 and melee kills add a bonus 20, with a maximum of 100. Check hits cancelled by a protection plugin do not award charge.
- At 99 charge, sneak and right-click. It should leave charge unchanged and reject the ultimate. At 100, it should start once and consume the meter once.
- After the optional camera, verify four ultimate damage waves occur 22 ticks apart, with radii of 2, 4, 6, and 8 blocks. The camera should show the awakening sword display and, when BetterModel assets are available, the animated hero at the original casting position.
- Verify ability-generated damage does not recursively grant normal melee charge or create unlimited effects.
- Disconnect and reconnect during a cooldown. The reconnect must not make that ability immediately reusable.
- Check beams and cone attacks beside walls, beyond their documented range, and just outside their area. Ensure effects do not damage entities behind walls or outside the intended area.

## Multiplayer and protections

- Leave PvP disabled and verify abilities do not hurt another player. Use two players and check direct, area, and ultimate effects.
- On a separate test world, enable PvP and verify that protected regions, invulnerable players, and cancelled damage events remain respected.
- Test a region or protection that allows damage but cancels entity ignition. Flameblade damage can land there, but it must not ignite the target. If a plugin changes the ignition duration, the accepted duration should apply without shortening a longer existing burn.
- Confirm passive animals, pets, allies, and armor stands behave as intended for your server. Adjust permissions and configuration before public play.
- Watch server performance while several players use particles and ultimates simultaneously. Keep cooldowns and effect sizes appropriate for your player count.

## Cinematic camera recovery

Use two players: one activates the ultimate, and the other watches. On normal completion, confirm the camera restores the activating player's location, game mode, flight state, and ordinary control after its short animation. Confirm the temporary camera, sword display, and BetterModel hero disappear. Cleanup must still remove those temporary entities if another restoration step fails.

Repeat recovery checks with each of these interruptions:

- `/ls cinematics` is disabled before activation.
- The player disconnects during the camera sequence, then reconnects.
- The player changes worlds or another plugin teleports them. Keep that legitimate destination; ending the cinematic must not teleport them back to the old casting position.
- Another plugin changes the player's game mode during the cinematic. Keep that legitimate new mode and location; remove the cinematic entities without reinstating an old mode.
- The player dies during the cinematic. Remove temporary entities without teleporting the dead player or changing their respawn destination.
- The plugin is disabled or the server stops during the sequence, then the server starts again.
- The player starts in survival, creative, or spectator mode.
- A second ultimate activation is attempted before the first sequence has finished.

Verify a player cannot escape a blocked region, bypass a protection rule, become permanently invulnerable, or keep spectator/flight privileges after the cinematic. If any recovery check fails, leave cinematic cameras disabled until it is resolved.

## Configuration and deployment

- Edit generated configuration and run `/ls reload`. Confirm new cooldown, damage, PvP, and pack settings take effect.
- After changing base attack damage or attack speed, give a fresh sword and compare it with an old one. Item attributes are set when the sword is created.
- Try malformed values on the test server and inspect logs and `/ls status` for clear validation behavior.
- Replace the resource ZIP and update its SHA-1. Reconnect with a clean client cache and verify that the new pack is accepted.
- Perform one final full restart. Join as a normal player, receive a sword from an operator, and use every ability and ultimate with the deployed plugins and pack.

Record the server build, BetterModel version, pack SHA-1, and results. Only claim runtime validation after these checks have actually been performed.
