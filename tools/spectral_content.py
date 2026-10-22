"""Editable, deterministic creature/equipment art. Run last after the older content generators.
Sources are native geometry recipes, not repainted screenshots or downloaded franchise assets.
"""
from pathlib import Path
import copy, hashlib, io, json, math, os, random, re
from PIL import Image, ImageDraw
from spectral_anatomy import anatomy
from reborn_creatures import build as reborn, DESIGNS, rgb
from creature_materials import paint as paint_material
from hero_creatures import build as hero, REFINED

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/jjkcraft';D=ROOT/'src/main/resources/data/jjkcraft'
SOURCES=json.loads((ROOT/'tools/spectral_model_sources.json').read_text(encoding='utf-8'))

def save(p,d):
    p.parent.mkdir(parents=True,exist_ok=True);t=p.with_suffix('.writing');t.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n');os.replace(t,p)
def png(p,im):
    p.parent.mkdir(parents=True,exist_ok=True);b=io.BytesIO();im.save(b,format='PNG');t=p.with_suffix('.writing');t.write_bytes(b.getvalue());os.replace(t,p)
def seed(name):return int(hashlib.sha256(name.encode()).hexdigest()[:12],16)
def cube(origin,size,material='skin',face=False,**kw):return dict(origin=list(origin),size=list(size),_material=material,_face=face,**kw)
def mix(a,b,t):return tuple(int(x*(1-t)+y*t) for x,y in zip(a,b))
def color(c,f=1):return tuple(max(0,min(255,int(x*f))) for x in c)+(255,)
def palette(name):
    if name=='shadow_mahoraga':return (207,198,176),(219,167,72)
    if name in DESIGNS:return rgb(DESIGNS[name][1]),rgb(DESIGNS[name][2])
    if any(n in name for n in ('paper','ledger','chalk','book','door','heron')):return (187,177,151),(76,181,178)
    if any(n in name for n in ('tide','salt','gutter','toad','abyss')):return (72,112,105),(133,226,185)
    if any(n in name for n in ('ember','ash','oni','moth','fire')):return (77,66,67),(242,155,63)
    if any(n in name for n in ('rabbit','deer','serpent','mahoraga')):return (198,200,178),(183,206,117)
    if 'elephant' in name:return (111,135,145),(224,173,119)
    if 'tiger' in name:return (180,133,74),(245,192,100)
    if 'nue' in name:return (111,82,67),(192,139,211)
    if any(n in name for n in ('dog','hound','ox','shadow')):return (49,58,70),(143,193,210)
    if any(n in name for n in ('flesh','mouth','soul','rika','dregs')):return (134,112,132),(236,171,180)
    return (103,101,118),(170,137,214)
def material(bone,name):
    if 'eye_' in bone:return 'eye'
    if 'mouth_' in bone:return 'mouth'
    if 'mask' in bone:return 'bone'
    if 'blade' in bone:return 'gold'
    if any(k in bone for k in ('horn','tooth','tusk','antler','spoke','wheel','skull','jaw')):return 'bone'
    if any(k in bone for k in ('cloth','robe','veil','banner','paper','ledger')):return 'cloth'
    if 'wing' in bone:return 'feather'
    if 'tiger' in name:return 'stripe'
    if any(k in name for k in ('dog','hound','rabbit','ox')):return 'fur'
    if 'serpent' in name or 'dregs' in name:return 'scale'
    return 'skin'

