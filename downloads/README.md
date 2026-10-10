# LegendarySwords 0.5.1 downloads

[Plugin JAR](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-0.5.1.jar) · [Combined resource pack](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-combined-resource-pack-0.5.1.zip) · [Full bundle](LegendarySwords-download.zip)

Rebuilt original boss forms: layered feathers and halo, stone armor and fingers, curved serpents with linked spines, opening jaws, dragon membranes and crystal armor. Each form has its own painted 128×128 material atlas. The uploaded example was used only as a reference.

55 weapons, including five boss implements with separate models, animated creature attacks and five original morphs. An ultimate activates a **five-minute form**, unlocks two abilities with separate cooldowns, and restores normal appearance/powers afterward. Left/right click uses the primary; sneak + right-click uses the secondary. The action bar shows the form countdown.

- Angel Sovereign: wing cleave, sanctuary healing and temporary flight.
- Granite Colossus Fists: rock monster, fist smash and a thrown boulder.
- Sky Leviathan Fang: flying sea serpent, snapping bite and tidal roar.
- Infernal Dragon Claw: dragon, aimed fire breath and claw rend.
- Frost Titan Hammer: giant, hammer smash and ice prison.

Stop the server, replace the old LegendarySwords JAR with 0.5.1 and restart. Models install automatically. Replace the old client pack with this combined ZIP and keep it zipped. Preserve existing configuration/data and LegendaryAdmin. Use `/swords` to browse; only admins can click for weapons.

`bosses.duration-seconds` defaults to 300. `bosses.smash-interval-ticks` and `bosses.special-interval-ticks` control form skill cooldowns. Flight is restored on expiry/interruption. Armor and protection rules still apply. Observers see the boss body; owners keep normal first-person/F5 rendering and see shared attack meshes/effects. Boss geometry does not enlarge the vanilla hitbox.

The combined pack contains 55 sword items and 180 official-compiler expansion attack/boss aliases, tailored to the supplied BetterModel 3.1-SNAPSHOT-526 namespace/named-alias setup. Changed namespaces, exporter settings or unrelated models require merging a fresh server build.zip.

[Standalone pack](LegendarySwords-resource-pack.zip) · [115-model ZIP](LegendarySwords-BetterModel-models.zip) · [Source ZIP](LegendarySwords-source.zip) · [Boss weapon preview](LegendarySwords-boss-weapons-preview.png) · [Boss form preview](LegendarySwords-boss-forms-preview.png) · [Installation](../docs/INSTALLATION.md).

Validation for 0.5.1: 326 Java tests passed, including official nested-bone export. Boss geometry/UV/texture/root-scaling checks and full model-generator reproduction passed. Client rendering and live-server performance require an in-game check.

## LegendaryAdmin

[LegendaryAdmin 0.1.1 JAR](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendaryAdmin-0.1.1.jar) remains current, with direct commands such as `/heal`, `/fly`, `/vanish`, `/invsee` and `/adminmenu`. [Commands](../admin-plugin/README.md) · [Source](LegendaryAdmin-source.zip). No resource pack required.

Plugin SHA-256: `cb1e8a1ac00afd5b4b86d99fda32deac16bc5e263ff00cf91f443bd7824fc2db`.

Combined pack SHA-1: `7cc8cf52b70ff1b0dbfed7d74a84fafe0c6d6251`.
