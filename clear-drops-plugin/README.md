# ClearDrops 1.0.1

Standalone Paper 1.21.11 plugin; Java 21+. Place ClearDrops-1.0.1.jar in plugins while the server is stopped, then restart.

Automatically removes loaded dropped-item entities every 600 seconds (10 minutes), with a 30-second warning. No mobs, armor stands, displays, item frames or inventory items are removed. All ground items in included loaded worlds are eligible, including recently dropped items. Unloaded chunks are not loaded or scanned. Minecraft also has its normal item despawn timer; this plugin does not extend item lifetimes.

/cleardrops shows time until cleanup.
/cleardrops now cleans immediately and resets the automatic timer.
/cleardrops reload reloads settings and resets the timer.
Management commands require cleardrops.admin (operators by default).

Edit plugins/ClearDrops/config.yml:
- interval-seconds: 600 (accepted range 10–86400)
- warning-seconds: 30 (0 disables warnings; capped below interval)
- excluded-worlds: [] (list world names to skip)

Intervals use server ticks, so cleanup can take longer during server lag. Timer restarts on server/plugin restart.
Build: mvn verify. Three tests cover removal/counting, excluded worlds, and management permissions. No live Paper test performed. This is a dropped-item cleaner; it does not guarantee improved server performance.

Command autocomplete added in 1.0.1: press Tab after a command/subcommand to see prefix-filtered suggestions. Suggestions respect management permissions. Existing command aliases work. No player-name fallback is returned for unsupported argument positions. Build and 5 tests passed for this module.