def sculpt(name,source):
    geo=copy.deepcopy(hero(name) or reborn(name) or anatomy(name,source));g=geo['minecraft:geometry'][0];bones=g['bones'];rng=random.Random(seed(name));extra=[]
    head=next((b for b in bones if b['name']=='head' and b.get('cubes')),None)
    for bone in list(bones):
        key=bone['name'];parts=bone.get('cubes',[]);kind=material(key,name);shaped=[]
        for old in parts:
            c=copy.deepcopy(old);c.pop('uv',None);c['_material']=kind;c['_face']=key in ('head','mask')
            if min(c['size'])>=4 and not any(k in key for k in ('wing','wheel','spoke')):
                x,y,z=c['origin'];w,h,d=c['size'];r=.65
                shaped.extend([cube((x+r,y,z+r),(w-2*r,h,d-2*r),kind),cube((x,y+r,z+r),(w,h-2*r,d-2*r),kind),cube((x+r,y+r,z),(w-2*r,h-2*r,d),kind,key=='head')])
            else:shaped.append(c)
        bone['cubes']=shaped
        # Separate knees/elbows preserve the parent rig while making gait and attack motion articulate.
        if parts and any(k in key for k in ('leg','arm')) and not any(k in key for k in ('pillar','wing')):
            base=parts[0];x,y,z=base['origin'];w,h,d=base['size']
            if h>=6 and h>max(w,d):
                joint=key+'_joint';cut=h*.45;bone['cubes']=[cube((x,y+cut,z),(w,h-cut,d),kind)]
                extra.append(dict(name=joint,parent=key,pivot=[x+w/2,y+cut,z+d/2],cubes=[cube((x,y,z),(w,cut,d),kind)]))
                if 'leg' in key:extra[-1]['cubes'].append(cube((x-.4,y,z-1),(w+.8,1.5,d+1.5),kind))
        if 'wing' in key and parts and 'moth' not in name:
            b=parts[0];x,y,z=b['origin'];w,h,d=b['size']
            for n in range(5):bone['cubes'].append(cube((x+n*w/5,y-.25,z+d*.5),(max(.5,w/5),.6,d*.65+1+n*.25),'feather'))
    if head and name not in REFINED:
        base=max(head['cubes'],key=lambda c:math.prod(c['size']));x,y,z=base['origin'];w,h,d=base['size']
        if w>=4 and h>=4:
            extra.append(dict(name='spectral_jaw',parent='head',pivot=[x+w/2,y+h*.22,z+d*.35],cubes=[cube((x+.2,y-.6,z-.35),(w-.4,1.7,d*.65),'bone')]))
            for side in (-1,1):extra.append(dict(name='spectral_eye_'+str(side),parent='head',pivot=[x+w/2+side*w*.25,y+h*.65,z],cubes=[cube((x+w/2+side*w*.25-.65,y+h*.6,z-.2),(1.3,.65,.3),'eye')]))
            # Species-seeded crests / folded growths distinguish former palette-only variants.
            if name not in {'rabbit_escape','round_deer','max_elephant','divine_dog','nue','toad','shadow_divine_dog'}:
                for n in range(2+rng.randrange(3)):
                    xx=x+w*(n+1)/5;hh=2+rng.randrange(4)
                    extra.append(dict(name='spectral_crest_'+str(n),parent='head',pivot=[xx,y+h,z+d*.6],cubes=[cube((xx-.5,y+h-.4,z+d*.5),(1,hh,1.2),'bone',rotation=[0,0,(n-1)*12],pivot=[xx,y+h,z+d*.6])]))
    if name=='shadow_mahoraga':
        for b in bones:
            if b['name'].startswith('spoke'):b['cubes']=[]
            if b['name']=='wheel':
                cy=b['pivot'][1]
                b['cubes']=[]
                for n in range(24):
                    angle=n*math.tau/24;xx,yy=9*math.cos(angle),cy+9*math.sin(angle)
                    b['cubes'].append(cube((xx-1.35,yy-.5,1.5),(2.7,1,1),'gold',rotation=[0,0,n*15+90],pivot=[xx,yy,2]))
                for n in range(8):
                    angle=n*math.tau/8;xx,yy=4.5*math.cos(angle),cy+4.5*math.sin(angle)
                    b['cubes'].append(cube((0,cy-.4,1.8),(9,.8,.8),'gold',rotation=[0,0,n*45],pivot=[0,cy,2]))
    bones.extend(extra);g['description'].update(visible_bounds_width=7,visible_bounds_height=7,visible_bounds_offset=[0,2,0])
    return geo

