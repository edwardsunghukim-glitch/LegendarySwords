# LegendarySwords 0.5.0 downloads

[Plugin JAR](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-0.5.0.jar) · [Combined resource pack](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-combined-resource-pack-0.5.0.zip) · [Full bundle](LegendarySwords-download.zip)

55 weapons, including five new boss implements with separate models, animated creature attacks and five original morphs. An ultimate activates a **five-minute form**, unlocks two abilities with separate cooldowns, and restores normal appearance/powers afterward. Left/right click uses the primary; sneak + right-click uses the secondary. The action bar shows the form countdown.

- Angel Sovereign: wing cleave, sanctuary healing and temporary flight.
- Granite Colossus Fists: rock monster, fist smash and a thrown boulder.
- Sky Leviathan Fang: flying sea serpent, snapping bite and tidal roar.
- Infernal Dragon Claw: dragon, aimed fire breath and claw rend.
- Frost Titan Hammer: giant, hammer smash and ice prison.

Stop the server, replace the old LegendarySwords JAR with 0.5.0 and restart. Models install automatically. Replace the old client pack with this combined ZIP and keep it zipped. Preserve existing configuration/data and LegendaryAdmin. Use `/swords` to browse; only admins can click for weapons.

`bosses.duration-seconds` defaults to 300. `bosses.smash-interval-ticks` and `bosses.special-interval-ticks` control form skill cooldowns. Flight is restored on expiry/interruption. Armor and protection rules still apply. Observers see the boss body; owners keep normal first-person/F5 rendering and see shared attack meshes/effects. Boss geometry does not enlarge the vanilla hitbox.

The combined pack contains 55 sword items and 166 official-compiler expansion attack/boss aliases, tailored to the supplied BetterModel 3.1-SNAPSHOT-526 namespace/named-alias setup. Changed namespaces, exporter settings or unrelated models require merging a fresh server build.zip.

[Standalone pack](LegendarySwords-resource-pack.zip) · [115-model ZIP](LegendarySwords-BetterModel-models.zip) · [Source ZIP](LegendarySwords-source.zip) · [Boss weapon preview](LegendarySwords-boss-weapons-preview.png) · [Installation](../docs/INSTALLATION.md).

Validation: 326 Java tests, 13 resource-pack checks, 11 model/animation checks and four geometry/texture checks passed. Client rendering and live-server performance require an in-game check.

## LegendaryAdmin

[LegendaryAdmin 0.1.1 JAR](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendaryAdmin-0.1.1.jar) remains current, with direct commands such as `/heal`, `/fly`, `/vanish`, `/invsee` and `/adminmenu`. [Commands](../admin-plugin/README.md) · [Source](LegendaryAdmin-source.zip). No resource pack required.

Plugin SHA-256: `17c619ef97ad64adf0b08c129f0722c8511c94f39afa1e96140bf36e618739cc`.

Combined pack SHA-1: `fae20a661b1cf4e7f24926b67c3498723174c84b`.
