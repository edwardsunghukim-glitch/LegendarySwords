#!/usr/bin/env python3
"""Meaningful regression checks for resource pack validation and packaging."""

import hashlib
import json
from pathlib import Path
import shutil
import tempfile
import unittest
import zipfile

from build_resource_pack import DEFAULT_PACK, ITEMS, PackValidationError, build, read_png, validate


class ResourcePackTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="legendaryswords-pack-test-")
        self.root = Path(self.temp.name)
        self.pack = self.root / "pack"
        shutil.copytree(DEFAULT_PACK, self.pack)

    def tearDown(self):
        self.temp.cleanup()

    def test_all_swords_validate(self):
        self.assertEqual(validate(self.pack)["items"], len(ITEMS))

    def test_animated_items_keep_inventory_first_person_and_fixed_visible(self):
        for name in ITEMS:
            item = json.loads((self.pack / "assets/legendaryswords/items" / (name + "_animated.json")).read_text())["model"]
            self.assertEqual(item["property"], "minecraft:display_context")
            hidden = {context for case in item["cases"] if case["model"]["type"] == "minecraft:empty"
                      for context in case["when"]}
            self.assertEqual(hidden, {"thirdperson_lefthand", "thirdperson_righthand"})
            for context in ("gui", "firstperson_lefthand", "firstperson_righthand", "ground", "fixed"):
                with self.subTest(sword=name, context=context):
                    self.assertNotIn(context, hidden)
                    self.assertEqual(item["fallback"]["type"], "minecraft:model")
                    self.assertTrue((self.pack / "assets/legendaryswords/models/item" / (name + "_animated.json")).is_file())

    def test_animated_inventory_hiding_is_rejected(self):
        path = self.pack / "assets/legendaryswords/items/master_sword_animated.json"
        data = json.loads(path.read_text())
        data["model"]["cases"][0]["when"].append("gui")
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(PackValidationError, "hide only vanilla third-person"):
            validate(self.pack)

    def test_mismatched_fixed_hand_transform_is_rejected(self):
        path = self.pack / "assets/legendaryswords/models/item/master_sword_animated.json"
        data = json.loads(path.read_text())
        data["display"]["fixed"]["rotation"][0] += 90
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(PackValidationError, "authored hand transforms"):
            validate(self.pack)

    def test_swords_have_distinct_geometry_and_high_resolution_energy(self):
        signatures = set()
        for name in ITEMS:
            model = json.loads((self.pack / "assets/legendaryswords/models/item" / (name + ".json")).read_text())
            self.assertLessEqual(len(model["elements"]), 80)
            self.assertGreater(len(model["elements"]), 35)
            # Distinct silhouettes must remain more than differently colored copies.
            signature = [(element["from"], element["to"], element.get("rotation"))
                         for element in model["elements"]]
            signatures.add(json.dumps(signature, sort_keys=True))
            texture = self.pack / "assets/legendaryswords/textures/item" / (name + ".png")
            self.assertEqual(read_png(texture), (128, 512))
            self.assertTrue(texture.with_suffix(".png.mcmeta").is_file())
            self.assertTrue(texture.with_name(name + "_e.png").is_file())
        self.assertEqual(len(signatures), len(ITEMS))

    def test_unbounded_model_geometry_is_rejected(self):
        path = self.pack / "assets/legendaryswords/models/item/master_sword.json"
        data = json.loads(path.read_text())
        data["elements"] = data["elements"] * 2
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(PackValidationError, "80-cube budget"):
            validate(self.pack)

    def test_missing_texture_is_rejected(self):
        (self.pack / "assets/legendaryswords/textures/item/master_sword.png").unlink()
        with self.assertRaisesRegex(PackValidationError, "[Mm]issing texture|no base texture"):
            validate(self.pack)

    def test_corrupt_png_is_rejected(self):
        path = self.pack / "assets/legendaryswords/textures/item/master_sword.png"
        data = bytearray(path.read_bytes())
        data[42] ^= 1
        path.write_bytes(data)
        with self.assertRaisesRegex(PackValidationError, "checksum mismatch"):
            validate(self.pack)

    def test_missing_modern_item_model_is_rejected(self):
        path = self.pack / "assets/legendaryswords/items/master_sword.json"
        path.write_text(json.dumps({"model": {"type": "minecraft:model", "model": "legendaryswords:item/missing"}}))
        with self.assertRaisesRegex(PackValidationError, "missing model"):
            validate(self.pack)

    def test_bad_animation_frame_is_rejected(self):
        path = self.pack / "assets/legendaryswords/textures/item/flameblade.png.mcmeta"
        data = json.loads(path.read_text())
        data["animation"]["frames"] = [4]
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(PackValidationError, "out of range"):
            validate(self.pack)

    def test_wrong_client_format_is_rejected(self):
        path = self.pack / "pack.mcmeta"
        data = json.loads(path.read_text())
        data["pack"]["min_format"] = [69, 0]
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(PackValidationError, "format 75.0"):
            validate(self.pack)

    def test_emissive_animation_stays_synchronized(self):
        path = self.pack / "assets/legendaryswords/textures/item/flameblade_e.png.mcmeta"
        data = json.loads(path.read_text())
        data["animation"]["frametime"] = 6
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(PackValidationError, "animation differs"):
            validate(self.pack)

    def test_packaging_is_deterministic_and_contains_root_metadata(self):
        first, checksum, _ = build(self.pack, self.root / "one.zip", self.root / "bundled.zip")
        second, checksum2, _ = build(self.pack, self.root / "two.zip", self.root / "bundled.zip")
        self.assertEqual(first.read_bytes(), second.read_bytes())
        self.assertEqual(checksum, checksum2)
        self.assertEqual(checksum, hashlib.sha1(first.read_bytes()).hexdigest())
        self.assertEqual(second.read_bytes(), (self.root / "bundled.zip").read_bytes())
        with zipfile.ZipFile(first) as archive:
            self.assertIn("pack.mcmeta", archive.namelist())
            self.assertNotIn("README.md", archive.namelist())
            for name in ITEMS:
                self.assertIn("assets/legendaryswords/items/" + name + ".json", archive.namelist())
                self.assertIn("assets/legendaryswords/items/" + name + "_animated.json", archive.namelist())
            self.assertIsNone(archive.testzip())


if __name__ == "__main__":
    unittest.main()
