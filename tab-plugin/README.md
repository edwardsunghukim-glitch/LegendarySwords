# SwordFoTab 1.0.1

Paper 1.21.11, Java 21+. Standalone UI plugin. No PlaceholderAPI or resource pack needed.
Displays swordfo.minehut.gg by default. Change ip in config.yml to edit.
Place JAR in plugins while server is stopped, then restart.

Optional live data: KillTokens 1.0.2 supplies wallet/combat values; LegendaryKoTH 1.0.2 supplies event status. Included in SwordFo-displays.zip. Replace old versions of those two plugins and keep data folders. Missing/older wallet integration shows —; absent KoTH shows Inactive.
Placeholders: {player}, {wallet}, {combat}, {koth}, {kills}, {deaths}, {online}, {maxplayers}, {ping}, {ip}. & color codes supported.
No player data stored by the UI plugins. WorldGuard is needed only for LegendaryKoTH.

No live Minecraft/client test performed.

Gold SwordFo header, online count, wallet/ping footer and server IP. Standard player names stay unchanged; no fake player rows or scoreboard teams.
/swordtab reload reloads config (requires swordfotab.admin; operators by default).
Config: plugins/SwordFoTab/config.yml, header/footer lists, refresh-ticks (10–200, default 40), ip.
This plugin manages tab header/footer. Use one header/footer provider. The header/footer are cleared on disable.
Build: mvn verify. Three tests cover multiline placeholders, colors and reload permission.

Command autocomplete added in 1.0.1: press Tab after a command/subcommand to see prefix-filtered suggestions. Suggestions respect management permissions. Existing command aliases work. No player-name fallback is returned for unsupported argument positions. Build and 5 tests passed for this module.
