# LegendarySwords 0.2.5 downloads

- [0.2.5 cast-only plugin JAR](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-0.2.5.jar)
- [Complete download](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-download.zip)
- [Existing combined pack](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-combined-resource-pack-0.2.2.zip)
- [Standalone sword pack](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-resource-pack.zip)
- [BetterModel files](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-BetterModel-models.zip)
- [Source ZIP](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-source.zip)

Stop the server, replace the old LegendarySwords JAR with 0.2.5 and restart. Keep your configuration, player data and existing 0.2.2 combined pack. BetterModel 3.1 requires Java 25 on the server. No new models, textures or client pack assets are required.

Character animations play only during abilities and ultimates. Normal movement, ordinary swings and manual poses remain standard. The new `bettermodel.abilities-only` and `bettermodel.cast-body-animations` flags default to true even in old configurations. The old `player-body-animations: false` flag controls only the optional legacy locomotion mode, not the new temporary casts.

Ultimates play a shared charge/reveal scene before releasing their attack and damage. The sword appears in front of the caster with sword-specific motion. Nearby visible players within 40 blocks see the title and world effects. The caster's last pose holds until scene completion, then normal movement returns. A guarded timeout releases a stalled scene once; cancellation on death, teleport, world change or shutdown disarms pending damage. This is an in-world scene without forced cameras or spectator teleports. Observers see the character animation; the caster keeps normal first-person and F5 rendering without a duplicate body.

Use `/ls cinematics on`, `/ls cinematics status`, `/ls give all`, `/ls charge 100` and `/ls resetcooldowns`. Server `cinematics.enabled` must be true. Right-click casts an ability; sneak + right-click uses a charged ultimate. Player damage still depends on PvP/protection settings.

Temporary cast bodies use BetterModel's equipment path. The copy/snapshot hardening remains, but the earlier disconnect requires a live-client test. If it recurs, set `bettermodel.cast-body-animations: false` and restart to keep scenes and attack effects without disguises. See [the server checklist](../docs/TESTING.md).

The combined pack in the complete bundle under a 0.2.5 filename is byte-identical to the existing 0.2.2 pack. Only regenerate BetterModel build.zip if other models or generated-client settings changed.

Validation: **114 Java tests passed**, with zero failures, errors or skips. Release ZIP/JAR CRCs passed; all sixteen model files and the embedded sword pack match 0.2.2 byte for byte, and bundle/source snapshots match the checked sources. The unchanged assets previously passed 13 pack and 9 BetterModel asset tests, including 47 pose/action clearance checks. No live-client test was performed.

Plugin SHA-256: `812f048395bc6139ef40e6a88fc2825f37f1c5387adf6f0d572f63d4d6730977`.

Standalone pack SHA-1: `fccfab27dd8b69005de2ec7eb0d6123363f4dadb`.

Combined pack SHA-1: `04f9980f0cb83a6da16bea5f33c3a509fbf31548`.

Complete download SHA-256: `8aea214e0673259f9937b4798a98fec4154561ae75c33784b607483445dfadf3`.

0.2.5: observers see one animated body with vanilla armor hidden; the caster keeps their normal view and inventory. Ultimate windup is 1.2 seconds by default (`cinematics.windup-ticks: 24`). Ability damage goes through standard Paper armor calculations.
