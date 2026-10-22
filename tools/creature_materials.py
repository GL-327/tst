"""Editable painted material recipes for the existing UV rigs. No sampled image noise.

Value groups, directional folds and sparse wear keep each material readable in game.
The same recipe creates every face, including the damaged and emissive variants.
"""
import math

def blend(a,b,t):return tuple(round(x*(1-t)+y*t) for x,y in zip(a,b))
def tint(a,f):return tuple(max(0,min(255,round(x*f))) for x in a)+(255,)

def paint(draw,glow,x,y,w,h,kind,face,base,accent,seed,origin,size):
    material={'bone':(218,205,175),'gold':(178,130,49),'eye':(22,15,24),'mouth':(40,14,22)}.get(kind,base)
    if kind=='cloth':material=blend(base,(20,23,38),.55)
    light={'north':1.03,'south':.83,'east':.88,'west':.96,'up':1.12,'down':.64}[face]
    for row in range(h):
        v=row/max(1,h-1)
        # Broad form shading rather than a black outline around every cube.
        world_y=origin[1]+size[1]*(1-v)
        form=.88+.10*math.sin(world_y*.075)
        draw.line((x,y+row,x+w-1,y+row),fill=tint(material,light*form))
    if kind=='eye':
        draw.rectangle((x,y,x+w-1,y+h-1),fill=accent+(255,))
        glow.rectangle((x,y,x+w-1,y+h-1),fill=accent+(180,))
        if w>7 and h>4:draw.line((x+w//2,y+1,x+w//2,y+h-2),fill=(13,9,20,255),width=max(1,w//7))
        return
    if w<5 or h<5:return
    edge=max(1,w//18)
    if kind in ('cloth','gold'):
        draw.line((x+edge,y+1,x+w-edge-1,y+1),fill=tint(material,light*1.15))
        draw.line((x+1,y+h-2,x+w-2,y+h-2),fill=tint(material,light*.6))
    if kind in ('cloth','feather','fur','stripe'):
        step=max(3,w//7)
        for i in range(2,w-1,step):
            shift=round(math.sin((seed%17+i)*.7)*min(w*.12,4))
            points=[(x+i,y+2),(x+max(1,min(w-2,i+shift)),y+h*.5),(x+max(1,min(w-2,i-shift)),y+h-2)]
            draw.line(points,fill=tint(material,.64 if kind=='cloth' else .74),width=max(1,w//30))
            if kind!='cloth':
                for v in range(4,h-2,5):draw.line((x+i,y+v,x+min(w-2,i+3),y+v-2),fill=tint(material,1.18))
        if kind=='cloth':
            for i in range(3,w-2,5):draw.line((x+i,y+h-4,x+i+1,y+h-4),fill=tint(accent,.7))
        if kind=='stripe':
            for v in range(3,h-2,max(5,h//5)):
                reach=max(2,int(w*(.26+.12*math.sin(v))))
                draw.polygon([(x,y+v),(x+reach,y+min(h-2,v+2)),(x,y+min(h-2,v+5))],fill=(38,31,34,255))
                draw.polygon([(x+w-1,y+min(h-2,v+3)),(x+w-reach,y+min(h-2,v+5)),(x+w-1,y+min(h-2,v+7))],fill=(38,31,34,255))
    elif kind=='scale':
        step=max(4,w//7)
        for row in range(2,h-2,step):
            for col in range(1,w-2,step):
                left=x+col;top=y+row
                draw.arc((left,top,min(x+w-2,left+step),min(y+h-2,top+step)),5,175,fill=tint(material,.58),width=1)
                draw.line((left+1,top+1,min(x+w-2,left+step//2),top+1),fill=tint(material,1.17))
    elif kind in ('bone','gold'):
        for col in range(max(2,w//5),w-2,max(4,w//4)):
            bend=1 if col%2 else -1
            draw.line([(x+col,y+2),(x+col+bend,y+h*.48),(x+col,y+h-3)],fill=tint(material,.71),width=max(1,w//45))
            draw.line((x+col+1,y+3,x+col+1,y+h*.43),fill=tint(material,1.21))
        if kind=='gold' and w>10 and h>8:
            mid=x+w//2
            draw.polygon([(mid,y+3),(mid+2,y+h//2),(mid,y+h-4),(mid-2,y+h//2)],outline=tint(accent,1.2))
    elif kind=='skin' and size[1]>=4:
        # A few deliberate tendon/scar lines; no salt-and-pepper texture on every face.
        for i in range(1,3):
            mid=x+int(w*(.25+i*.2));top=y+max(2,h//5)
            draw.line([(mid,top),(mid-1,y+h*.52),(mid+1,y+h-3)],fill=tint(blend(material,(66,38,53),.3),.8),width=max(1,w//40))
            draw.line((mid+2,top,mid+2,y+h*.45),fill=tint(material,1.13))
    if kind=='mouth':
        draw.rectangle((x+1,y+h//3,x+w-2,y+2*h//3),fill=(19,8,17,255))
        for col in range(2,w-2,4):draw.polygon([(x+col,y+h//3),(x+col+2,y+h//3),(x+col+1,y+2*h//3)],fill=(223,210,181,255))
