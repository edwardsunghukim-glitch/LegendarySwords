[Dragon boss + Angel/Dragon/Leviathan armor flight update](SwordFo-flight-update.zip) · [Flight configuration](../docs/FLIGHT-UPDATE.md)

[Stable armor rendering plugin 1.1.2](LegendaryArmor-1.1.2.jar): disables floating accessory displays; existing pack remains compatible.

[Dark fantasy knight armor 1.1.0: plugin](LegendaryArmor-1.1.0.jar) · [Combined pack](LegendaryArmor-Swords-combined-resource-pack-1.1.0.zip) · [Preview](LegendaryArmor-preview.png)

[LegendaryArmor bundle](LegendaryArmor-download.zip) · [Armor preview](LegendaryArmor-preview.png) · [Armor installation/configuration](../armor-plugin/README.md)

[Totem/pearl limits + combat update](SwordFo-limits-combat-update.zip) · [Limits configuration](../totem-limit-plugin/README.md) · [Combat configuration](../killtokens-plugin/README.md)

[Boss health + inventory editing update (two plugin JARs)](SwordFo-boss-admin-update.zip) · [Details](../docs/BOSS-HEALTH-UPDATE.md)

# LegendarySwords 0.5.3 downloads

[Plugin JAR](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-0.5.3.jar) · [Combined resource pack](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-combined-resource-pack-0.5.3.zip) · [Full bundle](LegendarySwords-download.zip)

0.5.2 colossal boss update: roughly nine-block angel, ten-block colossus/frost titan, nineteen-block winged dragon and twenty-four-block leviathan (rest-pose visual extents). All five receive armor/face/limb details; both serpents have independently rebuilt anatomy. Gold mannequins in the preview mark normal 1.8-block player height.

Original boss forms: layered feathers and halo, stone armor and fingers, curved serpents with linked spines, opening jaws, dragon membranes and crystal armor. Each form has its own painted 128×128 material atlas. The uploaded example was used only as a reference.

0.5.3 repairs upgrades of manually uploaded original boss files from 0.5.0–0.5.2; known originals are backed up and custom edits remain protected. Artwork is unchanged from 0.5.2. [Five boss files only](LegendarySwords-boss-models-0.5.3.zip) are available for a direct refresh into `plugins/BetterModel/models/` while the server is stopped.

55 weapons, including five boss implements with separate models, animated creature attacks and five original morphs. An ultimate activates a **five-minute form**, unlocks two abilities with separate cooldowns, and restores normal appearance/powers afterward. Left/right click uses the primary; sneak + right-click uses the secondary. The action bar shows the form countdown.

- Angel Sovereign: wing cleave, sanctuary healing and temporary flight.
- Granite Colossus Fists: rock monster, fist smash and a thrown boulder.
- Sky Leviathan Fang: flying sea serpent, snapping bite and tidal roar.
- Infernal Dragon Claw: dragon, aimed fire breath and claw rend.
- Frost Titan Hammer: giant, hammer smash and ice prison.

Stop the server, replace the old LegendarySwords JAR with 0.5.3 and restart. Models install automatically. Replace the old client pack with this combined ZIP and keep it zipped. Preserve existing configuration/data and LegendaryAdmin. Use `/swords` to browse; only admins can click for weapons.

`bosses.duration-seconds` defaults to 300. `bosses.smash-interval-ticks` and `bosses.special-interval-ticks` control form skill cooldowns. Flight is restored on expiry/interruption. Armor and protection rules still apply. Observers see the boss body; owners keep normal first-person/F5 rendering and see shared attack meshes/effects. Boss geometry does not enlarge the vanilla hitbox.

The combined pack contains 55 sword items and 180 official-compiler expansion attack/boss aliases, tailored to the supplied BetterModel 3.1-SNAPSHOT-526 namespace/named-alias setup. Changed namespaces, exporter settings or unrelated models require merging a fresh server build.zip.

[Standalone pack](LegendarySwords-resource-pack.zip) · [115-model ZIP](LegendarySwords-BetterModel-models.zip) · [Source ZIP](LegendarySwords-source.zip) · [Boss weapon preview](LegendarySwords-boss-weapons-preview.png) · [Boss form preview](LegendarySwords-boss-forms-preview.png) · [Installation](../docs/INSTALLATION.md).

Validation for 0.5.3: 328 Java tests passed, including official nested-bone export. Boss geometry/UV/texture/root-scaling checks and full model-generator reproduction passed. Client rendering and live-server performance require an in-game check.

## LegendaryAdmin

[LegendaryAdmin 0.1.1 JAR](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendaryAdmin-0.1.1.jar) remains current, with direct commands such as `/heal`, `/fly`, `/vanish`, `/invsee` and `/adminmenu`. [Commands](../admin-plugin/README.md) · [Source](LegendaryAdmin-source.zip). No resource pack required.

Plugin SHA-256: `db05335b3ead4a06913ac9b38525624353b958355557ef9797a337f83d9ea2e8`.

Combined pack SHA-1: `04e86663e74de020f8a26d641f39685ccfa3d3a8`.
