LegendarySwords 0.2.3 hotfix and retained 0.2.2 animation assets

0.2.3 responds to a reported Minecraft 1.21.11 equipment-packet decoder
disconnect with 557 unread bytes on Paper 1.21.11 / BetterModel 3.1-SNAPSHOT-526.
It copies item metadata before replacing inventory items and gives BetterModel
cached item snapshots captured on the server thread. The exact packet cause
has not been proven, and no live-client test has been performed.

Player body animations are TEMPORARILY PAUSED by default. Keep these settings
in plugins/LegendarySwords/config.yml, then restart:
bettermodel:
  enabled: true
  player-body-animations: false
An existing config with no player-body-animations key uses false. If it
explicitly says true, change it to false before restarting.
This avoids player-attached BetterModel trackers and their equipment/disguise
path. Custom standing, walking, running, pose and player action body animations
are paused. Sword textures, combat effects, abilities, slash models and ultimate
presentation remains available. /ls status reports the body-animation pause.
Only re-enable true for a controlled test after collecting equipment-packet
evidence; follow docs/TESTING.md. No new command is required.

Reuse LegendarySwords-combined-resource-pack-0.2.2.zip with the 0.2.3 JAR.
No resource-pack assets, model geometry, textures or slash assets change.
The 0.2.2 clips remain bundled: all eight idle/walk/run/pose adjustments and
Master Sword, Fierce Deity Sword, Thunderblade, Eightfold Blade and Biggoron
Sword action adjustments. Heavy swords retain their forward braces.
Packs from before 0.2.2 lack its item selector and must be replaced. When body
animations are deliberately enabled, the selector keeps hotbar/first-person
swords visible while hiding only the ordinary third-person held copy.

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

Engine integration uses BetterModel's 3.1 API (provided). BetterModel 3.1
requires Java 25 on the server; compiling this project's API dependency requires
a Java 25 or newer JDK. Enable module.player-animation and module.model in
BetterModel/config.yml, then run /bm reload after installing models.
The default generated pack is plugins/BetterModel/build.zip. Use the published
0.2.2 combined pack for first setup. Download and merge a new build.zip only when
other generated client assets change or a custom combined pack is needed.
The published 0.2.2 combined pack already merges compatible build (12).zip assets
with the sword item selectors. It remains compatible with 0.2.3 unchanged.

An ordinary resource pack does not replace vanilla first-person arm animation.
Displays use maximum brightness without emitting light into nearby blocks.

Switching away, quitting, dying, changing world, and plugin shutdown clean up
the animation trackers created by this integration. Abilities/ultimates do not
depend on the cosmetic display succeeding and do not teleport the player.
