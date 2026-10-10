[Download LegendaryAdmin 0.1.1](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendaryAdmin-0.1.1.jar): direct commands without `/la`, including `/heal`, `/fly`, `/vanish`, `/invsee`, and `/adminmenu`. Replace the old admin JAR and restart. 13 tests passed.

## Laser aiming hotfix 0.3.1

[Download plugin 0.3.1](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-0.3.1.jar). Beams use full vertical aim from eye height; display rotation no longer applies player yaw/pitch twice. Ultimates aim at release. Keep the 0.3.0 combined pack. Replace the old JAR and restart.

# LegendarySwords 0.3.0 — 24 weapons

- [Plugin JAR](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-0.3.0.jar)
- [NEW combined resource pack](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-combined-resource-pack-0.3.0.zip)
- [Complete bundle](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-download.zip)
- [Standalone 24-weapon sword pack](LegendarySwords-resource-pack.zip)
- [All 48 BetterModel models](LegendarySwords-BetterModel-models.zip)
- [Source ZIP](LegendarySwords-source.zip)

Stop the server, replace the old LegendarySwords JAR, restart, then replace the old Minecraft pack with the new combined ZIP. Keep configuration/player data. Models auto-install and import with backups for known originals; edited files are preserved. `/ls give all` gives all 24 weapons. BetterModel requires server Java 25.

The sixteen new weapons have additional attack patterns, models, colored animated atlases and cast animations. All 24 rigs include release follow-through plus draw/charge/recover authoring clips. Damage buffs and editable global/per-sword ultimate speed remain. Ordinary movement and the caster's view/inventory stay normal; observers see temporary animated bodies.

The combined pack preserves the previously supplied BetterModel 3.1-SNAPSHOT-526 assets, adding official-compiler geometry/textures for the new named `bettermodel:c/` slash aliases. For different namespace/export settings or changed unrelated models, download a fresh server `build.zip` and merge it with this release's standalone sword pack. This expansion needs new client assets; do not reuse the old eight-weapon pack.

Validation: **165 Java tests passed**, zero failures/errors/skips; **13 pack checks and 10 model/animation checks passed**. These cover all 24 IDs, imports, distinct motions/shapes, inventory rendering, protected new attack damage, and ability/ultimate/release clearance for every weapon. No live-client test was performed.

## Separate admin plugin

[LegendaryAdmin 0.1.0 JAR](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendaryAdmin-0.1.0.jar) remains unchanged. Install alongside the swords plugin; `/la` opens its admin menu. It needs no resource pack. [Commands and permissions](../admin-plugin/README.md). [Admin source ZIP](LegendaryAdmin-source.zip).

Plugin SHA-256: `2a7e18ccd7f5253b32224041d783d4743f8fad49d9ad9caf85fb09dfafc60e63`.

Combined pack SHA-1: `30f1a7978e4f194a516d727a48788ad355bde7e1`.
