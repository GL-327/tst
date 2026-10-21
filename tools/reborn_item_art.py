"""Original high-resolution item art, separate weapon atlases, woven armor and technique emblems."""
from pathlib import Path
import hashlib,json,math,os,random,time
from PIL import Image,ImageDraw,ImageFilter
from original_families import FAMILIES
from reborn_creatures import DESIGNS,rgb
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/jjkcraft'

def seed(n):return int(hashlib.sha256(n.encode()).hexdigest()[:10],16)
def replace(source,destination):
    for attempt in range(15):
        try:os.replace(source,destination);return
        except PermissionError:
            if attempt==14:raise
            time.sleep(.15)
def save(p,d):
    t=p.with_suffix('.writing');t.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8',newline='\n');replace(t,p)
def png(p,im):
    p.parent.mkdir(parents=True,exist_ok=True);t=p.with_suffix('.writing');im.save(t,format='PNG');replace(t,p)
def shade(c,f):return tuple(max(0,min(255,int(x*f))) for x in c)+(255,)
def poly(d,points,color,edge=(20,18,30,255),width=2):d.polygon(points,fill=color,outline=edge,width=width)
def grain(im,name):
    rng=random.Random(seed(name));pix=im.load()
    for y in range(im.height):
        for x in range(im.width):
            r,g,b,a=pix[x,y]
            if a:
                n=rng.randrange(-7,8);f=.88+.16*(1-y/im.height);pix[x,y]=(max(0,min(255,int(r*f)+n)),max(0,min(255,int(g*f)+n)),max(0,min(255,int(b*f)+n)),a)
    return im
WEAPONS={'inverted_spear_of_heaven','split_soul_katana','chain_of_a_thousand_miles','arsenal_blade','bonewood_staff','debt_cleaver','playful_cloud','dragon_bone','stormbell_spear','ember_wakizashi','sealbreaker_tanto','rumelian_blade','honoyagi'}
SETS={'sorcerer_uniform':((24,34,64),(193,61,76)),'binding_vestments':((66,43,77),(224,193,132)),'heavenly_hunter':((36,57,49),(183,159,91))}

