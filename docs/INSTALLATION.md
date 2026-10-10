# Minehut and Windows installation

Use **LegendarySwords-0.2.4.jar** with your existing **0.2.2 combined resource pack**. The new version changes server animation/scene behavior, without new client assets. You do not need Java or Maven on your computer to upload the compiled plugin.

## Install on Minehut

1. Stop the server.
2. Open **File Manager → plugins**. Remove the previous LegendarySwords JAR, keeping the LegendarySwords configuration folder and player data.
3. Upload **LegendarySwords-0.2.4.jar** directly into `plugins`. Keep only one LegendarySwords JAR.
4. Start the server. BetterModel 3.1 requires Java 25 on the server. Its player-animation and model modules must be enabled for cast bodies and slash models.
5. In game, run `/ls status`, `/ls cinematics on`, `/ls cinematics status`, `/ls give all` and `/ls charge 100`. Sneak + right-click casts an ultimate; right-click casts an ability. `/ls resetcooldowns` lets you test again.

Old configurations automatically use true for the new `bettermodel.abilities-only` and `bettermodel.cast-body-animations` keys. The old `player-body-animations: false` does not disable the new temporary cast bodies. Outside casts, movement and swings remain normal. Character animations end with their clip or ultimate scene; no idle/walking disguise stays attached.

The cinematic is a shared world scene in front of the caster, followed by the attack and damage. It does not move the player's camera or change game mode. Nearby players see its world effects and shared title. Use F5 to see your character's body animation. The first-person vanilla arm remains standard.

Server `cinematics.enabled` and your `/ls cinematics` preference both control scenes. `/ls cinematics on` is idempotent; the command without an argument still toggles. Player-target damage needs `pvp-enabled: true` and permission from protection plugins. Do not enable it if players should not take ability damage.

Temporary casts still use BetterModel's equipment path, despite the retained item-copy/snapshot hardening. If the earlier equipment decoder disconnect returns, set `bettermodel.cast-body-animations: false` and restart to keep attack effects and scenes without player disguises. No live-client test has been run for this version.

## Install the resource pack on Windows

Keep the previously installed **LegendarySwords-combined-resource-pack-0.2.2.zip** zipped. If it is already enabled in Minecraft, no replacement is needed. For first installation, open Minecraft **Options → Resource Packs → Open Pack Folder**, copy the ZIP there and enable it. Disable older combined packs. The standalone sword pack lacks BetterModel's generated model/shader assets.

If other BetterModel models/configuration changed, run `/bm reload`, download the fresh server `plugins/BetterModel/build.zip`, and merge it with the sword pack using [the BetterModel guide](BETTERMODEL.md). The 0.2.4 server code update itself needs no new `build.zip`.

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

Upload `target/LegendarySwords-0.2.4.jar` after removing the old server JAR. See [the testing checklist](TESTING.md) before public use.
