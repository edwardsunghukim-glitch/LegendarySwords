# LegendarySwords 0.2.6 downloads

- [0.2.6 cast-only plugin JAR](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-0.2.6.jar)
- [Complete download](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-download.zip)
- [Existing combined pack](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-combined-resource-pack-0.2.2.zip)
- [Standalone sword pack](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-resource-pack.zip)
- [BetterModel files](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-BetterModel-models.zip)
- [Source ZIP](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-source.zip)

Stop the server, replace the old LegendarySwords JAR with 0.2.6 and restart. Keep your configuration, player data and existing 0.2.2 combined pack. BetterModel 3.1 requires Java 25 on the server. No new models, textures or client pack assets are required.

Character animations play only during abilities and ultimates. Normal movement, ordinary swings and manual poses remain standard. The new `bettermodel.abilities-only` and `bettermodel.cast-body-animations` flags default to true even in old configurations. The old `player-body-animations: false` flag controls only the optional legacy locomotion mode, not the new temporary casts.

Ultimates play a shared charge/reveal scene before releasing their attack and damage. The sword appears in front of the caster with sword-specific motion. Nearby visible players within 40 blocks see the title and world effects. The caster's last pose holds until scene completion, then normal movement returns. A guarded timeout releases a stalled scene once; cancellation on death, teleport, world change or shutdown disarms pending damage. This is an in-world scene without forced cameras or spectator teleports. Observers see the character animation; the caster keeps normal first-person and F5 rendering without a duplicate body.

Use `/ls cinematics on`, `/ls cinematics status`, `/ls give all`, `/ls charge 100` and `/ls resetcooldowns`. Server `cinematics.enabled` must be true. Right-click casts an ability; sneak + right-click uses a charged ultimate. Player damage still depends on PvP/protection settings.

Temporary cast bodies use BetterModel's equipment path. The copy/snapshot hardening remains, but the earlier disconnect requires a live-client test. If it recurs, set `bettermodel.cast-body-animations: false` and restart to keep scenes and attack effects without disguises. See [the server checklist](../docs/TESTING.md).

The combined pack in the complete bundle under a 0.2.6 filename is byte-identical to the existing 0.2.2 pack. Only regenerate BetterModel build.zip if other models or generated-client settings changed.

Validation: **115 Java tests passed**, with zero failures, errors or skips. Release ZIP/JAR CRCs passed; all sixteen model files and the embedded sword pack match 0.2.2 byte for byte, and bundle/source snapshots match the checked sources. The unchanged assets previously passed 13 pack and 9 BetterModel asset tests, including 47 pose/action clearance checks. No live-client test was performed.

Plugin SHA-256: `0b155d24a298728c040f5835a01d8d2ebb0bc0583a33fbc4b03fcd67a5c165d1`.

Standalone pack SHA-1: `fccfab27dd8b69005de2ec7eb0d6123363f4dadb`.

Combined pack SHA-1: `04f9980f0cb83a6da16bea5f33c3a509fbf31548`.

Complete download SHA-256: `f8ed05c2a74e727785b09e0f13aec93b194bf1f25c7f7ab62b9216551813b26f`.

0.2.6: observers see one animated body with vanilla armor hidden; the caster keeps their normal view and inventory. Ultimate windup is 1.2 seconds by default (`cinematics.windup-ticks: 24`). Ability damage goes through standard Paper armor calculations.

0.2.6 adds editable global/per-sword ultimate speed with matching character clip speed. Ability damage is increased by 50% and ultimate damage by 100%, including existing configs; both multipliers are configurable. See the installation guide for config examples.

## Separate admin plugin

[Download LegendaryAdmin 0.1.0](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendaryAdmin-0.1.0.jar). Install alongside LegendarySwords, then use `/la` to choose a player and open admin tools. Includes moderation, heal/feed/fly, game modes, teleports, vanish and read-only inventory viewing. No resource pack or other plugin dependency. Features default to operators. [Commands and permissions](../admin-plugin/README.md). [Source ZIP](LegendaryAdmin-source.zip).

LegendaryAdmin validation: 12 tests passed, zero failures/errors/skips. No live-client test. JAR SHA-256: `882883f8e113f6414123f1b5e297fc45b0a624025f00ce3347a8d7fa32fa06ef`.
