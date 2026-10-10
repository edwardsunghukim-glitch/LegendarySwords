# LegendarySwords 0.2.2 downloads

- [Updated plugin JAR](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-0.2.2.jar)
- [All-in-one download](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-download.zip)
- [Eight-sword resource pack](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-resource-pack.zip)
- [Updated 0.2.2 combined eight-sword + BetterModel resource pack](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-combined-resource-pack-0.2.2.zip)
- [Sixteen BetterModel model files](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-BetterModel-models.zip)
- [Source code ZIP](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-source.zip)

Stop the server, replace the old LegendarySwords plugin JAR with version 0.2.2, and restart. Keep your config and player data. BetterModel integration targets the 3.1 API; BetterModel 3.1 requires Java 25 on the server. The core plugin can run on Java 21 without BetterModel. Building source requires JDK 25+.

The 0.2.2 patch adjusts all eight swords' idle, walk, run, and pose keyframes to reduce arm/blade clipping, with forward braces for heavy swords. It also updates swing, ability, and ultimate clips for Master Sword, Fierce Deity Sword, Thunderblade, Eightfold Blade, and Biggoron Sword. Original 0.2.0/0.2.1 player files update automatically with backups when the server starts. If edited files are preserved, back them up before manually replacing them from the updated sixteen-model ZIP.

The all-in-one bundle includes the matching combined pack and checksum.

**Install both the new JAR and updated combined pack.** The new item selector keeps the selected hotbar sword and first-person item visible while only the animated grip renders in third person. Ending the tracker restores the base item model. Earlier combined packs lack this selector.

This version has eight distinct swords, sustained lasers and fire rings, ice/lightning/earth/wind/lunar effects, in-place ultimate presentation, and sword-specific idle, walk, run, pose, swing, ability, and ultimate clips. The plugin upgrades only known original model files and backs them up; edited server models are preserved.

Run `/ls status` to check the animation integration. Test with `/ls give all`, `/ls charge 100`, `/ls resetcooldowns`, and `/ls pose`. Right click uses an ability; sneak + right click uses an ultimate. Use F5 for third-person body animations. Vanilla first-person arm swings remain standard.

The combined pack above includes the updated eight-sword resource pack and compatible BetterModel assets reused from `build (12).zip`; body geometry, textures, slash effects, names, and model IDs are unchanged. Replace the previous combined pack with this ZIP and keep it zipped when installing it in Minecraft. Use it as the single server resource pack if your host accepts only one pack. The generic combined-pack URL also points to this updated ZIP.

After future changes to generated client assets, run `/bm reload` and download the NEW `plugins/BetterModel/build.zip`. Merge that regenerated pack with the eight-sword resource pack using `scripts/merge_resource_packs.py` in the source ZIP, or send the freshly generated ZIP in the chat. Installing the item pack alone does not supply BetterModel's generated animation assets.

Validation: `mvn verify` passed 86 Java tests; resource-pack checks passed 13 tests; BetterModel asset checks passed 9 tests, including 47 carry/pose/action clearance combinations. The combined pack passed CRC, 5,855 JSON files and 6,074 custom reference checks, preserving 6,149 BetterModel assets unchanged. No live-client test has been performed. Client rendering, installed-snapshot behavior, and remaining transition clipping require the checks in docs/TESTING.md.

Standalone eight-sword resource-pack SHA-1: `fccfab27dd8b69005de2ec7eb0d6123363f4dadb`.

Combined eight-sword + BetterModel resource-pack SHA-1: `04f9980f0cb83a6da16bea5f33c3a509fbf31548`.

All-in-one bundle SHA-256: `5dc956cfff7f91b80243c0b896122cfe1ff49a3146b6f6d86c330b056f0d65a9`.
