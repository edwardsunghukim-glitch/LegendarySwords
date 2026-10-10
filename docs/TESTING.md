# Real-server validation

Automated tests exercise mechanics and mocked lifecycle behavior; they do not start Paper or render a Minecraft client. Validate Paper 1.21.11, Java 25, BetterModel 3.1 and a real 1.21.11 client. Record the exact BetterModel build, client, plugins, pack hash and test duration. The earlier equipment-packet disconnect still needs a live check.

## Cast-only animations

- Install 0.2.5 while the server is stopped and keep only one JAR. Reuse the 0.2.2 combined pack.
- Test fresh and existing configurations. Missing `abilities-only` and `cast-body-animations` keys should use true, even when the old `player-body-animations` key is false.
- Stand, walk, sprint, jump, fly and swing with each sword before casting: ordinary player movement should remain and no permanent body tracker should appear. `/ls pose` should explain cast-only mode.
- Have an observer watch while you right-click each sword, then walk during its cast. Only the ability clip should play; no idle/walk/run clip should start. The normal player body and base held-item model should return at the clip end.
- Inspect the selected hotbar slot, inventory icon, first-person sword and observer's third-person sword during and after casts. Items must retain amount, durability, enchantments, PDC and normal models after cleanup.
- Try swap, drop/pickup, slot change, inventory transfer, death, quit, world change, teleport, reload and restart during casts. Check restoration and no remaining displays/disguises.
- Repeat with `cast-body-animations: false`. Attack effects and shared scenes should remain, without player body animation. Player rigs and the player-animation module are unnecessary for this fallback.
- Repeatedly cast with an observer and record both client/server logs. If an equipment decoder error recurs, use the false fallback before more tests. Automated checks cannot prove the disconnect resolved.

## Ultimate scenes and phase timing

- Enable server cinematics and use `/ls cinematics on`; repeat it to confirm it does not toggle off. Inspect `/ls cinematics status`, then confirm the preference survives reconnect/restart. Check global config false produces a useful command message.
- With another player nearby, use `/ls charge 100`, `/ls resetcooldowns`, then sneak + right-click each sword. During the charge/reveal, no ultimate damage should occur. Both players should see the world blade scene and title if visible and within 40 blocks. Confirm the sword appears ahead of the first-person camera.
- Observe sword-specific reveal motions and colored wind-up effects. The scene must last at least the ultimate clip; its last pose holds until completion. At scene end the body is restored and the attack/damage releases once.
- Confirm the original position and game mode are preserved; this version does not force spectator cameras or teleport participants. Inspect caster first-person presentation and observer body poses separately.
- Simulate a missing/interrupted scene or callback. The timeout should release one attack rather than discard it or damage twice. A late completion callback after cancellation/shutdown must not spawn attacks.
- Death, world change or teleport during wind-up should cancel pending damage and scene cleanup. Disabling presentation should still allow the attack. Shutdown/reload should not schedule damage while closing.
- Test hidden/vanished players and out-of-range observers: shared title selection must honor `canSee` and range. World effects still follow server tracking/visibility rules.

## Eight attacks

| Sword | Ability | Ultimate |
| --- | --- | --- |
| Master Sword | Sustained laser | Sacred beam |
| Flameblade | Ring of fire | Inferno vortex |
| Fierce Deity Sword | Lunar crescent | Falling moon |
| Frostblade | Ice spikes | Blizzard |
| Thunderblade | Chain lightning | Thunderstorm |
| Royal Claymore | Ground fissure | Giant blade slam |
| Eightfold Blade | Wind ribbons | Wind cuts |
| Biggoron Sword | Spinning blade | Earth eruption |

Test mob groups and authorized player targets; PvP remains disabled by default. Check region/protection cancellations, walls/line of sight, damage caps, cooldowns and charge consumption. Ability damage must not generate charge. No terrain edits should remain. Test successful attacks, rejected casts and missing BetterModel/pack fallback independently.

## Assets and build

No `.bbmodel` or resource-pack assets change in 0.2.5. The earlier 0.2.2 asset checks covered 13 pack tests and 9 BetterModel asset tests, including 47 pose/action clearance combinations. Release checks should compare embedded pack/model bytes with that release and validate ZIP CRCs. The current Java suite must complete with no failed/error/skipped tests. These checks remain distinct from real-client rendering and packet validation.

In 0.2.5, cast-body viewers exclude the caster. Observers see the animated body with vanilla equipment hidden; the caster sees their normal character and inventory, plus all world effects. Ultimate windup defaults to `cinematics.windup-ticks: 24` (1.2 seconds), regardless of the old `duration-ticks` value. Ability damage uses Paper armor, toughness, protection and resistance rules.
