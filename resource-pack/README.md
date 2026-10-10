# LegendarySwords resource pack

This pack adds original voxel artwork for three Zelda sword names. The geometry,
pixel textures and icon were drawn for this project, rather than extracted from
Nintendo games. Models include a purple winged hilt and cyan inlay for the Master
Sword, animated molten cracks for the Flameblade, and an open double-helix blade
with animated lunar energy for the Fierce Deity Sword. Raised sacred crests,
filigree, grip wraps, jeweled pommels, rivets and carved blade channels add real
geometry rather than relying only on flat textures.

The target is **Minecraft Java 1.21.11**, resource pack format **75.0**. The pack
uses modern item definitions, not overrides of every vanilla diamond sword.

| Paper item model key | Item definition | Geometry |
| --- | --- | --- |
| `legendaryswords:master_sword` | `assets/legendaryswords/items/master_sword.json` | `models/item/master_sword.json` |
| `legendaryswords:flameblade` | `assets/legendaryswords/items/flameblade.json` | `models/item/flameblade.json` |
| `legendaryswords:fierce_deity_sword` | `assets/legendaryswords/items/fierce_deity_sword.json` | `models/item/fierce_deity_sword.json` |

The plugin sets `ItemMeta.setItemModel` to these keys. Texture animation metadata
defines four 64×64 frames for the Flameblade and Fierce Deity Sword. The Master
Sword uses a static 64×64 atlas. All models have custom held, inventory, dropped
item and item-frame transforms.

## Build and check

The normal Maven build packages the committed artwork; Python is optional.
For deeper validation or a reproducible standalone pack build, run from the
repository root:

```sh
python3 scripts/build_resource_pack.py --validate-only
python3 scripts/test_resource_pack.py
python3 scripts/build_resource_pack.py
```

The builder checks modern item definitions, geometry, resource references, PNG
checksums and animation frames. It creates:

- `target/LegendarySwords-resource-pack.zip`
- `target/LegendarySwords-resource-pack.sha1`, containing the ZIP's SHA-1
- `target/generated-resources/resource-pack.zip`, for bundling in the plugin JAR

The root of the ZIP contains `pack.mcmeta` and `assets/`. Documentation is
excluded. ZIP entries have fixed timestamps so unchanged assets produce the
same bytes and SHA-1 when built with the Python builder.

To edit the art, change the static JSON/PNG files with Blockbench or another
resource pack editor. Alternatively, change the authoring code and explicitly
run `python3 scripts/generate_resource_pack_assets.py`. That command regenerates
and overwrites the three original sword models, textures and the pack icon;
normal builds never run it.

![Actual original voxel item models](preview.png)

The preview shows the source geometry and base textures under simple lighting;
it is not a Minecraft screenshot and does not simulate enchantment glint or
shader glow. Regenerate it with `python3 scripts/render_resource_pack_preview.py`.

## Use with BetterModel

BetterModel generates its own resource pack for entity models and animations.
Players need both those assets and these item models. The most dependable setup
is one combined ZIP:

1. Let BetterModel generate its resource pack after installing the supplied
   models and completing a BetterModel reload.
2. Extract the BetterModel ZIP into an empty working directory.
3. Copy `assets/legendaryswords/` from this pack into that directory's `assets/`
   folder. Preserve BetterModel's other asset folders. If a BetterModel model
   uses the `legendaryswords` namespace, merge its subfolders rather than
   replacing the entire namespace.
4. Use the `pack.mcmeta` from this pack for a Minecraft 1.21.11 combined pack.
   If the BetterModel pack declares other top-level metadata such as an
   `overlays` section, preserve those sections and change only its `pack`
   version range and description to match this pack.
5. ZIP the **contents** of the directory. `pack.mcmeta` must be at the ZIP root,
   rather than inside another enclosing folder.
6. Calculate the combined ZIP's SHA-1 and set the plugin's resource pack URL
   and SHA-1 to that combined file. Use a direct HTTPS URL that returns ZIP
   bytes, rather than a webpage or a private download requiring login.
7. Configure your server so this combined file is the active required pack,
   and disable any separate automatic standalone pack offer that would
   replace it. BetterModel configuration names depend on its installed version.

For example, run `sha1sum combined-pack.zip` on Linux/macOS or
`Get-FileHash .\combined-pack.zip -Algorithm SHA1` in PowerShell. Regeneration
of BetterModel assets requires repeating the merge and updating the checksum.
Do not publish the checksum of the unmerged LegendarySwords pack for a merged
ZIP.

Minecraft also supports multiple active server packs when plugins correctly
use the additive resource pack API. That depends on the installed BetterModel
version and pack-offer configuration; a combined pack avoids those integration
assumptions. The LegendarySwords standalone ZIP is suitable for initial tests
of the item artwork, but contains no BetterModel-generated entity models.

## Glow and animation limits

The plugin can add enchantment glint, bright animated colors, particles and
server-driven model animations. Ordinary vanilla item textures still respond
to Minecraft lighting. This pack does not require a shader or client mod and
does not claim a light source illuminating nearby blocks.

Optional `_e.png` overlays and `assets/minecraft/optifine/emissive.properties`
make energy strips, crystals and runes emissive for an **OptiFine-compatible
client that supports and enables emissive textures**. The overlays preserve
the same four-frame animation as their base textures. Vanilla Minecraft
ignores this optional configuration. Bloom and dynamic emitted light still
depend on a suitable client shader/mod; the pack does not install those.

Item models and animated textures work in first person. A server resource pack
cannot replace the vanilla client's first-person arm/swing animation. BetterModel
animations present custom server-side models and effects; they are a different
visual layer from the player's local arm movement.

## Format references

Minecraft 1.21.11 version metadata records resource pack version 75 with minor
version 0. These links expose Mojang-generated assets/metadata as a versioned
mirror, rather than defining a new format:

- [1.21.11 version metadata](https://github.com/misode/mcmeta/blob/1.21.11-summary/version.json)
- [Vanilla modern sword item definition](https://github.com/misode/mcmeta/blob/1.21.11-assets/assets/minecraft/items/diamond_sword.json)
- [Minecraft resource pack documentation](https://minecraft.wiki/w/Resource_pack)

Automated validation verifies pack structure and references. Final scale,
hand positions, glint and texture animation should also be checked by a player
with a Minecraft 1.21.11 client after accepting the pack.
