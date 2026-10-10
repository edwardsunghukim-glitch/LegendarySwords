#!/usr/bin/env python3
"""Regression checks for actual authored motions, loops and effect assets."""
from __future__ import annotations

import base64
import importlib.util
import json
from pathlib import Path
import struct
import tempfile
import unittest
import zlib

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/bettermodel"
SWORDS = {"master_sword", "flameblade", "fierce_deity_sword", "frostblade",
          "thunderblade", "royal_claymore", "eightfold_blade", "biggoron_sword"}


def load(kind, sword):
    suffix = "_slash" if kind == "models" else ""
    return json.loads((ROOT / kind / f"legendaryswords_{sword}{suffix}.bbmodel").read_text())


def frames(animator, channel="rotation"):
    return [key for key in animator["keyframes"] if key["channel"] == channel]


def values(key):
    return tuple(float(key["data_points"][0][axis]) for axis in "xyz")


def clips(model):
    return {animation["name"]: animation for animation in model["animations"]}


def motion_signature(animation):
    # Ignore UUIDs/names so cosmetic metadata cannot hide identical motions.
    return tuple(sorted((animator["name"], tuple((key["channel"], key["time"], values(key))
                        for key in animator["keyframes"]))
                        for animator in animation["animators"].values()))


class BetterModelAssetsTest(unittest.TestCase):
    def test_all_eight_swords_have_complete_player_and_effect_clips(self):
        self.assertEqual(len(list(ROOT.rglob("*.bbmodel"))), 16)
        for sword in SWORDS:
            for kind, expected in (("players", {"idle", "walk", "run", "pose", "swing", "ability", "ultimate"}),
                                   ("models", {"swing", "ability", "ultimate"})):
                model = load(kind, sword)
                self.assertEqual(model["meta"]["format_version"], "5.0")
                self.assertEqual(set(clips(model)), expected)
                groups = {group["uuid"] for group in model["groups"]}
                for animation in model["animations"]:
                    self.assertGreater(animation["length"], 0)
                    self.assertTrue(animation["animators"])
                    self.assertLessEqual(set(animation["animators"]), groups)
                    for animator in animation["animators"].values():
                        for channel in {key["channel"] for key in animator["keyframes"]}:
                            keys = frames(animator, channel)
                            times = [key["time"] for key in keys]
                            self.assertEqual(times, sorted(set(times)))
                            self.assertGreaterEqual(times[0], 0)
                            self.assertLessEqual(times[-1], animation["length"])

    def test_locomotion_loops_match_seams_and_animate_split_legs(self):
        for sword in SWORDS:
            model = load("players", sword)
            for animation in model["animations"]:
                for animator in animation["animators"].values():
                    # Animations follow the real player; no vertical root hops.
                    self.assertFalse(animator["name"] == "player_root" and frames(animator, "position"))
                self.assertEqual(animation["loop"], "loop" if animation["name"] in {"idle", "walk", "run"} else "once")
            for name in ("idle", "walk", "run"):
                animation = clips(model)[name]
                for animator in animation["animators"].values():
                    keys = frames(animator)
                    self.assertEqual(values(keys[0]), values(keys[-1]))
                if name != "idle":
                    by_name = {animator["name"]: animator for animator in animation["animators"].values()}
                    for leg in ("prl_right_leg", "pll_left_leg", "prfl_right_foreleg", "plfl_left_foreleg"):
                        self.assertGreater(len({values(key) for key in frames(by_name[leg])}), 3)
            self.assertLess(clips(model)["run"]["length"], clips(model)["walk"]["length"])

    def test_each_sword_has_distinct_guard_actions_and_running_motion(self):
        for name in ("idle", "walk", "run", "swing", "ability", "ultimate", "pose"):
            signatures = {motion_signature(clips(load("players", sword))[name]) for sword in SWORDS}
            self.assertEqual(len(signatures), 8, f"repeated {name} choreography")
        for sword in SWORDS:
            for name in ("swing", "ability", "ultimate", "pose"):
                animation = clips(load("players", sword))[name]
                moving = sum(len({values(key) for key in frames(animator)}) > 1
                             for animator in animation["animators"].values())
                self.assertGreaterEqual(moving, 4, f"{sword} {name} lacks full-body motion")

    def test_effect_shapes_are_distinct_and_embedded_textures_are_valid(self):
        shapes = set()
        for sword in SWORDS:
            model = load("models", sword)
            shapes.add(tuple((tuple(cube["from"]), tuple(cube["to"])) for cube in model["elements"]))
            self.assertGreaterEqual(len(model["elements"]), 50)
            self.assertEqual(len(model["groups"]), 3)
            for animation in model["animations"]:
                for animator in animation["animators"].values():
                    scales = frames(animator, "scale")
                    self.assertLess(max(values(scales[0])), .02)
                    self.assertLess(max(values(scales[-1])), .02)
                    self.assertGreater(max(max(values(key)) for key in scales), 1)
            for texture in model["textures"]:
                data = base64.b64decode(texture["source"].split(",", 1)[1], validate=True)
                self.assertTrue(data.startswith(b"\x89PNG\r\n\x1a\n"))
                offset = 8
                while offset < len(data):
                    length = struct.unpack(">I", data[offset:offset + 4])[0]
                    block = data[offset + 4:offset + 8 + length]
                    crc = struct.unpack(">I", data[offset + 8 + length:offset + 12 + length])[0]
                    self.assertEqual(zlib.crc32(block) & 0xffffffff, crc)
                    offset += length + 12
                self.assertEqual(offset, len(data))
        self.assertEqual(len(shapes), 8, "signature effect shapes repeat")

    def test_generator_reproduces_committed_assets_without_external_files(self):
        path = Path(__file__).with_name("generate_bettermodel_assets.py")
        spec = importlib.util.spec_from_file_location("legendaryswords_model_authoring", path)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        with tempfile.TemporaryDirectory(prefix="legendaryswords-assets-") as tmp:
            generated = Path(tmp)
            module.ROOT = generated
            module.main()
            for expected in ROOT.rglob("*.bbmodel"):
                actual = generated / expected.relative_to(ROOT)
                self.assertEqual(actual.read_bytes(), expected.read_bytes(), expected.name)


if __name__ == "__main__":
    unittest.main(verbosity=2)
