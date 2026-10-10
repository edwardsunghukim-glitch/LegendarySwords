LegendarySwords animation assets

The three players/*.bbmodel files are generic Blockbench 5 models containing
original sword-specific swing, ability, and ultimate keyframes. Their player
limb geometry/skin UV template comes from BetterModel 1.15.2's steve.bbmodel:
https://github.com/toxicity188/BetterModel/blob/1.15.2/core/src/main/resources/steve.bbmodel
The template is covered by the included LICENSE-BetterModel.txt.

The models/*_slash.bbmodel files contain original luminous crescent geometry,
embedded textures, and animation keyframes. No external model download is needed.

When BetterModel is present, LegendarySwords copies files into the engine's
players/ and models/ folders only when their destination is absent. Existing
models are preserved, including any changes made by a server owner in Blockbench.
Names are prefixed with legendaryswords_ to avoid replacing other plugin assets.

Supported engine API: io.github.toxicity188:bettermodel:1.15.2 (provided).
Enable module.player-animation and module.model in BetterModel/config.yml.
The normal startup import creates the pack; /bm reload regenerates it after
editing a model. The engine's default generated file is plugins/BetterModel/build.zip.
Merge that pack with the LegendarySwords sword pack before distributing it.

Body animations are visible in third person and to other players. Cinematic
heroes are detached skin models anchored at the cast location. A server plugin
and an ordinary resource pack cannot replace vanilla first-person arm animation.
Energy displays use maximum display brightness; they do not emit world lighting.

Player body, cinematic hero, and slash trackers are closed when playback ends.
A watchdog also bounds their lifetimes. Quit, death, world change, and plugin
shutdown close only the trackers created by this integration.
