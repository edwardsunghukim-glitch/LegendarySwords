#!/usr/bin/env python3
"""Original geometry for the second collection; no prior weapon meshes are cloned."""
import base64
import copy
import math
import generate_resource_pack_assets as art
import generate_bettermodel_assets as motion

# Explicit, different blade profiles: (center offset, half-width, lower Y, upper Y).
PROFILES = [
 [(-.2,1.1,7,12),(0,1.4,12,18),(.2,.9,18,24),(0,.35,24,29)],
 [(0,1.6,7,11),(-.6,1.3,11,17),(-1.2,1,17,23),(-1.6,.45,23,28)],
 [(0,.7,7,13),(.3,.9,13,20),(-.2,.6,20,26),(0,.2,26,30)],
 [(0,1,7,12),(.5,1.6,12,17),(1,1.2,17,22),(1.2,.5,22,28)],
 [(0,.6,7,14),(0,1.8,14,16),(0,.6,16,23),(0,1.3,23,26),(0,.3,26,29)],
 [(0,1.2,7,12),(0,.55,12,16),(0,1.1,16,22),(0,.45,22,28)],
 [(0,.8,7,13),(-.4,1.1,13,19),(.5,1.1,19,24),(0,.3,24,29)],
 [(0,1.1,7,10),(-.3,.7,10,18),(.3,.7,18,26),(0,.25,26,29)],
 [(0,.7,7,14),(.4,.9,14,20),(.9,.8,20,25),(1.2,.3,25,29)],
 [(0,1.2,7,13),(0,.95,13,19),(0,.65,19,25),(0,.2,25,31)],
 [(0,1,7,12),(.5,1.1,12,18),(1,1,18,24),(1.8,.3,24,28)],
 [(0,1.5,7,14),(0,1.1,14,20),(0,.7,20,26),(0,.2,26,29)],
 [(-.3,.65,7,13),(.5,.65,13,19),(-.5,.65,19,25),(.2,.25,25,30)],
 [(0,1.7,7,14),(.3,1.5,14,21),(-.2,1.1,21,26),(0,.5,26,29)],
 [(0,.85,7,14),(0,1.1,14,20),(0,.7,20,26),(0,.25,26,30)],
 [(0,.45,7,20),(0,.4,20,26),(0,.15,26,30)],
 [(0,.9,7,13),(.4,.8,13,19),(1,.65,19,25),(1.5,.2,25,29)],
 [(0,1.3,7,14),(.2,1.6,14,20),(-.2,.9,20,26),(0,.3,26,30)],
 [(0,.9,7,13),(0,1.25,13,18),(0,.9,18,24),(0,.3,24,29)],
 [(0,.4,7,15),(0,.35,15,24),(0,.12,24,31)],
 [(0,1.6,7,13),(.6,1.9,13,20),(.6,1.4,20,27),(.3,.5,27,29)],
 [(0,1,7,13),(-.4,.7,13,19),(.4,.7,19,25),(0,.3,25,30)],
 [(0,.8,7,12),(0,1.3,12,17),(0,.65,17,22),(0,1.1,22,26),(0,.2,26,29)],
 [(0,.4,7,24),(-.7,.65,24,27),(-1.8,1.05,27,29),(-3,.9,26,28)],
 [(0,.9,7,13),(-.4,1.1,13,19),(.4,.8,19,25),(0,.25,25,29)],
 [(0,1.2,7,13),(.3,1.4,13,19),(-.3,1.1,19,25),(0,.4,25,30)],
]


