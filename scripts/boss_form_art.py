"""Original sculpted boss silhouettes and painted material atlases.

All geometry is authored here; uploaded references are never copied into releases.
The core control bones retain the runtime animation contract; serpents add linked spines and jaws.
"""
import base64
import io
import math
from PIL import Image, ImageDraw
import generate_bettermodel_assets as motion

PALETTES = [
 [(220,231,239),(153,177,201),(201,159,65),(255,241,181),(52,71,94),(227,191,155),(244,250,255),(76,127,164)],
 [(86,81,72),(124,116,97),(49,47,46),(111,222,192),(36,55,52),(151,145,126),(67,102,90),(198,178,118)],
 [(24,91,118),(49,153,169),(186,216,185),(87,228,235),(12,36,62),(210,226,211),(67,111,145),(198,158,86)],
 [(56,33,55),(108,48,67),(167,93,58),(255,177,57),(24,14,29),(219,191,145),(135,54,40),(237,93,37)],
 [(92,136,173),(178,220,236),(57,79,114),(160,244,255),(27,41,69),(223,237,246),(105,161,191),(206,176,117)],
]


def atlas(index):
    image=Image.new('RGBA',(128,128));draw=ImageDraw.Draw(image)
    for tile,color in enumerate(PALETTES[index]):
        x=(tile%4)*32;y=(tile//4)*64
        for v in range(64):
            for u in range(32):
                # Shaded borders and quiet brush variation, rather than repeating stripes.
                edge=min(u,31-u,v,63-v)
                shade=.72+.24*min(1,edge/5)+.08*(1-v/63)
                grain=math.sin(u*.7+v*.29+tile)*2
                draw.point((x+u,y+v),fill=tuple(max(0,min(255,round(c*shade+grain))) for c in color)+(255,))
        light=tuple(min(255,c+24) for c in color)+(255,)
        dark=tuple(max(0,c-30) for c in color)+(255,)
        if index in (2,3) and tile in (0,1,6):
            for row in range(8):
                for col in range(4):
                    cx=x+col*8+(4 if row%2 else 0);cy=y+row*8
                    draw.arc((cx-4,cy-3,cx+4,cy+5),0,180,fill=dark,width=1)
                    draw.arc((cx-3,cy-2,cx+3,cy+4),20,150,fill=light,width=1)
        elif index==1 and tile in (0,1,2):
            for n in range(4):
                cx=x+5+n*6
                draw.line([(cx,y+4),(cx-2,y+19),(cx+3,y+31),(cx,y+47),(cx+2,y+59)],fill=dark,width=1)
        elif index==0 and tile in (0,1,6):
            draw.line((x+16,y+3,x+16,y+60),fill=light,width=1)
            for n in range(8):
                yy=y+8+n*6
                draw.line((x+4,yy+5,x+16,yy,x+27,yy+5),fill=dark,width=1)
        elif index==4 and tile in (0,1,6):
            draw.polygon([(x+4,y+5),(x+26,y+14),(x+12,y+56)],outline=light)
            draw.line((x+26,y+14,x+4,y+44),fill=dark,width=1)
        elif tile in (2,7):
            draw.rectangle((x+3,y+3,x+28,y+60),outline=light,width=1)
            for yy in range(y+10,y+60,10):draw.line((x+10,yy,x+16,yy-3,x+22,yy),fill=dark,width=1)
    buf=io.BytesIO();image.save(buf,format='PNG')
    return base64.b64encode(buf.getvalue()).decode()


BOSS_SCALES = (3.5, 4.0, 3.0, 2.6, 3.5)


def serpent_point(index,t):
    if index == 2:
        return (math.sin(t*math.pi*1.8)*12, 24+math.sin(t*math.pi*1.3)*5, 2+96*t)
    return (math.sin(t*math.pi*1.8)*16, 25+math.sin(t*math.pi*1.7)*10-10*t, 10+72*t)


def rebuild(model,index):
    name=model['model_identifier'];model['elements']=[]
    bones={g['name']:g for g in model['groups']}
    nodes={g['uuid']:g for g in model['outliner']}
    for node in nodes.values():node['children']=[]
    if index in (2,3):
        jaw={'uuid':motion.uid(name+'/jaw'),'name':'jaw','origin':([0,23,-18] if index==2 else [0,29,-18]),'rotation':[0,0,0],'visibility':True}
        model['groups'].append(jaw);bones['jaw']=jaw
        node={'uuid':jaw['uuid'],'isOpen':True,'children':[]};model['outliner'].append(node);nodes[jaw['uuid']]=node
        bones['head']['origin']=[0,27 if index==2 else 32,-12];bones['left']['origin']=[-8,30,1];bones['right']['origin']=[8,30,1]
        for animation in model['animations']:
            frames=[]
            for phase,angle in [(0,0),(.2,12),(.45,32),(.65,20),(1,0)]:
                opening=angle if animation['name'] in ('smash','breath') else math.sin(phase*math.tau)*3
                frames.append(motion.keyframe(name+'/'+animation['name']+'/jaw','rotation',phase*animation['length'],(-opening,0,0)))
                if animation['name']=='transform':
                    scale=.05+min(1,phase/.45)*.95
                    frames.append(motion.keyframe(name+'/transform/jaw','scale',phase*animation['length'],(scale,scale,scale)))
            animation['animators'][jaw['uuid']]={'name':'jaw','type':'bone','rotation_global':False,'quaternion_interpolation':False,'keyframes':frames}
    if index in (2,3):
        for number in range(1,7):
            t=number*4/31
            origin=list(serpent_point(index,t))
            group={'uuid':motion.uid(name+'/spine_'+str(number)),'name':'spine_'+str(number),'origin':origin,'rotation':[0,0,0],'visibility':True}
            model['groups'].append(group);bones[group['name']]=group
            node={'uuid':group['uuid'],'isOpen':True,'children':[]};model['outliner'].append(node);nodes[group['uuid']]=node
            for animation in model['animations']:
                keys=[]
                for phase in (0,.2,.45,.65,1):
                    strength=3 if animation['name']=='idle' else 6
                    angle=math.sin(phase*math.tau+number*.4)*strength
                    keys.append(motion.keyframe(name+'/'+animation['name']+'/'+group['name'],'rotation',phase*animation['length'],(angle*.3,angle,0)))
                animation['animators'][group['uuid']]={'name':group['name'],'type':'bone','rotation_global':False,'quaternion_interpolation':False,'keyframes':keys}
    counter=0
    def cube(label,center,size,bone='body',tile=0,rotation=(0,0,0)):
        nonlocal counter
        counter+=1;ident=motion.uid(name+'/sculpt/'+label+'/'+str(counter))
        x=(tile%4)*32;y=(tile//4)*64
        model['elements'].append({'uuid':ident,'name':label,'type':'cube',
            'from':[round(c-s/2,4) for c,s in zip(center,size)],'to':[round(c+s/2,4) for c,s in zip(center,size)],
            'origin':list(center),'rotation':list(rotation),
            'faces':{f:{'uv':[x+1,y+1,x+31,y+63],'texture':0} for f in ('north','south','east','west','up','down')}})
        nodes[bones[bone]['uuid']]['children'].append(ident)
    def oval(label,center,radii,bone='body',tile=0,slices=7):
        # Overlapping cross sections make a closed, rounded silhouette without holes.
        for n in range(slices):
            yy=-1+(n+.5)*2/slices;factor=math.sqrt(max(.01,1-yy*yy))
            cube(label+' contour '+str(n),(center[0],center[1]+yy*radii[1],center[2]),
                 (2*radii[0]*factor,2*radii[1]/slices+.12,2*radii[2]*factor),bone,tile)
            for face in ('north','south','east','west'):
                model['elements'][-1]['faces'][face]['uv']=[(tile%4)*32+2,(tile//4)*64+2+n*60/slices,(tile%4)*32+30,(tile//4)*64+2+(n+1)*60/slices]
    def taper(label,a,b,radius,bone,tile):
        delta=[b[k]-a[k] for k in range(3)];length=math.sqrt(sum(v*v for v in delta))
        angle=math.degrees(math.atan2(-delta[0],delta[1]))
        pitch=math.degrees(math.atan2(delta[2],math.hypot(delta[0],delta[1])))
        for n in range(4):
            t=(n+.5)/4;center=[a[k]+delta[k]*t for k in range(3)]
            r=radius*(1-.78*t)
            cube(label+' tapered '+str(n),center,(r*2,length/4+.15,r*2),bone,tile,(pitch,0,angle))
    def eyes(y,z,width):
        for sign in (-1,1):
            cube('eye socket',(sign*width,y,z), (2.3,1.4,.6),'head',4)
            cube('glowing iris',(sign*width,y,z-.34),(.65,.9,.16),'head',3)
    if index in (2,3):
        # Two independently designed creatures: aquatic ribbon serpent and winged dragon.
        for n in range(32):
            t=n/31;cx,cy,cz=serpent_point(index,t)
            radius=(6.6*(1-.86*t) if index==2 else 5.6*(1-.86*t))
            bone='body' if n<4 else 'spine_'+str(n//4) if n<28 else 'tail'
            oval(('leviathan mantle ' if index==2 else 'dragon tail ')+str(n),
                 (cx,cy,cz),(radius,radius*.78,2.4 if index==2 else 1.95),bone,0,8)
            cube('ventral scute '+str(n),(cx,cy-radius*.64,cz),(radius*1.42,.6,3.3 if index==2 else 2.7),bone,2)
            if index==2:
                # Thin continuous ribbon on the back, with swept leading rays.
                height=5+math.sin(t*math.pi)*4
                cube('ribbon sail '+str(n),(cx,cy+radius*.74+height/2,cz),(.45,height,3.4),bone,6,(0,0,math.sin(t*math.pi)*12))
                if n%3==0:
                    taper('fin ray '+str(n),(cx,cy+radius*.7,cz),(cx,cy+radius*.7+height,cz+2),.28,bone,3)
            elif n%2==0:
                taper('obsidian crest '+str(n),(cx,cy+radius*.8,cz),(cx,cy+radius*.8+3.2*(1-t)+.8,cz+1.5),.7,bone,4)
        if index==2:
            oval('leviathan tapered skull',(0,29,-9),(6.5,4.5,9),'head',0,12)
            oval('wide sea muzzle',(0,26,-19),(7.4,2.3,8),'head',1,10)
            cube('sea mouth interior',(0,23.8,-19),(12.8,.75,12),'head',4)
            oval('hinged sea jaw',(0,22.6,-19),(6.8,1.3,7.6),'jaw',2,6)
            for sign,bone in [(-1,'left'),(1,'right')]:
                # Layered swept pectoral fins: long rays, progressively shorter webbing.
                for n in range(12):
                    width=10+math.sin(n/11*math.pi)*12
                    x=sign*(6+width/2);z=2+n*1.15
                    cube('pectoral web '+bone+str(n),(x,23-n*.35,z),(width+.3,.45,1.4),bone,1,(0,sign*12,sign*8))
                    if n%3==0:taper('pectoral ray '+bone+str(n),(sign*6,23,z),(sign*(6+width),20,z+2),.25,bone,3)
                for n in range(5):
                    taper('gill frill '+bone+str(n),(sign*6,28,-8+n),(sign*(10+n*.5),31+n*.5,-3+n),.5,'head',6)
                taper('sea antler base '+bone,(sign*4.5,32,-4),(sign*8,36,1),.85,'head',7)
                taper('sea antler tip '+bone,(sign*8,36,1),(sign*11,37,7),.55,'head',7)
                taper('flowing barbels '+bone,(sign*6,26,-25),(sign*13,25,-30),.25,'head',3)
                cube('sea eye socket '+bone,(sign*6.35,29,-15),(1.1,1.9,3.2),'head',4,(0,sign*15,0))
                cube('sea eye '+bone,(sign*6.94,29,-15),(.14,.55,2.1),'head',3,(0,sign*15,sign*18))
                taper('leviathan great fang '+bone,(sign*5,25,-23),(sign*5.5,20,-24),.65,'head',5)
                taper('leviathan jaw barb '+bone,(sign*5.5,22,-16),(sign*10,19,-11),.8,'jaw',7)
            for n in range(12):
                x=(n-5.5)*1.05
                taper('sea upper tooth '+str(n),(x,25,-25),(x,23.7,-25),.25,'head',5)
                taper('sea lower tooth '+str(n),(x,23,-25),(x,24,-25),.22,'jaw',5)
            cx,cy,cz=serpent_point(index,1)
            for sign in (-1,1):
                for n in range(8):
                    cube('crescent tail fin '+str(sign)+'/'+str(n),(cx+sign*(1+n*.8),cy+n*.5,cz-1+n*.35),(1.1,.7,8-n*.6),'tail',1,(0,sign*22,sign*18))
        else:
            oval('dragon powerful chest',(0,25,0),(5.6,4.8,8),'body',0,12)
            oval('dragon neck',(0,30,-10),(4.8,7.5,6),'body',0,12)
            oval('dragon angular skull',(0,35,-15),(5.6,4.4,7),'head',0,10)
            oval('dragon long snout',(0,32,-23),(4.2,2.4,7.5),'head',1,8)
            cube('dragon mouth shadow',(0,29.7,-23),(7,.8,11),'head',4)
            oval('dragon lower jaw',(0,28.4,-23),(3.8,1.25,7),'jaw',2,6)
            for sign,bone in [(-1,'left'),(1,'right')]:
                cube('swept cheek armor '+bone,(sign*4.7,33,-16),(2.2,4.5,8),'head',1,(0,sign*22,sign*15))
                cube('dragon brow '+bone,(sign*4.5,35,-20),(2.0,.8,4.3),'head',4,(0,sign*15,sign*18))
                cube('dragon eye recess '+bone,(sign*4.65,34,-20),(1.15,1.2,2.8),'head',4)
                cube('dragon ember eye '+bone,(sign*5.25,34,-20),(.12,.55,2.1),'head',3,(0,0,sign*18))
                taper('dragon great tusk '+bone,(sign*3.2,31,-25),(sign*3.8,26,-25),.65,'head',5)
                taper('dragon whisker base '+bone,(sign*3.8,32,-28),(sign*13,34,-29),.35,'head',2)
                taper('dragon wind whisker '+bone,(sign*13,34,-29),(sign*21,37,-22),.22,'head',2)
                taper('dragon swept horn '+bone,(sign*3.8,38,-12),(sign*7.5,43,-5),1.1,'head',5)
                taper('dragon horn tip '+bone,(sign*7.5,43,-5),(sign*10,44,3),.7,'head',5)
                taper('dragon jaw spike '+bone,(sign*4,31,-13),(sign*8,29,-7),.75,'head',4)
                # Small articulated claw limbs on a long airborne serpent, as in the reference style.
                for leg,t in [('fore',0),('hind',12/31)]:
                    center=serpent_point(index,t)
                    limb='body' if leg=='fore' else 'spine_3'
                    x=center[0]+sign*5;z=-4 if leg=='fore' else center[2]
                    y=24 if leg=='fore' else center[1]
                    oval(leg+' shoulder '+bone,(x,y,z),(2.4,3.1,2.6),limb,0,8)
                    oval(leg+' elbow '+bone,(x+sign*3,y-3,z-2),(1.5,2.7,1.8),limb,1,8)
                    oval(leg+' talon palm '+bone,(x+sign*4,y-4,z-5),(2.1,1.1,2.1),limb,0,6)
                    for digit in range(3):
                        fx=x+sign*4+(digit-1)*1.3
                        taper(leg+' claw '+bone+str(digit),(fx,y-4,z-6),(fx,y-6,z-9),.45,limb,5)
                # Broad bat-wing canopy, scalloped trailing edge and curved outer thumb.
                for n in range(24):
                    x=sign*(7+n*1.5);top=34+math.sin(n/23*math.pi)*9
                    height=15*(1-n/29)+2*math.sin(n*math.pi/6)**2
                    cube('bat membrane '+bone+str(n),(x,top-height/2,3+n*.3),(1.65,height,.45),bone,6)
                for n in range(5):
                    taper('wing finger '+bone+str(n),(sign*8,34,2),(sign*(17+n*6),29+n*2.3,3+n*1.4),.5,bone,2)
                taper('wing leading bone '+bone,(sign*8,34,2),(sign*29,42,7),.85,bone,0)
                taper('wing thumb '+bone,(sign*29,42,7),(sign*33,47,7),.65,bone,5)
                cube('nostril '+bone,(sign*2.5,33,-29),(.9,.6,.7),'head',4)
            for n in range(6):
                x=(n-2.5)*1.0
                taper('dragon hanging beard '+str(n),(x,28,-17),(x*1.7,21,-15),.45,'jaw',4)
            for n in range(10):
                x=(n-4.5)*.65
                taper('dragon upper fang '+str(n),(x,31,-28),(x,29.3,-28),.28,'head',5)
                taper('dragon lower fang '+str(n),(x,29,-28),(x,30.2,-28),.25,'jaw',5)
            for n in range(8):cube('dragon neck scute '+str(n),(0,23+n*1.5,-8-n*.9),(6-n*.35,1.5,.7),'body',2)
    else:
        width=8 if index in (1,4) else 5
        oval('ribcage',(0,23,0),(width,9,4.5),'body',0,9)
        oval('waist',(0,14,0),(width*.72,4,3.8),'body',1)
        oval('face',(0,33,-.5),(4.5,5,3.7),'head',4 if index==0 else 0 if index==1 else 2,10)
        # Narrow recessed eyes beneath a sloping brow, rather than a friendly square face.
        for sign in (-1,1):
            cube('recessed visor '+str(sign),(sign*2,34,-4.12),(3.1,.85,.4),'head',4,(0,0,-sign*12))
            cube('burning gaze '+str(sign),(sign*2,34,-4.36),(2.4,.24,.12),'head',3,(0,0,-sign*12))
        for sign,bone in [(-1,'left'),(1,'right')]:
            x=sign*(width+3)
            oval('shoulder '+bone,(x,26,0),(4,4,4),bone,1)
            oval('upper arm '+bone,(x,21,0),(2.8,5.5,3),bone,0)
            oval('forearm '+bone,(x+sign,13,-1),(3.3,5,3.3),bone,1)
            oval('hand '+bone,(x+sign,8,-1),(3.5,3,3),bone,0)
            for digit in range(4):
                fx=x+sign-2.6+digit*1.7
                oval('finger '+bone+str(digit),(fx,6,-3.2),(.75,2,1.1),bone,1,4)
                if index==3:taper('talon '+bone+str(digit),(fx,5,-4),(fx,3,-6),.65,bone,5)
            oval('thigh '+bone,(sign*3.3,8,0),(3,6,3),'body',0)
            oval('greave '+bone,(sign*3.3,3,-.3),(2.7,3,2.9),'body',1)
            oval('foot '+bone,(sign*3.3,1,-2),(3,1.3,4),'body',0,4)
        if index==0:
            # Two feather layers, separated flight feathers, and raised gold quills.
            for sign,bone in [(-1,'left'),(1,'right')]:
                for layer in range(2):
                    for n in range(13):
                        x=sign*(6+n*1.8);y=29+math.sin(n/12*math.pi)*6-layer*3
                        length=11+n*.55-layer*3;rot=(0,0,-sign*(12+n*3))
                        taper('feather '+bone+str(layer)+'/'+str(n),(x,y,4+layer*.65),
                              (x+sign*length*.3,y-length,4+layer*.65),1.2,bone,6 if layer==0 else 0)
                        taper('gold quill '+bone+str(layer)+'/'+str(n),(x,y,3.6+layer*.65),
                              (x+sign*length*.22,y-length*.75,3.6+layer*.65),.11,bone,2)
                taper('wing bone '+bone,(sign*5,28,4),(sign*26,34,4),1.2,bone,2)
            for n in range(24):
                a=n*math.tau/24
                cube('halo inlay '+str(n),(math.cos(a)*6.5,40,math.sin(a)*6.5),(2.0,.65,.65),'head',2,(0,90-math.degrees(a),0))
            for n in range(9):
                cube('robe pleat '+str(n),(-4+n,10,3.5),(1,11,.8),'tail',0,(0,0,(n-4)*2))
            cube('breastplate',(0,24,-4.35),(7.5,8,.7),'body',2)
            cube('sacred gem',(0,25,-4.9),(1.8,2.3,.6),'body',3,(0,0,45))
            # Faceless archangel mask and engraved layered ceremonial armor.
            for sign,bone in [(-1,'left'),(1,'right')]:
                cube('gold mask cheek '+bone,(sign*2.7,32,-3.9),(2.8,5.1,.65),'head',2,(0,sign*12,-sign*15))
                taper('helmet swept crest '+bone,(sign*3,36,0),(sign*6,42,3),.9,'head',2)
                for row in range(4):
                    cube('pauldron feather '+bone+str(row),(sign*(8+row*.55),27-row*.75,-1),(3.5,2.4,5),bone,6,(0,0,sign*(12+row*8)))
                for n in range(9):
                    x=sign*(5+n*1.1)
                    taper('secondary wing '+bone+str(n),(x,22,5),(x+sign*5,9-n*.35,5),.85,bone,1)
                for row in range(4):
                    cube('vambrace gold filigree '+bone+str(row),(sign*9,11+row*1.2,-4.15),(4.8,.25,.18),bone,2)
            cube('angel mask ridge',(0,32,-4.3),(1.1,8,.7),'head',2)
            for n in range(5):
                cube('breastplate radiance '+str(n),(0,22+n,-4.8),(4.9-n*.55,.23,.13),'body',3)
            for n in range(24):
                a=n*math.tau/24
                cube('outer halo rune '+str(n),(math.cos(a)*9,40,math.sin(a)*9),(1.35,.45,.45),'head',3,(0,90-math.degrees(a),0))
            for n in range(8):
                taper('waist armor petal '+str(n),((n-3.5)*1.2,14,-3.8),((n-3.5)*1.65,8,-4.8),.75,'body',2)

        elif index==1:
            for row in range(5):
                for col in range(4):
                    cube('overlapping stone plate '+str(row)+'/'+str(col),((col-1.5)*3.4,15+row*3,-4.1),(3.7,3.5,1.5),'body',row%2,(0,0,(col-1.5)*5))
            for sign,bone in [(-1,'left'),(1,'right')]:
                oval('giant knuckle '+bone,(sign*12,7,-2),(5,4,4),bone,1)
                for n in range(5):
                    cube('rune fissure '+bone+str(n),(sign*12+(n-2)*1.3,8,-5.95),(.22,2.8,.12),bone,3,(0,0,(n-2)*12))
            for n in range(9):
                oval('back boulder '+str(n),((n%3-1)*4,18+n//3*4,4),(2.9,3,2),'tail',1,4)
            cube('stone jaw',(0,30,-3),(7,2.5,2),'head',1)
            # Broken slate face, irregular shoulders and exposed luminous fault lines.
            for sign,bone in [(-1,'left'),(1,'right')]:
                cube('stone brow ridge '+bone,(sign*2.8,35,-4),(4.4,1.6,1.5),'head',1,(0,sign*12,-sign*14))
                cube('stone cheek plate '+bone,(sign*3.4,32,-3.7),(2.5,3.7,1.8),'head',0,(0,sign*18,sign*12))
                for n in range(6):
                    x=sign*(10+n*.55)
                    cube('fractured shoulder slate '+bone+str(n),(x,27+n%2*.6,1),(3.3,2.5,5.8),bone,n%2,(n*3,sign*n*6,sign*(n*7)))
                for digit in range(4):
                    x=sign*12+(digit-1.5)*2
                    oval('colossus armored finger '+bone+str(digit),(x,4.8,-3.3),(.95,3.1,1.4),bone,0,6)
                    cube('split knuckle rune '+bone+str(digit),(x,8,-6.08),(.28,1.6,.14),bone,3)
                taper('elbow shard '+bone,(sign*12,14,2),(sign*18,18,3),1.3,bone,1)
            for n in range(7):
                x=(n-3)*1.1
                cube('crag skull plate '+str(n),(x,37+n%2,0),(1.7,3.5+n%3,5),'head',1,(0,0,(n-3)*8))
            for n in range(9):
                cube('chest fissure '+str(n),((n%3-1)*3,16+n//3*4,-4.95),(.22,3.3,.15),'body',3,(0,0,(n%3-1)*20))
            cube('buried heart gem',(0,22,-5.2),(2.1,3.4,1),'body',3,(0,0,45))
            for n in range(5):
                cube('jaw tooth slab '+str(n),((n-2)*1.15,29,-4.25),(.85,1.5,.5),'head',2,(0,0,(n-2)*5))

        else:
            for sign,bone in [(-1,'left'),(1,'right')]:
                for n in range(5):
                    taper('ice pauldron '+bone+str(n),(sign*(9+n*.65),26,0),(sign*(11+n*1.2),32+n%2*2,0),1,bone,1)
            for n in range(7):taper('ice crown '+str(n),((n-3)*1.1,36,0),((n-3)*1.5,42+3*(1-abs(n-3)/3),0),.7,'head',1)
            for n in range(5):taper('ice beard '+str(n),((n-2)*1.1,31,-3.6),((n-2)*.8,26-abs(n-2),-3.6),.65,'head',5)
            cube('hammer shaft',(12,20,-2),(1.8,25,1.8),'right',2)
            oval('hammer ice mass',(12,33,-2),(9,4.5,4.5),'right',1,7)
            for n in range(6):cube('hammer binding '+str(n),(12,12+n*2,-2),(2.05,.6,2.05),'right',7)
            for n in range(9):cube('cape scale '+str(n),((n%3-1)*3,13+n//3*4,4.6),(3.3,5,.6),'tail',2,(0,0,(n%3-1)*8))
            cube('chest crystal',(0,24,-4.9),(3,5,1),'body',3,(0,0,45))
            # Layered ice helm, cold steel plates and ragged crystalline cloak.
            for sign,bone in [(-1,'left'),(1,'right')]:
                cube('frost helm cheek '+bone,(sign*3.7,32,-3.8),(2.2,4.5,1.1),'head',1,(0,sign*22,sign*12))
                cube('frost helm brow '+bone,(sign*2.6,35,-4.5),(4.5,1.1,.7),'head',1,(0,0,-sign*16))
                taper('frozen antler base '+bone,(sign*3.6,36,0),(sign*7.5,41,3),.85,'head',1)
                taper('frozen antler tip '+bone,(sign*7.5,41,3),(sign*12,43,6),.55,'head',1)
                for n in range(4):
                    cube('crystal bracer layer '+bone+str(n),(sign*12,12+n*1.1,-3.5),(5.5,1.1,1.6),bone,1,(0,0,sign*n*5))
                    taper('bracer ice barb '+bone+str(n),(sign*14,11+n*1.5,0),(sign*18,12+n*1.5,1),.65,bone,6)
            cube('frost face blade',(0,32,-4.2),(1,7,1),'head',1)
            for n in range(9):
                x=(n-4)*1.0
                taper('cloak icicle '+str(n),(x,20,5.5),(x*1.4,6+n%3,6.5),.8,'tail',6)
            for n in range(5):
                cube('frost armor engraving '+str(n),(0,19+n*1.3,-4.65),(8-n*.6,.25,.18),'body',7)
                taper('hammer crown spike '+str(n),(7+n*2.5,35,-2),(7+n*2.5,40+n%2*2,-2),.65,'right',1)

    # Children inherit their parent's pose; joints stay connected during motion.
    parents={'head':'body','left':'body','right':'body','tail':'body'}
    if index in (2,3):
        parents['jaw']='head';parents['tail']='spine_6'
        bones['tail']['origin']=list(serpent_point(index,28/31))
        for number in range(1,7):parents['spine_'+str(number)]='body' if number==1 else 'spine_'+str(number-1)
    for child,parent in parents.items():
        node=nodes[bones[child]['uuid']];model['outliner'].remove(node);nodes[bones[parent]['uuid']]['children'].append(node)
    for animation in model['animations']:
        if animation['name']=='transform':
            for uuid,animator in animation['animators'].items():
                if uuid!=bones['body']['uuid']:animator['keyframes']=[k for k in animator['keyframes'] if k['channel']!='scale']
    scale=BOSS_SCALES[index]
    for element in model['elements']:
        for key in ('from','to','origin'):element[key]=[round(v*scale,5) for v in element[key]]
    for group in model['groups']:group['origin']=[round(v*scale,5) for v in group['origin']]
    # Keep inline pivots/names for snapshot readers that do not use the BB5 group table.
    for group in model['groups']:nodes[group['uuid']].update(group)
    model['resolution']={'width':128,'height':128}
    model['textures']=[{'name':name+'_painted.png','id':'0','width':128,'height':128,'uv_width':128,'uv_height':128,'source':'data:image/png;base64,'+atlas(index)}]
    model['credit']='Original LegendarySwords sculpted boss forms and painted material textures'
    return model
