# LegendarySwords 0.2.0 downloads

- [Updated plugin JAR](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-0.2.0.jar)
- [All-in-one download](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-download.zip)
- [Eight-sword resource pack](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-resource-pack.zip)
- [Combined eight-sword + BetterModel resource pack](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-combined-resource-pack.zip)
- [Sixteen BetterModel model files](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-BetterModel-models.zip)
- [Source code ZIP](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-source.zip)

Stop the server, replace the old LegendarySwords plugin JAR with version 0.2.0, and restart. Keep your config and player data. BetterModel integration targets the 3.1 API; BetterModel 3.1 requires Java 25 on the server. The core plugin can run on Java 21 without BetterModel. Building source requires JDK 25+.

This version has eight distinct swords, sustained lasers and fire rings, ice/lightning/earth/wind/lunar effects, in-place ultimate presentation, and sword-specific idle, walk, run, pose, swing, ability, and ultimate clips. The plugin upgrades only known original model files and backs them up; edited server models are preserved.

Run `/ls status` to check the animation integration. Test with `/ls give all`, `/ls charge 100`, `/ls resetcooldowns`, and `/ls pose`. Right click uses an ability; sneak + right click uses an ultimate. Use F5 for third-person body animations. Vanilla first-person arm swings remain standard.

The combined pack above includes the eight-sword resource pack and the freshly uploaded BetterModel `build (12).zip`. Replace the previous combined pack with this ZIP and keep it zipped when installing it in Minecraft. Use it as the single server resource pack if your host accepts only one pack.

After future server model changes, run `/bm reload` and download the NEW `plugins/BetterModel/build.zip`. Merge that regenerated pack with the eight-sword resource pack using `scripts/merge_resource_packs.py` in the source ZIP, or send the freshly generated ZIP in the chat. Installing the item pack alone does not supply BetterModel's generated animation assets.

Validation: 55 Java tests, 10 sword-pack checks, and 5 BetterModel asset checks passed. The integration compiles against the real BetterModel 3.1 API. Actual installed-snapshot behavior, client rendering, and multiplayer protections require the live-server checks in docs/TESTING.md.

Standalone eight-sword resource-pack SHA-1: `28ddba9fa143f9e313b339a9b936e5518f6525aa`

Combined eight-sword + BetterModel resource-pack SHA-1: `579247218954fc3a8d66f4beefe438e47aba7065`

All-in-one bundle SHA-256: `f57901bbafc99d862e4f98c45e0d875e2c675bf49023c8811d1150b98f0ab15d`