def palette(weapon, index):
    color = weapon['color']
    mix = lambda rgb, t: tuple(round(rgb[a] * (1-t) + color[a] * t) for a in range(3))
    blade_materials = [(70,40,28),(30,22,45),(205,215,230),(70,120,135),(85,95,110),(170,135,75),(55,30,55),(165,225,205),(95,35,42),(205,180,95),(220,220,240),(145,95,65),(65,50,110),(28,25,24),(245,235,205),(35,75,85),(200,170,145),(85,65,55),(180,215,235),(115,185,170),(42,35,60),(175,150,190),(135,115,45),(110,145,130),(90,65,35),(130,65,30)]
    material = blade_materials[index]
    return [material,mix((245,250,255),.25),tuple(color),
            mix((75,90,110),.3),mix((45,28,32),.1),mix((220,170,80),.2),
            mix((230,245,255),.45),mix((18,20,30),.1),mix((250,245,205),.6),
            mix((135,150,165),.15),mix((240,250,255),.3),mix((105,95,115),.4),
            mix((190,150,95),.3),mix((80,75,90),.3),mix((255,255,255),.5),mix((12,15,22),.1)]


def item_model(weapon, index):
    e=[]
    def box(label,x0,y0,z0,x1,y1,z1,tile,side=None):
        art.box(e, weapon['title']+' '+label,[x0,y0,z0],[x1,y1,z1],tile,side)
    # A separately authored segmented grip, jeweled pommel and asymmetric guard.
    box('grip core',7.35,-1,7.35,8.65,5,8.65,'grip','dark')
    for i in range(7):
        y=-.7+i*.75
        box('braided wrap '+str(i),7.23,y,7.23,8.77,y+.16,8.77,'stripe','grip')
    box('pommel collar',7.1,-1.4,7.1,8.9,-.85,8.9,'metal','gold')
    box('faceted pommel',7.45,-2.4,7.45,8.55,-1.4,8.55,'gem','gold')
    guard=2.3+(index%4)*.22
    box('guard bridge',8-guard,5.2,7.1,8+guard,6.1,8.9,'guard','metal')
    for sign in [-1,1]:
        x=8+sign*(guard-.25)
        y=5.7+(index%3)*.35
        box('guard wing '+str(sign),x-.4,y,7.35,x+.4,y+1,8.65,'gold','accent')
    box('guard jewel',7.55,5.5,6.9,8.45,6.7,9.1,'gem','energy')
    for i,(offset,width,y0,y1) in enumerate(PROFILES[index]):
        x=8+offset
        box('forged blade facet '+str(i),x-width,y0,7.65,x+width,y1,8.35,'blade','metal')
        box('cutting edge '+str(i),x-width-.15,y0,7.8,x-width,y1,8.2,'edge','tip')
        box('spine ridge '+str(i),x+width,y0,7.8,x+width+.12,y1,8.2,'metal','edge')
        box('inlaid energy channel '+str(i),x-.14,y0+.2,7.56,x+.14,y1-.15,8.44,'energy','core')
    # Individual ornaments, not extra cubes on one reused blade.
    for rune in range(5+index%4):
        y=8+rune*2.25
        offset=PROFILES[index][min(len(PROFILES[index])-1,rune//2)][0]
        x=8+offset
        box('etched sigil '+str(rune),x-.35,y,7.51,x+.35,y+.22,7.65,'rune','energy')
    if index in [0,4,5,6,18,22]:
        for i in range(8):
            theta=i*math.pi/4
            x,y=8+math.cos(theta)*1.3,9+math.sin(theta)*1.3
            box('open halo '+str(i),x-.2,y-.2,7.48,x+.2,y+.2,7.66,'gold','energy')
    elif index==15:  # A real three-pronged silhouette.
        for side in [-1,1]:
            x=8+side*1.75
            box('trident tine '+str(side),x-.3,19,7.7,x+.3,27,8.3,'blade','energy')
            box('trident barb '+str(side),x-.15,27,7.8,x+.15,29,8.2,'edge','core')
            box('trident bridge '+str(side),min(x,8),18,7.7,max(x,8)+.1,19,8.3,'guard','gold')
    elif index in [8,13,17,20,25]:
        for i in range(4):
            side=-1 if i%2==0 else 1;y=12+i*3
            box('serrated thorn '+str(i),8+side*1.4-.28,y,7.8,8+side*1.4+.28,y+1.2,8.2,'tip','energy')
    elif index in [2,7,12,21]:
        for i in range(4):
            x=8+(-1 if i%2==0 else 1)*1.2;y=13+i*3
            box('floating crystal '+str(i),x-.22,y,7.7,x+.22,y+1,8.3,'gem','energy')
    else:
        for side in [-1,1]:
            box('wing filigree '+str(side),8+side*1.6-.2,7,7.68,8+side*1.6+.2,11,8.32,'inlay','gold')
    assert 30<=len(e)<=80, (weapon['id'],len(e))
    display=copy.deepcopy(art.DISPLAY)
    for context in ['thirdperson_righthand','thirdperson_lefthand']:
        display[context]['scale']=[.5]*3
    return {'credit':'Original second-collection geometry by LegendarySwords; no cloned weapon mesh.',
            'ambientocclusion':False,'gui_light':'front','textures':{'atlas':'legendaryswords:item/'+weapon['id'],'particle':'legendaryswords:item/'+weapon['id']},
            'elements':e,'display':display}


def effect_model(weapon,index):
    """Author new domain sigils, satellite fragments and ribs directly as tagged cubes."""
    name=weapon['id'];elements=[];groups=[];outliner=[];children={}
    for bone in ['energy_core','outer_halo','sparks']:
        ident=motion.uid(name+'/domain/'+bone)
        groups.append({'uuid':ident,'name':bone,'origin':[0,0,0],'rotation':[0,0,0],'visibility':True});children[bone]=[]
    def cube(label,center,size,bone,texture=0):
        ident=motion.uid(name+'/domain/'+label)
        elements.append({'uuid':ident,'name':label,'type':'cube','from':[round(c-s/2,4) for c,s in zip(center,size)],
            'to':[round(c+s/2,4) for c,s in zip(center,size)],'origin':[0,0,0],'rotation':[0,0,0],
            'faces':{face:{'uv':[0,0,16,16],'texture':texture} for face in ['north','east','south','west','up','down']}})
        children[bone].append(ident)
    for i in range(32):
        theta=i*math.tau/32
        # Match the attack: rune grid, thorn ring, crown, crescent, satellite or dome.
        if index in [4,5,7,11,16,21,24]:
            sides={4:6,5:12,7:4,11:4,16:5,21:3,24:5}[index]
            edge=int(i*sides/32);t=i*sides/32-edge
            a=edge*math.tau/sides;b=(edge+1)*math.tau/sides
            point=((math.cos(a)*(1-t)+math.cos(b)*t)*13,0,(math.sin(a)*(1-t)+math.sin(b)*t)*13)
        elif index in [10,14,19,23]:
            a=-math.pi*.8+theta*.8
            point=(math.cos(a)*13,math.sin(a)*13,(index-10)*.12)
        elif index in [2,6,12]:
            point=(math.cos(theta)*11,math.sin(theta*2)*5,math.sin(theta)*11)
        elif index in [3,15]:
            point=((i%8-3.5)*3,(i//8)*2,math.sin(theta)*2)
        else:
            point=(math.cos(theta)*13,math.sin(theta*(2+index%3))*(1+index%4),math.sin(theta)*13)
        cube('domain glyph '+str(i),point,(1.1,.7,1.1),'energy_core')
    for i in range(24):
        theta=i*math.tau/24
        height=2+index%5
        if index in [1,5,6,18,20]:
            point=(math.cos(theta)*10,math.sin(theta)*10,0)
        elif index in [9,13,17,22,25]:
            point=(math.cos(theta)*9,height+math.sin(theta*3)*4,math.sin(theta)*9)
        else:
            point=(math.cos(theta)*16,height,math.sin(theta)*16)
        cube('domain rib '+str(i),point,(.55,1.3,.55),'outer_halo',1)
    for i in range(8+index%5):
        theta=i*2.4
        cube('signature fragment '+str(i),(math.cos(theta)*19,3+i%4,math.sin(theta)*19),(.8,2.5+index*.04,.8),'sparks',i%2)
    for g in groups: outliner.append({'uuid':g['uuid'],'isOpen':True,'children':children[g['name']]})
    model={'meta':{'format_version':'5.0','model_format':'free','box_uv':False},'name':weapon['title']+' domain manifestation',
        'model_identifier':'legendaryswords_'+name+'_slash','credit':'Original domain geometry by LegendarySwords',
        'resolution':{'width':16,'height':16},'elements':elements,'groups':groups,'outliner':outliner,'textures':[],'animations':[]}
    for i in range(2):
        rgb=tuple(weapon['color']) if i==0 else tuple(min(255,c+60) for c in weapon['color'])
        model['textures'].append({'name':name+'_domain_'+str(i)+'.png','id':str(i),'width':16,'height':16,'uv_width':16,'uv_height':16,
            'source':'data:image/png;base64,'+base64.b64encode(motion.png(rgb,(255,245,225))).decode()})
    for action,length,peak in [('swing',.55,1.1),('ability',1.1+index*.015,1.6),('ultimate',1.5+index*.02,2.4)]:
        length = round(length, 5)
        animators={}
        for j,g in enumerate(groups):
            bone=g['name'];tag=name+'/domain/'+action+'/'+bone
            keys=[]
            for phase,scale in [(0,.01),(.18,peak*.65),(.45,peak),(.8,peak*1.12),(1,.01)]:
                keys.append(motion.keyframe(tag,'scale',phase*length,(scale,scale,scale)))
            for phase in [0,.5,1]:
                rot=[0,0,0];rot[(index+j)%3]=(phase-.5)*(35+index*2)*(-1 if j==1 else 1)
                keys.append(motion.keyframe(tag,'rotation',phase*length,rot))
            animators[g['uuid']]={'name':bone,'type':'bone','rotation_global':False,'quaternion_interpolation':False,'keyframes':keys}
        model['animations'].append({'uuid':motion.uid(name+'/domain/'+action),'name':action,'loop':'once','override':True,'length':length,'snapping':20,'animators':animators})
    return model


def texture(weapon, index):
    """Paint a separate themed atlas: etching topology and material finish vary by weapon."""
    name=weapon['id'];colors=art.PALETTES[name];pixels=[];emissive=[]
    themed_index=index-16 if weapon.get('generation')==2 else index+26
    # Each predicate draws a different motif, rather than tinting a shared texture.
    def motif(x,y,k):
        r=math.hypot(x-15.5,y-15.5);theta=math.atan2(y-15.5,x-15.5)
        if k==0: return abs(y-16-7*math.sin(x*.32))<1.3 or abs(x-15-y*.2)<1
        if k==1: return abs(r-(4+((theta+math.pi)*1.8)))<1 or r<2
        if k==2: return (x*13+y*7)%97<3 or abs(y-x*.6-7)<.6
        if k==3: return abs(y-12-5*math.sin(x*.25))<1 or abs(y-24-3*math.sin(x*.3))<1
        if k==4: return x%9==0 or (y%9==0 and x%9<4)
        if k==5: return abs(r-11)<1 or abs(theta-(-math.pi/2))<.07 or abs(y-16-x*.2)<.6
        if k==6: return (abs(r-9)<1 and x<18) or (abs(math.hypot(x-20,y-16)-7)<1 and x>14)
        if k==7: return abs((x+y)%16-8)<1 or abs((x-y)%16-8)<1
        if k==8: return abs(x-16-3*math.sin(y*.4))<1 or (y%8<2 and abs(x-16)<8)
        if k==9: return abs(r-6)<1 or (r<14 and abs(math.sin(theta*8))<.12)
        if k==10: return abs(r-10)<1 and x<20 or abs(r-7)<1 and x<14
        if k==11: return (x%10<2 and 5<y<27) or (y%10<2 and 5<x<27)
        if k==12: return abs(y-16-8*math.sin(x*.2))<1 or (x*17+y*23)%131<3
        if k==13: return abs(x-(y//6)*3-5)<1 or abs(x-(y//7)*-2-25)<1
        if k==14: return abs(abs(x-16)-(y*.45+2))<1 or (y%7<1 and abs(x-16)>4)
        if k==15: return abs(x-16)<1 or (y<18 and (abs(x-7)<1 or abs(x-25)<1)) or abs(y-18)<1
        if k==16: return abs(x-9-3*math.sin(y*.23))<1 or abs(x-23-3*math.sin(y*.23+1))<1
        if k==17: return (x*7+y*3)%71<2 or abs(x-y+7)<1 and y>14
        if k==18: return abs(x-16)<1 or abs(y-16)<1 or (abs(x-y)<1 or abs(x+y-32)<1) and r<12
        if k==19: return (x+2*y)%13<1 or abs(y-16-3*math.sin(x*.45))<1
        if k==20: return abs(r-4)<1 or abs(r-8)<1 or abs(r-13)<1
        if k==21: return abs(y-5-(x if x<16 else 32-x)*1.2)<1 or abs(y-25)<1
        if k==22: return abs(y-16-7*math.sin(x*.5))<1 or (x%8==0 and y%6<3)
        if k==23: return abs(r-12)<1 and y<18 or abs(y-16)<1 and x<15
        if k==24: return abs(x-16-2*math.sin(y*.2))<1 or abs(y%10-5)<1 and abs(x-16)<y*.25
        if k==25: return abs((x+y)%12-6)<1 or abs((x-y)%12-6)<1 and x%12<6
        # The older expansion gets sixteen individual knotwork / rune maps as well.
        a=k-26
        return ((x*(a%5+2)+y*(a%7+1))%(17+a)<2 or
                (abs(r-(5+a%6))<.8 and abs(math.sin(theta*(3+a%5)))<.45) or
                (x==5+a%9 and (y+a)%9<5))
    for frame in range(4):
        for y in range(128):
            for x in range(128):
                tile=x//32+4*(y//32);lx=x%32;ly=y%32
                luminous=tile in [2,6,8,13,14]
                base=colors[tile]
                # Hammered, brushed, pearlescent, stone and organic finishes alternate.
                finish=themed_index%5
                grain=[((lx*13+ly*19)%11)-5,round(6*math.sin(lx*.7)),round(8*math.cos((lx+ly)*.18)),((lx//4*7+ly//4*11)%13)-8,round(7*math.sin(lx*.23+ly*.09))][finish]
                bevel=12 if lx<3 or ly<2 else -18 if lx>28 else 0
                etched=motif(lx,(ly-frame*3)%32 if luminous else ly,themed_index)
                if tile==4: etched=(lx+ly*(1+themed_index%3))%(6+themed_index%4)<2
                if etched:
                    rgb=[min(255,c+55+frame*3) for c in weapon['color']] if luminous else [max(0,c-65) for c in base]
                else:
                    rgb=[max(0,min(255,c+grain+bevel)) for c in base]
                    if luminous: rgb=[round(c*.48) for c in rgb]
                pixels.extend(rgb+[255]);emissive.extend(rgb+[255] if luminous and etched else [0,0,0,0])
    path=art.ASSETS/'textures/item'/f'{name}.png'
    art.png(path,128,512,pixels);art.png(path.with_name(name+'_e.png'),128,512,emissive)
    metadata={'animation':{'frametime':3+themed_index%8,'width':128,'height':128,'interpolate':True,'frames':[0,1,2,3]}}
    art.write_json(path.with_suffix('.png.mcmeta'),metadata);art.write_json(path.with_name(name+'_e.png.mcmeta'),metadata)
