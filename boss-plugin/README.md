# LegendaryBosses 1.0.5

Requires Paper 1.21.11, Java 21+, BetterModel 3.1 and LegendaryArmor 1.2.0+.

Replace the old LegendaryBosses JAR in plugins with LegendaryBosses-1.0.5.jar;
restart and summon fresh bosses. Keep the existing combined resource pack and
boss models. Bundled BetterModel-models are the existing five model files;
install them in plugins/BetterModel/models only if not already loaded, then
run /bm reload and install the resulting combined pack.

## Commands

Admin permission: legendarybosses.admin (operators by default).

- /boss summon <angel|dragon|leviathan|stone|frost> [player]
- /boss list
- /boss remove <UUID|all>
- /boss reload

Tab completes commands, five boss types, players and active encounter IDs.
Console summon requires a player. Bosses spawn five blocks in front of them.
Use an open area outside spawn, in a non-Peaceful world allowing mob spawning.

## Encounters

Each boss has 100 hearts, a nearby boss bar, a colored entity name for vanilla
death messages and TEN moves. Angel, Dragon and Leviathan fly; titans walk.
Health, size, attack damage/cooldowns, pursuit range and flight height are configurable.
Ten attacks cycle in order. Each attack has a visible warning and uses native
entity damage, so player armor and enchantments apply.

Angel: Radiant Cleave, Judgment Circle, Falling Stars, Sanctuary Pulse, Halo Cage,
Seraph Cross, Feather Rain, Celestial Spiral, Golden Wings, Dawn Nova.

Dragon: Wyrmfire Breath, Claw Sweep, Meteor Storm, Inferno Ring, Ember Vortex,
Dragon Fang, Cinder Minefield, Volcanic Pillars, Wingfire Sweep, Apocalypse Bloom.

Leviathan: Tidal Surge, Whirlpool, Sky Bite, Storm Domain, Abyssal Coil,
Tsunami Wall, Maelstrom Orbs, Trident Current, Thunderfin Spiral, Ocean Crown.

Stone: Earthshatter, Boulder Barrage, Titan Fist, Quake Domain, Stone Cage,
Fault Line, Boulder Orbit, Obsidian Teeth, Titan Footfall, Mountain Collapse.

Frost: Frozen Crescent, Ice Prison, Hailstorm, Glacial Domain, Crystal Cage,
Frost Spear, Blizzard Orbs, Frozen Star, Avalanche Fan, Winter Crown.

Added attack scenes use themed particles and different arrangements/motion, not
newly authored BetterModel attack meshes. Existing idle, smash and dragon breath
clips accompany them. Native BetterModel hitboxes are created for model bones with bounding geometry,
including body/head, wings/arms and tail/spine parts. These follow animations
and forward hits to the same source mob health/damage events. The base zombie
now has normal scale instead of an oversized central hitbox. Set model-hitboxes
to false to disable model hitboxes for troubleshooting. Rectangular bone bounds
can include some empty space, especially around wings; this is not pixel collision.

## Spawn and damage protection

By default players in WorldGuard region `spawn` cannot be targeted, damaged,
slowed or pulled by boss abilities. Native boss melee/projectile damage is also
blocked there. Previously applied boss burns are cleared upon entering spawn.
WorldGuard cuboid/polygon geometry and region height are respected. Configure
spawn-protection.regions for other region names; failures reading WorldGuard
pause boss targeting rather than bypassing protection.

A 16-block horizontal radius around each world's native spawn is also protected.
Set spawn-protection.world-spawn-radius to 0 to rely only on configured regions.
Protection applies to admins too. Existing configs gain default protection keys.
No terrain blocks are changed by effects.

Bosses now accept player melee, arrows and attributed player ability damage only.
Environmental damage, mob hits, own projectiles and all Thorns are blocked.
Synchronous retaliation while a boss ability damages a player is blocked too.
Incoming damage defaults to 35%, with a 10-health-point cap per hit after that
multiplier, before native armor (at most 5 hearts before armor). Player attacks
outside the retaliation call still work normally. External plugins that directly
change entity health without damage events cannot be intercepted here.

Attacks are stronger by default: outgoing-damage-multiplier is 1.5, on top of each
boss's damage and the move's multiplier. This also works with existing configs
that lack the new key. Edit these settings and use /boss reload to adjust live
encounters; changed health, size and flight mode apply to new summons.

## Loot and cleanup

Player kills drop exactly one random matching boots, leggings, chestplate or
helmet. Real LegendaryArmor set metadata, equip models, name colors and lore
match /armor. Old trophy items retain their text and work the same way.
No ordinary zombie drops/XP. Environmental/admin removal never rewards.
Encounters are removed without rewards on unload, shutdown and expiration
(default 30 minutes). Active encounters are not persisted. Default maximum: five.

## Validation

Maven verify checks all 50 distinct move names, health/rewards, armor metadata,
autocomplete, direct mob damage caps, player damage acceptance, self/reflection
immunity, spawn damage/burn blocking and world-spawn fallback. Paper/BetterModel
rendering, flight, WorldGuard integration and all attack visuals still need an
in-game check on the actual server.

1.0.5 hitbox correction: each encounter uses a private copy of the renderer
with full body-part bounds, computed from all eight rotated/inflated corners
of every visible cube in that bone. The stock largest-single-cube bounds were
often thin scales or plates and missed most of the model. Native bone hitboxes
now use these complete bounds. Original shared renderers, models, animations
and resource-pack item references are preserved. No new model files or pack
are required. Rectangular bounds remain approximate around curved limbs/wings.
Restart and summon new bosses to apply this fix; after /bm reload, summon new
encounters to reapply the private bounds. hitbox-padding defaults to 0.08
model-local blocks (scaled with the model) and can be set from 0 to 0.5.

All newly dropped armor has Protection IV; boots also have Feather Falling IV.
Previously obtained pieces retain their enchantments. Get new armor to apply these.
