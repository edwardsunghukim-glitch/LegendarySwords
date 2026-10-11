# LegendaryArmor 1.2.0

Paper 1.21.11 / Java 21+. Five original armor sets inspired by the matching LegendarySwords boss forms. Colors follow the actual boss palettes: Angel ivory/gold, Dragon purple/ember, Leviathan blue/teal, Colossus stone/mint runes, and Frost ice-blue. Wearable textures include matching feathers, scales, cracks or icy facets. Inventory icons use the actual armor artwork. Native netherite protection, passives and sneak + F attack abilities remain.

Full sets have fitted 3D plates and helmet details using cosmetic item displays. No player disguise is used and held inventory items are never replaced by cosmetic rendering. **All extra cosmetic models are hidden from their wearer** to prevent first-person obstruction; other players see them. The wearer still sees normal custom wearable textures. Models use no teleport interpolation and update every tick; remote display parts can still have network delay and do not deform with vanilla arm animations. This is not fully rigged client-mod armor. Disable `accessories-enabled` if you prefer native texture-only rendering.

## Temporary wings

Wear all four matching Angel, Dragon or Leviathan pieces and **double-jump**. This activates wings and flight for **45 seconds** by default. Angel wings are feathered, Dragon wings membranous, and Leviathan wings finlike. Stone and Frost have no wings. The wings disappear and temporary flight ends on expiry, armor removal, death or disconnect. Previous flight settings are restored, and boss-form flight from LegendarySwords 0.5.5 remains independent. Creative/spectator movement is left to the server. Wings appear to other viewers; the wearer gets a countdown action bar.

Config:

```yaml
wings:
  duration-seconds: 45
  cooldown-seconds: 90
```

Cooldown starts when the duration ends and is shared across sets; taking armor off or switching sets does not reset it. Use `sets.angel.flight-enabled`, `sets.dragon.flight-enabled` and `sets.leviathan.flight-enabled` to disable a set's wings. Reload with `/armor reload`. Admin reload resets active windows and cooldowns.

## Install and commands

Stop server, remove the old LegendaryArmor JAR and install **LegendaryArmor-1.2.0.jar**. Keep data and other plugins, then restart. Install **LegendaryArmor-Swords-combined-resource-pack-1.2.0.zip** as the single resource pack and keep it zipped. This retains all known sword and BetterModel pack assets. No new BetterModel files are needed.

The first upgrade to rendering-version 3 enables accessory displays again for the new 3D armor; all extra displays are hidden from their wearer. Later `accessories-enabled` edits are preserved. Wing flight still works with accessories disabled, but wings are not rendered.

`/armor` opens a three-row catalog. Only `legendaryarmor.admin` (operators by default) can click to receive a full set; four empty inventory slots are required. `/armor give <player> <angel|dragon|leviathan|stone|frost>` gives a set. Everyone with `legendaryarmor.use` can browse and use abilities. `/armor ability` also activates the attack, not the wings. Damage, passives and ability cooldowns remain configurable.

## Validation

`mvn verify` in armor-plugin; `python scripts/generate_armor_pack.py` and `python scripts/test_armor_pack.py` from repository root (Pillow required). Tests cover set validation, ability input, native flight restoration/coordination, timed wing expiry and cooldowns, armor removal, model/texture references and preservation of existing assets. Preview images show actual geometry/textures in a front-view authoring render, not in-game screenshots. Live appearance, double-jump input and remote movement still need Minecraft-client verification.