def bake(name,geo):
    g=geo['minecraft:geometry'][0];tiles=[];u=v=row=0;width=1024;density=4
    for bone in g['bones']:
        for c in bone.get('cubes',[]):
            w,h,d=c['size'];faces={};kind=c.pop('_material','skin');facial=c.pop('_face',False)
            for face,fw,fh in [('north',w,h),('south',w,h),('east',d,h),('west',d,h),('up',w,d),('down',w,d)]:
                tw,th=max(1,math.ceil(fw*density)),max(1,math.ceil(fh*density))
                if u+tw+2>width:u=0;v+=row+2;row=0
                faces[face]={'uv':[u,v],'uv_size':[tw,th]};tiles.append((u,v,tw,th,kind,face,facial,tuple(c['origin']),tuple(c['size'])));u+=tw+2;row=max(row,th)
            c['uv']=faces
    height=2**max(8,math.ceil(math.log2(v+row+2)));assert height<=4096,(name,height)
    g['description'].update(texture_width=width,texture_height=height)
    im=Image.new('RGBA',(width,height));glow=Image.new('RGBA',im.size);draw=ImageDraw.Draw(im);gd=ImageDraw.Draw(glow);base,accent=palette(name);rng=random.Random(seed(name))
    for x,y,w,h,kind,face,facial,origin,size in tiles:
        paint_material(draw,gd,x,y,w,h,kind,face,base,accent,seed(name),origin,size)
    return im,glow

def curve(values,length):return {str(round(i*length/(len(values)-1),3)):v for i,v in enumerate(values)}
def animations(name,geo):
    bones=geo['minecraft:geometry'][0]['bones'];names=[b['name'] for b in bones];body=next((n for n in ('body','mass','torso','root') if n in names),'root');clips={}
    for action,length,loop in [('idle',2.8,True),('walk',.85,True),('run',.48,True),('attack',.7,False),('jab',.6,False),('heavy',1.1,False),('uppercut',.8,False),('sweep',.9,False),('cast',1.2,False),('attack_windup',.4,False),('guard',.65,False),('hurt',.3,False),('stagger',.5,False),('spawn',1.0,False),('death',1.0,False)]:
        motion={};moving=action in ('walk','run');attack=action in ('attack','jab','heavy','uppercut','sweep','attack_windup')
        for i,b in enumerate(bones):
            n=b['name'];data={};amp=26 if action=='walk' else 43
            if moving:
                sign=1 if i%2==0 else -1
                if any(k in n for k in ('leg','arm','haunch','tendril')):data['rotation']=curve([[amp*sign,0,0],[0,0,0],[-amp*sign,0,0],[0,0,0],[amp*sign,0,0]],length)
                if n.endswith('_joint'):data['rotation']=curve([[0,0,0],[32,0,0],[6,0,0],[0,0,0],[0,0,0]],length)
                if 'wing' in n:data['rotation']=curve([[0,0,-22*sign],[0,0,34*sign],[0,0,-22*sign]],length)
                if n==body:data['position']=curve([[0,0,0],[0,.65,0],[0,0,0],[0,.65,0],[0,0,0]],length)
            elif action=='idle':
                if n==body:data['scale']=curve([[1,1,1],[1.015,1.025,1.015],[1,1,1]],length)
                if 'tail' in n or 'tendril' in n:data['rotation']=curve([[0,-5,0],[2,5,0],[0,-5,0]],length)
                if n=='head':data['rotation']=curve([[0,-2,0],[2,2,0],[0,-2,0]],length)
                if n=='wheel':data['rotation']=curve([[0,0,0],[0,0,45],[0,0,90]],length)
            elif attack:
                if n==body:data={'position':curve([[0,0,0],[0,.4,1],[0,0,-2.2],[0,0,0]],length),'rotation':curve([[0,0,0],[-7,10,0],[12,-12,0],[0,0,0]],length)}
                if 'arm' in n or 'wing' in n:data['rotation']=curve([[0,0,0],[-35,0,8],[-105,0,-12],[0,0,0]],length)
                if n=='head':data['rotation']=curve([[0,0,0],[-12,0,0],[15,0,0],[0,0,0]],length)
                if 'jaw' in n:data['rotation']=curve([[0,0,0],[26,0,0],[4,0,0],[0,0,0]],length)
            elif action in ('hurt','stagger'):
                if n==body:data['rotation']=curve([[0,0,0],[-12,0,9],[0,0,0]],length)
            elif action in ('spawn','death'):
                if n==body:
                    values=[[.2,.15,.2],[1.08,1.12,1.08],[1,1,1]] if action=='spawn' else [[1,1,1],[.9,.5,.9],[.05,.05,.05]]
                    data['scale']=curve(values,length)
            elif action in ('cast','guard'):
                if any(k in n for k in ('arm','wing','jaw')):data['rotation']=curve([[0,0,0],[-65,0,10],[-60,0,8],[0,0,0]],length)
                if n==body:data['position']=curve([[0,0,0],[0,.5,0],[0,0,0]],length)
            if data:motion[n]=data
        clips[action]={'loop':loop,'animation_length':length,'bones':motion}
    clips['emerge']=copy.deepcopy(clips['spawn']);return {'format_version':'1.8.0','animations':clips}

