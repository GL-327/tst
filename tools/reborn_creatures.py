"""New creature blueprints. Curse geometry is built from an empty rig, never the old meshes."""
import math,hashlib,copy

# Each species has a different silhouette, material palette and signature structure.
DESIGNS={
'ash_oni':('furnace',0x372E36,0xFF8841),'blackwater_bride':('veil',0x253E48,0x82E8DD),
'bone_orchard':('tree',0xBCB59E,0xC3DE89),'chalk_biter':('crab',0xD4CBB3,0x4FBAAF),
'cracked_bell':('bell',0x7D6256,0xEBC473),'culling_warden':('shield',0x37354F,0xC293F5),
'damp_effigy':('sack',0x626D5D,0xCCDB8D),'doorframe_hunter':('portal',0x4C3540,0xF0AF8F),
'drain_skitter':('centipede',0x3B6965,0xA6E3BD),'dustling':('urchin',0x70687D,0xC6B4D9),
'echo_mimic':('horn',0x738188,0xB1E2E9),'gilded_mimic':('clam',0x8C672F,0xF2D485),
'gravebound_gate':('portal',0x414451,0xD9B88C),'grief_cathedral':('cathedral',0x545266,0xD7AAE4),
'grin_hound':('hound',0x6D394B,0xE3B29D),'gutter_grub':('worm',0x8B9C78,0xD5DB9C),
'hollow_hymnal':('book',0x4B435C,0xD7C9A2),'hunger_eclipse':('eclipse',0x171D32,0xCBB3EC),
'ink_leviathan':('eel',0x263C55,0x55D5C1),'knell_keeper':('bell',0x5B6874,0xC7E1D4),
'lacquer_butcher':('cauldron',0x812D3D,0xDEC0A1),'lantern_leech':('worm',0x705348,0xF5BC5D),
'ledger_crawler':('book',0x8F7663,0xE9D39D),'marrow_regent':('spine',0xC4BBA8,0xB394D6),
'mirror_wraith':('mirror',0x477780,0xC8F4F0),'needle_cricket':('cricket',0x697265,0xDDDB9D),
'needle_widow':('spider',0x4F374C,0xDB718A),'night_procession':('procession',0x4A435A,0xECBF83),
'ossuary_king':('throne',0xAFA69B,0xA9D3C1),'pale_stilt':('stilt',0xB4B5A6,0xC1D9CC),
'paper_tick':('tick',0xCBC5A9,0x68BEBA),'paper_tide_gravemind':('vortex',0xACA795,0xD7B0E4),
'porcelain_nipper':('cup',0xC6C6BB,0x858BC0),'railcrawler':('rail',0x424C58,0xC98466),
'redacted_saint':('reliquary',0xC8B9AF,0xA59DE4),'rust_pilgrim':('hermit',0x8C6E4F,0xB3D5A5),
'rustjaw_pup':('hound',0x8A5045,0xEDBB8A),'salt_lantern':('crystal',0x9BBAB8,0xDAFAE7),
'sewer_effigy':('pipes',0x356961,0xAED78F),'shriek_vessel':('vase',0x866A84,0xD5A7B4),
'soot_imp':('cauldron',0x3F3748,0xFFBA68),'static_mite':('urchin',0x4B556F,0x9BDDFC),
'ten_mouth_sovereign':('maws',0x784153,0xEBC69F),'thorn_matriarch':('rose',0x374F48,0xD4A6CC),
'tidewash':('ray',0x4E8C92,0xA4EBE1),'tithe_burrower':('purse',0x685E43,0xE1D19B),
'unfinished_ferryman':('boat',0x586771,0xB4D4C9),'veil_weaver':('jelly',0x647387,0xD7BEEB),
'vow_kin':('knot',0x857267,0xD9BD91),'whisper_knot':('braid',0x4F4566,0xBEA5DD),
'wick_moth':('moth',0x92794D,0xFFD180),'worry_doll':('doll',0x8F7180,0xDCBCA8)}

def rgb(n):return (n>>16&255,n>>8&255,n&255)

