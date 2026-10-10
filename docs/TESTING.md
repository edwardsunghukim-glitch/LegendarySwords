# Testing 0.5.1

Asset checks include separate engraving topology for all 55 weapons and connected geometry for the 26 additions. Combat tests also check enlarged reach, actual inward pull and weapon-specific elemental accents.

Second-collection regression checks also cover hollow ring footprints, mine arming delays, visibility protection, caster-death cleanup and bounded hits for all 26 attacks. Run `python3 scripts/test_domain_assets.py` for separate geometry and texture-pattern checks.

Automated checks cover all 55 IDs, item/resource references, PNG checksums, complete clips, distinct motions/effect shapes, importer execution, held-item transforms and pose clearance. Every new relic is exercised through ability and ultimate damage with per-target hit caps. Shared-scene callbacks remain guarded against duplicate or cancelled attacks. Official BetterModel blueprint export validates the new named effect aliases and textures.

Run `mvn verify`, `python3 scripts/test_resource_pack.py`, and `python3 scripts/test_bettermodel_assets.py`. The Python cast check includes every sword's ability, ultimate and release poses against actual arm/torso/head/item geometry. Model generation must reproduce committed assets.

Sword-browser checks cover complete paging, read-only player access, permission revocation, full inventories, shift/number-key/clone/drop/drag prevention and fresh admin grants.

On the server, install the new JAR and combined pack, verify `/ls status`, and give all 55 swords. Test each ability and charged ultimate near mobs and with an observer. Check sword inventory icons, first-person camera clearance, one observer body, windup speed overrides, release cleanup, and damage under different armor/protection levels. PvP must be enabled deliberately for player damage. Check interruption by death, quit, teleport, world change and restart. Edited model files should be preserved with clear missing-clip diagnostics.

The combined pack targets the previously supplied BetterModel 3.1-SNAPSHOT-526 named-alias setup. Regenerate/merge a fresh server `build.zip` for different export settings or changed unrelated models. Automated blueprint/geometry checks do not prove client rendering, native packet encoding or performance on a live server.

Boss checks cover all five models and attacks, owner exclusion and inventory safety, preexisting flight, expiry/cancellation, creative/adventure mode changes, primary/secondary cooldowns and repeated attacks and dragon breath direction. In-game, also verify wing/fin movement, leviathan jaws, dragon breath, hammer/fist impacts, duration overrides and safe landings after flight ends.

0.5.1 validation: all 326 Java tests passed, including official export of 180 named expansion aliases. The boss contract verifies 128×128 independent atlases, valid positive geometry and UV bounds, animated jaws/spines, and root-only transform scaling. The full generator reproduced all 115 committed models without the uploaded reference file. Five boss poses were rendered in Blender for art review.
