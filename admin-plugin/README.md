Direct commands in 0.1.2: `/heal`, `/feed`, `/fly`, `/vanish`, `/invsee`, `/ecsee`, `/kick`, `/ban`, `/unban`, `/mute`, `/freeze`, `/gm` (or `/gamemode`), `/tp`, `/tphere`, `/adminmenu`, `/adminhelp`. The existing `/la` commands still work. Permissions and argument order are unchanged. If another plugin owns a short command, use `/legendaryadmin:heal` (or the equivalent command).

# LegendaryAdmin 0.1.2

A separate admin plugin for Paper **1.21.11**, Java **21 or newer**. No BetterModel, resource pack, EssentialsX or LegendarySwords dependency.

Stop the server, upload `LegendaryAdmin-0.1.2.jar` into `plugins`, then restart. Keep LegendarySwords installed if you want swords too: the two plugins work independently. Do not upload the source ZIP as a plugin.

Run `/la` or `/la menu` in-game to open the paginated player selector. Choose a player to open their admin tools. `/la menu <player>` opens their tools directly. Kicking or banning through the menu requires a second confirmation click. Commands and menu actions check individual permissions; they default to operators.

| Command | Behavior |
| --- | --- |
| `/la heal [player]` | Restores health and extinguishes fire |
| `/la feed [player]` | Restores food and saturation |
| `/la fly [player]` | Toggles admin flight; double-jump to fly |
| `/la gm <survival\|creative\|adventure\|spectator> [player]` | Changes game mode |
| `/la tp <player>` | Teleports you to the player |
| `/la tp <player> <destination>` | Teleports one player to another |
| `/la tphere <player>` | Brings the player to you |
| `/la kick <player> [reason]` | Kicks an online player |
| `/la ban <player> [reason]` | Permanently profile-bans an online player; uses Paper's ban list |
| `/la unban <name\|UUID>` | Unbans a cached name or UUID; use the UUID from `banned-players.json` if a name is unknown |
| `/la mute <player>` | Toggles a public-chat mute; saved across restarts |
| `/la freeze <player>` | Toggles a freeze for the current session |
| `/la vanish [player]` | Hides the player and tab-list entry from viewers without see-vanished permission |
| `/la invsee <player>` | Opens a live editable inventory |
| `/la help` | Shows command help |

Omitting an optional player targets yourself. Console supports commands with explicit player targets; menus and inventory viewing are in-game only. `/la tp <player> <destination>` works from console.

Inventory editing includes storage, armor and offhand. `/ecsee <player>` edits the online player’s ender chest. Left-click swaps/merges stacks; right-click splits stacks or places one item. Changes apply immediately, and open views refresh each tick. Shift-click, drag and number-key transfers into the target inventory are blocked. Permissions are checked on every edit and refresh; views close when the target leaves or permission is removed.

Mutes affect public Paper chat, not private-message commands provided by other plugins. Freezes stop movement, block interactions and direct melee attacks, but allow looking around and administrative teleports. Freeze and vanish reset on disconnect/restart. Vanish does not suppress public join/quit messages or make the player invulnerable. Flight toggling restores the previous flight state; creative and spectator flight remain controlled by their game modes. Moderation actions are logged in the server console.

## Permissions

`legendaryadmin.admin` includes all features and `legendaryadmin.see-vanished`. Every node defaults to operators. With a permissions plugin such as LuckPerms, grant only the nodes a staff role needs:

`legendaryadmin.menu`, `.kick`, `.ban`, `.unban`, `.mute`, `.freeze`, `.heal`, `.feed`, `.fly`, `.gamemode`, `.teleport`, `.vanish`, `.invsee`, `.ecsee`, `.see-vanished` (all have the `legendaryadmin` prefix).

`/la`, `/ladmin` and `/legendaryadmin` are aliases for the same command. Existing `/ban`, `/fly`, `/tp` or EssentialsX commands are not replaced.

## Validation

Build with `mvn verify` in this directory. Tests cover permission denial, game-mode/teleport permission mapping, read-only inventory clicks and drags, freeze movement, vanish permissions and reconnect cleanup, mute persistence and save-failure rollback. No live Minecraft-client test has been performed. After installing, check permissions with a non-operator account and test vanish with another player before using it for moderation.
