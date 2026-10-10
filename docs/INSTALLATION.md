# Installing the 50-weapon expansion

Stop the server, remove the old LegendarySwords JAR, upload `LegendarySwords-0.4.0.jar`, and restart. Keep only one LegendarySwords JAR; keep LegendaryAdmin if installed. Preserve your configuration and player data.

The plugin auto-installs 50 player models into `plugins/BetterModel/players` and 50 slash models into `plugins/BetterModel/models`. Known bundled originals are backed up and updated; edited models are preserved. Check `/ls status` after imports. If installing manually, extract `LegendarySwords-BetterModel-models.zip` and copy its `players` and `models` contents to the corresponding BetterModel folders, then `/bm reload`.

Replace your old client pack with `LegendarySwords-combined-resource-pack-0.4.0.zip` in Minecraft Options → Resource Packs → Open Pack Folder. Keep the ZIP compressed and enable it. This combined pack targets the supplied BetterModel 3.1-SNAPSHOT-526 pack with namespace `bettermodel` and named `c/` item aliases; it includes all 50 weapons and the new compiled signature effects.

If your generated BetterModel client assets/settings changed, run `/bm reload`, download `plugins/BetterModel/build.zip`, then combine that fresh build with `LegendarySwords-resource-pack.zip`:

```sh
python3 scripts/merge_resource_packs.py build.zip LegendarySwords-resource-pack.zip combined.zip
```

Automatic server pack delivery uses `resource-pack.url` and its matching SHA-1. Update that URL/hash to the NEW combined pack if you use server delivery. Local installation does not need a server URL. Do not use the standalone sword-pack hash for a combined ZIP.

Open `/swords` or `/legendaryswords` to browse all 50 weapons. Normal players can only browse; admins can click to receive individual weapons. Test `/ls give all` (50 swords), `/ls charge 100`, `/ls cinematics on`, `/ls resetcooldowns`. Right-click for an ability; sneak + right-click for an ultimate. For player damage, `pvp-enabled` and world/protection permissions must allow it. Test with another player: the caster sees world effects; observers also see cast bodies. Ordinary movement stays normal.

Speed/damage tuning and the weapon list are in [README](../README.md). New weapon defaults work with old configurations; add a per-sword section only when you want to override its defaults. `/ls reload` applies damage/cooldown/speed changes; give fresh items for changed base melee attributes.
