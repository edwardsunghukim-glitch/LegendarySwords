# LegendaryKoTH 1.0.0

Standalone Paper 1.21.11 plugin, Java 21+. Requires WorldGuard 7 and its WorldEdit dependency already installed. Uses the public WorldGuard region API via reflection so no extra libraries are bundled.
Stop server, upload LegendaryKoTH-1.0.0.jar to plugins, restart.

## Existing region
Defaults to the WorldGuard region kothwin. If it exists in exactly one loaded world, the plugin finds it automatically. /koth setregion kothwin <world> explicitly chooses a world. Polygon and cuboid regions both use WorldGuard's actual contains check, including vertical boundaries; the plugin does not replace the polygon with a bounding rectangle.
If defining a new polygon: make a WorldEdit polygon selection and define a WorldGuard region, then select it with /koth setregion. WorldGuard controls the shape; this plugin does not implement a separate polygon editor.

## Commands
/koth or /koth status — event status (everyone).
/koth start — begin an event, zero capture progress.
/koth stop — stop and clear progress, no reward.
/koth setregion <region> [world] — select an existing WorldGuard region; stops any current event.
/koth setreward <console command> — replace the reward list with this command.
/koth reward add <console command> — append an additional reward.
/koth reward list — show configured rewards.
/koth reward clear — remove all rewards.
/koth reload — reload config and stop the current event; start explicitly afterward.
All management commands require legendarykoth.admin (operators by default).

Examples:
/koth setreward killtokengive %player% 10
/koth reward add give %player% diamond 3
/koth start
Commands run as console exactly once at a win. %player% and {player} are replaced with winner's name. Optional leading slash is stripped. Reward commands depend on the plugin providing that command; failed commands are logged. No reward commands are selected by default.

## Capture
One player inside the region gains progress once per second. Default goal: 100 uncontested seconds.
If another eligible player enters, the hill is contested: holder progress pauses by default. Set capture.pause-when-contested: false to reset progress instead.
Leaving the hill, dying, disconnecting, or changing world resets the holder immediately. A new holder starts at zero. Spectators, dead players and creative players do not capture by default. Creative capture can be enabled. Generic vanished metadata is excluded; staff permissions can be listed in capture.excluded-permissions for explicit exclusion. Operator status alone does not exclude survival players.
A gold/red boss bar shows holder, contested status and progress to online players. The event stops after one winner; /koth start begins another. WorldGuard region updates/deletion are respected during the event.

## Config
plugins/LegendaryKoTH/config.yml:
region.name: kothwin
region.world: '' (automatic loaded-world lookup)
capture.seconds: 100 (range 1–86400)
capture.pause-when-contested: true
capture.allow-creative: false
capture.excluded-permissions: []
start-on-server-start: false
rewards.commands: []
messages.started/stopped/captured/won: configurable & color messages; %player% and %seconds% placeholders.
Use /koth reload after file edits. Event state is not retained over restarts. The timer uses server ticks, so heavy server lag can extend real elapsed time.

Build: mvn verify. Nine tests verify exact 100-second win, contested pause/reset, changing/absent holders, stopped state, immediate death/disconnect reset, reward substitution and management permission checks. WorldGuard API signatures were checked against official WorldGuard 7/WorldEdit 7 sources. No live Paper/WorldGuard test performed.
