"""Authored hero silhouettes. Root/body/head names preserve GeckoLib controller compatibility."""
import copy

def anatomy(name,source):
    geo=copy.deepcopy(source);bones=[]
    def part(n,parent,pivot,boxes):
        b={'name':n,'pivot':list(pivot),'cubes':boxes}
        if parent:b['parent']=parent
        bones.append(b);return b
    def box(o,s,rotation=None):
        b={'origin':list(o),'size':list(s)}
        if rotation:b.update(rotation=list(rotation),pivot=[o[i]+s[i]/2 for i in range(3)])
        return b
    part('root',None,(0,0,0),[])
    if name in ('shadow_mahoraga','ash_oni','rika','agito','custom_chimera'):
        giant=name=='shadow_mahoraga';w=15 if giant else 14 if name in ('rika','agito') else 13
        part('body','root',(0,21,0),[box((-w/2,14,-4),(w,17,9)),box((-5,10,-3),(10,6,7))])
        part('head','body',(0,32,0),[box((-4.5,30,-4.5),(9,10,9))])
        for side in (-1,1):
            part('pectoral'+str(side),'body',(side*3.7,26,0),[box((side*3.7-3.5,24,-5),(7,6,2),[0,0,side*7])])
            part('shoulder'+str(side),'body',(side*9,28,0),[box((side*9-3.3,25,-3.8),(6.6,6.6,7.6),[0,0,side*12])])
            part('arm'+str(side),'body',(side*9,27,0),[box((side*9-2.5,10,-2.5),(5,17,5.5))])
            part('gauntlet'+str(side),'arm'+str(side),(side*9,12,0),[box((side*9-3.2,9,-3.2),(6.4,6,6.4))])
            part('leg'+str(side),'root',(side*3.5,13,0),[box((side*3.5-2.5,0,-2.5),(5,14,6))])
            part('cloth_sash'+str(side),'body',(side*4,14,0),[box((side*4-2.5,6,-4),(5,10,1),[0,0,side*8])])
        for y in (16,19,22):part('rib'+str(y),'body',(0,y,0),[box((-4,y,-4.6),(8,1.2,1))])
        if giant:
            part('wheel','body',(0,44,2),[])
            part('blade','arm1',(9,15,0),[box((11,10,-1),(2,19,2),[0,0,-12]),box((10,26,-1),(3,5,2),[0,0,-28])])
            for side in (-1,1):
                for i in range(3):part('mask_wing'+str(side)+'_'+str(i),'head',(side*4,36,0),[box((side*5-1,34+i*1.8,-2),(5,1.4,3),[0,0,side*(15+i*18)])])
        elif name=='ash_oni':
            part('mask','head',(0,35,-4),[box((-4,30,-5.4),(8,8,1.5))])
            for side in (-1,1):part('horn'+str(side),'head',(side*3,39,0),[box((side*3-1,38,-1),(2,8,2),[0,0,-side*18]),box((side*4-1,44,-1),(1.5,3,1.5),[0,0,-side*30])])
        elif name=='rika':
            for side in (-1,1):
                part('arm_extra'+str(side),'body',(side*7,21,2),[box((side*11-1.6,5,1),(3.2,18,4),[0,0,side*25])])
                part('horn'+str(side),'head',(side*3,38,0),[box((side*4-1,37,-1),(2,7,2),[0,0,side*30])])
            part('jaw','head',(0,32,-4),[box((-5.5,29,-6),(11,4,5))])
        else:
            for side in (-1,1):part('wing'+str(side),'body',(side*6,28,3),[box((side*13-7,25,3),(14,2,9),[0,0,side*20])])
            for i in range(4):part('tail'+str(i),'body',(0,14,5+i*4),[box((-1.5,12-i,4+i*4),(3,3,5),[-10,0,0])])
    elif name=='nue':
        part('body','root',(0,12,0),[box((-5,6,-5),(10,14,13))]);part('head','body',(0,21,-5),[box((-7,17,-9),(14,12,9))])
        part('mask','head',(0,23,-10),[box((-6,18,-10),(12,9,1.3))]);part('beak','head',(0,21,-10),[box((-1.5,18,-13),(3,5,4),[-20,0,0])])
        for side in (-1,1):
            part('wing'+str(side),'body',(side*4,17,0),[box((side*15-11,15,-1),(22,2,9),[0,0,-side*9])])
            for i in range(7):part('feather'+str(side)+'_'+str(i),'wing'+str(side),(side*(8+i*3),16,5),[box((side*(8+i*3)-1,14,3),(2,1.2,12+i*.7),[0,side*12,side*i*2])])
            part('leg'+str(side),'root',(side*3,7,1),[box((side*3-1,1,0),(2,7,2))])
            for claw in range(3):part('talon'+str(side)+'_'+str(claw),'leg'+str(side),(side*3,1,0),[box((side*3-1+claw*.8,0,-4),(.7,1,5),[0,(claw-1)*12,0])])
        for i in range(5):part('tail'+str(i),'body',(0,8,6),[box((i*2-5,6,5),(2,1,16),[-20,(i-2)*8,0])])
    elif name in ('divine_dog','shadow_divine_dog','divine_dog_totality','grave_hound','tiger_funeral','round_deer','piercing_ox'):
        ox=name=='piercing_ox';deer=name=='round_deer';tiger=name=='tiger_funeral'
        w,h,d=(20,19,30) if ox else (11,10,22) if deer else (13,12,25) if tiger else (10,10,22)
        leg=17 if deer else 12
        part('body','root',(0,leg+h/2,0),[box((-w/2,leg,-d/2),(w,h,d))]);headY=leg+h-1
        part('head','body',(0,headY,-d/2),[box((-w*.43,headY-3,-d/2-7),(w*.86,9 if not deer else 11,9)),box((-w*.3,headY-3,-d/2-11),(w*.6,4,6))])
        for side in (-1,1):
            part('ear'+str(side),'head',(side*w*.32,headY+5,-d/2-3),[box((side*w*.32-1,headY+4,-d/2-4),(2,5,3),[0,0,side*18])])
            for z in (-d*.35,d*.35):part('leg'+str(side)+'_'+str(z),'root',(side*w*.38,leg,z),[box((side*w*.38-1.6,0,z-1.8),(3.2,leg+2,3.6))])
            if ox or deer:
                for branch in range(3 if deer else 1):part('antler'+str(side)+'_'+str(branch),'head',(side*w*.35,headY+5,-d/2),[box((side*(w*.35+branch*1.8)-.6,headY+5+branch*3,-d/2),(1.2,7,1.2),[0,0,-side*(20+branch*15)])])
        for i in range(4):part('tail'+str(i),'body',(0,leg+h*.65,d/2+i*3),[box((-1,leg+h*.55-i,d/2+i*3),(2,2,4))])
        if not deer:
            for i in range(6):part('fur_ridge'+str(i),'body',(0,leg+h,i*3-d/2),[box((-2,leg+h-1,i*3-d/2),(4,2.5,3))])
    else:return source
    if name=='custom_chimera':
        for side in (-1,1):part('chimera_head'+str(side),'body',(side*7,30,0),[box((side*7-3,27,-4),(6,8,8))])
        for i in range(3):part('chimera_spine'+str(i),'body',(0,25,5+i*3),[box((-1,24,5+i*3),(2,9-i*2,2),[-15,0,0])])
    if name=='agito':
        for side in (-1,1):part('antler'+str(side),'head',(side*3,39,0),[box((side*4-.7,38,-1),(1.4,9,1.4),[0,0,-side*25])])
    if name=='divine_dog_totality':
        for side in (-1,1):
            for i in range(6):part('mane'+str(side)+'_'+str(i),'head',(side*4,20+i,0),[box((side*(5+i*.4)-1,17+i,-6+i),(2,7,4),[0,0,side*25])])
    if name=='shadow_divine_dog':
        for i in range(7):part('shadow_spine'+str(i),'body',(0,22,i*3-7),[box((-.6,21,i*3-7),(1.2,4+i%3,1.5),[-20,0,0])])
    if name=='grave_hound':
        for i in range(7):
            for side in (-1,1):part('rib'+str(side)+'_'+str(i),'body',(side*4,18,i*3-8),[box((side*4-.6,10,i*3-8),(1.2,10,1.2),[0,0,side*20])])
    geo['minecraft:geometry'][0]['bones']=bones
    return geo
