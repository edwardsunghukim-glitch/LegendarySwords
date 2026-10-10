LegendarySwords 0.2.2 animation assets

0.2.2 adjusts all eight swords' idle, walk, run and pose keyframes to reduce
arm/blade clipping. Heavy swords use a forward brace. Swing/ability/ultimate
clips also change for Master Sword, Fierce Deity Sword, Thunderblade,
Eightfold Blade and Biggoron Sword. Geometry, textures and slash assets are unchanged.
Install LegendarySwords-combined-resource-pack-0.2.2.zip with the new JAR.
It adds item selectors that keep hotbar/first-person swords visible while hiding
only the ordinary third-person held copy. The animated bone item stays visible.
The base item model is restored when the tracker ends. Old combined packs lack
the selector. Their build (12).zip BetterModel assets can be reused in the new
merge while other models/configuration stay unchanged.

Eight players/*.bbmodel files contain original sword-specific idle, walk, run,
pose, swing, ability, and ultimate animation keyframes. Their player limb/skin UV
template comes from BetterModel 1.15.2's steve.bbmodel:
https://github.com/toxicity188/BetterModel/blob/1.15.2/core/src/main/resources/steve.bbmodel
The template is covered by the included LICENSE-BetterModel.txt.

Eight models/*_slash.bbmodel files provide original luminous attack models.
No external model download is needed. Filenames use the legendaryswords_ prefix.

LegendarySwords installs missing files and upgrades known original old models,
including 0.2.0/0.2.1 player files, with a backup when the server starts.
Files edited by a server owner are preserved and reported.
For manual installation, upload players files to plugins/BetterModel/players
and models files to plugins/BetterModel/models. Back up edited models first.

Engine integration uses BetterModel's 3.1 API (provided). The server's reported
3.1-SNAPSHOT-526 build still needs a real-server/client animation check.
No live-client test was run; check carry/action transitions on your actual server.
BetterModel 3.1 requires Java 25 on the server; compiling this project's API
dependency requires a Java 25 or newer JDK.
Enable module.player-animation and module.model in BetterModel/config.yml.
Run /bm reload after installing models. The default generated pack is
plugins/BetterModel/build.zip. For first installation or changes to generated
client assets, download that NEW pack and merge it with the eight-sword item
pack. The published 0.2.2 combined pack already merges compatible build (12).zip
BetterModel assets with the new sword selectors. Replace older combined packs.

Body animations are third person and visible to other players. Idle, walk, and
run follow the currently equipped sword. /ls pose plays that sword's pose.
An ordinary resource pack does not replace vanilla first-person arm animation.
Displays use maximum brightness without emitting light into nearby blocks.

Switching away, quitting, dying, changing world, and plugin shutdown clean up
the animation trackers created by this integration. Abilities/ultimates do not
depend on the cosmetic display succeeding and do not teleport the player.
