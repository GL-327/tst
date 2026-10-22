"""Original 32px inventory sprite for the preserved, wax-sealed Sukuna relic."""
from pathlib import Path
from PIL import Image, ImageDraw
root=Path(__file__).resolve().parents[1]
out=root/'src/main/resources/assets/jjkcraft/textures/item/sukunas_finger.png'
out.parent.mkdir(parents=True,exist_ok=True)
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
outline='#271c2a';dark='#70414c';skin='#b77e6c';light='#dbad8c'
d.polygon([(9,27),(8,23),(10,19),(12,17),(13,12),(16,9),(17,5),(20,3),(23,4),(24,7),(22,12),(20,14),(19,19),(16,22),(15,27),(12,29)],fill=outline)
d.polygon([(10,26),(10,23),(13,19),(14,13),(17,11),(18,6),(21,5),(22,6),(21,11),(18,15),(17,20),(14,23),(13,27)],fill=skin)
d.polygon([(10,23),(13,20),(14,14),(16,13),(16,19),(13,24),(12,27),(10,26)],fill=dark)
d.line([(14,20),(16,17),(17,13),(20,10)],fill=light,width=1)
d.polygon([(18,7),(19,4),(22,4),(23,6),(22,8),(20,9)],fill='#432c42')
d.line([(20,5),(22,5)],fill='#be8190',width=1)
for coords in [[(13,15),(16,16)],[(12,18),(16,19)],[(10,23),(14,24)]]:d.line(coords,fill='#3c2636',width=2)
d.polygon([(8,25),(11,24),(15,26),(15,29),(12,30),(8,28)],fill='#641c39')
d.polygon([(9,26),(11,25),(14,27),(12,29),(9,28)],fill='#ac3b4e')
d.point((11,26),fill='#e49c78');d.line([(19,11),(21,12)],fill='#49273d')
im.save(out)
