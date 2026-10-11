# SwordFoScoreboard 1.0.1

Paper 1.21.11, Java 21+. Standalone UI plugin. No PlaceholderAPI or resource pack needed.
Displays swordfo.minehut.gg by default. Change ip in config.yml to edit.
Place JAR in plugins while server is stopped, then restart.

Optional live data: KillTokens 1.0.2 supplies wallet/combat values; LegendaryKoTH 1.0.2 supplies event status. Included in SwordFo-displays.zip. Replace old versions of those two plugins and keep data folders. Missing/older wallet integration shows —; absent KoTH shows Inactive.
Placeholders: {player}, {wallet}, {combat}, {koth}, {kills}, {deaths}, {online}, {maxplayers}, {ping}, {ip}. & color codes supported.
No player data stored by the UI plugins. WorldGuard is needed only for LegendaryKoTH.

No live Minecraft/client test performed.

A compact gold sidebar with player name, wallet, kills/deaths, combat, KoTH and online count. No red score numbers. Blank lines have unique invisible entries. Updates text in existing teams instead of recreating the objective on each refresh.
/swordboard toggles the sidebar for the current session.
/swordboard reload reloads config (requires swordfoscoreboard.admin; operators by default).
Config: plugins/SwordFoScoreboard/config.yml, title, lines (max 15), refresh-ticks (10–200, default 20), ip.
The sidebar takes over the player scoreboard while shown, and restores the previous scoreboard when hidden/disabled if it still owns it. keep-visible defaults to true and reattaches the sidebar when another plugin or a world change replaces it. Set keep-visible: false to leave another provider in control. Use one sidebar provider for predictable display. It does not copy other plugins' scoreboard teams.
Build: mvn verify. Three tests cover unique entries, formatting and reload permission.

Command autocomplete added in 1.0.1: press Tab after a command/subcommand to see prefix-filtered suggestions. Suggestions respect management permissions. Existing command aliases work. No player-name fallback is returned for unsupported argument positions. Build and 7 tests passed for this module.

Refresh runs every second by default. Player update failures are isolated so one exception cannot stop all scoreboards; console errors are throttled. Live provider data is read again each refresh, and changed rows update their existing teams. Regression tests cover fresh wallet/combat/KoTH provider reads and wallet-row updates. Wallet means BANKED tokens: collected kill drops appear after /deposit. Replace KillTokens and LegendaryKoTH with the included matching versions.
