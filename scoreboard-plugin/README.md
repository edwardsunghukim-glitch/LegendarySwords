# SwordFoScoreboard 1.0.0

Paper 1.21.11, Java 21+. Standalone UI plugin. No PlaceholderAPI or resource pack needed.
Displays swordfo.minehut.gg by default. Change ip in config.yml to edit.
Place JAR in plugins while server is stopped, then restart.

Optional live data: KillTokens 1.0.1 supplies wallet/combat values; LegendaryKoTH 1.0.1 supplies event status. Included in SwordFo-displays.zip. Replace old versions of those two plugins and keep data folders. Missing/older wallet integration shows —; absent KoTH shows Inactive.
Placeholders: {player}, {wallet}, {combat}, {koth}, {kills}, {deaths}, {online}, {maxplayers}, {ping}, {ip}. & color codes supported.
No player data stored by the UI plugins. WorldGuard is needed only for LegendaryKoTH.

No live Minecraft/client test performed.

A compact gold sidebar with player name, wallet, kills/deaths, combat, KoTH and online count. No red score numbers. Blank lines have unique invisible entries. Updates text in existing teams instead of recreating the objective on each refresh.
/swordboard toggles the sidebar for the current session.
/swordboard reload reloads config (requires swordfoscoreboard.admin; operators by default).
Config: plugins/SwordFoScoreboard/config.yml, title, lines (max 15), refresh-ticks (10–200, default 20), ip.
The sidebar takes over the player scoreboard while shown, and restores the previous scoreboard when hidden/disabled if it still owns it. If another plugin takes over later, this plugin does not fight it. Use one sidebar provider for predictable display. It does not copy other plugins' scoreboard teams.
Build: mvn verify. Three tests cover unique entries, formatting and reload permission.
