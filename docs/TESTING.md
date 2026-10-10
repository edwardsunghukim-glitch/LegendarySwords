# Testing 0.3.0

Automated checks cover all 24 IDs, item/resource references, PNG checksums, complete clips, distinct motions/effect shapes, importer execution, held-item transforms and pose clearance. Every new relic is exercised through ability and ultimate damage with per-target hit caps. Shared-scene callbacks remain guarded against duplicate or cancelled attacks. Official BetterModel blueprint export validates the new named effect aliases and textures.

Run `mvn verify`, `python3 scripts/test_resource_pack.py`, and `python3 scripts/test_bettermodel_assets.py`. The Python cast check includes every sword's ability, ultimate and release poses against actual arm/torso/head/item geometry. Model generation must reproduce committed assets.

On the server, install the new JAR and combined pack, verify `/ls status`, and give all 24 swords. Test each ability and charged ultimate near mobs and with an observer. Check sword inventory icons, first-person camera clearance, one observer body, windup speed overrides, release cleanup, and damage under different armor/protection levels. PvP must be enabled deliberately for player damage. Check interruption by death, quit, teleport, world change and restart. Edited model files should be preserved with clear missing-clip diagnostics.

The combined pack targets the previously supplied BetterModel 3.1-SNAPSHOT-526 named-alias setup. Regenerate/merge a fresh server `build.zip` for different export settings or changed unrelated models. Automated blueprint/geometry checks do not prove client rendering, native packet encoding or performance on a live server.