def icon(name):
    im=Image.new('RGBA',(128,128));d=ImageDraw.Draw(im);rng=random.Random(seed(name));accent=rgb(0x8570B7+(seed(name)%45)*0x010101);metal=(204,220,226,255);dark=(34,29,43,255)
    if name.endswith('_spawn_egg'):
        key=name[:-10];base,light=(rgb(DESIGNS[key][1]),rgb(DESIGNS[key][2])) if key in DESIGNS else ((74,59,97),(181,142,224));d.ellipse((25,7,103,121),fill=shade(base,1),outline=dark,width=4)
        for i in range(14):x=rng.randrange(35,93);y=rng.randrange(20,108);d.ellipse((x-4,y-5,x+4,y+5),fill=shade(light,.8))
        d.arc((32,14,96,114),110,255,fill=shade(base,1.5),width=3);poly(d,[(47,56),(64,40),(83,56),(77,83),(51,83)],shade(base,.5));d.line((55,62,60,66,71,62,76,66),fill=shade(light,1.1),width=3)
    elif any(name.startswith(s+'_') for s in SETS):
        family=next(s for s in SETS if name.startswith(s+'_'));base,accent=SETS[family];part=name[len(family)+1:]
        if part=='helmet':poly(d,[(29,21),(45,9),(84,9),(103,25),(105,90),(89,106),(85,42),(43,42),(39,106),(23,92)],shade(base,1.15));d.line((40,37,89,37),fill=shade(accent,1),width=5)
        elif part=='chestplate':poly(d,[(13,27),(39,10),(48,24),(79,24),(89,10),(115,27),(107,58),(93,53),(93,116),(35,116),(35,53),(21,58)],shade(base,1.15));d.line((40,34,64,66,88,34),fill=shade(accent,1),width=6);d.rectangle((35,87,93,98),fill=shade(accent,.8))
        elif part=='leggings':poly(d,[(31,12),(97,12),(99,115),(72,115),(67,59),(59,59),(55,115),(29,115)],shade(base,1));d.rectangle((31,16,96,28),fill=shade(accent,1));d.line((64,30,64,54),fill=shade(base,.5),width=4)
        else:
            for x in (22,70):poly(d,[(x,20),(x+29,20),(x+30,84),(x+39,99),(x+38,114),(x-5,114),(x-7,101)],shade(base,1));d.rectangle((x,28,x+28,35),fill=shade(accent,1))
        for y in range(44,110,13):d.line((42,y,48,y+3),fill=shade(accent,.8),width=2)
    elif name in WEAPONS:
        if name=='playful_cloud':
            for x,y in ((32,55),(61,30),(88,8)):
                d.line((x,y,x+11,y+49),fill=dark,width=13);d.line((x,y,x+11,y+49),fill=(151,43,56,255),width=8)
                for t in (3,39):d.line((x+t/5-3,y+t,x+t/5+6,y+t-1),fill=(221,185,118,255),width=4)
            d.line((42,58,62,77,83,51,96,56),fill=(195,201,205,255),width=3)
        elif name=='chain_of_a_thousand_miles':
            for i in range(11):x=34+math.sin(i*.9)*22;y=11+i*9;d.ellipse((x,y,x+22,y+14),outline=metal,width=5);d.ellipse((x+3,y+3,x+19,y+11),outline=dark,width=1)
        else:
            blade=[(42,83),(81,17),(98,7),(97,29),(56,91)]
            if name in ('dragon_bone','debt_cleaver'):blade=[(33,79),(69,14),(100,9),(104,35),(56,95)]
            if name=='inverted_spear_of_heaven':blade=[(45,87),(77,20),(79,7),(86,18),(103,9),(97,30),(88,33),(59,94)]
            if name=='stormbell_spear':blade=[(45,92),(81,19),(80,7),(96,17),(101,36),(88,33),(56,99)]
            if name=='sealbreaker_tanto':blade=[(42,82),(74,35),(95,23),(88,47),(57,92)]
            poly(d,blade,metal,width=3);d.line((48,82,87,20),fill=(249,252,240,255),width=3);d.line((39,86,19,118),fill=dark,width=13);d.line((39,86,19,118),fill=(105,53,67,255),width=8)
            for j in range(5):d.line((23+j*4,109-j*6,31+j*4,113-j*6),fill=(211,178,126,255),width=2)
            d.line((29,77,62,99),fill=(183,145,89,255),width=7)
            if name=='ember_wakizashi':d.line((54,78,88,24),fill=(236,120,52,255),width=4)
            if name=='dragon_bone':
                for j in range(5):poly(d,[(52+j*7,66-j*9),(49+j*7,51-j*9),(60+j*7,55-j*9)],(144,174,169,255),width=1)
            if name=='stormbell_spear':d.ellipse((36,66,58,90),fill=(131,96,155,255),outline=(225,203,151,255),width=3);d.rectangle((42,73,53,82),fill=dark)
    elif name in ('divine_chalice','drifting_gourd') or 'vessel' in name:
        if name=='divine_chalice':poly(d,[(25,17),(103,17),(96,58),(76,73),(73,99),(94,109),(94,118),(34,118),(34,109),(55,99),(52,73),(32,58)],(188,147,71,255));d.ellipse((27,11,101,31),fill=(78,48,90,255),outline=(242,210,122,255),width=4);d.ellipse((51,40,77,66),fill=(121,69,174,255),outline=(245,209,125,255),width=4)
        else:d.ellipse((30,11,98,65),fill=(141,90,51,255),outline=dark,width=4);d.ellipse((15,49,112,119),fill=(167,109,59,255),outline=dark,width=4);d.line((23,63,104,63),fill=(206,171,108,255),width=6)
    elif any(t in name for t in ('scroll','charm','seal','ledger','tablet','doll')):
        poly(d,[(27,13),(99,17),(104,114),(20,110)],(206,190,153,255));d.line((29,20,93,24),fill=(122,83,65,255),width=5)
        for i in range(5):x=39+(i%2)*18;y=37+i*13;d.line((x,y,x+16,y+3,x+10,y+10),fill=(67,52,66,255),width=3)
        d.line((21,77,104,70),fill=(146,43,59,255),width=9);d.ellipse((55,67,75,87),fill=(181,65,74,255),outline=dark,width=2)
    elif any(t in name for t in ('finger','digit','object','soul')):
        poly(d,[(45,112),(33,75),(39,28),(50,10),(72,14),(83,53),(89,107),(72,120)],(146,102,108,255));d.line((47,23,69,23),fill=(65,45,62,255),width=5)
        for y in (43,64,86):d.arc((40,y-10,83,y+9),10,180,fill=(70,42,62,255),width=3)
        d.ellipse((51,57,74,82),fill=(98,55,131,255),outline=(201,147,203,255),width=3)
    elif any(t in name for t in ('shard','crystal','pearl','mul','token','coin')):
        c=(110,89,191) if 'cursed' in name else (83,164,160) if 'mul' in name else accent
        pts=[(64,7),(102,44),(91,102),(55,121),(24,87),(29,35)];poly(d,pts,shade(c,1.05));poly(d,[(64,7),(62,65),(29,35)],shade(c,1.45),width=1);poly(d,[(62,65),(102,44),(91,102),(55,121)],shade(c,.8),width=1);d.line((34,40,61,17),fill=shade(c,1.65),width=3)
    elif any(t in name for t in ('horn','tooth','marrow','splinter','bone')):
        poly(d,[(30,112),(28,83),(58,53),(77,13),(98,7),(88,61),(68,100)],(204,195,157,255));d.line((39,100,69,57,85,22),fill=(245,226,179,255),width=4)
        for y in (68,83,98):d.line((36,y,65,y-9),fill=(129,115,97,255),width=2)
    else:
        # Trophy silhouettes vary deterministically in topology, not just tint.
        n=5+seed(name)%4;pts=[]
        for i in range(n*2):a=i*math.pi/n;r=47 if i%2==0 else 24+rng.randrange(8);pts.append((64+math.cos(a)*r,64+math.sin(a)*r))
        poly(d,pts,shade(accent,.75));d.ellipse((40,37,88,92),fill=shade(accent,1.1),outline=dark,width=3);d.line((48,65,62,48,76,65,62,82,48,65),fill=(225,208,164,255),width=4)
    return grain(im,name)

