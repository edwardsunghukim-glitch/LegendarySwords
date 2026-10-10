#!/usr/bin/env python3
"""Author the additional relics and original second-collection weapons using the project's original art and rigs."""
import base64
import copy
import json
import math
import uuid
from pathlib import Path
import generate_resource_pack_assets as art
import generate_bettermodel_assets as motion

ROOT = Path(__file__).resolve().parents[1]
WEAPONS = json.loads((ROOT / 'scripts/expanded_weapons.json').read_text())


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')


def extra_clips(model):
    clips = {a['name']: a for a in model['animations']}
    for name, source, start, end, length in [('draw', 'swing', 0, .5, .45),
            ('charge', 'ultimate', 0, .4, .7), ('release', 'ability', .3, 1, .8),
            ('recover', 'swing', .65, 1, .35)]:
        clip = copy.deepcopy(clips[source]); clip['name'] = name; clip['loop'] = 'once'; clip['length'] = length
        clip['uuid'] = str(uuid.uuid5(uuid.NAMESPACE_URL, model['model_identifier'] + '/' + name))
        for animator in clip['animators'].values():
            keys = animator['keyframes']; low = clips[source]['length'] * start; high = clips[source]['length'] * end
            selected = [k for k in keys if low <= k['time'] <= high]
            if not selected: selected = keys[-2:]
            if len(selected) == 1: selected = [copy.deepcopy(selected[0]), copy.deepcopy(selected[0])]
            first, last = selected[0]['time'], selected[-1]['time']
            for index, key in enumerate(selected):
                key['time'] = round(length * ((key['time'] - first) / (last - first) if last > first else index / (len(selected) - 1)), 5)
                key['uuid'] = str(uuid.uuid5(uuid.NAMESPACE_URL, clip['uuid'] + animator['name'] + str(index)))
            animator['keyframes'] = selected
        clips[name] = clip
    model['animations'] = list(clips.values())
    return model


def safe_casts(model, item, index):
    """Author cast anticipation/jab/follow-through from a clear forward guard."""
    from player_pose_clearance import PlayerPoseClearance
    existing = {clip['name']: clip for clip in model['animations']}
    baseline = {key: copy.deepcopy(animator['keyframes'][0]['data_points'][0])
                for key, animator in existing['idle']['animators'].items()}
    for name in ['ability', 'ultimate']:
        duration = existing[name]['length']
        for amplitude in [1.0, .65, .3, .1, 0.0]:
            clip = copy.deepcopy(existing['idle']); clip['name'] = name; clip['loop'] = 'once'; clip['length'] = duration
            clip['uuid'] = str(uuid.uuid5(uuid.NAMESPACE_URL, model['model_identifier'] + '/' + name))
            for key, animator in clip['animators'].items():
                frames = []
                phases = [0, .2, .5, .8, 1] if index < 16 else [0, .12 + (index % 5) * .02, .48, .72 + (index % 4) * .04, 1]
                for frame, phase in enumerate(phases):
                    pulse = math.sin(phase * math.pi)
                    point = {axis: float(value) for axis, value in baseline[key].items() if axis in 'xyz'}
                    bone = animator['name']
                    if bone == 'pra_right_arm': point['x'] += pulse * (14 + index * .3) * amplitude
                    if bone == 'prfa_right_forearm': point['x'] -= pulse * (7 if index < 16 else 5 + index % 7) * amplitude
                    if index >= 16 and bone == 'pra_right_arm': point['z'] += pulse * (index % 5 - 2) * amplitude
                    if index >= 16 and bone == 'pla_left_arm': point['z'] += pulse * (index % 7 - 3) * amplitude
                    if bone == 'pla_left_arm': point['x'] += pulse * 4 * amplitude
                    if bone in ('prl_right_leg', 'pll_left_leg'): point['x'] += pulse * (2 + index * .1) * (-1 if bone.startswith('pll') else 1)
                    if bone == 'h_ph_head': point['z'] += pulse * .25
                    frames.append({'uuid': str(uuid.uuid5(uuid.NAMESPACE_URL, clip['uuid'] + key + str(frame))),
                        'channel': 'rotation', 'time': round(phase * duration, 5),
                        'data_points': [{axis: str(round(point[axis], 5)) for axis in 'xyz'}],
                        'interpolation': 'linear'})
                animator['keyframes'] = frames
            trial = copy.deepcopy(model)
            trial['animations'] = [clip if a['name'] == name else a for a in trial['animations']]
            if not PlayerPoseClearance(trial, item, 'fixed').collisions(name):
                existing[name] = clip
                break
        else: raise ValueError(model['model_identifier'] + ' has no clear cast guard')
    model['animations'] = list(existing.values()); extra_clips(model)
    assert not PlayerPoseClearance(model, item, 'fixed').collisions('release')
    return model


def remap_ids(model, name):
    ids = set()
    def collect(value):
        if isinstance(value, dict):
            if 'uuid' in value: ids.add(value['uuid'])
            for child in value.values(): collect(child)
        elif isinstance(value, list):
            for child in value: collect(child)
    collect(model)
    mapping = {old: str(uuid.uuid5(uuid.NAMESPACE_URL, name + '/' + old)) for old in ids}
    def rewrite(value):
        if isinstance(value, dict): return {mapping.get(k, k): rewrite(v) for k, v in value.items()}
        if isinstance(value, list): return [rewrite(v) for v in value]
        return mapping.get(value, value) if isinstance(value, str) else value
    return rewrite(model)


