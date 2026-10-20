"""Draw dedicated arsenal glyphs using the repository's original vector-to-raster icon recipes."""
from pathlib import Path
import json,io,os
from PIL import ImageDraw
import gen_textures as ink
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data/jjkcraft/jjkcraft'
ASSETS=ROOT/'src/main/resources/assets/jjkcraft'
def generate():
 families=['electrification','granite_discharge','love_rendezvous','explosive_flesh','tool_manipulation','echo_ledger','mercury_mirror','mourning_thread','modulo_harmony_chaos','modulo_light_darkness']
 glyphs=[[ink.g_line,ink.g_strike,ink.g_fan],[ink.g_bolt,ink.g_fan,ink.g_strike],[ink.g_star,ink.g_attract,ink.g_repel],
  [ink.g_flame,ink.g_strike,ink.g_quake],[ink.g_crescent,ink.g_step,ink.g_fan],[ink.g_clock,ink.g_oath,ink.g_step],
  [ink.g_shape,ink.g_repel,ink.g_fan],[ink.g_doll,ink.g_line,ink.g_chain],[ink.g_step,ink.g_repel,ink.g_attract,ink.g_line],
  [ink.g_bolt,ink.g_attract,ink.g_fan,ink.g_quake]]
 count=0
 for family,shapes in zip(families,glyphs):
  ids=json.loads((DATA/'innate_family'/f'{family}.json').read_text(encoding='utf-8'))['techniques']
  for n,id in enumerate(ids):
   key=id.split(':')[1];p=DATA/'technique'/f'{key}.json';d=json.loads(p.read_text(encoding='utf-8'));color=ink.hexrgb(d['visuals']['color']);im=ink.icon_base(color);draw=ImageDraw.Draw(im)
   shapes[n%len(shapes)](draw,color)
   # The rank ticks distinguish related moves without relying only on color.
   for dot in range(n+1):draw.ellipse((39+dot*14,106,45+dot*14,112),fill=color+(255,))
   buffer=io.BytesIO();ink.finish(im,32).save(buffer,format='PNG');target=ASSETS/'textures/gui/technique'/f'{key}.png';temp=target.with_suffix('.tmp');temp.write_bytes(buffer.getvalue());os.replace(temp,target)
   d['icon']='jjkcraft:textures/gui/technique/'+key+'.png';temp=p.with_suffix('.tmp');temp.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8',newline='\n');os.replace(temp,p);count+=1
 print('Generated',count,'dedicated technique icons.')
if __name__=='__main__':generate()
