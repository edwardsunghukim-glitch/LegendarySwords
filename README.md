# LegendarySwords

Paper **1.21.11** plugin with eight detailed Zelda sword designs, visible abilities, charged ultimates, and sword-specific BetterModel animations. **BetterModel 3.1 requires Java 25**; core combat without BetterModel can run on Java 21. Includes distinct laser, fire, lunar, ice, lightning, ground, wind, and heavy-blade effects. Ultimates start independently of the cosmetic presentation, which stays at the casting position without moving the player or changing game mode.

The artwork and animation keyframes are original fan-made assets inspired by the named swords. Server compilation and automated checks do not verify how Minecraft renders them; complete the [real-server checklist](docs/TESTING.md).

Version **0.2.2** adjusts all eight swords' idle, walk, run, and pose keyframes to reduce arm/blade clipping, with forward braces for heavy swords. It also updates swing, ability, and ultimate keyframes for Master Sword, Fierce Deity Sword, Thunderblade, Eightfold Blade, and Biggoron Sword. A new item-model selector preserves the selected hotbar sword while the animated body displays its own grip. Known original server models update automatically. Body geometry, sword artwork, textures, and slash effects are unchanged; **the updated resource pack is required** for the item selector.

![Preview rendered from the supplied sword geometry](resource-pack/preview.png)

## Install or update

1. Stop the server, remove the old LegendarySwords JAR from `plugins`, and upload **`LegendarySwords-0.2.2.jar`**. Keep only one version of the plugin.
2. Start the server. Replace your old combined pack with **`LegendarySwords-combined-resource-pack-0.2.2.zip`** in Minecraft's Resource Packs menu. Keep it zipped. If using the standalone sword pack, replace it with the new `LegendarySwords-resource-pack.zip`.
3. If using BetterModel, let the plugin install/update its bundled models and run `/bm reload`. Download the newly generated `plugins/BetterModel/build.zip` if your other BetterModel models or configuration changed.
4. For first installation or changes to other generated BetterModel assets, combine that fresh BetterModel pack with the new sword pack if your server accepts one resource-pack URL. The published 0.2.2 combined pack already contains the existing `build (12).zip` BetterModel assets plus the updated sword pack. See [the BetterModel guide](docs/BETTERMODEL.md).
5. Join as an operator and run `/ls status`, `/ls give all`, and `/ls charge 100`.

**Upgrading from 0.2.0 or 0.2.1:** replace the JAR, restart, and install the **0.2.2 combined pack**. Unedited bundled player files update automatically; if edited files are preserved, back them up before installing the new files from `LegendarySwords-BetterModel-models.zip`. The earlier combined pack lacks the new item selector. Existing `build (12).zip` BetterModel assets can be reused in the merge while other models/configuration stay unchanged, but the sword pack must be updated.

You do not need Java or Maven installed on your computer to upload the compiled plugin. The server needs **Java 25 when using BetterModel 3.1**. BetterModel is optional for combat and particle/display effects; its generated pack is required for body animations and slash models. The integration uses **BetterModel 3.1's API**. The installed `3.1-SNAPSHOT-526` build needs a real-server animation check; compilation cannot prove snapshot runtime behavior.

**Right-click** uses an ability. **Sneak + right-click** uses the ultimate at 100 charge. Successful normal melee hits earn **10 charge**, and melee kills add **20** more. Ability damage does not generate charge. Use `/ls pose` while holding a sword to play its pose. In third person, idle, walking, and sprinting select the equipped sword's own animation.

| Sword ID | Right-click ability | Charged ultimate | Default cooldowns |
| --- | --- | --- | --- |
| `master_sword` | Skyward Laser: sustained luminous beam | Triforce Judgment: sacred beam attack | 7 s / 75 s |
| `flameblade` | Ring of Fire: visible ring of flames | Inferno Vortex: expanding fire columns | 9 s / 90 s |
| `fierce_deity_sword` | Lunar Crescent: traveling crescent | Falling Moon: lunar meteors | 8 s / 90 s |
| `frostblade` | Ice Fang: rising ice spikes | Frozen Sanctuary: swirling blizzard | 8 s / 80 s |
| `thunderblade` | Chain Lightning: connected lightning strikes | Thunderstorm: overhead storm strikes | 9 s / 85 s |
| `royal_claymore` | Royal Faultline: ground fissure | King's Verdict: giant blade slam | 10 s / 95 s |
| `eightfold_blade` | Gale Step: traveling wind ribbons | Eightfold Tempest: angled wind cuts | 6 s / 70 s |
| `biggoron_sword` | Goron Spin: oversized spinning blade | Mountain Breaker: radial earth eruption | 12 s / 100 s |

