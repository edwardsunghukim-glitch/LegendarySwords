# BetterModel cast animations

LegendarySwords 0.2.6 uses BetterModel 3.1 on Paper 1.21.11. BetterModel requires Java 25 on the server; core combat without it can run on Java 21. The reported installation is `3.1-SNAPSHOT-526`, which still needs live-client validation.

## Cast-only mode

```yaml
bettermodel:
  enabled: true
  abilities-only: true
  cast-body-animations: true
  player-body-animations: false
```

The two new flags default to true when absent, so existing configurations use cast-only animations automatically. `ability` and `ultimate` are the only character clips played in this mode. Holding a sword, standing, walking, running, ordinary swings and manual poses do not create a body tracker. The cast body closes at clip end; an ultimate holds its last pose until its scene releases it. Scene completion removes only the cast body, leaving attack effects running.

Temporary body trackers still use BetterModel's equipment/disguise path. The clone-before-inventory-update and immutable mapper snapshot fixes from 0.2.3 are retained, but the prior decoder disconnect is not proven resolved on a live client. If it recurs, set `cast-body-animations: false` and restart: ability/ultimate effects, shared scenes and damage remain, without player disguises. This fallback does not require the player-animation module or player models, only slash models.

The legacy `player-body-animations` setting is consulted only when `abilities-only` is false. Leave that combination disabled for normal movement outside casts. Vanilla first-person arm swings cannot be replaced by BetterModel; use another player to inspect character poses.

## Models and resource pack

The JAR contains sixteen unchanged `.bbmodel` files: eight player rigs and eight slash models. Unedited known older originals are upgraded with backups; edited files are preserved. Automatic installation uses `plugins/BetterModel/players` and `plugins/BetterModel/models`. For manual installation, extract `LegendarySwords-BetterModel-models.zip` and put its respective files into those directories after backing up edits.

Keep `LegendarySwords-combined-resource-pack-0.2.2.zip` from the previous release. No geometry, textures, clips, selectors or generated BetterModel assets change in 0.2.6. A matching-version ZIP included in the new bundle is a byte-identical alias. First installations or packs from before 0.2.2 need that combined pack.

Run `/bm reload` after model installation. Generate and merge a fresh `plugins/BetterModel/build.zip` only when other models or generated-client settings changed:

```sh
python3 scripts/merge_resource_packs.py path/to/build.zip path/to/LegendarySwords-resource-pack.zip combined.zip
```

Check the script's `--help` for its accepted arguments. Host the final merged ZIP if your server permits only one pack; set its SHA-1 rather than the standalone sword pack's hash. The sword item pack alone does not include BetterModel's generated rig/shader assets.

## Scenes and troubleshooting

Run `/ls status` to confirm **abilities/ultimates only** and temporary cast bodies. With BetterModel enabled, its `module.player-animation` and `module.model` must be enabled for character casts and slash effects. If models are missing, inspect import errors and preserved edits before replacing files. In attack-effects-only fallback, the player-animation module is not required.

Use `/ls cinematics on` to enable your scenes and `/ls cinematics status` to inspect the preference. Server `cinematics.enabled` must also be true. Scenes show a sword reveal in front of the caster, a shared title and world effects before impact; they do not force a camera or spectator mode. Nearby visible players within 40 blocks see the shared title and world scene. Actual damage is released once after the scene, with a timeout fallback. Player damage still respects PvP/protection settings.

Test all eight swords with `/ls give all`, `/ls charge 100` and `/ls resetcooldowns`. Verify observer poses, ordinary inventory icons, first-person swords, scene timing, observer visibility and restoration after casts. Follow [the server checklist](TESTING.md).

In 0.2.6, cast-body viewers exclude the caster. Observers see the animated body with vanilla equipment hidden; the caster sees their normal character and inventory, plus all world effects. Ultimate windup defaults to `cinematics.windup-ticks: 24` (1.2 seconds), regardless of the old `duration-ticks` value. Ability damage uses Paper armor, toughness, protection and resistance rules.