class Rig:
    def __init__(self,name):
        self.name=name;self.bones=[{'name':'root','pivot':[0,0,0],'cubes':[]},{'name':'body','parent':'root','pivot':[0,14,0],'cubes':[]}];self.i=0
    def bone(self,name,pivot=(0,0,0),parent='body'):
        if name in {b['name'] for b in self.bones}:name+=str(self.i);self.i+=1
        b={'name':name,'parent':parent,'pivot':list(pivot),'cubes':[]};self.bones.append(b);return b
    def box(self,b,o,s,r=None,pivot=None):
        c={'origin':list(o),'size':list(s)}
        if r:c.update(rotation=list(r),pivot=list(pivot or [o[i]+s[i]/2 for i in range(3)]))
        b['cubes'].append(c)
    def ellipsoid(self,b,at,size,bands=5):
        w,h,d=size
        for i in range(bands):
            t=(i+.5)/bands*2-1;f=math.sqrt(max(.08,1-t*t));self.box(b,(at[0]-w*f/2,at[1]-h/2+i*h/bands,at[2]-d*f/2),(w*f,h/bands+.15,d*f))
    def rod(self,name,a,b,width=1,parent='body'):
        bone=self.bone(name,a,parent);delta=[b[i]-a[i] for i in range(3)];length=math.sqrt(sum(v*v for v in delta));length=max(.2,length)
        self.box(bone,(a[0]-width/2,a[1]-width/2,a[2]),(width,width,length),[-math.degrees(math.asin(max(-1,min(1,delta[1]/length)))),math.degrees(math.atan2(delta[0],delta[2])),0],a);return bone
    def ring(self,name,at,r,width=1,plane='xy',segments=12,parent='body'):
        bone=self.bone(name,at,parent)
        for i in range(segments):
            a=i*math.tau/segments;x,y,z=(at[0]+math.cos(a)*r,at[1]+math.sin(a)*r,at[2]) if plane=='xy' else (at[0]+math.cos(a)*r,at[1],at[2]+math.sin(a)*r)
            self.box(bone,(x-width/2,y-width/2,z-width/2),(width,width,width))
        return bone
    def head(self,at,size=(8,8,7)):
        b=self.bone('head',at);self.ellipsoid(b,at,size,4)
        eye_count=1+int(hashlib.sha256(self.name.encode()).hexdigest()[:4],16)%3
        for i in range(eye_count):
            e=self.bone('eye_'+str(len(self.bones)),at,b['name']);xx=at[0]+(i-(eye_count-1)/2)*size[0]*.23
            self.box(e,(xx-.55,at[1]+size[1]*.12,at[2]-size[2]/2-.22),(1.1,.75,.35))
        m=self.bone('mouth_'+str(len(self.bones)),at,b['name']);self.box(m,(at[0]-size[0]*.25,at[1]-size[1]*.26,at[2]-size[2]/2-.18),(size[0]*.5,size[1]*.15,.3))
        for i in range(3):
            tooth=self.bone('tooth_'+str(len(self.bones)),at,b['name']);self.box(tooth,(at[0]+(i-1)*size[0]*.13-.25,at[1]-size[1]*.16,at[2]-size[2]/2-.3),(.5,.7,.4))
        return b
    def limbs(self,count,radius=6,height=12,spider=False):
        for i in range(count):
            a=(i+.5)*math.tau/count;hip=(math.cos(a)*radius*.5,height,math.sin(a)*radius*.5);knee=(math.cos(a)*radius*1.4,height*.6,math.sin(a)*radius*1.4);foot=(math.cos(a)*radius*1.7,0,math.sin(a)*radius*1.7)
            upper=self.rod('leg'+str(i),hip,knee,1.3 if spider else 2.3,parent='root');self.rod('leg'+str(i)+'_joint',knee,foot,.8 if spider else 1.5,upper['name'])
    def tentacles(self,count,at,length=14):
        for i in range(count):
            a=i*math.tau/count;last=at;parent='body'
            for j in range(4):
                p=(at[0]+math.cos(a+j*.24)*(3+j*1.7),max(0,at[1]-length*(j+1)/4),at[2]+math.sin(a+j*.24)*(3+j*1.7));b=self.rod('tendril'+str(i)+'_'+str(j),last,p,max(.6,2-j*.4),parent);last=p;parent=b['name']
    def finish(self):return {'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.'+self.name,'texture_width':512,'texture_height':512,'visible_bounds_width':8,'visible_bounds_height':8,'visible_bounds_offset':[0,2,0]},'bones':self.bones}]}

