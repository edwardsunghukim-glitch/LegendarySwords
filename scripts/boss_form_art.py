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


def rebuild(model,index):
    name=model['model_identifier'];model['elements']=[]
    bones={g['name']:g for g in model['groups']}
    nodes={g['uuid']:g for g in model['outliner']}
    for node in nodes.values():node['children']=[]
    if index in (2,3):
        jaw={'uuid':motion.uid(name+'/jaw'),'name':'jaw','origin':[0,20,-9],'rotation':[0,0,0],'visibility':True}
        model['groups'].append(jaw);bones['jaw']=jaw
        node={'uuid':jaw['uuid'],'isOpen':True,'children':[]};model['outliner'].append(node);nodes[jaw['uuid']]=node
        bones['head']['origin']=[0,24,-8];bones['tail']['origin']=[0,12,22]
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
            origin=[math.sin(t*math.pi*2)*9*(1-.4*t),21-10*t,-3+52*t]
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
        # A curved sea serpent, with a continuous belly, swept fins and narrow tail.
        for n in range(32):
            t=n/31;cx=math.sin(t*math.pi*2)*9*(1-.4*t);cy=21-10*t;cz=-3+52*t
            r=5.2*(1-.80*t);bone='body' if n<4 else 'spine_'+str(n//4) if n<28 else 'tail'
            oval('sea coil '+str(n),(cx,cy,cz),(r,r*.68,1.8),bone,n%2,4)
            cube('belly plate '+str(n),(cx,cy-r*.5,cz),(r*1.35,.7,3.1),bone,2)
            taper('dorsal fin '+str(n),(cx,cy+r*.6,cz),(cx,cy+r*.6+4*(1-t),cz+2),.65,bone,6)
        oval('sea crown',(0,24,-9),(5,5,6),'head',0)
        oval('sea muzzle',(0,22,-15),(3.8,2.3,5),'head',1)
        cube('dark mouth',(0,20,-15),(6.8,.65,7),'head',4)
        cube('lower jaw',(0,19,-14),(6.7,1.5,8),'jaw',2)
        eyes(25,-14,3)
        for sign,bone in [(-1,'left'),(1,'right')]:
            for n in range(9):
                x=sign*(5+n*1.5)
                cube('swept fin '+bone+str(n),(x,17-n*.4,1+n*.55),(2.1,1,8-n*.55),bone,1,(0,sign*22,sign*12)) if index==2 else None
            taper('sea horn '+bone,(sign*3.5,27,-7),(sign*7,33,-3),1,'head',7)
            taper('sea whisker '+bone,(sign*3,22,-18),(sign*10,24,-20),.3,'head',2)
        if index==3:
            for sign,bone in [(-1,'left'),(1,'right')]:
                oval('dragon haunch '+bone,(sign*6,16,9),(3,4,3),bone,0)
                oval('dragon wrist '+bone,(sign*9,12,6),(2,3,2),bone,1)
                for n in range(3):
                    taper('dragon claw '+bone+str(n),(sign*9+(n-1)*1.2,11,4),(sign*9+(n-1)*1.2,9,1),.5,bone,5)
                for n in range(12):
                    x=sign*(6+n*1.4);top=28+math.sin(n/11*math.pi)*6;h=10*(1-n/16)
                    cube('dragon membrane '+bone+str(n),(x,top-h/2,4),(1.6,h,.45),bone,6)
                taper('dragon wing spar '+bone,(sign*5,26,4),(sign*22,30,4),.8,bone,2)
            for n in range(6):
                taper('dragon beard '+str(n),((n-2.5)*.7,21,-14),((n-2.5)*1.2,16,-12),.45,'head',4)
        for n in range(8):
            taper('upper fang '+str(n),(-2.8+n*.8,22,-18),(-2.8+n*.8,20.7,-18),.3,'head',5)
            taper('lower fang '+str(n),(-2.8+n*.8,19,-18),(-2.8+n*.8,20.3,-18),.3,'jaw',5)
    else:
        width=8 if index in (1,4) else 5
        oval('ribcage',(0,23,0),(width,9,4.5),'body',0,9)
        oval('waist',(0,14,0),(width*.72,4,3.8),'body',1)
        oval('face',(0,33,-.5),(4.5,5,3.7),'head',0 if index==3 else 5,8)
        eyes(34,-4.1,2)
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
    # Children inherit their parent's pose; joints stay connected during motion.
    parents={'head':'body','left':'body','right':'body','tail':'body'}
    if index in (2,3):
        parents['jaw']='head';parents['tail']='spine_6'
        bones['tail']['origin']=[-3.4,12,44]
        for number in range(1,7):parents['spine_'+str(number)]='body' if number==1 else 'spine_'+str(number-1)
    for child,parent in parents.items():
        node=nodes[bones[child]['uuid']];model['outliner'].remove(node);nodes[bones[parent]['uuid']]['children'].append(node)
    for animation in model['animations']:
        if animation['name']=='transform':
            for uuid,animator in animation['animators'].items():
                if uuid!=bones['body']['uuid']:animator['keyframes']=[k for k in animator['keyframes'] if k['channel']!='scale']
    model['resolution']={'width':128,'height':128}
    model['textures']=[{'name':name+'_painted.png','id':'0','width':128,'height':128,'uv_width':128,'uv_height':128,'source':'data:image/png;base64,'+atlas(index)}]
    model['credit']='Original LegendarySwords sculpted boss forms and painted material textures'
    return model
