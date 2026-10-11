# LegendaryKits 1.2.2

Standalone Paper 1.21.11 plugin, Java 21+. No dependency on PlayerKits2.

## Install
Stop the server, place LegendaryKits-1.2.2.jar in plugins, restart.
If another plugin owns /kits, use /legendarykits or /legendarykits:kits.
A sample starter kit is created on first launch (one-hour cooldown).

## Players
/kits opens a single 27-slot menu (three rows), with gold title and kit names. No pagination buttons. New kit creation is capped at 27; older files with more kits retain their data, with excess kits claimable by name.
Left-click a kit to claim, or /kit <name>.
legendarykits.use is granted by default. A kit may additionally require its own permission.
Cooldowns are per player/kit and persist across restarts. Admins obey claim cooldowns too.
Full inventories receive nothing and consume no cooldown. Armor and offhand equip only into empty slots; otherwise they go into storage.

## Admins
legendarykits.admin defaults to operators. Permissions are rechecked for editor interactions.
/kits create <name> copies your current inventory, armor and offhand without consuming them.
Use names of 1–32 lowercase letters, numbers, underscores or dashes.
Right-click the kit icon to open the contents editor. Normal players cannot edit.
First four rows: storage items. Fifth row: boots, leggings, chestplate, helmet, offhand (first five slots).
Click Save to save contents; Cancel or closing the menu discards unsaved contents.
Use ordinary clicks/drags to arrange items; shift transfers and double-click collection are blocked in the editor.
The editor is an admin creative tool: its copied items can be taken by admins.
Cooldown button: left adds 60 seconds, right subtracts 60, shift sets zero.
Icon button: click with an item on your cursor to copy its appearance (does not consume cursor item).
Delete button: click twice to confirm deletion.
Cooldown/icon changes save immediately, independently of the contents Save/Cancel buttons.
/kits cooldown <name> <seconds> sets an exact cooldown (0–31536000).
/kits permission <name> <node> restricts claiming; /kits permission <name> none makes it public.
Custom item metadata, enchantments and persistent data are preserved using Bukkit item serialization.
Kits and claim times live in plugins/LegendaryKits/kits.yml. No resource pack is included.

Build: mvn verify in this directory. Fourteen tests verify inventory insertion/failure isolation, spawn top-ups/equipment preservation, command permissions, and menu capacity.
A live Paper server/client GUI test has not been performed. This implements kit menus/editing, not every PlayerKits2 feature.

## Default spawn kit
/kits default <name> sets the kit granted on every join and respawn; /kits default none disables it.
Both require legendarykits.admin. The editor also has a Set default spawn kit button.
Save edited contents first; setting a default uses the saved kit.
The default selection persists in kits.yml. No automatic kit is selected on upgrade.
Spawn grants intentionally bypass claim cooldowns and kit permissions, since an admin selects the server-wide default.
Default spawn grants reset kit items to the exact saved slot layout and amounts, including separate stacks and deliberately oversized saved stacks. Existing items matching the kit by material and metadata are replaced, including extra copies, rather than accumulating on each join or keep-inventory respawn.
Saved kit armor/offhand equip into their exact slots. Unrelated inventory items or equipment are preserved in their existing slots when free, otherwise moved to unused storage slots.
Saved kit slots are never used to pack displaced items. If there is insufficient room to preserve unrelated items, the grant is skipped without changing the inventory. Normal manual kit claims retain their additive, stack-size-aware behavior.
The contents editor remains six rows to support all 41 player inventory/equipment slots and its controls.

Upgrade: stop server, replace LegendaryKits-1.0.0.jar with LegendaryKits-1.2.2.jar, keep the LegendaryKits data folder, restart.

## Kit menu positions
/kits slot <name> <1–27> requires legendarykits.admin and persists the position.
Slots are counted left to right: first row 1–9, second 10–18, third 19–27. Example: /kits slot starter 14 centers the starter kit.
Moving to an occupied position swaps the two kits, preserving both icons. Empty positions stay blank.
The editor includes a Menu slot button showing its current position and the command.
Existing kits automatically receive available slots when upgraded. kits.yml stores zero-based slot numbers (0–26); the command uses 1–27.

Command autocomplete added in 1.2.2: press Tab after a command/subcommand to see prefix-filtered suggestions. Suggestions respect management permissions. Existing command aliases work. No player-name fallback is returned for unsupported argument positions. Build and 17 tests passed for this module.
