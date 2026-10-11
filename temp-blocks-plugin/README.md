# TemporaryBlocks 1.0.0

Paper 1.21.11; Java 21 or newer. Separate from LegendarySwords.

Stop the server, upload TemporaryBlocks-1.0.0.jar into plugins, then restart.
All players (including operators) place temporary blocks by default.
Blocks disappear without item drops after 30 seconds. Existing world blocks are not tracked.

`/tempblocks bypass` (alias `/tb bypass`) toggles permanent placement for the calling player.
Requires `tempblocks.bypass` (operators by default). Permission is checked again on every placement.
The toggle resets on logout/restart. It only affects new placements; old timers continue.

Edit `plugins/TemporaryBlocks/config.yml`, `duration-seconds: 30`, and restart to change duration.
Pending timers survive orderly restarts. Unloaded chunks are cleaned when loaded.
Doors/beds track both blocks. Tracked gravity blocks cannot fall, and pistons cannot move tracked blocks.
Containers are removed with their contents, without drops; do not store valuables in temporary blocks.
This plugin tracks block placement, not bucket liquids, entities, or blocks produced by growth/spread.
Other plugins that replace blocks directly with the same material can still be affected by an old timer.

Build: `mvn verify` in this directory. Two focused tests check denied bypass and block state matching.
No live Paper server test has been performed.