def creatures():
    report={}
    for name,source in SOURCES.items():
        geo=sculpt(name,source)
        if name in DESIGNS:
            java=(ROOT/'src/main/java/com/jjkcraft/curse/CurseEntities.java').read_text();match=re.search(r'(?:ground|flying)\("'+re.escape(name)+r'",\s*([\d.]+)f?,\s*([\d.]+)f?',java)
            data=json.loads((D/'jjkcraft/curse'/f'{name}.json').read_text());scale=data['model'].get('scale',1)
            if match:
                # Fit the rebuilt body to its actual server hitbox, retaining the authored silhouette.
                cubes=[c for b in geo['minecraft:geometry'][0]['bones'] for c in b.get('cubes',[])]
                low=min(c['origin'][1] for c in cubes);high=max(c['origin'][1]+c['size'][1] for c in cubes);factor=float(match.group(2))*16/(max(1,high-low)*scale)
                for b in geo['minecraft:geometry'][0]['bones']:
                    b['pivot']=[v*factor-(low*factor if i==1 else 0) for i,v in enumerate(b['pivot'])]
                    for c in b.get('cubes',[]):
                        c['origin']=[v*factor-(low*factor if i==1 else 0) for i,v in enumerate(c['origin'])];c['size']=[v*factor for v in c['size']]
                        if 'pivot' in c:c['pivot']=[v*factor-(low*factor if i==1 else 0) for i,v in enumerate(c['pivot'])]
        im,glow=bake(name,geo);save(A/'geckolib/models/entity'/f'{name}.geo.json',geo);save(A/'geckolib/animations/entity'/f'{name}.animation.json',animations(name,geo))
        png(A/'textures/entity'/f'{name}.png',im);png(A/'textures/entity'/f'{name}_glow.png',glow)
        for suffix,factor in [('wounded',.9),('critical',.78)]:
            damaged=im.copy();dd=ImageDraw.Draw(damaged);rng=random.Random(seed(name+suffix))
            for n in range(22 if suffix=='wounded' else 45):
                x,y=rng.randrange(im.width),rng.randrange(im.height)
                if im.getpixel((x,y))[3]:dd.line((x,y,min(im.width-1,x+3),min(im.height-1,y+5)),fill=palette(name)[1]+(255,))
            png(A/'textures/entity'/f'{name}_{suffix}.png',damaged)
        lod=copy.deepcopy(geo)
        for b in lod['minecraft:geometry'][0]['bones']:
            c=b.get('cubes',[]);b['cubes']=sorted(c,key=lambda v:math.prod(v['size']),reverse=True)[:3] if b['name'] in ('body','head') else [max(c,key=lambda v:math.prod(v['size']))] if c else []
        save(A/'geckolib/models/entity'/f'{name}_lod.geo.json',lod)
        report[name]={'bones':len(geo['minecraft:geometry'][0]['bones']),'cubes':sum(len(b.get('cubes',[])) for b in geo['minecraft:geometry'][0]['bones']),'texture':im.size}
        print('Sculpted',name,report[name],flush=True)
    save(ROOT/'build/spectral-creature-inventory.json',report)

