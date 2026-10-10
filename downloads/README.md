# LegendarySwords 0.2.3 downloads

- [0.2.3 hotfix plugin JAR](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-0.2.3.jar)
- [All-in-one download](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-download.zip)
- [Eight-sword resource pack](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-resource-pack.zip)
- [Existing 0.2.2 combined eight-sword + BetterModel resource pack](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-combined-resource-pack-0.2.2.zip)
- [Sixteen BetterModel model files](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-BetterModel-models.zip)
- [Source code ZIP](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-source.zip)

Stop the server, replace the old LegendarySwords JAR with **0.2.3**, and restart. Keep your configuration and player data. Leave `bettermodel.enabled: true` and `bettermodel.player-body-animations: false`. Existing configurations without the new body-animation key use false; if yours explicitly sets true, change it to false before restarting.

This hotfix responds to a reported Minecraft 1.21.11 `clientbound/minecraft:set_equipment` decoder disconnect with 557 unread bytes on Paper 1.21.11 and BetterModel `3.1-SNAPSHOT-526`. It copies item metadata before replacing inventory items and supplies BetterModel with cached item snapshots captured on the server thread. The exact packet cause has not been proven, and no live-client test has been performed.

**Custom player body animations are temporarily paused by default.** Standing, walking, running, pose, and player action body clips are disabled to avoid player-attached BetterModel trackers and their equipment/disguise path. Sword textures, all eight swords, combat effects, abilities, BetterModel slash models, and ultimate presentation remain available. `/ls status` reports the pause. Only re-enable `bettermodel.player-body-animations: true` for a controlled test after collecting equipment-packet evidence; restart after changing it and use [the real-server checklist](../docs/TESTING.md).

**Keep the 0.2.2 combined pack if you already use it.** Version 0.2.3 changes no resource-pack assets, model geometry, textures, or slash assets. The 0.2.2 carry/pose/action clips remain bundled unchanged. A combined pack included in the 0.2.3 bundle under a 0.2.3 filename is a byte-identical alias of the existing 0.2.2 ZIP, not a new pack. No client pack replacement or hash change is required for a 0.2.2 setup.

If upgrading from 0.2.0 or 0.2.1, install the 0.2.2 combined pack linked above because earlier packs lack its item selectors. Known original old player models update automatically with backups when the server starts; edited files are preserved. Back up your edits before manually replacing files from `LegendarySwords-BetterModel-models.zip`.

BetterModel integration targets the 3.1 API and requires **Java 25 on the server**. Core combat without BetterModel can run on Java 21. Building source requires JDK 25+. The combined pack contains compatible BetterModel assets reused from `build (12).zip` plus the eight-sword resource pack. Keep it zipped in Minecraft, or use it as the single hosted server pack.

Test with `/ls give all`, `/ls status`, `/ls charge 100`, and `/ls resetcooldowns`. Right-click uses an ability; sneak + right-click uses an ultimate. With the default mitigation, `/ls pose` and custom player body movement/action animations remain paused. Vanilla first-person arm swings stay standard.

Regenerate `plugins/BetterModel/build.zip` and merge it with the sword pack only if other BetterModel models or generated-client configuration changed. The 0.2.3 hotfix itself requires no new `build.zip`; see [the BetterModel guide](../docs/BETTERMODEL.md).

Validation: `mvn verify` passed **101 Java tests** for 0.2.3, with no failures, errors, or skipped tests. The unchanged 0.2.2 assets previously passed **13 resource-pack tests** and **9 BetterModel asset tests**, including 47 carry/pose/action clearance combinations; those earlier results are not a new 0.2.3 runtime test. The release archives passed CRC checks; all sixteen `.bbmodel` files and the embedded sword pack match 0.2.2 byte for byte. The bundled combined-pack alias is also byte-identical to 0.2.2.. No live-client test has been performed. The reported equipment-packet disconnect, installed-snapshot behavior, and client rendering still need validation on the actual server.

Standalone eight-sword resource-pack SHA-1: `fccfab27dd8b69005de2ec7eb0d6123363f4dadb`.

Combined eight-sword + BetterModel resource-pack SHA-1: `04f9980f0cb83a6da16bea5f33c3a509fbf31548`.

All-in-one bundle SHA-256: `c39145124319ce022d39d6a16d307525b4dfb60d49d475a770ba9a14c85ea35e`.
