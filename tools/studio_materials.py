"""Crisp clustered pixel materials at one texel per model unit, with restrained palettes."""
from PIL import ImageDraw


def mix(a,b,t):return tuple(round(x*(1-t)+y*t) for x,y in zip(a,b))
def shade(c,k):return tuple(max(0,min(255,round(v*k))) for v in c)+(255,)


def paint(draw,glow,x,y,w,h,kind,face,base,accent,seed,origin,size):
    neutral={'bone':(202,195,168),'metal':mix(base,(83,91,99),.42),'gold':(163,129,65),'wood':mix(base,(94,70,50),.55),'stone':mix(base,(94,97,103),.58),'paper':(209,201,171),'cloth':mix(base,(40,44,53),.2),'belly':mix(base,(190,176,144),.35),'shadow':(32,33,48),'glass':mix(base,(130,174,181),.45),'chitin':mix(base,(37,41,44),.30),'plant':mix(base,(54,78,54),.5),'wing':mix(base,(149,131,115),.25)}
    color=neutral.get(kind,base)
    value={'north':1,'south':.91,'east':.93,'west':.96,'up':1.10,'down':.78}[face]
    draw.rectangle((x,y,x+w-1,y+h-1),fill=shade(color,value))
    if kind=='eye':
        eye=mix(accent,(218,219,188),.18)
        draw.rectangle((x,y,x+w-1,y+h-1),fill=eye+(255,))
        if w>=3:draw.rectangle((x+w//2,y,x+w//2,y+h-1),fill=(27,27,29,255))
        if face=='north':glow.rectangle((x,y,x+w-1,y+h-1),fill=eye+(115,))
        return
    if kind=='mouth':
        draw.rectangle((x,y,x+w-1,y+h-1),fill=(35,27,32,255))
        if face=='north' and w>=4:
            for n in range(1,w-1,3):draw.point((x+n,y),fill=(200,190,164,255))
        return
    if min(w,h)<3:return
    # Broad, low-contrast clusters; never per-pixel random speckle or a border on every cube.
    dark=shade(color,value*.88);light=shade(color,min(1.22,value*1.08))
    if kind in ('cloth','paper'):
        if face in ('north','south'):
            if h>7:draw.line((x+1,y+h-2,x+w-2,y+h-2),fill=dark)
            if w>6 and h>9:
                draw.line((x+w//3,y+2,x+w//3,y+h-4),fill=dark)
                draw.line((x+w-3,y+h//3,x+w-3,y+h-3),fill=light)
            if kind=='paper' and w>=7 and h>=6:
                draw.rectangle((x+2,y+2,x+min(w-3,4),y+min(h-3,4)),fill=shade(mix(base,(83,72,63),.45),.9))
    elif kind in ('metal','gold','chitin'):
        if face=='north' and w>5 and h>5:
            draw.line((x+2,y+1,x+w-3,y+1),fill=light)
            if kind!='gold':draw.line((x+w-2,y+2,x+w-2,y+h-3),fill=dark)
        if w>10 and h>8:draw.rectangle((x+2,y+h-4,x+3,y+h-3),fill=dark)
    elif kind in ('bone','stone','wood'):
        if w>6 and h>8:
            u=x+2+(seed%max(1,w-5));v=y+2
            draw.line([(u,v),(u,v+h//3),(min(x+w-2,u+1),v+h//3),(min(x+w-2,u+1),y+h-3)],fill=dark)
            if kind=='wood' and w>9:draw.line((x+w-3,y+h//2,x+w-3,y+h-2),fill=light)
    elif kind in ('fur','wing','scale','stripe'):
        for n in range(max(1,min(4,w//5))):
            u=x+1+(n*5+seed%3)%max(1,w-3);v=y+2+(n*3)%max(1,h-4)
            draw.line((u,v,min(x+w-2,u+2),v),fill=dark)
            if v+1<y+h-1:draw.point((u,v+1),fill=light)
        if kind=='stripe' and w>8:
            for v in range(y+3,y+h-2,5):draw.rectangle((x,v,x+max(1,w//3),v+1),fill=shade(color,.55))
    else:
        if w>=6 and h>=6:
            u=x+1+seed%max(1,w-4);v=y+h//2
            draw.rectangle((u,v,min(x+w-2,u+2),min(y+h-2,v+1)),fill=dark)
            if w>10 and h>10:draw.rectangle((x+w-4,y+2,x+w-3,y+3),fill=light)


def damage(image,name,critical=False):
    out=image.copy();d=ImageDraw.Draw(out)
    opaque=[(x,y) for y in range(2,image.height-5,3) for x in range(2,image.width-5,3) if all(image.getpixel((x+n,y+m))[3] for n in range(3) for m in range(3))]
    if not opaque:return out
    seed=sum((i+1)*ord(c) for i,c in enumerate(name))
    for n in range(6 if critical else 3):
        x,y=opaque[(seed+n*97)%len(opaque)]
        for k in range(3 if critical else 2):
            px=x+k//2;py=y+k
            if image.getpixel((px,py))[3]:d.point((px,py),fill=(63,45,48,255))
    return out