def armor():
    for family,(base,accent) in SETS.items():
        for layer in ('humanoid','humanoid_leggings'):
            im=Image.new('RGBA',(256,128),shade(base,1));d=ImageDraw.Draw(im);rng=random.Random(seed(family+layer))
            for y in range(128):
                for x in range(256):
                    f=.82+.16*math.sin((x%32)/32*math.pi)+.06*(y%4==0)+rng.random()*.05;im.putpixel((x,y),shade(base,f))
            for x,y,w,h in [(80,80,32,48),(128,80,32,48),(176,80,16,48),(16,80,16,48),(32,32,32,32)]:
                d.rectangle((x+1,y+1,x+w-2,y+h-2),outline=shade(base,1.6),width=1)
                for yy in range(y+3,y+h-3,5):d.line((x+2,yy,x+4,yy+1),fill=shade(accent,.75),width=1);d.line((x+w-5,yy,x+w-3,yy+1),fill=shade(accent,.75),width=1)
                if family=='binding_vestments':
                    for yy in range(y+4,y+h,9):d.line((x+2,yy,x+w-3,yy-3),fill=(187,177,154,255),width=3)
                elif family=='heavenly_hunter':
                    for yy in range(y+4,y+h,12):d.rounded_rectangle((x+2,yy,x+w-3,min(y+h-2,yy+9)),radius=2,fill=shade(base,1.3),outline=shade(accent,.65),width=1)
            d.rectangle((35,35,60,63),fill=(0,0,0,0));d.line((82,82,96,103,109,82),fill=shade(accent,1),width=3);d.rectangle((81,109,110,117),fill=shade(accent,.75));d.line((96,84,96,109),fill=shade(base,.5),width=2)
            for yy in (88,96,104):d.ellipse((98,yy,100,yy+2),fill=(222,197,128,255))
            d.line((84,91,88,86,91,91,88,97,84,91),fill=shade(accent,1.2),width=1)
            png(A/'textures/entity/equipment'/layer/f'{family}.png',im)

