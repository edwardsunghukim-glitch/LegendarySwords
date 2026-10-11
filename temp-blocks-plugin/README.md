# TemporaryBlocks 1.1.1

Paper 1.21.11; Java 21+. Separate from LegendarySwords.

Stop the server, replace the old TemporaryBlocks JAR with TemporaryBlocks-1.1.1.jar in plugins, keep the TemporaryBlocks data folder, restart. Existing placement timers migrate automatically.

Player-placed blocks reset after 30 seconds. Player-broken original world blocks restore their original block data (including orientation) after 30 seconds. Temporary breaking suppresses block drops and XP to prevent repeated harvesting. Breaking a temporary placed block does not resurrect it. Placing into a temporary hole preserves its original restoration target. A subsequent temporary edit starts a fresh timer for that position. Restoration occurs only if the tracked block type still matches; a changed block of a different type is left intact. Doors track their second half; beds with tile data are protected by the special-block restriction below.

/tempblocks bypass (alias /tb bypass) toggles permanent placement AND breaking. Requires tempblocks.bypass (operators by default). Permission is checked on every action. The toggle resets on logout/restart, affects only new edits, and leaves unrelated pending timers running. Permanent edits to a tracked position cancel its timer.

Containers and other TileState blocks (signs, special stored data, etc.) cannot be broken temporarily; use bypass for permanent breaking. This avoids reconstructing incomplete inventories or tile data. Temporary placed containers still disappear with contents, without drops.

Edit plugins/TemporaryBlocks/config.yml duration-seconds: 30 and restart (range 1–86400 seconds).
Timers persist across orderly restarts. Saves are batched every five seconds, plus shutdown; a crash can lose the latest unsaved changes. Unloaded chunks are cleaned when loaded. Tracked gravity blocks cannot fall, and pistons cannot move tracked blocks.

Tracks player block place/break events, not bucket liquids, entities, explosions, growth/spread, or collateral changes caused by physics. Other plugins that replace a block directly with the same material may still be affected by an old timer. This is not a complete region rollback system.

Build: mvn verify in this directory. Five tests check denied bypass, block state matching, original break restoration, temporary addition breaking, and replacement restoration target preservation. No live Paper server test performed.

Command autocomplete added in 1.1.1: press Tab after a command/subcommand to see prefix-filtered suggestions. Suggestions respect management permissions. Existing command aliases work. No player-name fallback is returned for unsupported argument positions. Build and 7 tests passed for this module.
