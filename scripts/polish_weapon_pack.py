#!/usr/bin/env python3
"""Consistent first-person and inventory sizing without changing combat reach."""
def fit_display(model):
    lower=min(e['from'][1] for e in model['elements']);upper=max(e['to'][1] for e in model['elements'])
    factor=min(1,28/(upper-lower))
    for context in ['firstperson_righthand','firstperson_lefthand']:
        model['display'][context]['scale']=[round(.48*factor,5)]*3
    model['display']['gui']['scale']=[round(.48*factor,5)]*3


def continuous_uv(model):
    """Map long blade faces to one engraving, preserving tiny fittings and runes."""
    from generate_resource_pack_assets import TILES
    blade=[e for e in model['elements'] if e['from'][1]>=6 and e['to'][1]-e['from'][1]>=1]
    if not blade: return
    ymin=min(e['from'][1] for e in blade);ymax=max(e['to'][1] for e in blade)
    xmin=min(e['from'][0] for e in blade);xmax=max(e['to'][0] for e in blade)
    for e in blade:
        for face in ['north','south']:
            old=e['faces'][face]['uv']
            tile=next((k for k,v in TILES.items() if old==v),None)
            if tile not in ['blade','edge','metal','tip','energy','core','inlay','accent']: continue
            u,v,_,_=TILES[tile];a,b=e['from'],e['to']
            e['faces'][face]['uv']=[round(u+4*(a[0]-xmin)/(xmax-xmin),5),round(v+4*(ymax-b[1])/(ymax-ymin),5),round(u+4*(b[0]-xmin)/(xmax-xmin),5),round(v+4*(ymax-a[1])/(ymax-ymin),5)]
