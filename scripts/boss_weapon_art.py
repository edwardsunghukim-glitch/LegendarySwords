#!/usr/bin/env python3
"""Original boss implements and ordinary BetterModel monster meshes; no player equipment bones."""
import base64
import copy
import math
import generate_resource_pack_assets as art
import generate_bettermodel_assets as motion
from domain_weapon_art import effect_model

BOSS_IDS = ['angel_sovereign','granite_colossus','sky_leviathan','infernal_dragon','frost_titan']


def item_model(w):
    e=[]
    def box(name,a,b,tile='metal',side=None): art.box(e,w['title']+' '+name,a,b,tile,side)
    i=BOSS_IDS.index(w['id'])
    if i in [1,3]:
        # A hand-sized stone gauntlet / claw, rather than a recolored blade.
        box('wrist cuff',[6,0,6],[10,4,10],'guard','gold')
        box('palm',[5.5,3.5,5.5],[10.5,8.5,10.5],'metal','dark')
        for n in range(4 if i==1 else 3):
            x=5.6+n*(1.2 if i==1 else 1.8)
            box('finger '+str(n),[x,7.8,5.8],[x+1.15,11+n%2,9.8],'blade','metal')
            if i==3: box('talon '+str(n),[x,10.5+n%2,5.8],[x+.75,16+n%2,7],'edge','energy')
        box('thumb',[4.2,4.5,6.1],[5.7,7,9.4],'blade','rune')
        for n in range(3): box('glowing seam '+str(n),[5.45,4.2+n,5.45],[10.55,4.4+n,5.6],'energy','rune')
    elif i==4:
        box('shaft',[7.35,-1,7.35],[8.65,17,8.65],'grip','stripe')
        box('ice head',[2.5,15,5.5],[13.5,21,10.5],'blade','metal')
        box('hammer crown',[5.5,20.5,6.5],[10.5,23,9.5],'edge','energy')
        for n in range(5):
            x=3+n*2
            box('ice facets '+str(n),[x,15.5,5.25],[x+1.2,20.5,5.6],'rune','energy')
    else:
        box('grip',[7.4,-1,7.4],[8.6,6,8.6],'grip','gold')
        box('guard',[4.5,5.5,7],[11.5,7,9],'guard','gold')
        if i==0:
            for n in range(4):
                for side in [-1,1]:
                    x=8+side*(3+n*.8)
                    box('wing guard '+str(n)+str(side),[min(x,x+side),6,7.4],[max(x,x+side),8+n*.4,8.6],'edge','gold')
            box('sacred blade',[7,6.5,7.65],[9,25,8.35],'blade','edge')
            box('blade point',[7.6,24.8,7.65],[8.4,29,8.35],'edge','blade')
            box('central light',[7.8,7,7.55],[8.2,27,7.7],'energy','rune')
        else:
            for n in range(6):
                x=7+n*.18
                box('hooked fang '+str(n),[x,6.5+n*3,7.6],[x+2.1-n*.23,9.6+n*3,8.4],'blade','edge')
                box('dorsal fin '+str(n),[x-.8,7+n*3,7.7],[x+.1,9+n*3,8.3],'guard','rune')
    for part,element in enumerate(e[:8]):
        a,b=element['from'],element['to']
        for row in range(2):
            for col in range(2):
                x=a[0]+(b[0]-a[0])*(.2+col*.45); y=a[1]+(b[1]-a[1])*(.2+row*.45)
                box('flush carving '+str(part)+'/'+str(row)+'/'+str(col),[x-.08,y-.08,a[2]-.02],[x+.08,y+.08,a[2]+.02],'rune' if part%2 else 'energy','metal')
    return {'textures':{},'elements':e,'display':copy.deepcopy(art.DISPLAY)}


