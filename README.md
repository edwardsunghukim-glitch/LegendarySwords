# LegendarySwords

A first playable Paper 1.21.11 plugin with the Master Sword, Flameblade, and Fierce Deity Sword: custom item models, animated textures, abilities, charged ultimates, particles, sounds, and short optional cinematic cameras. BetterModel adds third-person character animations when its bundled animation assets are installed.

The artwork and animations are original fan-made assets inspired by the named Zelda swords. This is an initial implementation to try and tune on a test server.

![Preview rendered from the supplied sword geometry](resource-pack/preview.png)

## Install and try it

You can install the compiled release without installing Java or Maven on your own computer. Your server needs Paper **1.21.11** and **Java 21**. BetterModel is optional; the swords and abilities work without it.

1. Put `LegendarySwords-0.1.0.jar` in your server's `plugins` folder and restart the server.
2. Install `LegendarySwords-resource-pack.zip` as a server resource pack, or add it to your Minecraft client's Resource Packs screen for an initial test.
3. Join as an operator and run `/ls give all`.
4. Hold a sword. **Right-click** uses its ability; **sneak + right-click** uses its ultimate when charge reaches 100.
5. Run `/ls charge 100` to test an ultimate without fighting mobs first.

Normal sword hits earn **10 charge**, and melee kills add **20** more. Charge caps at 100. The action bar shows charge and ability cooldown. `/ls cinematics` toggles your cinematic camera.

| Sword | Right-click ability | Charged ultimate | Default cooldowns |
| --- | --- | --- | --- |
| Master Sword | Skyward Slash: a blue beam reaching 14 blocks | Sacred Storm: sacred rings, light pillars, and regeneration | 7 seconds / 75 seconds |
| Flameblade | Flame Arc: a burning cone reaching 6 blocks | Inferno Awakening: expanding waves of fire | 9 seconds / 90 seconds |
| Fierce Deity Sword | Crescent Moon: a slowing crescent reaching 7 blocks | Lunar Judgment: slowing lunar waves | 8 seconds / 90 seconds |

Ultimates create four themed damage waves, 22 ticks apart, with radii of 2, 4, 6, and 8 blocks. An optional camera lasting about 1.8 seconds shows an awakening sword; with BetterModel it also shows an animated hero at the casting position. The waves start when that camera finishes. Damage, cooldowns, camera settings, and charge gains are configurable.

See [installation steps for Minehut and Windows](docs/INSTALLATION.md), [BetterModel setup](docs/BETTERMODEL.md), and the [server testing checklist](docs/TESTING.md). The [resource pack guide](resource-pack/README.md) covers pack merging and visual limitations.

## Commands

`/legendaryswords` and `/ls` are equivalent. Give and charge commands can target yourself when used in-game, or name an online player. Use the full form from the console.

| Command | Purpose |
| --- | --- |
| `/ls give [player] <sword\|all>` | Give one sword or all three |
| `/ls list` | List sword IDs, abilities, and ultimates |
| `/ls charge [player] <0-100>` | Set ultimate charge for testing |
| `/ls cinematics` | Toggle your cinematic camera |
| `/ls pack` | Request the configured resource pack |
| `/ls status` | Show plugin, pack, and animation status |
| `/ls reload` | Reload plugin configuration |

Sword IDs are `master_sword`, `flameblade`, and `fierce_deity_sword`.

`legendaryswords.admin` controls administrative commands and defaults to operators. `legendaryswords.use` permits sword abilities and defaults to everyone. Player versus player ability damage is disabled by default. Test interactions with your server's protection plugins before enabling PvP.

## Build from source

Install a **Java 21 JDK** and **Maven 3.9+**, then open a terminal in the folder containing `pom.xml`:

```sh
mvn --batch-mode verify
```

The build runs unit tests and creates:

- `target/LegendarySwords-0.1.0.jar`
- `target/LegendarySwords-resource-pack.zip`

Optionally, use Python 3's standard library to create a source ZIP and a download bundle containing the JAR, pack, hash, source, and installation notes:

```sh
python3 scripts/create_release.py
```

This produces `target/LegendarySwords-source.zip` and `target/LegendarySwords-download.zip`.

Build outputs in `target/` are ignored by Git. To publish changes, commit the source files using GitHub Desktop and press **Push origin**. [The installation guide](docs/INSTALLATION.md#put-the-source-in-your-github-repository) explains how to extract a source ZIP into an empty cloned repository.

## What the visuals can do

The resource pack supplies custom sword geometry and textures. Enchantment glint, particles, and animated textures give a glowing appearance. Actual light cast into the world or shader-style emissive textures requires compatible client support.

BetterModel animations are visible as third-person player limb movement. A normal resource pack cannot replace the vanilla first-person arm swing with an arbitrary animation. The plugin provides an optional short third-person camera for ultimates; it does not require a client mod for the core abilities.

BetterModel integration targets **1.15.2**. To see its animated bodies and slash models, distribute BetterModel's generated pack together with the sword pack; [follow the integration guide](docs/BETTERMODEL.md).

Automated tests check charge, cooldowns, sword IDs, hit geometry, mocked combat event handling, and mocked cinematic cleanup and saved-state recovery. Loading the plugin, client asset acceptance, animations, camera presentation, and multiplayer protection behavior still require the [manual checks](docs/TESTING.md) on a real Paper server.
