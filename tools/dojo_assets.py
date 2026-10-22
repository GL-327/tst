"""Reproducible original training props, recipes and crisp physical equipment art."""
from pathlib import Path
import json,math,hashlib
from PIL import Image,ImageDraw
import spectral_content as s
R=Path(__file__).resolve().parents[1];A=R/'src/main/resources/assets/jjkcraft';D=R/'src/main/resources/data/jjkcraft'
def save(path,value):
    if path.is_relative_to(A):
        generated=R/'src/main/generated/assets/jjkcraft'/path.relative_to(A)
        if generated.exists():path=generated
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
def image(path,im):path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
MATS=['straw','wood','cloth','red','stone','jade','dark','stone'];COLORS=[(189,160,106),(113,86,56),(52,63,81),(164,76,75),(199,195,173),(101,134,123),(52,49,57),(180,175,154)]
CUBES={'training_dummy':[(-5,0,-5,10,2,10,1),(-1,2,-1,2,23,2,1),(-5,12,-3,10,11,6,0),(-4,24,-4,8,7,8,0),(-11,17,-2,6,3,4,0),(5,17,-2,6,3,4,0),(-5,14,-3,10,2,7,3)],'training_mat':[(0,0,0,16,2,16,2),(1,2,1,14,1,14,3)],'focus_stone':[(2,0,2,12,6,12,6),(4,6,4,8,4,8,4)],'balance_beam':[(6,0,2,4,12,4,1),(6,0,10,4,12,4,1),(5,12,0,6,4,16,1)]}
def props():
    atlas=Image.new('RGBA',(128,16))
    for i,(name,color) in enumerate(zip(MATS,COLORS)):
        im=Image.new('RGBA',(16,16),color+(255,));d=ImageDraw.Draw(im);dark=tuple(int(v*.82) for v in color)+(255,);light=tuple(min(255,int(v*1.1)) for v in color)+(255,)
        for n in range(5):
            x=2+n*3;d.line((x,1,x,13-n%3),fill=dark);d.line((x+1,3,x+1,7),fill=light)
        if name in ('cloth','red'):d.rectangle((1,1,14,14),outline=dark);d.line((2,12,13,12),fill=light)
        atlas.paste(im,(i*16,0));image(A/'textures/block'/f'training_{name}.png',im)
    image(A/'textures/entity/training_dummy.png',atlas)
    bones=[{'name':'root','pivot':[0,0,0]},{'name':'body','parent':'root','pivot':[0,12,0],'cubes':[]}]
    for x,y,z,w,h,d,m in CUBES['training_dummy']:bones[1]['cubes'].append({'origin':[x,y,z],'size':[w,h,d],'uv':{f:{'uv':[m*16,0],'uv_size':[16,16]} for f in ('north','south','east','west','up','down')}})
    save(A/'geckolib/models/entity/training_dummy.geo.json',{'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.training_dummy','texture_width':128,'texture_height':16,'visible_bounds_width':4,'visible_bounds_height':4,'visible_bounds_offset':[0,1,0]},'bones':bones}]})
    save(A/'geckolib/animations/entity/training_dummy.animation.json',{'format_version':'1.8.0','animations':{'idle':{'loop':True,'animation_length':2,'bones':{'body':{'rotation':{'0':[0,0,-1],'1':[0,0,1],'2':[0,0,-1]}}}},'hit':{'loop':False,'animation_length':.4,'bones':{'body':{'rotation':{'0':[0,0,0],'.1':[12,0,-8],'.25':[-5,0,3],'.4':[0,0,0]}}}}}})
    for key,cubes in CUBES.items():
        if key=='training_dummy':continue
        elements=[]
        for x,y,z,w,h,d,m in cubes:elements.append({'from':[x,y,z],'to':[x+w,y+h,z+d],'faces':{f:{'uv':[0,0,16,16],'texture':'#m'+str(m)} for f in ('north','south','east','west','up','down')}})
        save(A/'models/block'/f'{key}.json',{'textures':dict(particle='jjkcraft:block/training_wood',**{'m'+str(i):'jjkcraft:block/training_'+m for i,m in enumerate(MATS)}),'elements':elements})
        save(A/'blockstates'/f'{key}.json',{'variants':{'':{'model':'jjkcraft:block/'+key}}});save(A/'models/item'/f'{key}.json',{'parent':'jjkcraft:block/'+key});save(A/'items'/f'{key}.json',{'model':{'type':'minecraft:model','model':'jjkcraft:item/'+key}})
        save(D/'loot_table/blocks'/f'{key}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'jjkcraft:'+key}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    icon=Image.new('RGBA',(32,32));d=ImageDraw.Draw(icon);d.rectangle((14,10,17,29),fill=COLORS[1]);d.rectangle((10,11,21,22),fill=COLORS[0]);d.rectangle((11,2,20,10),fill=COLORS[0]);d.rectangle((4,14,27,17),fill=COLORS[0]);d.rectangle((10,19,21,21),fill=COLORS[3]);d.rectangle((8,28,24,30),fill=COLORS[1]);d.line((13,4,14,7),fill=COLORS[1]);d.line((17,4,18,7),fill=COLORS[1]);image(A/'textures/item/training_straw_doll.png',icon)
    save(A/'models/item/training_straw_doll.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jjkcraft:item/training_straw_doll'}});save(A/'items/training_straw_doll.json',{'model':{'type':'minecraft:model','model':'jjkcraft:item/training_straw_doll'}})
    recipes={'training_straw_doll':([' H ','SHS',' P '],{'H':'minecraft:hay_block','S':'minecraft:stick','P':'minecraft:oak_planks'}),'training_mat':(['WWW','HHH'],{'W':'minecraft:red_wool','H':'minecraft:wheat'}),'focus_stone':([' A ','SSS'],{'A':'minecraft:amethyst_shard','S':'minecraft:stone'}),'balance_beam':(['PPP',' S '],{'P':'minecraft:oak_planks','S':'minecraft:stick'})}
    for name,(pattern,keys) in recipes.items():save(D/'recipe'/f'{name}.json',{'type':'minecraft:crafting_shaped','pattern':pattern,'key':keys,'result':{'id':'jjkcraft:'+name,'count':2 if name=='balance_beam' else 1}})
    p=R/'src/main/generated/assets/jjkcraft/lang/en_us.json';lang=json.loads(p.read_text(encoding='utf-8'));lang.update({'key.jjkcraft.design_studio':'Open visual design studio','item.jjkcraft.training_straw_doll':'Training Straw Doll','entity.jjkcraft.training_dummy':'Training Straw Doll','block.jjkcraft.training_mat':'Breathing Mat','block.jjkcraft.focus_stone':'Reverse-flow Focus Stone','block.jjkcraft.balance_beam':'Balance Beam'});p.write_text(json.dumps(lang,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def equipment():
    definitions={}
    for base in (R/'src/main/generated/data/jjkcraft/jjkcraft/cursed_tool',D/'jjkcraft/cursed_tool'):
        if base.exists():
            for p in base.glob('*.json'):
                d=json.loads(p.read_text(encoding='utf-8'))
                if d.get('kind')=='weapon':definitions[d['item'].split(':')[1]]=d
    for name in definitions:
        hue=int(hashlib.sha256(name.encode()).hexdigest()[:6],16);accent=((hue>>16&127)+80,(hue>>8&127)+65,(hue&127)+65);metal=(184,198,198);outline=(31,32,39)
        im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
        if any(k in name for k in ('staff','cloud','chain')):
            d.line((6,27,25,5),fill=outline,width=5);d.line((6,27,25,5),fill=accent,width=3)
            for x,y in [(8,24),(15,16),(22,8)]:d.line((x-2,y-1,x+2,y+2),fill=metal,width=2)
        else:
            d.line((5,28,25,7),fill=outline,width=5);d.line((5,28,12,21),fill=accent,width=3);d.line((12,21,26,4),fill=metal,width=3);d.line((14,20,27,3),fill=(233,235,219),width=1);d.line((9,17,16,24),fill=accent,width=3)
            if any(k in name for k in ('cleaver','bone','spear')):d.polygon([(14,18),(24,5),(29,4),(24,14),(17,21)],fill=metal,outline=outline)
            for x,y in [(6,26),(8,24),(10,22)]:d.point((x,y),fill=(222,201,159))
        image(A/'textures/item'/f'{name}.png',im)
        # Inventory silhouette and held material atlas are separate assets.
        atlas=Image.new('RGBA',(16,16),metal+(255,));ad=ImageDraw.Draw(atlas);ad.rectangle((0,11,15,15),fill=accent+(255,));ad.rectangle((0,10,15,11),fill=(202,172,105));ad.line((8,0,8,9),fill=(232,237,223));ad.line((10,0,10,9),fill=(106,125,139));
        image(A/'textures/item'/f'{name}_material.png',atlas)
        elements=s.weapon_model(name)
        if any(k in name for k in ('katana','blade','sword','wakizashi','tanto')):
            elements=[]
            for a,b,uv in [([7.5,-5,7.4],[8.5,3.5,8.6],[7,12,9,16]),([5.4,3.4,6.8],[10.6,4.2,9.2],[5,10,11,12]),([7.4,4.2,7.7],[8.6,17,8.3],[7,0,9,10]),([7.8,17,7.7],[8.8,23,8.3],[7,0,9,10])]:elements.append({'from':a,'to':b,'faces':{f:{'uv':uv,'texture':'#layer0'} for f in ('north','south','east','west','up','down')}})
        save(A/'models/item'/f'{name}_held.json',{'textures':{'layer0':'jjkcraft:item/'+name+'_material','particle':'jjkcraft:item/'+name+'_material'},'elements':elements,'display':{'thirdperson_righthand':{'rotation':[0,-90,55],'translation':[0,4,0],'scale':[.75,.75,.75]},'thirdperson_lefthand':{'rotation':[0,90,-55],'translation':[0,4,0],'scale':[.75,.75,.75]},'firstperson_righthand':{'rotation':[0,-90,20],'translation':[1,3,0],'scale':[.65,.65,.65]},'firstperson_lefthand':{'rotation':[0,90,-20],'translation':[1,3,0],'scale':[.65,.65,.65]}}})
        save(A/'models/item'/f'{name}_icon.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jjkcraft:item/'+name}})
        save(A/'items'/f'{name}.json',{'model':{'type':'minecraft:select','property':'minecraft:display_context','cases':[{'when':['gui','ground','fixed'],'model':{'type':'minecraft:model','model':'jjkcraft:item/'+name+'_icon'}}],'fallback':{'type':'minecraft:model','model':'jjkcraft:item/'+name+'_held'}}})
    # Physical loot, armour icons and relics retain their silhouettes at a consistent crisp pixel density.
    for p in (A/'textures/item').glob('*.png'):
        if p.stem in definitions or p.stem.endswith('_material'):continue
        im=Image.open(p).convert('RGBA')
        if im.width>=64:image(p,im.resize((32,32),Image.Resampling.NEAREST))
    for p in (A/'textures/entity/equipment').rglob('*.png'):
        im=Image.open(p).convert('RGBA');im=im.resize((64,32),Image.Resampling.NEAREST);image(p,im)
    print('Rebuilt',len(definitions),'held weapons and physical pixel textures')
if __name__=='__main__':
    props();equipment()
    s.SOURCES={k:v for k,v in s.SOURCES.items() if k in s.DESIGNS};s.creatures()
