"""Authored analytic VFX materials: 16 guttered glows and 16 seamless volume surfaces.

The left bank supports soft accents; the right bank wraps actual three-dimensional
meshes. Animation happens in world space, independently of the viewer's camera.
"""
from pathlib import Path
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
N = 256
y, x = np.mgrid[-1:1:complex(N), -1:1:complex(N)]
r = np.hypot(x, y)
a = np.arctan2(y, x)
atlas = Image.new('RGBA', (N*8, N*4))

def smooth(edge, width):
    return np.exp(-((edge)/width)**2)

def stroke(points, width):
    value = np.zeros_like(x)
    for (ax, ay), (bx, by) in zip(points, points[1:]):
        dx, dy = bx-ax, by-ay
        u = np.clip(((x-ax)*dx+(y-ay)*dy)/(dx*dx+dy*dy), 0, 1)
        value = np.maximum(value, smooth(np.hypot(x-ax-u*dx, y-ay-u*dy), width))
    return value

def save(i, alpha, color, brightness=1):
    alpha = np.clip(alpha, 0, 1) * np.clip((.985-np.maximum(abs(x), abs(y)))*50, 0, 1)
    if np.ndim(color)==1:
        rgb=np.broadcast_to(np.array(color), (*x.shape, 3)).copy()
    else:
        rgb=color
    rgb=np.clip(rgb*np.asarray(brightness)[..., None], 0, 255)
    rgba=np.dstack([rgb, alpha*255]).astype(np.uint8)
    atlas.paste(Image.fromarray(rgba), ((i%4)*N, (i//4)*N))

for i, (dark, mid, light) in enumerate([
    ((2,8,28), (22,99,245), (164,237,255)),
    ((61,4,18), (247,34,62), (255,224,180)),
    ((19,4,40), (143,50,236), (246,202,255)),
    ((16,23,40), (115,154,206), (234,248,255)),
]):
    warp=.027*np.sin(a*5+r*18)+.022*np.sin(a*3-r*23)
    rim=smooth(r-.43-warp,.033)
    flowing=(.5+.5*np.sin(a*3-r*21+.6*np.sin(a*7+r*10)))**8
    veil=smooth(r-.55,.20)*(.12+.8*flowing)
    glare=smooth(r-.45,.11)
    strength=np.clip(rim*.85+veil*.65+glare*.12,0,1)
    rgb=np.array(dark)+(np.array(mid)-dark)*np.clip(r/.45,0,1)[...,None]
    rgb=rgb*(1-strength[...,None])+np.array(light)*strength[...,None]
    if i==1:
        hot=smooth(r,.30)
        rgb=rgb*(1-hot[...,None])+np.array((255,236,213))*hot[...,None]
    alpha=np.maximum(np.clip((.47-r)*30,0,1)*.98, np.clip(glare*.75+veil*.65+rim,0,1))
    save(i,alpha,rgb)

spiral=(.5+.5*np.cos(a*3+r*18))**12*smooth(r-.52,.23)
save(4,spiral,(228,243,255))
save(5,smooth(y,.025)*smooth(x,.65),(255,249,240))
save(6,smooth(r-(.65+.018*np.sin(a*7)),.025)*(.55+.45*np.cos(a*3)**2),(215,237,255))
save(7,smooth(y-(.2-.38*x*x),.022+.045*np.clip(1-abs(x),0,1))*np.clip((.95-abs(x))*5,0,1),(255,244,236))
flame=np.zeros_like(x)
for k in range(7):
    cx=(k-3)*.17
    flame=np.maximum(flame,smooth(x-cx-.08*np.sin(y*6+k),.055+.1*(y+1)/2)*smooth(y-.1-(k%3)*.12,.5))
save(8,flame,(255,222,168))
splat=smooth(r,.27)
for k in range(13):
    t=k*2.399
    splat=np.maximum(splat,stroke([(0,0),(.3*np.cos(t),.3*np.sin(t)),(.8*np.cos(t),.8*np.sin(t))],.011+.012*(k%3)))
save(9,splat,(222,180,183))
diamond=np.maximum(abs(x)*1.4+abs(y)*.9,0)
save(10,np.clip((.9-diamond)*18,0,1),np.stack([174+60*(x>0),213+30*(x>0),242+13*(x>0)],axis=-1),.65+.35*(x+y>0))
edge=.56+.065*np.sin(a*7)+.035*np.sin(a*13+r*9)
save(11,np.clip((edge-r)*12,0,1),np.stack([12+14*r,10+12*r,26+27*r],axis=-1))
bolt=stroke([(-.8,-.1),(-.4,.07),(-.19,-.22),(.1,.12),(.26,-.05),(.82,.16)],.012)
bolt=np.maximum(bolt,stroke([(-.18,-.2),(-.28,-.51),(-.06,-.61)],.009)*.7)
bolt=np.maximum(bolt,stroke([(.1,.12),(.19,.48),(.44,.60)],.009)*.7)
save(12,bolt,(232,245,255))
save(13,np.clip((.75-abs(x)*2.3-abs(y)) * 22,0,1),(240,235,218),.6+.4*(x>0))
helix=(smooth(x-.33*np.sin(y*7),.027)+smooth(x+.33*np.sin(y*7),.018))*smooth(y,.60)
save(14,helix,(207,255,231))
sparks=np.zeros_like(x)
for k in range(17):
    t=k*2.399;d=.25+.6*((k*7)%17)/17
    sparks=np.maximum(sparks,smooth(np.hypot(x-np.cos(t)*d,y-np.sin(t)*d),.012+(k%3)*.007))
save(15,sparks,(238,247,255))
# The right half is a seamless UV surface bank for true volume meshes. Unlike the
# left-hand glow sprites, these have no circular cutout or camera-facing silhouette.
u=(x+1)*.5;v=(y+1)*.5
for i in range(16):
    warp=u+.045*np.sin(v*np.pi*4+i*.7)+.022*np.sin(v*np.pi*8-u*np.pi*4)
    flow=(.5+.5*np.sin(warp*np.pi*(8+i%3*2)+np.sin(v*np.pi*4)))**6
    broad=.5+.5*np.sin(u*np.pi*4+np.sin(v*np.pi*2)*1.6)
    value=np.clip(150+55*broad+90*flow,0,255)
    if i in (7,10,13):value=np.clip(90+100*(.5+.5*np.sin(u*np.pi*8+v*np.pi*4))**12,0,240)
    if i==11:value=np.clip(30+55*broad+65*flow,0,180)
    rgba=np.dstack([value,value,value,np.full_like(value,255)]).astype(np.uint8)
    atlas.paste(Image.fromarray(rgba),((4+i%4)*N,(i//4)*N))
target=ROOT/'src/main/resources/assets/jjkcraft/textures/vfx/energy_materials.png'
target.parent.mkdir(parents=True,exist_ok=True)
atlas.save(target)
print(f'Wrote {target.name}: sixteen glow sprites and sixteen seamless volume surfaces')
