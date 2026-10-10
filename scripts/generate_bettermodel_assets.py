#!/usr/bin/env python3
"""Author the eight original sword motion sets and visual-effect models.

Run explicitly when editing animations, not during an ordinary plugin build.
Uses only the Python standard library. The compressed player limb template is
BetterModel's MIT-licensed Steve model; its license accompanies these assets.
The template keeps the engine's player-skin, equipment and split-limb tags.
"""
from __future__ import annotations

import base64
import copy
import json
import math
from pathlib import Path
import struct
import uuid
import zlib

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/bettermodel"
TEMPLATE_B64 = "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"
NAMESPACE = uuid.UUID("695a21cf-3cd7-4c7f-bfaa-65d86569962e")
BONES = {
    "hip": "phip_hip", "waist": "pw_waist", "chest": "pc_chest",
    "head": "h_ph_head", "ra": "pra_right_arm", "rf": "prfa_right_forearm",
    "la": "pla_left_arm", "lf": "plfa_left_forearm", "rl": "prl_right_leg",
    "rk": "prfl_right_foreleg", "ll": "pll_left_leg", "lk": "plfl_left_foreleg",
    "item": "pri_right_item", "cape": "cape_cape",
}
ZERO = (0.0, 0.0, 0.0)


def uid(name: str) -> str:
    return str(uuid.uuid5(NAMESPACE, name))


def stance(**angles):
    result = {bone: ZERO for bone in BONES}
    result.update(angles)
    return result


# Every carry style has a separate silhouette, step cadence and action timing.
STYLES = {
    "master_sword": dict(color=(86, 224, 255), accent=(249, 238, 153),
        idle=stance(ra=(-32, -18, -12), rf=(-12, 0, 0), la=(-8, 0, -7),
                    chest=(0, -5, 0), rl=(0, -4, 2), ll=(0, 4, -2)),
        run=stance(ra=(-48, -24, -18), rf=(-18, 0, 0), la=(-12, 5, -14),
                   chest=(12, -8, 0)), lengths=(.70, 1.40, 3.00), pose=2.20,
        stride=(22, 39), cadence=(1.05, .62), free_arm=28),
    "flameblade": dict(color=(255, 90, 12), accent=(255, 231, 80),
        idle=stance(ra=(-58, -25, -28), rf=(-35, 0, 0), la=(-58, 25, 28),
                    lf=(-42, 0, 0), chest=(8, 13, 0), rl=(-6, 0, 8), ll=(6, 0, -8)),
        run=stance(ra=(-66, -20, -20), rf=(-28, 0, 0), la=(-62, 23, 20),
                   lf=(-35, 0, 0), chest=(17, 9, 0)), lengths=(.90, 1.65, 3.20),
        pose=2.40, stride=(20, 35), cadence=(1.15, .70), free_arm=5),
    "fierce_deity_sword": dict(color=(137, 172, 255), accent=(223, 248, 255),
        idle=stance(ra=(-66, 14, -12), rf=(-20, 0, 0), la=(-62, -13, 13),
                    lf=(-28, 0, 0), chest=(-4, 0, 0), rl=(0, 0, 9), ll=(0, 0, -9)),
        run=stance(ra=(-76, 10, -17), rf=(-16, 0, 0), la=(-72, -12, 17),
                   lf=(-22, 0, 0), chest=(9, 0, 0)), lengths=(.85, 1.65, 3.20),
        pose=2.70, stride=(18, 34), cadence=(1.16, .72), free_arm=3),
    "frostblade": dict(color=(115, 237, 255), accent=(227, 255, 255),
        idle=stance(ra=(-67, 28, 4), rf=(-27, 0, 0), la=(15, -18, -28),
                    lf=(-20, 0, 0), chest=(-3, -22, 0), rl=(-4, 8, 3), ll=(4, -8, -3)),
        run=stance(ra=(-85, 24, 4), rf=(-13, 0, 0), la=(8, -12, -25),
                   chest=(12, -20, 0)), lengths=(.62, 1.40, 3.00), pose=2.20,
        stride=(16, 30), cadence=(.96, .59), free_arm=22),
    "thunderblade": dict(color=(255, 218, 42), accent=(255, 255, 220),
        idle=stance(ra=(-22, -34, -32), rf=(-23, 0, 0), la=(-16, 10, -9),
                    chest=(5, 0, -4), rl=(-4, 0, 5), ll=(4, 0, -5)),
        run=stance(ra=(-34, -17, -20), rf=(-36, 0, 0), la=(-32, 12, 17),
                   lf=(-34, 0, 0), chest=(22, 0, 0)), lengths=(.55, 1.35, 2.90),
        pose=2.05, stride=(25, 45), cadence=(.90, .50), free_arm=8),
    "royal_claymore": dict(color=(204, 139, 255), accent=(255, 222, 122),
        idle=stance(ra=(-114, -23, 13), rf=(-64, 0, 0), la=(-85, 30, -20),
                    lf=(-55, 0, 0), chest=(1, 13, 0), rl=(0, 0, 5), ll=(0, 0, -5)),
        run=stance(ra=(-118, -20, 14), rf=(-56, 0, 0), la=(-88, 27, -18),
                   lf=(-48, 0, 0), chest=(14, 9, 0)), lengths=(1.00, 1.70, 3.40),
        pose=2.60, stride=(17, 28), cadence=(1.25, .82), free_arm=2),
    "eightfold_blade": dict(color=(255, 93, 124), accent=(255, 216, 237),
        idle=stance(ra=(-26, -12, -24), rf=(-44, 0, 0), la=(-28, 12, 32),
                    lf=(-44, 0, 0), chest=(2, -28, 0), rl=(-6, 10, 4), ll=(6, -10, -4)),
        run=stance(ra=(-38, -15, -24), rf=(-25, 0, 0), la=(-40, 12, 22),
                   lf=(-24, 0, 0), chest=(20, -10, 0)), lengths=(.60, 1.45, 3.00),
        pose=2.45, stride=(21, 40), cadence=(1.00, .56), free_arm=4),
    "biggoron_sword": dict(color=(255, 165, 72), accent=(255, 240, 180),
        idle=stance(ra=(-137, -24, 11), rf=(-48, 0, 0), la=(-97, 34, -18),
                    lf=(-48, 0, 0), chest=(3, 11, 0), rl=(0, 0, 11), ll=(0, 0, -11)),
        run=stance(ra=(-139, -21, 11), rf=(-41, 0, 0), la=(-100, 29, -17),
                   lf=(-44, 0, 0), chest=(18, 8, 0)), lengths=(1.15, 1.80, 3.60),
        pose=2.70, stride=(14, 25), cadence=(1.35, .90), free_arm=1),
}


