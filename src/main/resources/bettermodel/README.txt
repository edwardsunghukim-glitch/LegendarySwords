LegendarySwords 0.2 animation assets

Eight players/*.bbmodel files contain original sword-specific idle, walk, run,
pose, swing, ability, and ultimate animation keyframes. Their player limb/skin UV
template comes from BetterModel 1.15.2's steve.bbmodel:
https://github.com/toxicity188/BetterModel/blob/1.15.2/core/src/main/resources/steve.bbmodel
The template is covered by the included LICENSE-BetterModel.txt.

Eight models/*_slash.bbmodel files provide original luminous attack models.
No external model download is needed. Filenames use the legendaryswords_ prefix.

LegendarySwords installs missing files and upgrades known original old models
with a backup. Files edited by a server owner are preserved and reported.
For manual installation, upload players files to plugins/BetterModel/players
and models files to plugins/BetterModel/models. Back up edited models first.

Engine integration uses BetterModel's 3.1 API (provided). The server's reported
3.1-SNAPSHOT-526 build still needs a real-server/client animation check.
BetterModel 3.1 requires Java 25 on the server; compiling this project's API
dependency requires a Java 25 or newer JDK.
Enable module.player-animation and module.model in BetterModel/config.yml.
Run /bm reload after installing models. The default generated pack is
plugins/BetterModel/build.zip. Download that NEW pack and merge it with the
0.2.0 LegendarySwords item pack. Old v0.1 combined packs are outdated.

Body animations are third person and visible to other players. Idle, walk, and
run follow the currently equipped sword. /ls pose plays that sword's pose.
An ordinary resource pack does not replace vanilla first-person arm animation.
Displays use maximum brightness without emitting light into nearby blocks.

Switching away, quitting, dying, changing world, and plugin shutdown clean up
the animation trackers created by this integration. Abilities/ultimates do not
depend on the cosmetic display succeeding and do not teleport the player.
