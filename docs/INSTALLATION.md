# Minehut and Windows installation

Use **LegendarySwords-0.2.6.jar** with your existing **0.2.2 combined resource pack**. The new version changes server animation/scene behavior, without new client assets. You do not need Java or Maven on your computer to upload the compiled plugin.

## Install on Minehut

1. Stop the server.
2. Open **File Manager → plugins**. Remove the previous LegendarySwords JAR, keeping the LegendarySwords configuration folder and player data.
3. Upload **LegendarySwords-0.2.6.jar** directly into `plugins`. Keep only one LegendarySwords JAR.
4. Start the server. BetterModel 3.1 requires Java 25 on the server. Its player-animation and model modules must be enabled for cast bodies and slash models.
5. In game, run `/ls status`, `/ls cinematics on`, `/ls cinematics status`, `/ls give all` and `/ls charge 100`. Sneak + right-click casts an ultimate; right-click casts an ability. `/ls resetcooldowns` lets you test again.

Old configurations automatically use true for the new `bettermodel.abilities-only` and `bettermodel.cast-body-animations` keys. The old `player-body-animations: false` does not disable the new temporary cast bodies. Outside casts, movement and swings remain normal. Character animations end with their clip or ultimate scene; no idle/walking disguise stays attached.

The cinematic is a shared world scene in front of the caster, followed by the attack and damage. It does not move the player's camera or change game mode. Nearby players see its world effects and shared title. Other players see your character animation; your own view keeps the normal player and inventory to prevent camera obstruction. The first-person vanilla arm remains standard.

Server `cinematics.enabled` and your `/ls cinematics` preference both control scenes. `/ls cinematics on` is idempotent; the command without an argument still toggles. Player-target damage needs `pvp-enabled: true` and permission from protection plugins. Do not enable it if players should not take ability damage.

Temporary casts still use BetterModel's equipment path, despite the retained item-copy/snapshot hardening. If the earlier equipment decoder disconnect returns, set `bettermodel.cast-body-animations: false` and restart to keep attack effects and scenes without player disguises. No live-client test has been run for this version.

## Install the resource pack on Windows

Keep the previously installed **LegendarySwords-combined-resource-pack-0.2.2.zip** zipped. If it is already enabled in Minecraft, no replacement is needed. For first installation, open Minecraft **Options → Resource Packs → Open Pack Folder**, copy the ZIP there and enable it. Disable older combined packs. The standalone sword pack lacks BetterModel's generated model/shader assets.

If other BetterModel models/configuration changed, run `/bm reload`, download the fresh server `plugins/BetterModel/build.zip`, and merge it with the sword pack using [the BetterModel guide](BETTERMODEL.md). The 0.2.6 server code update itself needs no new `build.zip`.

If hosting one pack URL, use the combined ZIP and its SHA-1 `04f9980f0cb83a6da16bea5f33c3a509fbf31548`:

```yaml
resource-pack:
  url: 'https://your-host.example/LegendarySwords-combined-resource-pack-0.2.2.zip'
  sha1: '04f9980f0cb83a6da16bea5f33c3a509fbf31548'
```

Keep your existing hosted pack URL/hash when reusing that same pack. A plugin JAR cannot replace a resource pack already installed on your computer.

## Put the source in your GitHub repository

Extract `LegendarySwords-source.zip`. In GitHub Desktop, select LegendarySwords and click **Show in Explorer**. Copy the extracted source ZIP's contents into that repository folder; keep its existing `.git` folder. Enter a summary, commit and push to GitHub.

To build source yourself, install JDK 25+ and Maven 3.9+, open the folder containing `pom.xml`, then run:

```sh
mvn --batch-mode verify
```

Upload `target/LegendarySwords-0.2.6.jar` after removing the old server JAR. See [the testing checklist](TESTING.md) before public use.

In 0.2.6, cast-body viewers exclude the caster. Observers see the animated body with vanilla equipment hidden; the caster sees their normal character and inventory, plus all world effects. Ultimate windup defaults to `cinematics.windup-ticks: 24` (1.2 seconds), regardless of the old `duration-ticks` value. Ability damage uses Paper armor, toughness, protection and resistance rules.

## Ultimate speed and damage tuning

Edit `plugins/LegendarySwords/config.yml`, then run `/ls reload`:

```yaml
ultimates:
  speed: 2.0 # Twice as fast; 1.0 normal, 0.5 half speed.
damage:
  ability-multiplier: 1.5
  ultimate-multiplier: 2.0
```

Speed controls the charge/reveal delay before the attack starts and scales the character clip to fit it. It does not change cooldowns, beam travel speed or repeated-hit intervals. The base delay is `cinematics.windup-ticks: 24` (1.2 seconds); speed 2.0 makes it 0.6 seconds. Set `swords.master_sword.ultimate-speed: 0.5` for an optional per-sword override. Speed is bounded to 0.25–4.0, with a final delay of 4–160 ticks.

Ability damage now defaults to 1.5 times each sword's configured `ability-damage`; ultimate damage defaults to 2 times its configured `ultimate-damage`. These multipliers also apply when the keys are missing in an older configuration. Set either multiplier to 1.0 to restore previous damage. Melee damage is unchanged. Armor, toughness, protection and resistance remain handled by Paper.