def pose(base, **angles):
    return {**base, **angles}


def keyframe(seed, channel, time, values):
    return {
        "uuid": uid(f"{seed}/{channel}/{time:.5f}"), "channel": channel,
        "data_points": [{axis: f"{value:.5f}" for axis, value in zip("xyz", values)}],
        "time": round(time, 5), "color": -1, "interpolation": "linear",
    }


def animation(sword, name, length, frames, groups, loop=False):
    """Frame positions are normalized; each motion owns all limb channels."""
    animators = {}
    for short, bone in BONES.items():
        # Carry choreography uses negative X to raise a downward arm forward.
        # Blockbench 5's player rotations invert X in BetterModel's loader;
        # serialize the opposite X so wrists stay in front, knees bend back,
        # and the running chest leans forward. VFX rotations are separate.
        keys = [keyframe(f"{sword}/{name}/{bone}", "rotation", phase * length,
                         (-frame[short][0] if frame[short][0] else 0.0,
                          frame[short][1], frame[short][2])) for phase, frame in frames]
        animators[groups[bone]] = {
            "name": bone, "type": "bone", "rotation_global": False,
            "quaternion_interpolation": False, "keyframes": keys,
        }
    return {"uuid": uid(f"{sword}/{name}"), "name": name,
            "loop": "loop" if loop else "once", "override": True,
            "length": length, "snapping": 20, "animators": animators}


def locomotion(sword, style, running):
    base = style["run"] if running else style["idle"]
    stride = style["stride"][int(running)]
    frames = []
    for step in range(17):
        phase = step / 16
        wave = math.sin(phase * math.tau)
        weight = math.cos(phase * math.tau * 2)
        frame = copy.deepcopy(base)
        for limb, sign in (("rl", 1), ("ll", -1)):
            frame[limb] = (base[limb][0] + wave * stride * sign,
                           base[limb][1], base[limb][2])
        # Split shins bend only in the recovery half of the stride.
        frame["rk"] = (max(0, -wave) * stride * .70, 0, 0)
        frame["lk"] = (max(0, wave) * stride * .70, 0, 0)
        # The template's legs are siblings of the hip, not its children.
        # Rotating this bone moves only the torso and opens the pelvis seam.
        frame["hip"] = ZERO
        frame["chest"] = (base["chest"][0] + weight * (2 if running else .7),
                            base["chest"][1] - wave * (5 if running else 3),
                            base["chest"][2] - wave * 1.5)
        frame["la"] = (base["la"][0] - wave * style["free_arm"],
                         base["la"][1], base["la"][2])
        frame["ra"] = (base["ra"][0] + wave * (5 if running else 2),
                         base["ra"][1], base["ra"][2])
        frame["head"] = (-base["chest"][0] * .35, -base["chest"][1] * .45, 0)
        frame["cape"] = (-12 if running else -4, wave * 3, 0)
        frames.append((phase, frame))
    # Exact seam equality avoids floating-point drift at a loop boundary.
    frames[-1] = (1.0, copy.deepcopy(frames[0][1]))
    return frames


def idle(style):
    frames = []
    for step in range(9):
        phase = step / 8
        breath = math.sin(phase * math.tau)
        frame = copy.deepcopy(style["idle"])
        x, y, z = frame["chest"]
        frame["chest"] = (x + breath * .85, y, z)
        x, y, z = frame["ra"]
        frame["ra"] = (x + breath * .65, y, z)
        frame["head"] = (-style["idle"]["chest"][0] * .3,
                           -style["idle"]["chest"][1] * .5, 0)
        frames.append((phase, frame))
    frames[-1] = (1.0, copy.deepcopy(frames[0][1]))
    return frames