def generate_models(root):
    for index, weapon in enumerate(WEAPONS):
        name, base = weapon['id'], weapon['base']
        for kind in ['players', 'models']:
            suffix = '_slash' if kind == 'models' else ''
            model = json.loads((root / kind / f'legendaryswords_{base}{suffix}.bbmodel').read_text())
            if kind == 'models' and weapon.get('generation') == 2:
                from domain_weapon_art import effect_model
                write(root / kind / f'legendaryswords_{name}{suffix}.bbmodel', effect_model(weapon, index - 16))
                continue
            model['name'] = weapon['title'] + (' signature effect' if suffix else ' cast choreography')
            model['model_identifier'] = f'legendaryswords_{name}{suffix}'
            if kind == 'players':
                for clip in model['animations']:
                    length_scale = 1 + (index + 1) * .009
                    clip['length'] = round(clip['length'] * length_scale, 5)
                    for animator in clip['animators'].values():
                        for key in animator['keyframes']:
                            key['time'] = round(key['time'] * length_scale, 5)
                            # Small full-body variations preserve the tested arm/item silhouette.
                            if key['channel'] == 'rotation' and animator['name'] in ('prl_right_leg', 'pll_left_leg', 'prfl_right_foreleg', 'plfl_left_foreleg'):
                                point = key['data_points'][0]
                                point['x'] = str(round(float(point['x']) * (1 + (index + 1) * .006), 5))
                item = json.loads((art.ASSETS / "models/item" / (name + "_animated.json")).read_text())
                safe_casts(model, item, index)
            else:
                sx, sy = 1 + (index + 1) * .015, 1 - (index + 1) * .008
                for element in model['elements']:
                    for field in ['from', 'to']:
                        element[field][0] = round(element[field][0] * sx, 5)
                        element[field][1] = round(element[field][1] * sy, 5)
                for texture in model['textures']:
                    texture['source'] = 'data:image/png;base64,' + base64.b64encode(motion.png(tuple(weapon['color']), (255, 245, 215))).decode()
            write(root / kind / f'legendaryswords_{name}{suffix}.bbmodel', remap_ids(model, name + suffix))


def generate_pack():
    for index, weapon in enumerate(WEAPONS):
        name, base = weapon['id'], weapon['base']
        palette = copy.deepcopy(art.PALETTES[base])
        if weapon.get('generation') != 2:
            materials=[(235,238,230),(165,175,205),(110,135,175),(205,225,195),(110,200,185),(85,65,110),(95,160,180),(165,115,130),(185,145,65),(205,130,175),(160,130,95),(75,135,170),(105,170,165),(185,150,105),(110,120,70),(70,60,100)]
            palette[0]=materials[index]
            palette[4]=tuple(round(c*.23) for c in materials[index])
        if weapon.get('generation') == 2:
            from domain_weapon_art import palette as new_palette
            palette = new_palette(weapon, index - 16)
        for tile in range(16):
            mix = .8 if tile in (2, 6, 8, 13, 14) else .25
            palette[tile] = tuple(round(c * (1 - mix) + weapon['color'][axis] * mix) for axis, c in enumerate(palette[tile]))
        art.PALETTES[name] = palette
        from domain_weapon_art import texture
        texture(weapon, index)
        model = json.loads((art.ASSETS / 'models/item' / (base + '.json')).read_text())
        if weapon.get('generation') == 2:
            from domain_weapon_art import item_model
            model = item_model(weapon, index - 16)
        for element in (model['elements'] if weapon.get('generation') != 2 else []):
            for field in ['from', 'to']:
                element[field][0] = round(8 + (element[field][0] - 8) * (1 - .005 * (index + 1)), 5)
                if element[field][1] > 6: element[field][1] = round(6 + (element[field][1] - 6) * (1 - .004 * (index + 1)), 5)
        for rune in range(0 if weapon.get("generation") == 2 else 3):
            y = 9 + rune * 2
            nearby = [e for e in model['elements'] if e['from'][1] <= y <= e['to'][1] and e['to'][0]-e['from'][0] > .5]
            if nearby:
                surface = min(nearby, key=lambda e: e['from'][2]); x = (surface['from'][0]+surface['to'][0])/2; z = surface['from'][2]
                art.box(model['elements'], weapon['title'] + ' flush luminous sigil ' + str(rune),
                        [x-.2, y, z-.02], [x+.2, y+.2, z+.02], 'rune', 'energy')
        assert len(model['elements']) <= 80, name
        from polish_weapon_pack import fit_display, continuous_uv
        fit_display(model)
        if weapon.get('generation') != 2: continuous_uv(model)
        model['textures'] = {'atlas': 'legendaryswords:item/' + name, 'particle': 'legendaryswords:item/' + name}
        write(art.ASSETS / 'models/item' / (name + '.json'), model)
        write(art.ASSETS / 'items' / (name + '.json'), {'model': {'type': 'minecraft:model', 'model': 'legendaryswords:item/' + name}})
        animated = copy.deepcopy(model); animated['display']['fixed'] = copy.deepcopy(model['display']['thirdperson_righthand'])
        write(art.ASSETS / 'models/item' / (name + '_animated.json'), animated)
        write(art.ASSETS / 'items' / (name + '_animated.json'), {'model': {
            'type': 'minecraft:select', 'property': 'minecraft:display_context',
            'cases': [{'when': ['thirdperson_lefthand', 'thirdperson_righthand'], 'model': {'type': 'minecraft:empty'}}],
            'fallback': {'type': 'minecraft:model', 'model': 'legendaryswords:item/' + name + '_animated'}}})
    metadata = json.loads((art.ROOT / 'pack.mcmeta').read_text()); metadata['pack']['description'] = 'LegendarySwords — 50 detailed legendary weapons'; write(art.ROOT / 'pack.mcmeta', metadata)


if __name__ == '__main__':
    generate_pack()
    generate_models(ROOT / 'src/main/resources/bettermodel')
    print('Generated additional item models, atlases, player rigs and signature effects.')
