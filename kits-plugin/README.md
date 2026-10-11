# LegendaryKits 1.1.0

Standalone Paper 1.21.11 plugin, Java 21+. No dependency on PlayerKits2.

## Install
Stop the server, place LegendaryKits-1.1.0.jar in plugins, restart.
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

Build: mvn verify in this directory. Eight tests verify inventory insertion/failure isolation, spawn top-ups/equipment preservation, command permissions, and menu capacity.
A live Paper server/client GUI test has not been performed. This implements kit menus/editing, not every PlayerKits2 feature.

## Default spawn kit
/kits default <name> sets the kit granted on every join and respawn; /kits default none disables it.
Both require legendarykits.admin. The editor also has a Set default spawn kit button.
Save edited contents first; setting a default uses the saved kit.
The default selection persists in kits.yml. No automatic kit is selected on upgrade.
Spawn grants intentionally bypass claim cooldowns and kit permissions, since an admin selects the server-wide default.
Missing kit items are replenished, rather than multiplying the kit on each login or keep-inventory respawn.
Armor/offhand equip into empty slots; existing gear and unrelated items are preserved.
Identical gear already in storage is moved to an empty equipment slot. If storage cannot fit the full grant, nothing changes.
The contents editor remains six rows to support all 41 player inventory/equipment slots and its controls.

Upgrade: stop server, replace LegendaryKits-1.0.0.jar with LegendaryKits-1.1.0.jar, keep the LegendaryKits data folder, restart.