def build(name):
    if name not in DESIGNS:return summon(name)
    kind,_,_=DESIGNS[name];r=Rig(name);body=r.bones[1];seed=int(hashlib.sha256(name.encode()).hexdigest()[:8],16);q=seed%5
    if kind in ('spider','cricket','tick','centipede','crab','hermit'):
        r.ellipsoid(body,(0,10,3),(12+q,10,16+q),6);r.head((0,10,-8),(8,6,7));r.limbs(8 if kind=='spider' else 6 if kind in ('cricket','hermit') else 10 if kind=='centipede' else 4,8+q/2,10,True)
        if kind=='cricket':
            for s in (-1,1):r.rod('spring_leg'+str(s),(s*4,12,3),(s*10,21,10),2);r.rod('spring_foot'+str(s),(s*10,21,10),(s*15,0,15),1)
        if kind=='centipede':
            for i in range(5):r.ring('drain_grate'+str(i),(0,10,2+i*3),6-i*.4,.9)
        if kind=='hermit':
            for i in range(7):r.ring('shell_spiral'+str(i),(0,16+i*.8,4),8-i,.8,plane='xz')
        if kind=='spider':
            r.rod('needle',(0,14,8),(0,28,12),1.6);r.ring('web_spinner',(0,13,12),6,.9)
        if kind=='tick':
            for s in (-1,1):r.box(r.bone('paper_fold'+str(s)),(s*8-7,13,-3),(14,.5,16),[0,0,s*30])
        if kind=='crab':
            for s in (-1,1):r.rod('pincer_arm'+str(s),(s*4,9,-4),(s*11,7,-10),2);r.ring('chalk_claw'+str(s),(s*11,7,-12),3,1.4)
    elif kind in ('worm','eel','braid'):
        n=8 if kind=='eel' else 6
        for i in range(n):
            a=i*.45;at=(math.sin(a)*4,5+math.sin(i*.6)*2,i*4-10);b=body if i==0 else r.bone('tail'+str(i),at);r.ellipsoid(b,at,(9-i*.6,8-i*.4,6),3)
            if kind=='eel':r.box(r.bone('fin'+str(i)),(at[0],at[1]+2,at[2]-1),(.6,5,4),[0,0,25])
        r.head((0,7,-13),(10,9,8));r.tentacles(3 if kind=='braid' else 2,(0,8,-16),5)
        if name=='lantern_leech':r.ring('lantern_cage',(0,10,6),7,1.7);r.ellipsoid(r.bone('lantern_core'),(0,10,6),(8,9,7),4)
    elif kind in ('veil','jelly','moth','ray','book','vortex','mirror'):
        height=20 if kind in ('veil','jelly','mirror','vortex') else 12
        r.ellipsoid(body,(0,height,0),(12,7,9),5);r.head((0,height+5,-2),(9,7,6))
        if kind in ('veil','jelly'):r.tentacles(8 if kind=='jelly' else 6,(0,height-2,0),height-1)
        else:
            count=12 if kind in ('vortex','mirror') else 4 if kind=='moth' else 6
            for i in range(count):
                side=-1 if i%2==0 else 1;a=i*math.tau/count;at=(math.cos(a)*(8+q),height+math.sin(a)*6,math.sin(a)*3)
                b=r.bone(('wing' if kind in ('moth','ray','book') else 'shard')+str(i),at)
                r.box(b,(at[0]-4,at[1]-3,at[2]),(8 if kind!='ray' else 14,6 if kind!='ray' else 1,1 if kind!='ray' else 10),[0,(i%3-1)*20,side*(15+i*4)])
                if kind=='moth':r.ring('wick_eye'+str(i),(at[0],at[1],at[2]-.5),2,.6)
        if kind=='veil':
            for i in range(9):r.box(r.bone('veil_ribbon'+str(i)),((i-4)*1.8,5,1+abs(i-4)),(1.5,20-i%3,1),[0,0,(i-4)*3])
        if kind=='book':
            for s in (-1,1):
                for j in range(5):r.box(r.bone('page'+str(s)+'_'+str(j)),(s*7-5,height-2+j*.6,-5),(10,.35,12),[0,0,s*(20+j*6)])
            r.limbs(6,5,7,True) if name=='ledger_crawler' else r.tentacles(3,(0,9,1),9)
    elif kind in ('portal','cathedral','reliquary','shield','eclipse'):
        radius=10 if kind!='eclipse' else 13
        r.ring('outer_frame',(0,20,0),radius,2,segments=16 if kind=='eclipse' else 8);r.head((0,20,0),(7,7,4))
        if kind=='portal':
            for s in (-1,1):r.box(r.bone('hinge_pillar'+str(s)),(s*9-1,1,-2),(2,31,4),[0,0,s*6]);r.rod('hinge_leg'+str(s),(s*9,5,0),(s*14,0,7),2)
            for i in range(5):r.box(r.bone('gate_tooth'+str(i)),((i-2)*3,10,-1),(1.8,7,2),[0,0,(i-2)*6])
        elif kind=='cathedral':
            for s in (-1,1):r.box(r.bone('tower'+str(s)),(s*9-3,8,-3),(6,22+q,6));r.rod('spire'+str(s),(s*9,29,0),(s*9,40,0),1.8)
            for i in range(4):r.rod('buttress'+str(i),((i-1.5)*5,26,2),((i-1.5)*8,4,8),1.5)
        elif kind=='eclipse':r.tentacles(7,(0,11,1),11);r.ring('inner_orbit',(0,20,-1),7,.8,segments=13)
        elif kind=='shield':
            for s in (-1,1):r.rod('blade_arm'+str(s),(s*9,18,0),(s*17,8,-3),3);r.rod('blade_tip'+str(s),(s*17,8,-3),(s*15,24,-5),.9)
            r.tentacles(4,(0,11,0),11)
        else:
            r.rod('cross',(0,12,1),(0,36,1),2);r.rod('crossbar',(-10,27,1),(10,27,1),1.4);r.tentacles(6,(0,16,1),14)
    elif kind=='crystal':
        r.ellipsoid(body,(0,13,0),(11,13,11),5);r.head((0,15,-2),(8,8,6));r.limbs(4,7,7,True)
        for i in range(9):
            a=i*math.tau/9;at=(math.cos(a)*7,7+i%3*3,math.sin(a)*7);r.rod('salt_spire'+str(i),at,(at[0]*1.4,at[1]+7+i%4,at[2]*1.4),1.8)
        r.ring('salt_cage',(0,17,0),10,.9,plane='xz')
    elif kind in ('bell','vase','cup','cauldron','purse','clam','sack','doll','furnace'):
        width=14+q;height=18 if kind in ('bell','vase','furnace') else 12
        r.ellipsoid(body,(0,height/2+5,0),(width,height,12),7);r.head((0,height+5,-2),(8,6,6));r.limbs(3+q%3,7,height*.45)
        if kind in ('bell','furnace'):
            r.ring('rim',(0,6,0),width*.6,1.5,plane='xz',segments=16)
            for i in range(8):a=i*math.tau/8;r.rod('rib'+str(i),(math.cos(a)*4,height+3,math.sin(a)*4),(math.cos(a)*9,6,math.sin(a)*9),1.1)
            r.rod('clapper',(0,20,0),(0,2,0),2);r.ellipsoid(r.bone('ember_core'),(0,12,0),(7,7,7),4)
        if kind in ('cup','vase'):r.ring('porcelain_lip',(0,height+4,0),width*.45,1.5,plane='xz');r.ring('handle',(width*.5,12,0),5,1.2)
        if kind=='clam':
            for s in (-1,1):r.box(r.bone('vault_jaw'+str(s)),(-8,11+s*5,-8),(16,2,16),[s*25,0,0])
            r.rod('gilded_tongue',(0,11,-7),(0,5,-22),3)
        if kind=='purse':r.ring('purse_mouth',(0,17,-2),6,2);r.tentacles(4,(0,8,0),8)
        if kind=='doll':
            for s in (-1,1):r.head((s*7,17,0),(6,7,6));r.rod('needle_arm'+str(s),(s*6,12,0),(s*14,4,-5),.8)
        if kind=='furnace':
            for s in (-1,1):r.rod('horn'+str(s),(s*3,24,0),(s*8,34,1),2);r.rod('arm'+str(s),(s*7,17,0),(s*13,3,-2),3)
    elif kind in ('tree','rose','spine','throne','stilt','pipes','knot','maws','urchin','horn','rail','hound','procession','boat'):
        r.ellipsoid(body,(0,13,0),(10+q,13,10+q),5);r.head((0,23,-1),(9,9,7))
        if kind in ('tree','rose','spine','throne','stilt','pipes'):
            r.limbs(6 if kind in ('tree','rose') else 3 if kind=='stilt' else 4,7,15 if kind=='stilt' else 9,kind in ('rose','stilt'))
            for i in range(6+q):
                a=i*2.399;at=(math.cos(a)*(6+i*.7),18+(i%3)*4,math.sin(a)*5);r.rod('branch'+str(i),(0,16+i,0),at,1.4)
                if kind in ('tree','throne'):r.ellipsoid(r.bone('skull_fruit'+str(i)),at,(4,4,4),3)
                elif kind=='rose':r.box(r.bone('thorn_petal'+str(i)),(at[0]-3,at[1]-1,at[2]-2),(6,1,5),[0,i*22,25])
                elif kind=='pipes':r.ring('pipe_mouth'+str(i),at,2,1)
            if kind=='spine':
                for j in range(7):r.ring('ribcage'+str(j),(0,8+j*3,0),4+math.sin(j)*2,.8,plane='xz')
            if kind=='throne':r.box(r.bone('throne_back'),(-9,5,5),(18,28,3));r.ring('crown',(0,30,-1),7,1.4,segments=7)
        elif kind in ('maws','urchin','horn','knot'):
            for i in range(10 if kind=='maws' else 6+q):
                a=i*2.399;at=(math.cos(a)*(7+q),10+(i%4)*5,math.sin(a)*(6+q));r.ring('mouth'+str(i),at,3+i%2,1,segments=8)
                r.rod('tendril'+str(i),(0,14,0),at,.8 if kind=='urchin' else 1.8)
            r.tentacles(5,(0,9,0),9)
        elif kind=='hound':
            body['cubes']=[];r.ellipsoid(body,(0,9,2),(10,9,23),5);r.limbs(6 if name=='grin_hound' else 4,7,8);r.head((0,9,-12),(12,7,12));r.ring('jaw_hinge',(0,8,-17),6,1.5);r.rod('tail',(0,11,12),(5,19,23),2)
        elif kind=='rail':
            body['cubes']=[];r.box(body,(-7,7,-15),(14,10,30))
            for i in range(4):
                for s in (-1,1):r.ring('wheel'+str(i)+'_'+str(s),(s*8,5,i*7-10),4,1.4)
            for i in range(3):r.rod('chimney'+str(i),((i-1)*4,15,0),((i-1)*4,26+i*3,2),2)
        elif kind in ('boat','procession'):
            body['cubes']=[]
            for i in range(7):r.box(body,(-7,7+abs(i-3)*.5,(i-3)*4),(14,2,4))
            r.limbs(6,8,7,True)
            for i in range(5):r.ellipsoid(r.bone('mask'+str(i)),((i-2)*3,19+i%2*4,0),(4,6,3),3)
            r.rod('oar',(-6,23,1),(13,0,-5),1.2) if kind=='boat' else r.ring('procession_banner',(0,27,1),10,1.1)
    # Paired architectural themes have different large structures, not just different paint.
    if name=='knell_keeper':
        r.rod('bell_staff',(9,0,3),(9,36,3),1.6);r.ring('staff_bell',(9,32,3),5,1.2)
        for s in (-1,1):r.box(r.bone('keeper_cowl'+str(s)),(s*9-2,5,2),(4,23,3),[0,0,s*18])
    elif name=='cracked_bell':
        for i in range(7):r.rod('broken_rim_tooth'+str(i),((i-3)*2,6,-8),((i-3)*2,1+(i%2)*2,-10),1.1)
    elif name=='doorframe_hunter':
        for i in range(6):r.rod('hinge_finger'+str(i),((i-2.5)*3,7,-2),((i-2.5)*4,1,-13-i%2*3),1.3)
    elif name=='gravebound_gate':
        r.box(r.bone('torii_roof'),(-17,31,-3),(34,3,6),[0,0,4]);r.box(r.bone('torii_crossbar'),(-14,26,-2),(28,2,4))
    elif name=='lacquer_butcher':
        for s in (-1,1):r.rod('hook_arm'+str(s),(s*6,16,0),(s*16,12,-2),3);r.box(r.bone('cleaver'+str(s)),(s*16-2,3,-4),(5,14,1.5),[0,0,s*18])
    elif name=='soot_imp':
        for i in range(4):r.ellipsoid(r.bone('soot_plume'+str(i)),(math.sin(i)*3,20+i*5,2),(8-i,6,6-i),4)
        for s in (-1,1):r.box(r.bone('bat_wing'+str(s)),(s*12-7,13,2),(14,.6,10),[0,0,s*24])
    elif name=='rustjaw_pup':
        for i in range(5):r.box(r.bone('rust_scale'+str(i)),(-6,13-i*.3,i*4-5),(12,1.5,5),[0,0,(i%2*2-1)*12])
    elif name=='grin_hound':
        for j in range(3):r.ring('nested_jaw'+str(j),(0,7,-18-j*2),5-j,1.2)
    elif name=='static_mite':
        r.limbs(6,8,6,True)
        for s in (-1,1):r.rod('antenna'+str(s),(s*3,23,0),(s*10,31,-4),.7);r.ring('electric_coil'+str(s),(s*10,28,-4),3,.7)
    elif name=='dustling':
        for i in range(10):a=i*math.tau/10;r.rod('dust_whisker'+str(i),(0,14,0),(math.cos(a)*16,7+math.sin(a)*10,math.sin(a)*13),.55)
    elif name=='shriek_vessel':
        for i in range(6):a=i*math.tau/6;r.ring('shriek_mouth'+str(i),(math.cos(a)*7,14,math.sin(a)*7),2.2,.8)
    elif name=='porcelain_nipper':
        for s in (-1,1):r.rod('nipper'+str(s),(s*5,10,-5),(s*12,5,-13),1.6);r.ring('nipper_claw'+str(s),(s*12,5,-14),3,.9)
    # Every species receives its own strongly visible crest/signature, rather than a palette swap.
    for i in range(3+q):
        a=i*math.tau/(3+q);at=(math.cos(a)*(4+q*.5),27+(i%2)*3,math.sin(a)*(4+q*.5));r.rod('signature_'+str(i),at,(at[0]*1.4,at[1]+3+q,at[2]*1.4),.8+(seed%3)*.3)
    return r.finish()