def weapon_model(key):
    elements=[]
    def add(a,b,part='blade',angle=0):
        uv=[7,0,9,10] if part=='blade' else [5,10,11,12] if part=='accent' else [7,11,9,16]
        e={'from':a,'to':b,'faces':{f:{'uv':uv,'texture':'#layer0'} for f in ('north','south','east','west','up','down')}}
        if angle:e['rotation']={'origin':[(a[i]+b[i])/2 for i in range(3)],'axis':'z','angle':angle}
        elements.append(e)
    if key=='playful_cloud':
        for y in (-7,2,11):
            add([7.3,y,7.3],[8.7,y+7,8.7],'accent')
            for yy in (y+.3,y+5.7):add([7.15,yy,7.15],[8.85,yy+.7,8.85],'blade')
        for y in (0,9):
            for x in (7.5,8.2):add([x,y,7.7],[x+.3,y+2,8.3],'blade')
            for yy in (y,y+1.7):add([7.5,yy,7.7],[8.5,yy+.3,8.3],'blade')
        return elements
    add([7.4,-5,7.4],[8.6,4,8.6],'handle');add([5.5,3.8,7],[10.5,4.6,9],'accent')
    if key=='stormbell_spear':
        add([7.55,4,7.55],[8.45,18,8.45],'handle');add([6.5,17,7.5],[9.5,22,8.5]);add([7.2,22,7.6],[8.8,26,8.4])
        for x in (5.5,10):add([x,8,7],[x+.5,12,9],'accent')
        for y in (8,11.5):add([5.5,y,7],[10.5,y+.5,9],'accent')
        add([7.6,8,7.5],[8.4,10,8.5])
    elif key=='dragon_bone':
        add([6.2,4.6,7],[9.8,19,9]);add([6.7,19,7.2],[9.3,23,8.8],angle=-22.5)
        for n,y in enumerate((6,9,12,15,18)):
            x=5 if n%2==0 else 9;add([x,y,7.2],[x+2,y+1.8,8.8],'accent',22.5 if n%2==0 else -22.5)
    elif key=='ember_wakizashi':
        for x,y,h in ((7,4.5,6),(7.4,10,5),(8.2,14.5,4)):
            add([x,y,7.7],[x+1.8,y+h,8.3],angle=-22.5 if y>10 else 0)
            add([x+.1,y,7.65],[x+.5,y+h-.3,8.35],'accent')
    else:
        add([7,4.5,7.5],[9,13,8.5]);add([7.6,12,7.5],[9.4,15,8.5],angle=-22.5)
        add([7,0,7.1],[9,2,8.9],'accent',45)
    return elements