def action_frames(sword, base):
    """Deliberately authored windup / release / follow-through silhouettes."""
    p = lambda **values: pose(base, **values)
    if sword == "master_sword":
        swing = [(0, base), (.23, p(chest=(-4, 28, 0), ra=(32, -65, 25), rf=(-62, 0, 0), la=(-12, 0, -30), rl=(-14, 0, 4), ll=(14, 0, -4))),
                 (.40, p(chest=(9, -35, -7), ra=(-88, 58, -34), rf=(-7, 0, 0), la=(30, -12, -18), rl=(16, 0, 3), ll=(-16, 0, -3))),
                 (.58, p(chest=(5, -48, 0), ra=(-44, 72, -25), rf=(-14, 0, 0))), (.84, base), (1, base)]
        ability = [(0, base), (.22, p(chest=(-8, 18, 0), ra=(-143, -10, 7), rf=(-52, 0, 0), la=(-35, 0, -28))),
                   (.42, p(chest=(-5, 8, 0), ra=(-151, 0, 5), rf=(-12, 0, 0))),
                   (.52, p(chest=(17, -18, 0), ra=(-90, -8, -5), rf=(0, 0, 0), la=(25, -12, -25), rl=(23, 0, 3), ll=(-23, 0, -3))),
                   (.72, p(chest=(12, -14, 0), ra=(-82, -8, -4))), (1, base)]
        ultimate = [(0, base), (.18, p(chest=(-9, 0, 0), ra=(-174, 0, 6), rf=(-6, 0, 0), la=(-70, 0, -38), head=(-15, 0, 0))),
                    (.38, p(chest=(-12, 0, 0), ra=(-178, 0, 4), la=(-110, 0, -55), head=(-22, 0, 0))),
                    (.50, p(chest=(14, -25, 0), ra=(-78, 20, 80), la=(-66, -10, -65), rl=(-12, 0, 12), ll=(12, 0, -12))),
                    (.70, p(chest=(0, 32, 0), ra=(-80, -25, 70), la=(-75, 20, -70))), (.90, base), (1, base)]
        signature = p(chest=(-5, -5, 0), ra=(-154, 0, 4), rf=(-8, 0, 0), la=(-20, 0, -30), head=(-12, 0, 0), rl=(0, 0, 5), ll=(0, 0, -5))
    elif sword == "flameblade":
        swing = [(0, base), (.26, p(chest=(0, 52, -6), ra=(-62, -58, 5), rf=(-80, 0, 0), la=(-62, -12, 38), lf=(-70, 0, 0), rl=(-18, 0, 11), ll=(18, 0, -11))),
                 (.43, p(chest=(12, -63, 7), ra=(-75, 65, -38), rf=(-12, 0, 0), la=(-80, 45, 24), lf=(-14, 0, 0), rl=(20, 0, 11), ll=(-20, 0, -11))),
                 (.66, p(chest=(10, -80, 3), ra=(-55, 73, -25), la=(-66, 58, 20))), (.88, base), (1, base)]
        ability = [(0, base), (.25, p(chest=(-18, 0, 0), ra=(-155, -10, 14), rf=(-15, 0, 0), la=(-145, 10, -14), lf=(-20, 0, 0), head=(-15, 0, 0))),
                   (.45, p(chest=(-16, 0, 0), ra=(-167, 0, 8), la=(-165, 0, -8))),
                   (.58, p(chest=(35, 0, 0), ra=(22, -8, -12), rf=(-10, 0, 0), la=(19, 8, 12), lf=(-10, 0, 0), hip=(8, 0, 0), rl=(-23, 0, 12), ll=(-23, 0, -12), rk=(35, 0, 0), lk=(35, 0, 0))),
                   (.78, p(chest=(15, 0, 0), ra=(-55, 0, 80), la=(-55, 0, -80), rf=(-15, 0, 0), lf=(-15, 0, 0))), (1, base)]
        ultimate = [(0, base), (.18, p(chest=(-20, 0, 0), ra=(-150, -10, 20), la=(-150, 10, -20), rf=(-18, 0, 0), lf=(-18, 0, 0), head=(-25, 0, 0))),
                    (.36, p(chest=(-24, 0, 0), ra=(-180, 0, 0), la=(-180, 0, 0), rl=(0, 0, 15), ll=(0, 0, -15))),
                    (.47, p(chest=(38, 0, 0), hip=(12, 0, 0), ra=(30, -10, -15), la=(30, 10, 15), rf=(-8, 0, 0), lf=(-8, 0, 0), rl=(-32, 0, 14), ll=(-32, 0, -14), rk=(48, 0, 0), lk=(48, 0, 0))),
                    (.65, p(chest=(-5, 0, 0), ra=(-85, 0, 80), la=(-85, 0, -80), head=(-12, 0, 0))), (.83, p(chest=(-8, 0, 0), ra=(-100, 0, 75), la=(-100, 0, -75))), (1, base)]
        signature = p(chest=(-8, 20, 0), ra=(-165, 0, 10), rf=(-18, 0, 0), la=(-80, 0, -75), lf=(-25, 0, 0), head=(-18, -15, 0))
    elif sword == "fierce_deity_sword":
        swing = [(0, base), (.24, p(chest=(-18, 0, 0), ra=(-163, -5, -9), la=(-158, 5, 9), rf=(-35, 0, 0), lf=(-35, 0, 0), rl=(-12, 0, 12), ll=(12, 0, -12))),
                 (.43, p(chest=(26, 0, 0), ra=(28, 12, -14), la=(24, -12, 14), rf=(-6, 0, 0), lf=(-6, 0, 0), rl=(18, 0, 13), ll=(-18, 0, -13))),
                 (.63, p(chest=(15, 0, 0), ra=(10, 10, -15), la=(8, -10, 15))), (.87, base), (1, base)]
        ability = [(0, base), (.22, p(chest=(-6, 60, -8), ra=(-88, -50, 0), la=(-75, -5, 20), rf=(-55, 0, 0), lf=(-50, 0, 0))),
                   (.43, p(chest=(4, -62, 8), ra=(-91, 50, -42), la=(-83, 35, 35), rf=(-9, 0, 0), lf=(-12, 0, 0))),
                   (.59, p(chest=(-9, -48, 0), ra=(-148, 35, -12), la=(-140, 20, 15))),
                   (.72, p(chest=(24, 28, 0), ra=(-12, -18, -8), la=(-10, -14, 12), rl=(20, 0, 10), ll=(-20, 0, -10))), (1, base)]
        ultimate = [(0, base), (.20, p(chest=(-15, 0, 0), ra=(-173, 0, -5), la=(-168, 0, 5), head=(-24, 0, 0))),
                    (.40, p(chest=(-12, 0, 0), ra=(-160, 0, -30), la=(-155, 0, 30), rl=(0, 0, 15), ll=(0, 0, -15))),
                    (.53, p(chest=(5, -85, 0), ra=(-78, 25, 82), la=(-70, -15, -75), rf=(-10, 0, 0), lf=(-12, 0, 0))),
                    (.67, p(chest=(6, 85, 0), ra=(-80, -25, 78), la=(-74, 15, -75))),
                    (.80, p(chest=(18, 0, 0), ra=(-90, 0, -8), la=(-90, 0, 8), rf=(0, 0, 0), lf=(0, 0, 0))), (1, base)]
        signature = p(chest=(-12, 0, 0), ra=(-156, 0, -12), rf=(-10, 0, 0), la=(-150, 0, 12), lf=(-14, 0, 0), head=(-20, 0, 0), rl=(0, 0, 16), ll=(0, 0, -16))
    elif sword == "frostblade":
        swing = [(0, base), (.25, p(chest=(-7, -45, 0), ra=(-104, 45, 6), rf=(-82, 0, 0), la=(20, -20, -45), rl=(-19, 0, 4), ll=(19, 0, -4))),
                 (.40, p(chest=(18, 18, 0), ra=(-93, -20, 4), rf=(0, 0, 0), la=(30, -30, -45), rl=(32, 0, 4), ll=(-22, 0, -4))),
                 (.62, p(chest=(10, 10, 0), ra=(-91, -15, 5), rf=(-4, 0, 0))), (.85, base), (1, base)]
        ability = [(0, base), (.22, p(chest=(-10, -30, 0), ra=(-170, 12, 2), rf=(-12, 0, 0), la=(-60, 0, 55), head=(-15, 0, 0))),
                   (.42, p(chest=(-7, -28, 0), ra=(-172, 14, 2), la=(-85, 0, 65))),
                   (.60, p(chest=(10, 40, 0), ra=(-80, -30, -48), rf=(-5, 0, 0), la=(-65, 20, 60), rl=(15, 0, 5), ll=(-15, 0, -5))), (1, base)]
        ultimate = [(0, base), (.18, p(chest=(-3, 0, 0), ra=(-84, -45, 25), la=(-84, 45, -25), rf=(-45, 0, 0), lf=(-45, 0, 0), head=(12, 0, 0))),
                    (.37, p(chest=(-6, 0, 0), ra=(-110, -50, 25), la=(-110, 50, -25), rl=(0, 0, 8), ll=(0, 0, -8))),
                    (.50, p(chest=(-10, 0, 0), ra=(-80, 0, 85), la=(-80, 0, -85), rf=(-7, 0, 0), lf=(-7, 0, 0), head=(-18, 0, 0))),
                    (.72, p(chest=(-8, -15, 0), ra=(-100, -12, 75), la=(-85, 12, -75))), (.87, base), (1, base)]
        signature = p(chest=(-4, -32, 0), ra=(-152, 15, 5), rf=(-12, 0, 0), la=(-58, 12, 55), lf=(-35, 0, 0), head=(-10, 16, 0), rl=(-7, 8, 6), ll=(7, -8, -6))
    elif sword == "thunderblade":
        swing = [(0, base), (.18, p(chest=(-4, 42, -10), ra=(8, -58, 40), rf=(-56, 0, 0))),
                 (.35, p(chest=(11, -47, 8), ra=(-102, 52, -45), rf=(-3, 0, 0), rl=(24, 0, 4), ll=(-24, 0, -4))),
                 (.49, p(chest=(0, 38, -6), ra=(-46, -56, 25), rf=(-15, 0, 0))),
                 (.66, p(chest=(6, -28, 0), ra=(-78, 40, -30), la=(25, -15, -20))), (.9, base), (1, base)]
        ability = [(0, base), (.20, p(chest=(18, -42, -10), ra=(20, -50, -28), rf=(-60, 0, 0), la=(-50, 20, 25), rl=(-18, 0, 7), ll=(18, 0, -7))),
                   (.36, p(chest=(6, 45, 9), ra=(-130, 32, -38), rf=(-3, 0, 0), la=(30, -18, -20), rl=(30, 0, 7), ll=(-30, 0, -7))),
                   (.53, p(chest=(-12, 0, 0), ra=(-178, 0, 3), la=(-38, 0, -40), head=(-22, 0, 0))),
                   (.76, p(chest=(-8, 0, 0), ra=(-169, 0, 5), rf=(-8, 0, 0))), (1, base)]
        ultimate = [(0, base), (.17, p(chest=(14, -50, -8), ra=(14, -60, 30), rf=(-60, 0, 0))),
                    (.28, p(chest=(5, 65, 10), ra=(-110, 60, -40), rf=(0, 0, 0))),
                    (.40, p(chest=(0, -65, -10), ra=(-56, -65, 40), rf=(-15, 0, 0))),
                    (.51, p(chest=(8, 70, 8), ra=(-122, 65, -40), rf=(0, 0, 0))),
                    (.64, p(chest=(-18, 0, 0), ra=(-178, 0, 3), la=(-80, 0, -75), head=(-25, 0, 0), rl=(0, 0, 13), ll=(0, 0, -13))),
                    (.83, p(chest=(-8, 0, 0), ra=(-170, 0, 3), la=(-70, 0, -65))), (1, base)]
        signature = p(chest=(-12, 0, -3), ra=(-176, 0, 4), rf=(-4, 0, 0), la=(-45, 15, -60), head=(-25, 0, 0), rl=(0, 0, 9), ll=(0, 0, -9))
    elif sword == "royal_claymore":
        swing = [(0, base), (.30, p(chest=(-20, 0, 0), ra=(-167, -6, 7), la=(-160, 6, -7), rf=(-38, 0, 0), lf=(-38, 0, 0), rl=(-12, 0, 8), ll=(12, 0, -8))),
                 (.40, p(chest=(-23, 0, 0), ra=(-178, 0, 4), la=(-175, 0, -4))),
                 (.54, p(chest=(37, 0, 0), ra=(34, -10, -8), la=(31, 10, 8), rf=(-6, 0, 0), lf=(-7, 0, 0), rl=(20, 0, 9), ll=(-23, 0, -9), lk=(20, 0, 0))),
                 (.73, p(chest=(25, 0, 0), ra=(15, -10, -8), la=(12, 10, 8))), (1, base)]
        ability = [(0, base), (.27, p(chest=(0, 70, -8), ra=(-72, -55, 10), la=(-68, -5, 35), rf=(-65, 0, 0), lf=(-65, 0, 0))),
                   (.48, p(chest=(12, -80, 8), ra=(-84, 58, -45), la=(-80, 45, 35), rf=(-8, 0, 0), lf=(-10, 0, 0), rl=(20, 0, 12), ll=(-20, 0, -12))),
                   (.68, p(chest=(8, -85, 2), ra=(-55, 72, -32), la=(-53, 55, 28))), (.86, base), (1, base)]
        ultimate = [(0, base), (.20, p(chest=(-22, 0, 0), ra=(-162, 0, 10), la=(-155, 0, -10), rf=(-40, 0, 0), lf=(-40, 0, 0))),
                    (.37, p(chest=(-28, 0, 0), ra=(-179, 0, 5), la=(-178, 0, -5), rl=(-8, 0, 15), ll=(-8, 0, -15))),
                    (.49, p(chest=(42, 0, 0), hip=(13, 0, 0), ra=(38, -12, -10), la=(35, 12, 10), rf=(-4, 0, 0), lf=(-4, 0, 0), rl=(-32, 0, 15), ll=(-32, 0, -15), rk=(46, 0, 0), lk=(46, 0, 0))),
                    (.61, p(chest=(30, 0, -6), ra=(20, -12, -10), la=(18, 12, 10))),
                    (.72, p(chest=(20, 0, 4), ra=(5, -12, -10), la=(3, 12, 10))), (.92, base), (1, base)]
        signature = p(chest=(-7, 0, 0), ra=(-130, 0, 8), la=(-128, 0, -8), rf=(-35, 0, 0), lf=(-35, 0, 0), head=(-8, 0, 0), rl=(0, 0, 8), ll=(0, 0, -8))
    elif sword == "eightfold_blade":
        swing = [(0, base), (.23, p(chest=(12, -50, -8), ra=(38, -30, -28), rf=(-82, 0, 0), la=(-24, 15, 35), rl=(-17, 10, 6), ll=(17, -10, -6))),
                 (.38, p(chest=(4, 50, 8), ra=(-96, 45, -35), rf=(-4, 0, 0), la=(25, -12, -25), rl=(27, 8, 5), ll=(-27, -8, -5))),
                 (.62, p(chest=(2, 62, 0), ra=(-75, 56, -24), rf=(-15, 0, 0))), (.87, base), (1, base)]
        ability = [(0, base), (.23, p(chest=(6, -48, -10), ra=(25, -42, 35), rf=(-70, 0, 0))),
                   (.39, p(chest=(8, 55, 9), ra=(-120, 45, -42), rf=(-4, 0, 0))),
                   (.52, p(chest=(-3, 40, 0), ra=(-138, 35, -5), rf=(-36, 0, 0), la=(-40, 0, -35))),
                   (.65, p(chest=(24, -45, -7), ra=(14, -28, 28), rf=(-4, 0, 0), rl=(22, 5, 6), ll=(-22, -5, -6))), (.85, base), (1, base)]
        ultimate = [(0, base), (.18, p(chest=(16, -55, -8), ra=(32, -35, -25), rf=(-82, 0, 0), la=(-35, 14, 35))),
                    (.32, p(chest=(5, 80, 8), ra=(-115, 58, -42), rf=(-3, 0, 0))),
                    (.44, p(chest=(0, -80, -8), ra=(-48, -66, 35), rf=(-12, 0, 0))),
                    (.56, p(chest=(-8, 50, 0), ra=(-160, 30, -12), rf=(-35, 0, 0))),
                    (.67, p(chest=(25, -45, 0), ra=(20, -32, 25), rf=(-4, 0, 0), rl=(23, 0, 9), ll=(-23, 0, -9))),
                    (.84, p(chest=(12, -42, -5), ra=(24, -30, -25), rf=(-70, 0, 0))), (1, base)]
        signature = p(chest=(10, -42, -7), ra=(28, -28, -30), rf=(-82, 0, 0), la=(-35, 18, 38), lf=(-52, 0, 0), head=(-3, 25, 0), rl=(-10, 8, 8), ll=(10, -8, -8))
    else:  # Biggoron: visibly slower, braced whole-body greatsword strikes.
        swing = [(0, base), (.31, p(chest=(-25, 0, 0), ra=(-170, -7, 8), la=(-162, 7, -8), rf=(-48, 0, 0), lf=(-48, 0, 0), rl=(-12, 0, 14), ll=(12, 0, -14))),
                 (.45, p(chest=(-30, 0, 0), ra=(-178, 0, 5), la=(-175, 0, -5))),
                 (.57, p(chest=(42, 0, 0), hip=(10, 0, 0), ra=(42, -12, -10), la=(39, 12, 10), rf=(-5, 0, 0), lf=(-5, 0, 0), rl=(22, 0, 14), ll=(-27, 0, -14), lk=(25, 0, 0))),
                 (.76, p(chest=(29, 0, 0), ra=(20, -12, -10), la=(18, 12, 10))), (1, base)]
        ability = [(0, base), (.25, p(chest=(-10, 40, 0), ra=(-110, -30, 4), la=(-100, 10, 20), rf=(-75, 0, 0), lf=(-72, 0, 0), rl=(-20, 0, 12), ll=(20, 0, -12))),
                   (.45, p(chest=(30, -12, 0), ra=(-92, -5, -6), la=(-90, 8, 10), rf=(0, 0, 0), lf=(0, 0, 0), rl=(34, 0, 12), ll=(-28, 0, -12))),
                   (.65, p(chest=(24, -10, 0), ra=(-85, -5, -6), la=(-83, 8, 10))), (.85, base), (1, base)]
        ultimate = [(0, base), (.22, p(chest=(-24, 0, 0), ra=(-160, -8, 10), la=(-152, 8, -10), rf=(-52, 0, 0), lf=(-52, 0, 0))),
                    (.41, p(chest=(-32, 0, 0), ra=(-180, 0, 5), la=(-178, 0, -5), rl=(-10, 0, 18), ll=(-10, 0, -18))),
                    (.52, p(chest=(48, 0, 0), hip=(15, 0, 0), ra=(48, -14, -12), la=(45, 14, 12), rf=(-4, 0, 0), lf=(-4, 0, 0), rl=(-38, 0, 18), ll=(-38, 0, -18), rk=(57, 0, 0), lk=(57, 0, 0))),
                    (.61, p(chest=(34, 0, -8), ra=(24, -14, -12), la=(22, 14, 12))),
                    (.70, p(chest=(29, 0, 7), ra=(12, -14, -12), la=(10, 14, 12))),
                    (.79, p(chest=(22, 0, -4), ra=(-8, -10, -9), la=(-10, 10, 9))), (1, base)]
        signature = p(chest=(-5, 15, 0), ra=(-145, -30, 16), rf=(-70, 0, 0), la=(-70, 0, -60), lf=(-50, 0, 0), head=(-8, -12, 0), rl=(0, 0, 18), ll=(0, 0, -18))
    return {"swing": swing, "ability": ability, "ultimate": ultimate,
            "pose": [(0, base), (.25, signature), (.78, signature), (1, base)]}


