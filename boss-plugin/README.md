# LegendaryBosses 1.0.1

Paper 1.21.11, Java 21+, BetterModel 3.1, LegendaryArmor 1.2.0+.

Install LegendaryBosses-1.0.1.jar in plugins and restart. Use the existing
LegendaryArmor/Swords combined resource pack. This plugin does not replace
LegendarySwords or LegendaryArmor.

The five matching models must be loaded by BetterModel. If not already installed,
copy the five bundled .bbmodel files to plugins/BetterModel/models, run /bm reload,
and use the resulting combined resource pack. The bundled models are the existing
LegendarySwords models, not a new artwork revision. Do not install build.zip as a plugin.

Admin commands (permission legendarybosses.admin, operators by default):
- /boss summon angel [player]
- /boss summon dragon [player]
- /boss summon leviathan [player]
- /boss summon stone [player]
- /boss summon frost [player]
- /boss list
- /boss remove <UUID|all>
- /boss reload

Tab completes subcommands, all five types, player names, and active boss IDs.
Console summon requires the player argument. Bosses appear five blocks in front
of that player. Summon in an open area on a non-Peaceful world with mob spawning
permitted by your region settings.

Each boss has 100 hearts (200 health), a nearby health bar and four distinct attacks.
Angel: radiant cleave, judgment circle, falling stars, sanctuary pulse.
Dragon: fire breath, claw sweep, meteors, expanding fire ring.
Leviathan: tidal wave, pulling whirlpool, sky bite, stacked storm domain.
Stone: ground spikes, boulders, fist strike, quake domain.
Frost: icy crescent, ice prison, hailstorm, glacial domain.

Angel, Dragon and Leviathan fly and pursue players in Survival/Adventure; the two
titans walk. Attacks have a visible warning and use native entity damage so armor,
enchantments and cancelled damage events apply. Effects do not change terrain.
The shared boss scenes stop on death/removal. Existing model idle/smash/breath
animations are used; these are not twenty newly authored model animation clips.
Large models use an enlarged central vanilla mob hitbox, not individual wing/tail
hitboxes. Look and movement need an in-game check with your BetterModel snapshot.

One random boots/leggings/chestplate/helmet piece from the corresponding set drops
on player kill, with actual LegendaryArmor metadata, textures and full-set powers.
No ordinary zombie drops or XP. By default environmental/admin kills do not reward.
Removal, plugin shutdown, unloaded chunks and the default 30-minute lifetime also
remove encounters without rewards. Active encounters are not persisted.

Edit plugins/LegendaryBosses/config.yml to change hearts, attack damage (health
points), cooldown seconds, flight height, pursuit range, model size multiplier,
active encounter cap and lifetime. Run /boss reload; summon new bosses to apply
changed health, size or flying settings. The model geometry is already enlarged;
scale 1.5 means another 50% larger. Default: at most five encounters.

Validation: Maven verify runs health/reward/config/autocomplete and matching armor
metadata checks. Live Paper/BetterModel rendering and flight are not tested here.

1.0.1 fix: boss abilities cannot self-hit or damage the casting boss through
reflected Thorns. Dragon bosses are immune to fire/lava damage. Player attacks
still work. Incoming damage defaults to 35% (100 hearts remain unchanged); edit
incoming-damage-multiplier and use /boss reload to adjust existing encounters.
Existing configs without the new option also default to 0.35.

Boss loot now has the same colored name and full passive/ability lore as /armor.
Previously dropped trophy items keep their old display text; their powers are the same.