Cooldowns, damage, charge, PvP, and presentation settings are configurable. Particles and temporary display models provide visible attacks even when BetterModel is absent. These effects do not permanently alter terrain. Player ability damage is disabled by default; test your protection plugins before enabling PvP.

See [Minehut and Windows installation](docs/INSTALLATION.md), [BetterModel setup and pack merging](docs/BETTERMODEL.md), and [the testing checklist](docs/TESTING.md).

## Commands

`/legendaryswords`, `/ls`, and `/swords` refer to the same command. Give, charge, and cooldown reset commands can target yourself in-game or name an online player; use a named player from the console.

| Command | Purpose |
| --- | --- |
| `/ls give [player] <sword\|all>` | Give one sword or all eight |
| `/ls list` | List sword IDs, abilities, and ultimates |
| `/ls charge [player] <0-100>` | Set ultimate charge for testing |
| `/ls resetcooldowns [player]` | Clear that player's cooldowns for testing |
| `/ls pose` | Play the equipped sword's pose |
| `/ls cinematics` | Toggle the in-place ultimate presentation |
| `/ls pack` | Request the configured resource pack |
| `/ls status` | Show plugin, pack, and animation status |
| `/ls reload` | Reload plugin configuration |

`legendaryswords.admin` controls give, charge, reset, and reload commands and defaults to operators. `legendaryswords.use` permits sword abilities and poses and defaults to everyone. The pose has a short two-second cooldown.

## Build from source

Install a **Java 25+ JDK** and **Maven 3.9+**, then open a terminal in the folder containing `pom.xml`. The BetterModel API dependency requires the newer JDK even though core plugin classes target Java 21:

```sh
mvn --batch-mode verify
```

This runs Java unit tests and creates `target/LegendarySwords-0.2.2.jar` and `target/LegendarySwords-resource-pack.zip`. Python 3's standard library can validate the pack and create the source, model, and complete download ZIPs:

```sh
python3 scripts/build_resource_pack.py --validate-only
python3 scripts/test_resource_pack.py
python3 scripts/test_bettermodel_assets.py
python3 scripts/create_release.py
```

The complete bundle contains the JAR, sword pack and hash, source ZIP, separate sixteen-model BetterModel ZIP, and beginner installation notes. It does not contain a freshly generated BetterModel `build.zip`: BetterModel must generate that on the server after the updated models are installed.

Build outputs in `target/` are ignored by Git. [The installation guide](docs/INSTALLATION.md#put-the-source-in-your-github-repository) explains extracting the source into an existing GitHub Desktop repository.

## Visual limits and validation

The sword pack provides custom geometry, animated energy textures, glint, and optional emissive overlays. Shader bloom and light illuminating nearby blocks depend on compatible client support. Temporary attack displays use full brightness but do not emit world lighting.

BetterModel replaces the rendered third-person body with an animated player model while a sword is held. It provides distinct idle, walk, run, pose, swing, ability, and ultimate clips for each sword. **The vanilla first-person arm swing is unchanged.** Use F5 or another player's view to inspect body animations.

While its tracker is active, the plugin selects the sword's `_animated` item model. The new pack hides the ordinary held copy only in third-person contexts and keeps the animated grip, inventory icon, and first-person sword visible. Ending the tracker restores the base item model. This requires the plugin and pack to be updated together.

Automated tests cover mechanics and mocked lifecycle/protection behavior; pack checks verify file structure, references, textures, and animation data. Real client presentation, installed BetterModel compatibility, and multiplayer behavior remain manual server checks.