def png(color, accent):
    """Original two-tone energy texture with a bright center and rim."""
    raw = bytearray()
    for y in range(16):
        raw.append(0)
        for x in range(16):
            blend = max(0, 1 - abs(x - 7.5) / 6.5)
            raw.extend(int(a * (1 - blend) + b * blend) for a, b in zip(color, accent))
            raw.append(255)
    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xffffffff))
    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">2I5B", 16, 16, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))


def effect_model(sword, style):
    elements, groups, outlines = [], [], []
    group_elements = {}
    def group(name):
        ident = uid(f"{sword}/effect/{name}")
        groups.append({"uuid": ident, "name": name, "origin": [0, 0, 0],
                       "rotation": [0, 0, 0], "visibility": True})
        group_elements[name] = []
        return name

    def cube(name, center, size, parent, texture=0):
        ident = uid(f"{sword}/effect/{name}")
        elements.append({"uuid": ident, "name": name, "type": "cube",
            "from": [round(c - s / 2, 4) for c, s in zip(center, size)],
            "to": [round(c + s / 2, 4) for c, s in zip(center, size)],
            "origin": [0, 0, 0], "rotation": [0, 0, 0],
            "faces": {face: {"uv": [0, 0, 16, 16], "texture": texture}
                      for face in ("north", "east", "south", "west", "up", "down")}})
        group_elements[parent].append(ident)

    def arc(parent, radius, start, stop, count=24, plane="xy", thickness=1.4, offset=0):
        for i in range(count):
            angle = math.radians(start + (stop - start) * i / (count - 1))
            a, b = math.cos(angle) * radius, math.sin(angle) * radius
            center = (a, b, offset) if plane == "xy" else (a, offset, b)
            cube(f"{parent}_segment_{len(elements)}", center,
                 (thickness, thickness, .65) if plane == "xy" else (thickness, .65, thickness), parent)

    core, halo, sparks = group("energy_core"), group("outer_halo"), group("sparks")
    if sword == "master_sword":
        arc(core, 15, -110, 110, 34, thickness=2.4)
        arc(halo, 18, -105, 105, 32, thickness=.85, offset=.9)
        for i in range(8):
            angle = math.radians(-95 + i * 27)
            cube(f"sacred_ray_{i}", (math.cos(angle) * 21, math.sin(angle) * 21, 0), (1, 4, .8), sparks, 1)
    elif sword == "flameblade":
        arc(core, 17, 0, 360, 48, plane="xz", thickness=2.5)
        arc(halo, 21, 0, 360, 48, plane="xz", thickness=1.1, offset=-1)
        for i in range(16):
            angle = i * math.tau / 16
            x, z = math.cos(angle) * 17, math.sin(angle) * 17
            height = 5 + (i % 3) * 2
            cube(f"flame_spire_{i}", (x, height / 2, z), (2.5, height, 2), sparks)
            cube(f"flame_tip_{i}", (x, height + 1, z), (1.2, 2.5, 1.1), sparks, 1)
    elif sword == "fierce_deity_sword":
        arc(core, 16, -130, 110, 36, thickness=2.3, offset=-2)
        arc(halo, 13, 50, 290, 36, thickness=1.7, offset=2)
        for i in range(12):
            angle = i * math.tau / 12
            cube(f"lunar_star_{i}", (math.cos(angle) * 23, math.sin(angle) * 23, 0), (1.8, 1.8, 1.8), sparks, 1)
    elif sword == "frostblade":
        arc(core, 16, 0, 360, 36, plane="xz", thickness=1.25)
        arc(halo, 20, 0, 360, 36, plane="xz", thickness=.8, offset=1)
        for i in range(12):
            angle = i * math.tau / 12
            x, z = math.cos(angle) * 16, math.sin(angle) * 16
            for layer in range(3):
                cube(f"ice_spike_{i}_{layer}", (x, 2 + layer * 3, z),
                     (3 - layer * .8, 3, 3 - layer * .8), sparks, int(layer == 2))
    elif sword == "thunderblade":
        for branch in range(5):
            bx = (branch - 2) * 7
            for i in range(12):
                x = bx + (2.5 if i % 2 else -2.5)
                cube(f"bolt_{branch}_{i}", (x, -17 + i * 3, (branch % 2) * 2),
                     (4.8, 3.5, .8), core)
        arc(halo, 20, -125, 125, 26, thickness=1.1)
        for i in range(12):
            angle = i * math.tau / 12
            cube(f"electric_mote_{i}", (math.cos(angle) * 24, math.sin(angle) * 24, 0), (1.5, 1.5, 1.5), sparks, 1)
    elif sword == "royal_claymore":
        for i in range(27):
            coord = -22 + i * 1.7
            cube(f"royal_cross_a_{i}", (coord, coord * .62, 0), (2.7, 2.1, .8), core)
            cube(f"royal_cross_b_{i}", (coord, -coord * .62, 1), (1.9, 1.7, .8), halo, 1)
        for i in range(16):
            angle = i * math.tau / 16
            cube(f"royal_dust_{i}", (math.cos(angle) * 20, -5 + (i % 3), math.sin(angle) * 12), (2, 2, 2), sparks)
    elif sword == "eightfold_blade":
        arc(core, 17, -75, 75, 38, thickness=1.05, offset=-1.5)
        arc(halo, 15, 105, 255, 34, thickness=.85, offset=1.5)
        for i in range(9):
            cube(f"petal_{i}", (-16 + i * 4, (i % 3 - 1) * 6, (i % 2) * 3), (1.9, .9, 1.3), sparks, 1)
    else:
        arc(core, 18, 0, 360, 40, plane="xz", thickness=2.8)
        arc(halo, 24, 0, 360, 48, plane="xz", thickness=1.5, offset=-2)
        for i in range(16):
            angle = i * math.tau / 16
            cube(f"seismic_chunk_{i}", (math.cos(angle) * 18, 2 + i % 4, math.sin(angle) * 18),
                 (3.3, 3 + i % 3, 3.3), sparks, int(i % 3 == 0))
    for entry in groups:
        outlines.append({"uuid": entry["uuid"], "isOpen": True,
                         "children": group_elements[entry["name"]]})
    model = {"meta": {"format_version": "5.0", "model_format": "free", "box_uv": False},
        "name": f"LegendarySwords {sword} signature effect", "model_identifier": f"legendaryswords_{sword}_slash",
        "credit": "Original geometry and animations by LegendarySwords",
        "resolution": {"width": 16, "height": 16}, "elements": elements,
        "groups": groups, "outliner": outlines, "textures": [], "animations": []}
    for i, (color, accent) in enumerate(((style["color"], style["accent"]), (style["accent"], (255, 255, 255)))):
        model["textures"].append({"name": f"{sword}_energy_{i}.png", "id": str(i),
            "width": 16, "height": 16, "uv_width": 16, "uv_height": 16,
            "source": "data:image/png;base64," + base64.b64encode(png(color, accent)).decode()})
    ring = sword in {"flameblade", "frostblade", "biggoron_sword"}
    for name, length, peak in (("swing", .55, 1.05), ("ability", 1.20, 1.70), ("ultimate", 2.10, 2.80)):
        animators = {}
        for index, entry in enumerate(groups):
            ident, bone = entry["uuid"], entry["name"]
            rotation_axis = 1 if ring else 2
            rotations = [0, 0, 0]
            rotations[rotation_axis] = (-22 if index != 1 else 25)
            end_rotation = [0, 0, 0]
            end_rotation[rotation_axis] = (48 if index != 1 else -42)
            multiplier = (1, 1.07, 1.12)[index]
            grow = peak * multiplier
            scale_frames = [(0, (.015, .015, .015)), (.16, (grow * .82,) * 3),
                            (.30, (grow,) * 3), (.64, (grow * 1.08,) * 3),
                            (.84, (grow * 1.15, grow * .45, grow * 1.15)),
                            (1, (.01, .01, .01))]
            if ring and index == 2:
                scale_frames = [(0, (.01, .01, .01)), (.22, (grow, grow * .8, grow)),
                                (.48, (grow * 1.10, grow * 1.4, grow * 1.10)),
                                (.78, (grow * 1.3, grow * .55, grow * 1.3)), (1, (.01,) * 3)]
            keys = [keyframe(f"{sword}/effect/{name}/{bone}", "scale", phase * length, values)
                    for phase, values in scale_frames]
            keys += [keyframe(f"{sword}/effect/{name}/{bone}", "rotation", 0, rotations),
                     keyframe(f"{sword}/effect/{name}/{bone}", "rotation", length, end_rotation)]
            # Scatter accent particles upward / outward during their brief life.
            if index == 2:
                keys += [keyframe(f"{sword}/effect/{name}/{bone}", "position", 0, ZERO),
                         keyframe(f"{sword}/effect/{name}/{bone}", "position", length * .7,
                                  (0, 7 if ring else 3, 0)),
                         keyframe(f"{sword}/effect/{name}/{bone}", "position", length, (0, 9, 0))]
            animators[ident] = {"name": bone, "type": "bone", "rotation_global": False,
                               "quaternion_interpolation": False, "keyframes": keys}
        model["animations"].append({"uuid": uid(f"{sword}/effect/{name}"), "name": name,
            "loop": "once", "override": True, "length": length, "snapping": 20,
            "animators": animators})
    return model


