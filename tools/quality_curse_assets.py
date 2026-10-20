"""Bake clean material planes from the repo's editable box-UV art recipes.
Keeps all existing model UVs, silhouettes and facial features. No downloaded art.
"""
from pathlib import Path
import ast,json,random,io,os
from PIL import Image,ImageDraw
import gen_curses as art
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/jjkcraft'
DATA=ROOT/'src/main/resources/data/jjkcraft/jjkcraft/curse'
def save(image,path):
    buffer=io.BytesIO();image.save(buffer,format='PNG')
    temp=path.with_suffix('.quality-tmp');temp.write_bytes(buffer.getvalue());os.replace(temp,path)
metadata=ast.parse((ROOT/'tools/expand_content.py').read_text())
extra=next(ast.literal_eval(n.value) for n in metadata.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='NEW_CURSES' for t in n.targets))
recipes={f.__name__:(f.__name__,None) for f in art.CURSES}
for ident,title,source,scale,accent in extra:
    recipes[ident]=(source,json.loads((DATA/(ident+'.json')).read_text())['codex']['color'])
for ident,(source,tint) in recipes.items():
    model,_=getattr(art,source)()
    if tint:
        hue=tuple(int(tint[n:n+2],16) for n in (1,3,5)) if isinstance(tint,str) else tuple(tint)
        model.palette={k:(base,hue) for k,(base,accent) in model.palette.items()}
    image=model.texture(sum(map(ord,ident)))
    if ident!=source:
        image=image.resize((256,256),Image.Resampling.NEAREST)
        ImageDraw.Draw(image).rectangle((240,230,255,250),fill=(hue if tint else (190,180,160))+(255,))
    definition=json.loads((DATA/(ident+'.json')).read_text());color=tuple(int(definition['codex']['color'][n:n+2],16) for n in (1,3,5))
    path=ASSETS/definition['model']['texture'].split(':')[1]
    glow=Image.new('RGBA',image.size)
    for y in range(image.height):
        for x in range(image.width):
            r,g,b,a=image.getpixel((x,y))
            if a and max(r,g,b)-min(r,g,b)>65 and max(r,g,b)>100:glow.putpixel((x,y),color+(190,))
    save(image,path);save(glow,path.with_name(ident+'_glow.png'))
    for state,marks in [('wounded',3),('critical',7)]:
        # Render damage over the same authored atlas, clipped to opaque UV islands.
        damaged=image.copy();draw=ImageDraw.Draw(damaged);rng=random.Random(ident+state)
        opaque=[(x,y) for y in range(image.height) for x in range(image.width) if image.getpixel((x,y))[3]>0]
        for _ in range(marks):
            x,y=rng.choice(opaque)
            for n in range(5 if state=='wounded' else 9):
                xx=min(image.width-1,x+n//3);yy=min(image.height-1,y+n)
                if image.getpixel((xx,yy))[3]:draw.point((xx,yy),fill=color+(255,))
        save(damaged,path.with_name(ident+'_'+state+'.png'))
    print(ident)