def boss_model(w):
    index=BOSS_IDS.index(w['id']);name='legendaryswords_'+w['id']+'_boss'
    model=effect_model(w,index+26)
    model.update(name=w['title']+' boss form',model_identifier=name,credit='Original LegendarySwords boss mesh and animation')
    model['elements']=[];model['groups']=[];model['outliner']=[];model['animations']=[]
    origins={'body':[0,14,0],'head':[0,29,0],'left':[-9,23,0],'right':[9,23,0],'tail':[0,11,5]}
    groups={}
    for bone,origin in origins.items():
        group={'uuid':motion.uid(name+'/'+bone),'name':bone,'origin':origin,'rotation':[0,0,0],'visibility':True}
        model['groups'].append(group);node={'uuid':group['uuid'],'isOpen':True,'children':[]};model['outliner'].append(node);groups[bone]=node
    def cube(label,a,b,bone='body',texture=0):
        ident=motion.uid(name+'/'+label)
        model['elements'].append({'uuid':ident,'name':label,'type':'cube','from':a,'to':b,'origin':origins[bone],'rotation':[0,0,0],
          'faces':{f:{'uv':[0,0,16,16],'texture':texture} for f in ['north','east','south','west','up','down']}})
        groups[bone]['children'].append(ident)
    if index==2:
        # Serpentine leviathan with a raised head, whiskers, dorsal fins and a long swimming tail.
        for n in range(9):
            y=12+math.sin(n*.65)*3;z=n*4-4;size=6-n*.35
            cube('serpent segment '+str(n),[-size,y-size/2,z],[size,y+size/2,z+4.2],'body' if n<4 else 'tail',n%2)
            cube('dorsal sail '+str(n),[-.7,y+size/2,z],[.7,y+size/2+5,z+3],'body' if n<4 else 'tail',1)
        cube('leviathan head',[-5,18,-12],[5,27,-3],'head')
        cube('sea jaws',[-4,18,-17],[4,23,-10],'head',1)
        for sign,bone in [(-1,'left'),(1,'right')]:
            cube('sweeping sea fin '+bone,[min(sign*5,sign*22),14,-3],[max(sign*5,sign*22),16,5],bone,1)
            cube('whisker '+bone,[min(sign*4,sign*15),24,-16],[max(sign*4,sign*15),25,-14],bone,1)
    else:
        broad=9 if index in [1,4] else 5.5
        cube('torso',[-broad,13,-4],[broad,28,5])
        cube('head',[-5,27,-4.5],[5,36,4.5],'head',1)
        for sign,bone in [(-1,'left'),(1,'right')]:
            x=sign*(broad+3)
            cube('upper arm '+bone,[x-3,15,-3],[x+3,27,4],bone)
            cube('forearm '+bone,[x-3.5,7,-4],[x+3.5,17,4.5],bone)
            cube('foot '+bone,[sign*4-3,0,-4],[sign*4+3,14,4.5],'body')
            if index==1:
                cube('boulder fist '+bone,[x-5,5,-6],[x+5,13,5],bone)
                for n in range(4): cube('stone knuckle '+bone+str(n),[x-4+n*2,9,-7],[x-2+n*2,13,-5],bone,1)
            if index in [0,3]:
                for n in range(7):
                    wx=sign*(broad+3+n*3)
                    cube('wing feather '+bone+str(n),[wx-1.8,18-n*.6,3],[wx+1.8,31-n*.6,4.2],bone,1 if index==0 else n%2)
                    if index==3: cube('wing spar '+bone+str(n),[wx-1.6,30-n*.6,2.5],[wx+1.6,32-n*.6,5],bone,1)
            if index==4:
                cube('ice shoulder '+bone,[x-5,23,-5],[x+5,30,6],bone,1)
        if index==0:
            for n in range(12):
                a=n*math.tau/12;x=math.cos(a)*7;z=math.sin(a)*7
                cube('halo '+str(n),[x-.9,38,z-.9],[x+.9,39,z+.9],'head',1)
        if index==3:
            cube('dragon snout',[-4,28,-12],[4,33,-4],'head')
            for sign in [-1,1]: cube('horn '+str(sign),[sign*4-1,33,0],[sign*4+1,42,2],'head',1)
            for n in range(7): cube('dragon tail '+str(n),[-3+n*.25,10-n*.5,4+n*3],[3-n*.25,14-n*.5,8+n*3],'tail',n%2)
        if index==4:
            cube('giant hammer handle',[7,8,-2],[9,34,0],'right',0)
            cube('giant ice hammer head',[2,30,-5],[17,37,4],'right',1)
            for n in range(5): cube('frost crown '+str(n),[-5+n*2,34,-1],[-4+n*2,42+(n%2)*2,1],'head',1)
    if index in [0,1,4]:
        for n in range(8):
            cube('back mantle '+str(n),[-4+n%2,12+n*1.5,4],[4-n%2,14+n*1.5,6],'tail',n%2)
    # Distinct stances, wing/fin beats, a visible reveal, and a heavy wind-up/impact/recovery.
    for action,length,loop in [('idle',2,'loop'),('transform',.65,'once'),('smash',.85,'once')]:
        animators={}
        for g in model['groups']:
            bone=g['name'];tag=name+'/'+action+'/'+bone;keys=[]
            for phase in [0,.2,.45,.65,1]:
                if action=='idle':
                    axis=2 if index in [0,2,3] and bone in ['left','right'] else 0
                    angle=math.sin(phase*math.tau)*(12+index*2) * (-1 if bone=='left' else 1)
                elif action=='smash':
                    axis=0;angle=([0,-55,65,20,0][[0,.2,.45,.65,1].index(phase)] if bone in ['left','right'] else [0,-12,20,6,0][[0,.2,.45,.65,1].index(phase)])
                    if index==2 and bone=='tail': angle*=.6
                else: axis=2;angle=math.sin(phase*math.pi)*(-25 if bone=='left' else 25 if bone=='right' else 0)
                rot=[0,0,0];rot[axis]=angle;keys.append(motion.keyframe(tag,'rotation',phase*length,rot))
                if action=='transform':
                    scale=.05+(phase/.45 if phase<.45 else 1)*.95
                    keys.append(motion.keyframe(tag,'scale',phase*length,(scale,scale,scale)))
            animators[g['uuid']]={'name':bone,'type':'bone','rotation_global':False,'quaternion_interpolation':False,'keyframes':keys}
        model['animations'].append({'uuid':motion.uid(name+'/'+action),'name':action,'loop':loop,'override':True,'length':length,'snapping':20,'animators':animators})
    if index == 3:
        breath = copy.deepcopy(next(a for a in model['animations'] if a['name'] == 'smash'))
        breath['name']='breath'; breath['uuid']=motion.uid(name+'/breath'); breath['length']=1.4
        for animator in breath['animators'].values():
            for key in animator['keyframes']:
                key['time']=round(key['time']*1.4/.85,5)
                key['uuid']=motion.uid(name+'/breath/'+animator['name']+'/'+str(key['time']))
                if animator['name']=='head':
                    key['data_points'][0]['x']=str(-18*math.sin(key['time']/1.4*math.pi))
        model['animations'].append(breath)
    return model