def summon(name):
    names={'toad','max_elephant','great_serpent','rabbit_escape','moon_dregs','garuda','paper_heron','lantern_moth','iron_tortoise','well_unknown_abyss','transfigured_soul'}
    if name not in names:return None
    r=Rig(name);body=r.bones[1]
    if name in ('toad','well_unknown_abyss'):
        r.ellipsoid(body,(0,8,1),(21,13,22),7);r.head((0,11,-8),(17,10,12));r.limbs(4,10,8)
        for s in (-1,1):r.ellipsoid(r.bone('eye_stalk'+str(s)),(s*6,17,-10),(5,5,5),3);r.box(r.bone('web_foot'+str(s)),(s*12-4,0,-12),(8,1,8))
        r.rod('tongue',(0,8,-13),(0,6,-29),2)
        if name=='well_unknown_abyss':
            for s in (-1,1):
                for j in range(5):r.box(r.bone('wing'+str(s)+'_'+str(j)),(s*(11+j*3)-2,13-j*.5,1),(4,1,14),[0,s*15,s*20])
    elif name=='max_elephant':
        r.ellipsoid(body,(0,24,4),(26,25,34),8);r.head((0,28,-17),(20,21,17));r.limbs(4,12,16)
        for s in (-1,1):r.box(r.bone('ear'+str(s)),(s*15-6,19,-16),(12,20,2),[0,s*25,0]);r.rod('tusk'+str(s),(s*7,21,-24),(s*11,26,-37),2)
        for j in range(6):r.ellipsoid(r.bone('trunk'+str(j)),(0,24-j*3,-27-j*.8),(6-j*.6,4,6-j*.6),3)
    elif name in ('great_serpent','garuda'):
        for j in range(14):
            a=j*.43;at=(math.cos(a)*(8+j*.25),4+j*.7,math.sin(a)*(8+j*.25));r.ellipsoid(body if j==0 else r.bone('tail'+str(j)),at,(8-j*.3,7-j*.25,9-j*.3),4)
        r.head((8,12,-5),(12,9,16));r.rod('fork_tongue',(8,10,-13),(8,10,-24),.7)
        for s in (-1,1):r.rod('fang'+str(s),(8+s*3,11,-12),(8+s*3,5,-14),.8)
        if name=='garuda':
            for s in (-1,1):
                for j in range(5):r.box(r.bone('wing'+str(s)+'_'+str(j)),(s*(8+j*3),16,-2),(3,1,14),[0,s*12,s*20])
    elif name=='rabbit_escape':
        r.ellipsoid(body,(0,5,1),(8,9,10),6);r.head((0,9,-4),(7,7,7));r.limbs(4,4,4)
        for s in (-1,1):r.box(r.bone('ear'+str(s)),(s*2-1,11,-4),(2,10,3),[0,0,s*12]);r.box(r.bone('hind_foot'+str(s)),(s*4-2,0,1),(4,2,7))
        r.ellipsoid(r.bone('tail'),(0,6,7),(4,4,4),3)
    elif name=='moon_dregs':
        r.ellipsoid(body,(0,21,0),(18,11,18),7);r.head((0,22,-6),(11,7,9));r.tentacles(9,(0,18,0),18);r.ring('moon_halo',(0,29,0),9,.8,plane='xz')
    elif name in ('paper_heron','lantern_moth'):
        r.ellipsoid(body,(0,13,0),(8,10,12),5);r.head((0,20,-4),(6,7,8));r.limbs(2,4,12,True)
        for s in (-1,1):
            for j in range(7):r.box(r.bone('wing'+str(s)+'_'+str(j)),(s*(6+j*2)-1,15-j*.5,0),(3,.6,12+j),[0,s*12,s*18])
        r.rod('beak',(0,20,-8),(0,19,-19),1) if name=='paper_heron' else r.ring('lantern',(0,13,1),7,1.4)
    elif name=='iron_tortoise':
        r.ellipsoid(body,(0,9,2),(24,16,28),7);r.head((0,6,-15),(8,7,10));r.limbs(4,10,6)
        for x in (-1,0,1):
            for z in (-1,0,1):r.box(r.bone('shell_plate'+str(x)+'_'+str(z)),(x*6-3,15-abs(x)*1.7-abs(z),z*7-1),(6,3,7),[0,0,-x*12])
    else:
        r.ellipsoid(body,(0,14,0),(10,17,9),6);r.head((1,23,-2),(12,8,9));r.limbs(3,7,8)
        for s in (-1,1):r.rod('arm'+str(s),(s*5,18,0),(s*12,5,-3),2);r.ring('soul_mouth'+str(s),(s*4,15,-5),3,.7)
    return r.finish()
