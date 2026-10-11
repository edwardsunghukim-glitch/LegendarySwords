# Combat update 1.1.0

Successful player melee/projectile hits tag both attacker and victim. Throwing an ender pearl also starts/refreshes the configured combat timer (20 seconds by default). Quitting, disconnecting or being kicked while tagged kills the player. Canceled hits and zero damage do not tag. `killtokens.combat-bypass` bypasses tagging and defaults to nobody.

With WorldGuard 7 + WorldEdit installed, combat-tagged players cannot enter region `spawn` by movement or teleport, including pearl teleports. Players tagged while already inside may leave. A nearby red glass outline is sent only to the tagged player's client; real blocks stay unchanged. Both polygon and cuboid regions are supported. The barrier only covers the region's configured height. Missing WorldGuard disables the barrier with a startup warning; other combat rules continue working.

Config: `combat.kill-on-logout`, `combat.pearl-tag`, `combat.duration-seconds`, `combat.spawn.enabled`, `combat.spawn.region`, `combat.spawn.wall`. `/killtokens reload` applies changes. On the first upgrade to 1.1.0, combat logout death, pearl tagging and spawn protection are enabled automatically, region set to `spawn`, and `combat.rules-version: 2` saved; later config edits are preserved. Ordinary server shutdown/plugin disable is not treated as a combat logout.

# KillTokens 1.0.2

Standalone Paper 1.21.11 plugin, Java 21+. No Vault or economy plugin required.
Stop server, upload KillTokens-1.0.2.jar to plugins, restart.

## Tokens and kills
Each player kill drops one gold-named sunflower KillToken at the defeated player by default. It is a normal collectible ground drop, not automatically banked. Projectile kills count when Bukkit credits the killer. KillTokens are identified by a persistent plugin marker, not just the display name; renamed ordinary sunflowers do not count. KillTokens cannot be placed or crafted into dye, including automatic crafters.
Ground KillTokens follow ordinary despawn and ClearDrops cleanup behavior; anyone may pick them up. Physical tokens in a player's inventory follow the server's death/drop rules.

## Player commands
/wallet — banked KillToken balance and active global multiplier.
/deposit — deposit all marked KillTokens from your entire inventory, including offhand (not items on your cursor).
/deposit <amount> or /deposit all — deposit a specific amount or all.
/withdraw <amount> — debit wallet, put tokens in inventory, drop overflow at your feet.
/withdraw defaults to one token. /withdraw all withdraws up to the configured per-transaction maximum.
/combat — seconds remaining on your combat tag.
killtokens.use defaults to everyone.
If another plugin owns a command, use /killtokens:wallet, /killtokens:deposit, etc.

## Admin commands
/killtokengive <player> <amount> — ADD to the player's wallet, not physical items.
/killtokenset <player> <amount> — SET wallet exactly; zero allowed.
/killtoken give <player> <amount> and /killtoken set <player> <amount> also work.
Known cached players can be managed offline; unknown players must first join.
/killtoken multiplier 2 30 — activate 2x kill drops for 30 minutes.
/killtoken multiplier 2 — activate 2x without a time limit.
/killtoken multiplier off — deactivate the timed/global admin multiplier.
/killtoken reload — reload config without resetting balances or active multiplier.
Aliases /killtokens, /kt and /tokens also work.
Admin commands require killtokens.admin (operators by default).

## Config
plugins/KillTokens/config.yml controls:
- rewards.tokens-per-kill: 1
- rewards.base-multiplier: 1.0
- rewards.permission-multipliers: {} (e.g. literal YAML permission key 'killtokens.vip': 2.0; highest matching factor wins)
- rewards.max-tokens-per-kill: 2304 (hard maximum 2304)
- rewards.same-victim-cooldown-seconds: 0 (optional anti-farming delay; in-memory, resets on restart)
- rewards.excluded-worlds: []
- token.name: '&6&lKillToken' and token.lore (standard & color codes)
- banking.max-per-transaction: 2304 (withdrawals, range 1–2304; deposits can bank the whole inventory)
- combat.enabled: true
- combat.duration-seconds: 20 (range 1–3600)
- combat.action-bar: true (disable if another plugin's action bar should take priority)
- combat.kill-on-logout: false (optional punishment: kill players who disconnect while tagged)
- combat.blocked-commands: [spawn, home, homes, tpa, tpaccept, warp, warps, fly]
Both attacker and victim receive a refreshed tag for successful melee/projectile PvP damage. Namespaced forms of listed commands are blocked too. It is a command blocker, not a universal teleport detector. Extra command aliases must be listed explicitly.
killtokens.combat-bypass defaults to nobody; grant explicitly to exempt a player from tags.
Tag timers reset on restart. Death clears the victim's tag. Disabling combat and reloading clears active tags.
Kill drops = floor(base reward × baseline multiplier × active admin multiplier × highest permission multiplier), capped by max-tokens-per-kill. Multipliers affect new kill drops only, never deposits, withdrawals or wallet admin commands. Factors support 1–100 via command, with durations up to 10080 minutes (0 means indefinite).

## Storage and verification
Wallets and admin multiplier persist in plugins/KillTokens/balances.yml via temporary-file replacement. Failed writes roll back the balance and leave inventory unchanged. Invalid wallet YAML disables the plugin instead of overwriting balances with empty accounts. Orderly restarts preserve banked tokens; physical player inventories are managed by Minecraft. A process crash between bank persistence and inventory update is not a distributed transaction across Minecraft player data.
Build: mvn verify in this directory. 16 tests verify inventory-wide deposits, insufficient deposits, overflow accounting, safe full-inventory reward planning, balance persistence/limits/write failure, multiplier expiry, permissions, namespaced commands, reward factors, and token placement protection.
No live Paper/client test performed.

Version 1.0.1 adds read-only display API methods for the SwordFo scoreboard/tab plugins. Existing gameplay and config remain compatible. Replace old JAR, keep data folder.

Command autocomplete added in 1.0.2: press Tab after a command/subcommand to see prefix-filtered suggestions. Suggestions respect management permissions. Existing command aliases work. No player-name fallback is returned for unsupported argument positions. Build and 20 tests passed for this module.