def main():
    template = json.loads(zlib.decompress(base64.b64decode(TEMPLATE_B64)))
    groups = {group["name"]: group["uuid"] for group in template["groups"]}
    (ROOT / "players").mkdir(parents=True, exist_ok=True)
    (ROOT / "models").mkdir(parents=True, exist_ok=True)
    for sword, style in STYLES.items():
        model = copy.deepcopy(template)
        model["name"] = f"LegendarySwords {sword} full-body motion set"
        model["model_identifier"] = f"legendaryswords_{sword}"
        model["credit"] = "BetterModel Steve limb template (MIT); original LegendarySwords motion choreography"
        clips = [animation(sword, "idle", 2.6, idle(style), groups, True)]
        for running, name in ((False, "walk"), (True, "run")):
            clips.append(animation(sword, name, style["cadence"][int(running)],
                                   locomotion(sword, style, running), groups, True))
        for name, frames in action_frames(sword, style["idle"]).items():
            duration = style["pose"] if name == "pose" else style["lengths"][("swing", "ability", "ultimate").index(name)]
            clips.append(animation(sword, name, duration, frames, groups))
        model["animations"] = clips
        target = ROOT / "players" / f"legendaryswords_{sword}.bbmodel"
        target.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
        target = ROOT / "models" / f"legendaryswords_{sword}_slash.bbmodel"
        target.write_text(json.dumps(effect_model(sword, style), indent=2) + "\n", encoding="utf-8")
    print(f"Generated {len(STYLES)} player motion sets and {len(STYLES)} distinct signature effects.")


if __name__ == "__main__":
    main()
