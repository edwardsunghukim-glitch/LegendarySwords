#!/usr/bin/env python3
"""Render a front-view contact sheet from actual new model geometry and atlas UVs.

Optional development tool: requires Pillow. It does not alter authored assets.
"""
from PIL import Image,ImageDraw
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
assets=ROOT/'resource-pack/assets/legendaryswords'
weapons=json.loads((ROOT/'scripts/expanded_weapons.json').read_text())[16:]
canvas=Image.new('RGB',(1560,980),(17,23,34));draw=ImageDraw.Draw(canvas)
for i,w in enumerate(weapons):
    x0=(i%9)*173;y0=(i//9)*325
    model=json.loads((assets/'models/item'/f'{w["id"]}.json').read_text())
    atlas=Image.open(assets/'textures/item'/f'{w["id"]}.png').convert('RGBA')
    for e in sorted(model['elements'],key=lambda e:e['from'][2],reverse=True):
        uv=e['faces']['north']['uv'];a,b=e['from'],e['to']
        rect=(round(x0+82+(a[0]-8)*8),round(y0+280-(b[1]+2.5)*8),round(x0+82+(b[0]-8)*8),round(y0+280-(a[1]+2.5)*8))
        width=max(1,rect[2]-rect[0]);height=max(1,rect[3]-rect[1])
        tile=atlas.crop(tuple(round(v*8) for v in uv)).resize((width,height))
        canvas.paste(tile,(rect[0],rect[1]),tile)
    draw.text((x0+8,y0+296),w['title'],fill=(225,230,240))
output=ROOT/'target/LegendarySwords-new-weapons-preview.png'
output.parent.mkdir(exist_ok=True);canvas.save(output);print(output)
