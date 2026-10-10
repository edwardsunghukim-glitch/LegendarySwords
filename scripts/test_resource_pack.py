#!/usr/bin/env python3
"""Meaningful regression checks for resource pack validation and packaging."""

import hashlib
import json
from pathlib import Path
import shutil
import tempfile
import unittest
import zipfile

from build_resource_pack import DEFAULT_PACK, PackValidationError, build, validate


class ResourcePackTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="legendaryswords-pack-test-")
        self.root = Path(self.temp.name)
        self.pack = self.root / "pack"
        shutil.copytree(DEFAULT_PACK, self.pack)

    def tearDown(self):
        self.temp.cleanup()

    def test_all_swords_validate(self):
        self.assertEqual(validate(self.pack)["items"], 3)

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
            self.assertIn("assets/legendaryswords/items/fierce_deity_sword.json", archive.namelist())
            self.assertIsNone(archive.testzip())


if __name__ == "__main__":
    unittest.main()
