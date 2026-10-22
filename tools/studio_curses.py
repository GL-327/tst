"""Block-built curse art direction: connected forms, deliberate faces and low visual noise.

All 52 species are original designs. Mesh detail describes anatomy or a material edge;
there are no automatic bevel stacks, random crests or detached decorative cube clouds.
"""
import math
from reborn_creatures import Rig, DESIGNS


class Creature:
    def __init__(self, name):
        self.r = Rig(name)

    def box(self, name, at, size, material='skin', parent='body', rotation=None):
        bone = self.r.bone(name, at, parent)
        origin = [at[i] - size[i] / 2 for i in range(3)]
        self.r.box(bone, origin, size, rotation, at)
        bone['cubes'][0]['_material'] = material
        return bone['name']

    def limb(self, name, hip, knee, foot, width=2, material='skin', parent='body'):
        a = self.r.rod(name, hip, knee, width, parent)
        b = self.r.rod(name+'_joint', knee, foot, width*.8, a['name'])
        for bone in (a, b):
            for cube in bone['cubes']:
                cube['_material'] = material
                # GeckoLib converts model X rotation to a negative world angle.
                # Correct the old rod helper's pitch so both segments reach the joint.
                cube['rotation'][0] *= -1
        return b['name']

    def head(self, at=(0, 22, -2), size=(8, 7, 7), material='skin', eyes=2, mouth=True):
        name=self.box('head',at,size,material)
        x,y,z=at;w,h,d=size
        for n in range(eyes):
            xx=x+(n-(eyes-1)/2)*w*.36
            self.box('eye_'+str(n),(xx,y+h*.1,z-d/2-.13),(max(1,w*.16),max(.65,h*.14),.26),'eye',name)
        if mouth:self.box('mouth_', (x,y-h*.25,z-d/2-.15),(w*.48,max(.7,h*.14),.28),'mouth',name)
        return name

    def feet(self, count=4, spread=5, height=8, depth=5, material='skin'):
        for n in range(count):
            side=-1 if n%2==0 else 1;z=(n//2-(count/2-1)/2)*depth
            foot=self.limb('leg'+str(n),(side*spread*.55,height,z),(side*spread,height*.48,z-.5),(side*(spread+1),1,z-1),2.1,material,'root')
            self.box('foot'+str(n),(side*(spread+1),.8,z-1.5),(2.7,1.6,3.5),material,foot)

    def arms(self, height=17, spread=6, length=10, material='skin'):
        for side in (-1,1):
            hand=self.limb('arm'+str(side),(side*spread,height,0),(side*(spread+2),height-length*.55,-1),(side*(spread+2),height-length,-2),2.8,material)
            self.box('hand'+str(side),(side*(spread+2),height-length-1,-2.3),(3.4,2.5,3.3),material,hand)

    def horns(self, y=27, width=3, length=5, parent='head', material='bone'):
        anchor=[b for b in self.r.bones if b['name']==parent and b.get('cubes')]
        if not anchor:anchor=[b for b in self.r.bones if b['name'] in ('body_mass','shoulder_cap')]
        top=max(c['origin'][1]+c['size'][1] for b in anchor for c in b['cubes'])
        base=min(y,top-.2)
        for side in (-1,1):
            name=self.box('horn'+str(side),(side*width,base+length/2,0),(1.8,length,2),material,parent,[0,0,-side*22])
            next(b for b in self.r.bones if b['name']==name)['cubes'][0]['pivot']=[side*width,base,0]

    def face(self, at, width=5, height=3, eyes=2, parent='body'):
        x,y,z=at
        for n in range(eyes):self.box('eye_face'+str(len(self.r.bones)),(x+(n-(eyes-1)/2)*width*.4,y+height*.28,z),(width*.15,.8,.25),'eye',parent)
        self.box('mouth_face'+str(len(self.r.bones)),(x,y-height*.3,z),(width*.55,.9,.3),'mouth',parent)

    def frame(self, at, width, height, material='bone', parent='body'):
        x,y,z=at
        for side in (-1,1):self.box('frame_post'+str(len(self.r.bones)),(x+side*width/2,y,z),(2,height,2.5),material,parent)
        for side in (-1,1):self.box('frame_lintel'+str(len(self.r.bones)),(x,y+side*height/2,z),(width+2,2,2.5),material,parent)

    def robe(self, y=12, width=11, height=17):
        self.box('body_mass',(0,y,1),(width,height,7),'cloth')
        self.box('robe_hem',(0,y-height/2+1,1),(width+2,3,8),'cloth')
        self.box('collar',(0,y+height/2-1,-.2),(width+1,2,8),'cloth')


def build(name):
    if name not in DESIGNS:return None
    c=Creature(name);kind=DESIGNS[name][0]
    if kind in ('tick','crab','spider','cricket','centipede','hermit'):
        width=12 if kind in ('crab','hermit') else 9
        c.box('carapace',(0,8,3),(width,7,12),'chitin')
        c.box('belly',(0,5.5,1),(width-2,3,11),'belly')
        c.head((0,8,-5),(7,5,6),'bone' if kind in ('tick','crab') else 'skin',eyes=1 if kind=='cricket' else 2)
        c.feet(8 if kind=='spider' else 6,7,8,4,'chitin')
        if kind=='tick':
            for s in (-1,1):c.box('paper_wing'+str(s),(s*5,11,3),(7,1,12),'paper',rotation=[0,0,s*14])
            c.box('paper_seal',(0,11.6,2),(2,.3,6),'cloth')
        elif kind=='crab':
            for s in (-1,1):
                hand=c.limb('arm'+str(s),(s*4,8,-2),(s*9,7,-5),(s*10,8,-9),2.5,'bone')
                c.box('pincer_top'+str(s),(s*10,9,-10),(4,2,5),'bone',hand,[12,0,0]);c.box('pincer_bottom'+str(s),(s*10,6.7,-10),(4,1.5,5),'bone',hand,[-12,0,0])
        elif kind=='cricket':
            for s in (-1,1):
                c.limb('spring_leg'+str(s),(s*3,8,4),(s*7,16,8),(s*9,1,9),2,'chitin','root')
                c.box('antenna'+str(s),(s*2.8,14,-5),(1,8,1),'bone','head',[0,0,-s*16])
        elif kind=='centipede':
            for n in range(3):c.box('tail_segment'+str(n),(0,7,10+n*4),(8-n,5,5),'chitin')
            c.box('drain_plate',(0,11.6,2),(7,1,10),'metal')
        elif kind=='hermit':
            c.box('shell',(0,13,5),(13,13,12),'metal');c.box('shell_cap',(0,20,5),(9,2,10),'metal')
            c.box('pack_strap',(0,13,-1.3),(2,13,.5),'cloth')
        else:
            c.box('abdomen',(0,10,10),(12,9,13),'chitin');c.box('needle',(0,17,12),(1,10,1),'bone',rotation=[-20,0,0])
            for s in (-1,1):c.box('fang'+str(s),(s*2,5.5,-8),(1.4,3,1.5),'bone','head',[0,0,-s*15])
    elif kind in ('hound','rail'):
        c.box('body_mass',(0,11,1),(10,9,17),'fur' if kind=='hound' else 'metal');c.feet(4,4,9,10,'fur' if kind=='hound' else 'metal')
        if kind=='hound':
            c.head((0,17,-7),(8,8,8),'fur');c.box('muzzle',(0,15,-12),(7,4,5),'belly','head');c.box('mouth_muzzle',(0,14,-14.6),(5,1,.25),'mouth','head')
            for s in (-1,1):c.box('ear'+str(s),(s*3,22,-5),(2,5,3),'fur','head',[0,0,s*15])
            c.box('tail',(0,13,13),(2,3,10),'fur',rotation=[-25,0,0])
            if name=='rustjaw_pup':c.box('jaw_guard',(0,12.5,-11),(9,2,7),'metal','head')
            else:c.box('back_mane',(0,16,0),(11,3,10),'skin')
        else:
            c.head((0,14,-10),(12,10,6),'metal',eyes=3);c.box('grille',(0,11,-13.2),(9,3,.5),'mouth','head')
            for n in range(3):c.box('rail_plate'+str(n),(0,16,1+n*5),(11,2,4),'metal')
            c.box('smoke_stack',(0,20,-2),(3,7,3),'metal')
    elif kind in ('worm','eel','braid'):
        for n in range(5 if kind=='eel' else 4):
            c.box('body_mass' if n==0 else 'tail'+str(n),(math.sin(n*.45)*2,5,2+n*5),(9-n,7-n*.8,6),'scale' if kind=='eel' else 'skin')
        c.head((0,6,-4),(10,8,8),'scale' if kind=='eel' else 'skin',eyes=1 if name=='lantern_leech' else 2)
        if name=='lantern_leech':
            c.box('lantern',(0,11,6),(7,8,7),'gold');c.box('lantern_window',(0,11,2.4),(4,5,.3),'eye')
        elif kind=='braid':
            for s in (-1,1):c.box('braid'+str(s),(s*4,6,5),(3,5,15),'cloth',rotation=[0,s*18,0])
        elif kind=='eel':
            for n in range(3):c.box('fin'+str(n),(0,9,7+n*5),(1,4,5),'cloth',rotation=[15,0,0])
        else:c.box('belly',(0,2.1,5),(7,1,17),'belly')
    elif kind in ('veil','jelly','stilt','reliquary','procession'):
        c.robe(13,10 if kind!='procession' else 16,17)
        c.head((0,25,0),(7,7,6),'bone',eyes=1 if kind=='reliquary' else 2)
        c.arms(20,6,12,'cloth' if kind in ('veil','procession') else 'skin')
        if kind=='veil':
            c.box('veil_crown',(0,29,1),(11,1,9),'cloth','head');c.box('veil_back',(0,21,4),(11,16,1),'cloth','head')
            for s in (-1,1):c.box('veil_side'+str(s),(s*5,22,0),(1,14,8),'cloth','head')
        elif kind=='jelly':
            c.box('mantle',(0,28,1),(16,4,12),'belly')
            for n in range(4):c.limb('tendril'+str(n),((n-1.5)*3,6,1),((n-1.5)*3,3,1),((n-1.5)*4,0,0),1,'skin')
        elif kind=='stilt':
            for s in (-1,1):c.limb('leg'+str(s),(s*3,6,1),(s*4,-2,1),(s*5,-10,-1),1.6,'bone','root')
        elif kind=='reliquary':
            c.frame((0,22,1),11,17,'gold');c.box('halo',(0,31,1),(12,2,3),'gold')
        else:
            for s in (-1,1):c.head((s*6,22,1),(5,7,5),'bone',mouth=False)
            c.box('procession_hat',(0,29,1),(18,2,10),'cloth');c.box('lantern',(8,9,-3),(5,7,5),'gold')
    elif kind in ('book','vortex','moth','ray','mirror'):
        if kind=='mirror':
            c.frame((0,18,0),13,21,'metal');c.box('mirror_surface',(0,18,.4),(11,19,1),'glass');c.face((0,19,-.3),6,5)
            c.arms(18,7,12,'cloth');c.box('robe_tail',(0,4,1),(6,7,3),'cloth')
        elif kind in ('book','vortex'):
            c.box('body_mass',(0,10,0),(5,11,10),'paper');c.head((0,16,-3),(7,6,5),'bone')
            for s in (-1,1):
                c.box('wing_cover'+str(s),(s*6,10,0),(9,2,13),'cloth',rotation=[0,0,s*18]);c.box('page_block'+str(s),(s*6,11,0),(8,2,11),'paper',rotation=[0,0,s*18])
            if name=='ledger_crawler':c.feet(4,5,7,7,'bone');c.box('clasp',(0,13,-7),(4,2,2),'gold')
            elif kind=='vortex':
                for s in (-1,1):c.box('wing_fold'+str(s),(s*13,13,2),(7,1,14),'paper',rotation=[0,0,-s*25])
                c.box('ribbon',(0,5,5),(3,9,2),'cloth')
            else:c.box('tongue',(0,6,-8),(2,1,9),'cloth')
        else:
            c.box('body_mass',(0,11,0),(5,7,11),'fur' if kind=='moth' else 'skin');c.head((0,13,-6),(6,5,5),'bone' if kind=='moth' else 'skin',mouth=False)
            for s in (-1,1):
                c.box('wing'+str(s),(s*8,12,1),(13,1.5,13 if kind=='ray' else 11),'wing',rotation=[0,s*8,s*10]);c.box('wing_tip'+str(s),(s*15,13,4),(5,1,8),'wing',rotation=[0,s*15,-s*12])
            if kind=='moth':
                for s in (-1,1):c.box('antenna'+str(s),(s*2,18,-6),(1,6,1),'bone','head',[0,0,-s*25])
            else:c.box('tail',(0,10,13),(1.5,1.5,16),'skin',rotation=[-10,0,0])
    elif kind in ('portal','cathedral','shield','eclipse'):
        if kind=='eclipse':
            c.box('body_mass',(0,16,0),(14,14,10),'shadow');c.face((0,16,-5.2),8,5,eyes=1)
            for n in range(8):
                a=n*math.pi/4;c.box('halo'+str(n),(math.sin(a)*10,16+math.cos(a)*10,0),(8,1.5,2),'bone',rotation=[0,0,-n*45])
            for s in (-1,1):c.limb('tendril'+str(s),(s*5,10,1),(s*7,5,1),(s*4,0,1),2,'shadow')
        else:
            width=14 if name=='gravebound_gate' else 11
            c.frame((0,17,1),width,27,'bone' if kind=='portal' else 'metal');c.head((0,24,0),(7,7,6),'bone',eyes=1)
            c.box('crossbar',(0,14,1),(width,3,3),'metal');c.feet(2,6,7,4,'bone')
            if kind=='cathedral':
                for s in (-1,1):c.box('tower'+str(s),(s*8,20,2),(5,27,7),'stone');c.box('roof'+str(s),(s*8,34,2),(6,3,8),'stone',rotation=[0,0,s*15])
                c.box('chapel',(0,13,2),(10,14,9),'stone');c.face((0,16,-2.7),6,7)
            elif kind=='shield':c.arms(22,8,14,'metal');c.box('chest_seal',(0,15,-2),(9,12,2),'cloth')
            elif name=='doorframe_hunter':c.arms(17,7,15,'wood');c.box('door_panel',(3,15,1),(6,20,1),'wood',rotation=[0,-30,0])
            else:
                for s in (-1,1):c.box('gate_leaf'+str(s),(s*4,13,1),(6,20,1.5),'metal',rotation=[0,s*18,0])
    elif kind in ('bell','vase','cup','cauldron','purse','clam','sack','furnace'):
        if kind=='bell' and name=='knell_keeper':
            c.robe(11,11,18);c.arms(18,7,12,'bone');c.box('bell_head',(0,25,0),(11,9,10),'metal');c.box('bell_rim',(0,21,0),(13,2,12),'gold');c.face((0,25,-5.2),7,3)
        else:
            wide=14 if kind in ('bell','furnace','clam') else 11;height=15 if kind in ('bell','vase','furnace') else 10
            c.box('body_mass',(0,8+height/2,0),(wide,height,11),'metal' if kind in ('bell','cauldron','furnace') else 'bone' if kind in ('vase','cup') else 'cloth' if kind in ('purse','sack') else 'skin')
            c.box('rim',(0,8,0),(wide+2,2,13),'gold' if kind=='bell' else 'bone' if kind in ('vase','cup') else 'metal');c.feet(4,4,7,6,'bone' if kind in ('bell','vase','cup') else 'skin');c.face((0,12+height*.25,-5.7),wide*.65,4,eyes=1 if kind in ('vase','purse') else 2)
            if kind in ('bell','furnace'):
                c.box('shoulder_cap',(0,9+height,0),(wide-3,3,9),'metal');c.box('clapper',(0,5,0),(3,6,3),'gold')
                if kind=='furnace':c.horns(27,5,6,parent='body');c.arms(20,8,15,'stone');c.box('ember_vent',(0,14,-5.8),(8,5,.5),'mouth')
                else:c.box('bell_crack',(2,18,-5.7),(1,9,.4),'mouth')
            elif kind in ('vase','cup'):
                c.box('lip',(0,9+height,0),(wide+1,2,12),'bone')
                for n,(at,size) in enumerate([((wide/2+2,13,0),(2,7,2)),((wide/2+1,17,0),(4,2,2)),((wide/2+1,9,0),(4,2,2))]):c.box('handle'+str(n),at,size,'bone')
                if kind=='vase':c.box('neck',(0,11+height,0),(6,3,7),'bone')
            elif kind=='clam':
                c.box('upper_shell',(0,20,0),(16,3,15),'metal',rotation=[-18,0,0]);c.box('lower_shell',(0,11,0),(16,3,15),'metal');c.box('tongue',(0,13,-11),(3,2,13),'skin')
            elif kind=='purse':c.box('purse_clasp',(0,20,0),(12,2,3),'gold');c.box('coin',(0,16,-6),(3,4,.6),'gold')
            elif kind=='sack':c.box('seam',(0,15,-5.6),(1,10,.3),'cloth');c.box('stalk',(0,22,0),(3,5,3),'plant')
            else:
                c.arms(15,7,10,'skin');c.horns(23,4,4,parent='body',material='metal')
                if name=='lacquer_butcher':
                    c.box('butcher_apron',(0,11,-5.7),(8,10,.7),'cloth');c.box('cleaver',(10,5,-3),(5,10,1.5),'metal','arm1_joint')
                else:c.box('ember_tail',(0,10,9),(2,2,11),'skin',rotation=[-22,0,0]);c.box('soot_cap',(0,21,0),(8,4,9),'stone')
    elif kind=='crystal':
        c.box('body_mass',(0,12,0),(10,12,10),'stone');c.feet(4,5,7,5,'bone');c.face((0,13,-5.2),7,5,eyes=1)
        c.box('lantern_cap',(0,19,0),(12,2,12),'bone');c.box('lantern_handle',(0,23,0),(2,7,2),'bone')
        for n in range(4):
            a=n*math.pi/2;c.box('crystal'+str(n),(math.cos(a)*7,10,math.sin(a)*7),(3,8+n%2*3,3),'glass',rotation=[math.sin(a)*18,0,-math.cos(a)*18])
    elif kind in ('tree','rose','spine','throne','pipes'):
        c.box('body_mass',(0,15,1),(10,16,8),'plant' if kind in ('tree','rose') else 'bone' if kind in ('spine','throne') else 'metal');c.feet(4,5,9,6,'plant' if kind=='rose' else 'bone');c.head((0,27,0),(8,8,7),'bone');c.arms(22,7,14,'plant' if kind in ('tree','rose') else 'bone')
        if kind in ('tree','rose'):
            for s in (-1,1):
                c.limb('branch'+str(s),(s*3,26,2),(s*7,32,2),(s*10,35,2),2,'bone' if kind=='tree' else 'plant')
                c.box('crown_leaf'+str(s),(s*9,35,2),(7,4,7),'bone' if kind=='tree' else 'wing')
            if kind=='rose':c.box('flower',(0,29,-4.2),(9,7,1),'cloth','head')
        elif kind=='spine':
            for n in range(4):c.box('rib'+str(n),(0,10+n*4,-3.2),(12-n,1.5,2),'bone')
            c.horns(33,3,6);c.box('tail',(0,6,9),(2,2,13),'bone',rotation=[-15,0,0])
        elif kind=='throne':
            c.box('throne_back',(0,19,7),(20,27,3),'stone');c.box('throne_seat',(0,8,3),(20,3,16),'stone')
            for s in (-1,1):c.box('throne_arm'+str(s),(s*10,12,0),(3,11,13),'stone');c.box('crown'+str(s),(s*3,34,0),(2,6,2),'gold','head')
        else:
            for s in (-1,1):c.box('pipe'+str(s),(s*6,25,5),(3,17,3),'metal');c.box('pipe_cap'+str(s),(s*6,34,4),(4,2,5),'metal')
            c.box('valve',(0,18,-4.2),(5,5,1),'gold')
    elif kind in ('doll','urchin','horn','knot','maws','boat'):
        if kind=='boat':
            c.box('hull',(0,5,1),(15,5,25),'wood');c.box('prow',(0,8,-13),(11,3,5),'wood');c.robe(17,8,18);c.head((0,30,0),(7,7,6),'bone');c.arms(24,5,12,'cloth');c.box('oar',(9,14,0),(1.5,30,2),'wood',rotation=[15,0,15])
        elif kind=='urchin':
            c.box('body_mass',(0,7,0),(10,9,10),'fur' if name=='dustling' else 'chitin');c.face((0,8,-5.2),6,3);c.feet(4,4,5,5,'skin')
            for s in (-1,1):c.box('ear'+str(s),(s*4,13,0),(3,5,3),'fur' if name=='dustling' else 'metal',rotation=[0,0,s*20])
            if name=='static_mite':c.box('back_coil',(0,12,2),(6,2,7),'metal')
        elif kind=='knot':
            c.box('body_mass',(0,13,0),(10,12,7),'cloth');c.head((0,23,0),(7,7,6),'cloth');c.arms(18,6,11,'cloth');c.feet(2,3,7,5,'cloth')
            for s in (-1,1):c.box('knot_band'+str(s),(s*2,14,-4),(3,13,2),'cloth',rotation=[0,0,s*35])
        else:
            c.box('body_mass',(0,13,0),(13 if kind=='maws' else 9,12,8),'skin' if kind=='maws' else 'cloth' if kind=='doll' else 'metal');c.feet(2,3.5,8,5,'cloth' if kind=='doll' else 'skin');c.head((0,24,-1),(10,9,8),'cloth' if kind=='doll' else 'bone');c.arms(18,7,12,'cloth' if kind=='doll' else 'skin')
            if kind=='maws':
                for n in range(4):c.box('mouth_body'+str(n),((-1 if n%2==0 else 1)*3,10+n//2*5,-4.3),(4,2,.5),'mouth')
                c.horns(31,4,5)
            elif kind=='doll':c.box('patch',(2,13,-4.2),(4,5,.3),'belly');c.box('thread_eye',(2,24,-5.3),(1.5,1.5,.4),'mouth','head')
            else:
                c.box('horn_mouth',(0,24,-7),(7,6,6),'metal','head');c.box('mouth_bell',(0,24,-10.2),(6,5,.4),'mouth','head')
    else:raise AssertionError((name,kind))
    return c.r.finish()
