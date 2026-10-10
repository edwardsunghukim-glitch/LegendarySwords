# LegendarySwords 0.1.0 download

[Download the complete ZIP](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-download.zip)

The ZIP contains the Paper 1.21.11 plugin, its resource pack, source code, and installation instructions. Extract it on your computer and follow INSTALL-FIRST.txt.

Validation: 30 Java tests and 8 resource-pack checks passed. Live Minecraft server/client testing remains.

SHA-256: `5ff60f9141ee6fc73acb541b10e517c9c4bd7bdf6e09aee6de11c664a3e4e323`

## Combined sword and BetterModel resource pack

[Download one combined resource pack](https://github.com/edwardsunghukim-glitch/LegendarySwords/raw/refs/heads/main/downloads/LegendarySwords-combined-resource-pack.zip)

This Minecraft Java 1.21.11 pack merges the supplied BetterModel server pack with all three sword models and textures. It preserves all 6,085 BetterModel asset files. Keep the ZIP compressed, copy it into Minecraft's Resource Packs folder, and enable it in place of the two individual packs.

For a server accepting one resource-pack URL, use the combined download URL above and its SHA-1 below. Configure the active server pack offer to use this combined file. If the server offers an old standalone pack, update that offer too. BetterModel's generated model assets and this combined ZIP must stay in sync: regenerate and merge again after changing models or animations. `/ls status` reports whether the plugin loaded its animation integration.

SHA-1: `f820ff8990a7df10534e72e489836c2a8c76fe57`

SHA-256: `e3ed142750429542a9b1a11f3b8cf3e80bb79403333f52cbc1f8aec474a2d2f0`

Validation: both input ZIPs and the combined ZIP passed CRC checks; all JSON/metadata files parsed; all copied assets were verified byte-for-byte; all three custom sword item definitions resolve to their models. Live Minecraft rendering and server animation playback remain to be checked.
