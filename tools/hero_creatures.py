"""Hand-authored hero forms: clear anatomy and large connected masses, without stacked rings."""
import math
from reborn_creatures import Rig
REFINED={'shadow_mahoraga','toad','well_unknown_abyss','nue','rika'}

def build(name):
    if name not in REFINED:return None
    r=Rig(name);body=r.bones[1]
    def box(n,o,s,parent='body',rot=None):
        b=r.bone(n,[o[i]+s[i]/2 for i in range(3)],parent);r.box(b,o,s,rot);return b
    if name in ('toad','well_unknown_abyss'):
        r.box(body,(-9,3,-7),(18,8,18));r.box(body,(-7,9,-5),(14,3,14));box('belly_bone',(-7,2,-8),(14,5,14))
        head=box('head',(-10,7,-13),(20,7,11));box('mouth',(-8,7.4,-13.25),(16,1.2,.35),'head')
        for s in (-1,1):
            box('eye_brow'+str(s),(s*6-2,13,-10),(4,3,4),'head')
            box('eye_'+str(s),(s*6-1.2,13.4,-10.3),(2.4,1.8,.4),'head')
            r.rod('leg_front'+str(s),(s*7,8,-6),(s*10,2,-10),2.5,parent='root')
            box('leg_back'+str(s),(s*10-3,2,4),(6,7,9),'root',[0,s*18,s*8])
            for n in range(3):
                box('web_foot'+str(s)+'_'+str(n),(s*11-2+n*1.3,0,-12),(1.1,1,5+n%2),'root',[0,(n-1)*12,0])
        box('tongue',(-1,7.5,-17),(2,.5,4),'head')
        if name=='well_unknown_abyss':
            for s in (-1,1):
                for n in range(5):box('wing'+str(s)+'_'+str(n),(s*(11+n*2)-1.5,10,1),(3,.7,11+n),'body',[0,s*8,s*14])
    elif name=='nue':
        r.box(body,(-5,12,-4),(10,11,10));box('breast',(-4,10,-5),(8,10,3))
        box('head',(-4,23,-5),(8,8,8));box('mask',(-3.5,22.5,-5.6),(7,7,1.2),'head')
        for s in (-1,1):
            box('eye_'+str(s),(s*1.8-.8,26,-6), (1.6,.8,.5),'head')
            r.rod('leg'+str(s),(s*3,13,1),(s*4,2,-1),1.5,parent='root')
            for n in range(3):r.rod('claw'+str(s)+'_'+str(n),(s*4+(n-1)*.8,2,-1),(s*4+(n-1)*1.2,1,-4),.6,parent='leg'+str(s))
            wing=box('wing'+str(s),(s*9-5,18,0),(10,2,7),'body',[0,0,s*8])
            for n in range(7):box('wing_feather'+str(s)+'_'+str(n),(s*(8+n*2)-1.6,17-n*.45,3+n*.4),(3.2,1,13-n*.8),wing['name'],[0,s*(n*3),s*8])
        for n in range(4):box('tail'+str(n),(-3+n*1.6,10,5),(1.5,1,13-n%2*2),'body',[10,0,0])
    else:
        giant=name=='shadow_mahoraga'
        r.box(body,(-7,18,-3.5),(14,10,7));r.box(body,(-5.5,14,-3),(11,5,6));r.box(body,(-4.5,10,-2.7),(9,5,5.4))
        for s in (-1,1):
            box('pectoral'+str(s),(s*3.4-3.1,22,-4.4),(6.2,4.5,2),'body',[0,0,s*6])
            box('shoulder'+str(s),(s*8.5-3,23,-3),(6,6,6),'body',[0,0,s*10])
            arm=box('arm'+str(s),(s*9-2.1,11,-2),(4.2,15,4.8),'body',[0,0,s*4])
            for n in range(3):box('claw'+str(s)+'_'+str(n),(s*9-1.7+n*1.2,8,-2.3),(1,4,1.2),arm['name'])
            box('leg'+str(s),(s*3-2,0,-2),(4,12,5.5),'root')
            box('foot'+str(s),(s*3-2.1,0,-4.3),(4.2,2,7),'leg'+str(s))
            box('cloth_sash'+str(s),(s*3-2,7,-3.4),(4,7,1.1),'body',[0,0,s*6])
        if giant:
            box('head',(-3.8,30,-3.8),(7.6,8,7.5));box('mouth',(-2.7,31.5,-4.05),(5.4,1.3,.5),'head')
            for s in (-1,1):
                for n in range(3):box('mask_wing'+str(s)+'_'+str(n),(s*(4+n*.9)-1.4,34+n*.7,-3.7),(2.8,.8,3),'head',[0,0,-s*(12+n*9)])
                box('tooth'+str(s),(s*1.3-.4,31.5,-4.4),(.8,1,.7),'head')
            box('blade',(11,10,-1),(1.8,19,2.2),'arm1',[0,0,-9])
            r.bone('wheel',(0,45,1.5))
        else:
            box('head',(-5,28,-5),(10,8,10));box('jaw_bone',(-5.5,25,-5.6),(11,3,7),'head')
            box('mouth',(-4,27,-5.8),(8,3,.5),'head')
            for n in range(7):box('tooth'+str(n),(-3.6+n*1.1,27.5,-6),(.65,1.3,.6),'head')
            for s in (-1,1):
                box('eye_'+str(s),(s*2.5-.5,32,-5.3),(1,.7,.5),'head')
                box('horn'+str(s),(s*4-1,35,-1),(2,7,2),'head',[0,0,-s*18])
    return r.finish()