def techniques():
    family_colors={k:c for k,_,c,_,_ in FAMILIES}
    for path in (ROOT/'src/main/resources/data/jjkcraft/jjkcraft/technique').glob('*.json'):
        data=json.loads(path.read_text());name=path.stem;color=data.get('visuals',{}).get('color','#9576D1');color=int(color.lstrip('#'),16) if isinstance(color,str) else color;c=rgb(color);im=Image.new('RGBA',(128,128));d=ImageDraw.Draw(im);rng=random.Random(seed(name))
        d.rounded_rectangle((3,3,125,125),radius=19,fill=(20,21,36,248),outline=shade(c,.65),width=3)
        for rad in range(47,4,-3):d.ellipse((64-rad,64-rad,64+rad,64+rad),fill=shade(c,.12+.0018*(47-rad)))
        family=next((k for k in family_colors if name.startswith(k+'_')),None);idx=seed(family or name);corners=3+idx%5;pts=[(64+math.cos(i*math.tau/corners-math.pi/2)*39,64+math.sin(i*math.tau/corners-math.pi/2)*39) for i in range(corners)]
        d.line(pts+[pts[0]],fill=shade(c,1.1),width=3)
        for i in range(3+idx%4):a=i*2.399+idx%20;d.line((64,64,64+math.cos(a)*31,64+math.sin(a)*31),fill=shade(c,.7),width=3)
        category=data.get('category','utility');op=name.rsplit('_',1)[-1]
        if op=='guard' or category=='passive':poly(d,[(44,45),(64,37),(85,45),(81,75),(64,91),(48,75)],shade(c,.85),shade(c,1.35),3)
        elif category in ('projectile','strike') or op in ('salvo','mark'):d.line((39,88,84,41),fill=shade(c,1.4),width=8);poly(d,[(83,34),(91,62),(68,42)],shade(c,1.4),shade(c,1.6),2)
        elif op=='step':
            for i in range(3):d.line((39+i*15,46,53+i*15,64,39+i*15,82),fill=shade(c,1.1+i*.15),width=5)
        elif category=='healing':d.line((64,44,64,85),fill=shade(c,1.35),width=9);d.line((44,64,85,64),fill=shade(c,1.35),width=9)
        else:d.ellipse((48,48,80,80),fill=shade(c,.6),outline=shade(c,1.4),width=4);d.line((54,64,64,53,75,64,64,76,54,64),fill=(238,227,197,255),width=2)
        # Individual constellations differentiate moves within the same family.
        for i in range(3):x=15+i*10;y=108;d.ellipse((x,y,x+4,y+4),fill=shade(c,1.2 if (seed(name)>>i)&1 else .35))
        png(A/'textures/gui/technique'/f'{name}.png',im)

def main():
    for p in (A/'textures/item').glob('*.png'):png(p,icon(p.stem))
    for key in WEAPONS:
        model=A/'models/item'/f'{key}.json';item=A/'items'/f'{key}.json'
        if not model.is_file() or not item.is_file():continue
        old=json.loads(model.read_text());save(A/'models/item'/f'{key}_gui.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jjkcraft:item/'+key}})
        if old.get('elements'):
            atlas=Image.new('RGBA',(64,64),(112,123,135,255));dr=ImageDraw.Draw(atlas)
            for x in range(64):dr.line((x,0,x,63),fill=(min(245,125+x*2),min(248,133+x*2),min(250,140+x*2),255))
            metal={'dragon_bone':(150,180,177),'stormbell_spear':(152,129,183),'ember_wakizashi':(82,69,75),'sealbreaker_tanto':(158,202,192)}.get(key,(198,216,221))
            dr.rectangle((28,0,36,39),fill=metal+(255,));dr.line((30,0,30,40),fill=(246,249,244,255) if key!='ember_wakizashi' else (239,126,58,255),width=2);dr.rectangle((20,40,44,48),fill=(195,157,101,255));dr.rectangle((28,48,36,63),fill=(98,47,63,255))
            for y in range(49,63,3):dr.line((28,y,36,y+2),fill=(216,185,136,255),width=1)
            if key=='playful_cloud':dr.rectangle((28,0,36,63),fill=(151,42,54,255));dr.rectangle((20,40,44,48),fill=(195,157,101,255))
            png(A/'textures/item/materials'/f'{key}.png',atlas);old['textures']['layer0']='jjkcraft:item/materials/'+key;save(model,old)
            save(item,{'model':{'type':'minecraft:select','property':'minecraft:display_context','cases':[{'when':['gui','ground','fixed','on_shelf'],'model':{'type':'minecraft:model','model':'jjkcraft:item/'+key+'_gui'}}],'fallback':{'type':'minecraft:model','model':'jjkcraft:item/'+key}}})
    armor();techniques();print('Repainted every item, all armor atlases and all technique emblems; weapon icons use separate in-hand material atlases.')

if __name__=='__main__':main()