def attack_model(w):
    """Physical wing, fist, jaw, claw and hammer attacks with separate moving groups."""
    index=BOSS_IDS.index(w['id']);m=effect_model(w,index+26)
    m['elements']=[]
    names=[['left_wing','right_wing','feathers'],['left_fist','right_fist','rubble'],['upper_jaw','lower_jaw','sea_fangs'],['upper_jaw','dragon_claws','embers'],['hammer_head','hammer_handle','ice_shards']][index]
    for n,g in enumerate(m['groups']):
        g['name']=names[n]
        m['outliner'][n]['children']=[]
        for a in m['animations']: a['animators'][g['uuid']]['name']=names[n]
    def cube(label,center,size,group,texture=0):
        ident=motion.uid(w['id']+'/attack/'+label)
        m['elements'].append({'uuid':ident,'name':label,'type':'cube','from':[round(c-s/2,4) for c,s in zip(center,size)],
            'to':[round(c+s/2,4) for c,s in zip(center,size)],'origin':[0,0,0],'rotation':[0,0,0],
            'faces':{f:{'uv':[0,0,16,16],'texture':texture} for f in ['north','east','south','west','up','down']}})
        m['outliner'][group]['children'].append(ident)
    if index==0:
        for sign,group in [(-1,0),(1,1)]:
            for n in range(18): cube('sweeping feather '+str(group)+'/'+str(n),(sign*(4+n*.85),4+math.sin(n*.12)*4,0),(.8,6+n*.15,.4),group,n%2)
        for n in range(20): cube('shed feather '+str(n),(math.cos(n*2.4)*20,math.sin(n*2.4)*14,n%4),(.4,2,.3),2,1)
    elif index==1:
        for group in [0,1]:
            x=-8 if group==0 else 8
            for y in range(4):
                for z in range(4): cube('stone fist facet '+str(group)+'/'+str(y)+'/'+str(z),(x, y*2-3,z*2-3),(9,2.1,2.1),group,(y+z)%2)
            for n in range(4): cube('knuckle '+str(group)+'/'+str(n),(x-3+n*2,4,-4),(1.8,2,3),group,1)
        for n in range(16): cube('impact rubble '+str(n),(math.cos(n)*18,math.sin(n*1.7)*8,math.sin(n)*14),(2,2+n%3,2),2,n%2)
    elif index in [2,3]:
        for group in [0,1]:
            for n in range(18):
                a=n*math.pi/17;x=math.cos(a)*9;z=-math.sin(a)*11
                y=(1 if group==0 else -1)*(3+math.sin(a)*2)
                cube('jaw scale '+str(group)+'/'+str(n),(x,y,z),(2.2,2.2,2.2),group,n%2)
                if n%2==0: cube('fang '+str(group)+'/'+str(n),(x,y*.55,z),(1,4,1),2,1)
        if index==3:
            for claw in range(3):
                for n in range(8): cube('curved dragon claw '+str(claw)+'/'+str(n),((claw-1)*5,n*1.2-4,-n*1.5),(1.8-n*.13,2,2),1,1)
    else:
        for x in range(6):
            for y in range(4): cube('hammer ice facet '+str(x)+'/'+str(y),((x-2.5)*3,(y-1.5)*2,0),(3.1,2.1,8),0,(x+y)%2)
        for n in range(8): cube('hammer grip '+str(n),(0,-n*2-3,0),(2.2,2.2,2.2),1,n%2)
        for n in range(24): cube('shattered ice '+str(n),(math.cos(n)*18,math.sin(n*1.4)*8,math.sin(n)*14),(1,3+n%4,1),2,1)
    for a in m['animations']:
        for n,g in enumerate(m['groups']):
            animator=a['animators'][g['uuid']]
            animator['keyframes']=[k for k in animator['keyframes'] if k['channel']!='rotation']
            for phase,angle in [(0,-40),(.25,-45),(.5,15),(.8,5),(1,0)]:
                rot=[0,0,0];rot[0 if index in [1,2,3,4] else 2]=angle*(-1 if n==1 else 1)
                animator['keyframes'].append(motion.keyframe(w['id']+'/attack/'+a['name']+'/'+str(n),'rotation',phase*a['length'],rot))
    m['name']=w['title']+' physical attack';m['credit']='Original animated creature attack geometry by LegendarySwords'
    return m