def equipment():
    langpath=ROOT/'src/main/generated/assets/jjkcraft/lang/en_us.json';lang=json.loads(langpath.read_text(encoding='utf-8'))
    sets=[('sorcerer_uniform','Sorcerer Uniform',(38,47,76),(154,184,211),'leather','Each piece reduces technique CE cost by 2% (8% for a full set).'),('binding_vestments','Binding Vestments',(44,31,57),(196,145,226),'iron_ingot','Each piece wards 5% damage at 1.75 CE per damage prevented; no protection without CE.'),('heavenly_hunter','Heavenly Hunter',(55,65,57),(191,177,121),'diamond','Each piece grants 3% movement speed and physical knockback resistance. No CE required.')]
    for key,title,base,accent,ingredient,desc in sets:
        lang['equipment.jjkcraft.'+key+'.desc']=desc
        save(A/'equipment'/f'{key}.json',{'layers':{'humanoid':[{'texture':'jjkcraft:'+key}],'humanoid_leggings':[{'texture':'jjkcraft:'+key}]}})
        for layer in ['humanoid','humanoid_leggings']:
            im=Image.new('RGBA',(256,128));d=ImageDraw.Draw(im)
            for x,y,w,h in [(0,0,64,64),(64,64,96,64),(160,64,64,64),(0,64,64,64)]:
                d.rectangle((x,y,x+w-1,y+h-1),fill=base+(255,))
                for yy in range(y+3,y+h,8):d.line((x,yy,x+w-1,yy),fill=color(base,1.13))
                d.rectangle((x+1,y+1,x+w-2,y+h-2),outline=accent+(255,),width=2)
                for xx in range(x+5,x+w,12):d.line((xx,y+4,xx,y+h-4),fill=color(base,.72))
            # Open face; embroidered lapels, waist sash and protective knee plates.
            d.rectangle((32,32,63,63),fill=(0,0,0,0));d.line((80,80,96,105,111,80),fill=accent+(255,),width=5);d.rectangle((80,109,111,119),fill=accent+(255,))
            png(A/'textures/entity/equipment'/layer/f'{key}.png',im)
        for part,pattern in [('helmet',['AAA','ASA']),('chestplate',['ASA','AAA','AAA']),('leggings',['AAA','ASA','A A']),('boots',['A A','ASA'])]:
            item=key+'_'+part;lang['item.jjkcraft.'+item]=title+' '+part.title()
            im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im);shape=(7,3,24,17) if part=='helmet' else (5,5,26,25) if part=='chestplate' else (7,4,24,28) if part=='leggings' else (6,13,25,28)
            d.rounded_rectangle(shape,3,fill=base+(255,),outline=accent+(255,),width=2);d.line((11,7,16,18,21,7),fill=accent+(255,),width=2)
            if part in ('leggings','boots'):d.rectangle((14,16,17,30),fill=(0,0,0,0))
            png(A/'textures/item'/f'{item}.png',im);save(A/'models/item'/f'{item}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jjkcraft:item/'+item}});save(A/'items'/f'{item}.json',{'model':{'type':'minecraft:model','model':'jjkcraft:item/'+item}})
            save(D/'recipe'/f'{item}.json',{'type':'minecraft:crafting_shaped','category':'equipment','pattern':pattern,'key':{'A':'minecraft:'+ingredient,'S':'jjkcraft:cursed_energy_shard'},'result':{'id':'jjkcraft:'+item}})
    tools=[('playful_cloud','Playful Cloud','special_grade',None,'none',0,'A three-section physical staff. Relies on the wielder and works without CE.','netherite_ingot'),('dragon_bone','Dragon Bone','grade_1',None,'recoil',.65,'Discharge impact force for knockback, spending 3 CE per hit.','diamond'),('stormbell_spear','Stormbell Spear','grade_2','return_stroke','shock',4,'An original bell-tipped spear: briefly stuns at a cost of 4 CE per hit.','amethyst_shard'),('ember_wakizashi','Ember Wakizashi','grade_3','disaster_flames','ignite',3,'An original short cursed blade that ignites its target.','blaze_powder'),('sealbreaker_tanto','Sealbreaker Tanto','grade_1',None,'dispel',8,'An original sealing dagger: ends active/charging techniques and drains 8 CE.','echo_shard')]
    for n,(key,title,grade,tech,onhit,amount,desc,mat) in enumerate(tools):
        lang['item.jjkcraft.'+key]=title;lang['item.jjkcraft.'+key+'.desc']=desc
        definition={'item':'jjkcraft:'+key,'kind':'weapon','grade':'jjkcraft:'+grade,'on_hit':onhit,'amount':amount,'description':'item.jjkcraft.'+key+'.desc'}
        if tech:definition['technique']='jjkcraft:'+tech
        save(D/'jjkcraft/cursed_tool'/f'{key}.json',definition)
        im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im);accent=[(160,53,57),(118,167,166),(185,141,225),(241,148,61),(146,198,201)][n]
        d.rectangle((27,2,36,42),fill=(198,204,204,255),outline=accent+(255,),width=2);d.rectangle((21,41,42,46),fill=accent+(255,));d.rectangle((29,46,34,62),fill=(58,40,42,255))
        for y in range(48,61,4):d.line((29,y,34,y+2),fill=accent+(255,),width=1)
        png(A/'textures/item'/f'{key}.png',im)
        elements=[{'from':[7.4,-5,7.4],'to':[8.6,4,8.6],'faces':{f:{'uv':[7,11,9,16],'texture':'#layer0'} for f in ['north','south','east','west','up','down']}},{'from':[6,3.8,7],'to':[10,4.6,9],'faces':{f:{'uv':[5,10,11,12],'texture':'#layer0'} for f in ['north','south','east','west','up','down']}},{'from':[7,4.5,7.6],'to':[9,18,8.4],'faces':{f:{'uv':[7,0,9,10],'texture':'#layer0'} for f in ['north','south','east','west','up','down']}}]
        if key=='playful_cloud':
            elements=[]
            for xx in [4.5,8,11.5]:elements.append({'from':[xx-.6,-3,7.4],'to':[xx+.6,15,8.6],'faces':{f:{'uv':[7,11,9,16],'texture':'#layer0'} for f in ['north','south','east','west','up','down']}})
        save(A/'models/item'/f'{key}.json',{'parent':'minecraft:item/handheld','textures':{'layer0':'jjkcraft:item/'+key},'elements':weapon_model(key)});save(A/'items'/f'{key}.json',{'model':{'type':'minecraft:model','model':'jjkcraft:item/'+key}})
        save(D/'recipe'/f'{key}.json',{'type':'minecraft:crafting_shaped','category':'equipment','pattern':[' M ',' M ',' S '],'key':{'M':'minecraft:'+mat,'S':'jjkcraft:cursed_energy_shard'},'result':{'id':'jjkcraft:'+key}})
    save(langpath,lang)

if __name__=='__main__':creatures();equipment()
