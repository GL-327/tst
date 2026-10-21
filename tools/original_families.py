"""Twenty original six-move families, with primers, defenses, movement, pressure, control and payoffs."""
import json,os
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];D=ROOT/'src/main/resources/data/jjkcraft/jjkcraft';L=ROOT/'src/main/generated/assets/jjkcraft/lang/en_us.json'
FAMILIES=[
('glass','Shatterglass',0x8FDAD9,'Glass marks crack into additional CE-draining shards when shattered.','Splinter Needle|Prism Mantle|Refraction Step|Shard Squall|Tessellation Cage|Catastrophic Shatter'),
('salt','Salt Covenant',0xD2E9CB,'Salt marks drain cursed energy; the mantle cleanses poison.','Encrust|Purifying Circle|Brine Slip|Crystal Hail|Desiccation Brand|Salt Burial'),
('lodestone','Lodestone',0x91A4C1,'Polarity draws enemies inward. Worn armor strengthens magnetic strikes; the guard repels attackers.','Iron Filing|Polarity Shell|Rail Step|Repulsion Wave|Magnetic Pin|Orbit Collapse'),
('momentum','Stolen Momentum',0xF3C478,'Movement builds resonance; strikes convert velocity to extra knockback.','Impulse Tap|Kinetic Bracing|Vector Vault|Ricochet Strike|Velocity Clamp|Terminal Velocity'),
('suture','Suture Line',0xD78FAD,'Threads pull marked targets into the caster and tighten into a short binding.','Needle Stitch|Patchwork Guard|Tether Step|Thread Flurry|Severance Knot|Closed Circuit'),
('origami','Cursed Origami',0xDCCAA8,'The paper mantle calls an owned heron; seals reveal opponents before the folding payoff.','Fold Seal|Paper Mantle|Crane Hop|Razor Flock|Folded Cage|Thousandfold Collapse'),
('wax','Funeral Wax',0xE8B46B,'Sticky wax slows and ignites. Candle Coat protects from fire while guarding.','Drip Brand|Candle Coat|Wick Step|Molten Fan|Tallow Prison|Funeral Candle'),
('rust','Red Oxide',0xC98262,'Corrosive pressure weakens foes and gains a small bonus against worn armor.','Oxide Brand|Patina Guard|Abrasive Dash|Corrosion Burst|Armor Snag|Red Ruin'),
('kintsugi','Kintsugi',0xE6CA72,'Suffering damage builds fractures. Gold Mending spends stored fractures on bounded healing.','Fracture Seal|Gold Mending|Ceramic Step|Seam Strike|Golden Join|Restoration Cascade'),
('tollbell','Tolling Bell',0xC9B8E7,'A guarded hit weakens its attacker. A two-layer Funeral Brand interrupts active techniques.','First Chime|Stillness Bell|Echo Hop|Peal Wave|Funeral Brand|Grand Toll'),
('ink','Ink Testament',0x8BA8DB,'Ink stains briefly obscure sight. The veil conceals the caster through nearby blindness.','Sigil Stamp|Inky Veil|Brush Step|Ink Lance|Blot Seal|Black Page'),
('stencil','Stencil Gates',0x80CFC9,'Cutout marks pull victims toward the caster; routes cannot pass through terrain.','Template Mark|Paper Frame|Doorway Step|Cutout Flurry|Frame Anchor|Negative Volume'),
('pressure','Pressure Weave',0xA3D9E7,'Vacuum strokes pull targets inward; Jet Step crosses a collision-checked path.','Pressure Tag|Air Mantle|Jet Step|Vacuum Cut|Pressure Pocket|Implosion'),
('orbit','Orbital Mandala',0xC4A3E6,'Orbital strikes lift their target briefly; marks prepare the aligned final impact.','Satellite Brand|Orbital Guard|Slingshot|Comet Salvo|Gravity Tether|Syzygy'),
('thorn','Briar Testament',0xB2CF91,'Poisoned thorns punish proximity; a paid guard reflects a bounded fraction of damage.','Sprout Brand|Briar Mantle|Vine Spring|Thorn Volley|Root Prison|Funeral Garden'),
('carrion','Carrion Choir',0x9CAAD6,'Feathers reveal enemies. Wing Cloak summons a real owned heron to harry them.','Feather Brand|Wing Cloak|Raven Step|Beak Squall|Murder Circle|Sky Burial'),
('porcelain','Porcelain Husk',0xDFDAD0,'Glaze weakens its victim, while guarded plates absorb part of three incoming hits.','Glaze|China Plate|Shard Step|Ceramic Salvo|Kiln Seal|White Collapse'),
('echo_memory','Afterimage Archive',0xA2D0D4,'Successful strokes replay a smaller delayed hit. Rewind Step returns to the previous anchor within range.','Trace|Second Skin|Rewind Step|Recorded Strike|Repeat Mark|Last Refrain'),
('sand','Hourglass Dune',0xD1BA87,'Abrasive grains briefly blind opponents; the dune mantle hides the caster in close quarters.','Hourglass Grain|Dune Wall|Mirage Step|Abrasion|Quicksand|Timeless Dune'),
('resin','Amber Testament',0xE7AF64,'Sap slows opponents; amber guarding grants movement speed without invulnerability.','Sap Mark|Amber Guard|Skate Stride|Amber Lance|Amber Seal|Fossil Burst')]

def save(p,d):
    t=p.with_suffix('.writing');t.write_text(json.dumps(d,indent=2,ensure_ascii=False)+'\n',encoding='utf-8',newline='\n');os.replace(t,p)

def main():
    lang=json.loads(L.read_text(encoding='utf-8'));guide=['# Original technique families -- 1.12.0-preview','','Twenty original families add 120 moves. Each has a grade-4 primer and defense, a grade-3 movement skill, two grade-2 pressure/control skills and a grade-1 payoff. They use actual damage, status, summons and collision-checked movement.','','Primers stack up to three marks per enemy for eight seconds and build resonance up to eight. Finishers consume that family marks and resonance. Other players do not share the marks. Guards affect three hits, last a bounded time and spend CE per hit; they cannot grant immunity. Movement vows and healing vows apply.','']
    ops=['mark','guard','step','salvo','snare','finish'];grades=['grade_4','grade_4','grade_3','grade_2','grade_2','grade_1'];costs=[12,24,20,36,34,70];damage=[4,0,0,7,3,12];radius=[3,3,4,6,4,6];cooldowns=[30,130,90,85,110,200]
    for family,title,color,identity,names in FAMILIES:
        ids=[];guide+=['## '+title+' (original)','',identity,'']
        for i,move in enumerate(names.split('|')):
            key=family+('_testament_' if family=='ink' and i==2 else '_')+ops[i];ids.append('jjkcraft:'+key)
            mechanics=["Aim at one enemy to add a mark (three layers, eight seconds) and one resonance.","Guard three hits at 20% base reduction (output scales it up to 35%), spending CE on each; family-specific utility also applies.","Move along a collision-checked path up to six blocks and gain one resonance.","Strike enemies in the aimed cone; each hit applies this family's secondary mechanic.","Bind visible enemies briefly at the aimed point; existing marks extend the short bind.","Consume marks and stored resonance for a stronger area strike. An unprepared finisher is much weaker."][i]
            desc=mechanics+' '+identity
            lang['technique.jjkcraft.'+key]=title+': '+move;lang['technique.jjkcraft.'+key+'.desc']=desc
            data={'name':'technique.jjkcraft.'+key,'description':'technique.jjkcraft.'+key+'.desc','category':'healing' if family=='kintsugi' and i==1 else 'utility' if i in (1,2) else 'area' if i>=3 else 'strike','grade':'jjkcraft:'+grades[i],'cast':{'cost':costs[i],'cooldown':cooldowns[i],'range':0 if i==1 else 6 if i==2 else 14,'cast_time':8 if i==5 else 3 if i==3 else 0},'behavior':{'type':'jjkcraft:manga_ability','ability':'original/'+family+'/'+ops[i],'damage':damage[i],'duration':160 if i==0 else 80 if i==1 else 60,'radius':radius[i]},'visuals':{'color':f'#{color:06X}','particle':'jjkcraft:cursed_spark','sound':'jjkcraft:technique.cast','hit_sound':'jjkcraft:technique.bolt_hit','shake':.15 if i==5 else .04,'hit_stop':1 if i==5 else 0,'trail':['slash','aura','afterimage','fan','ring','ring'][i]},'icon':'jjkcraft:textures/gui/technique/'+key+'.png','innate_weight':0,'mastery_uses':35+i*8}
            # Trail enum names are authored below using the supported native values.
            data['visuals']['trail']=['slash','aura','ring','slash','ring','ring'][i]
            save(D/'technique'/f'{key}.json',data);guide+=['- **'+move+'** -- '+mechanics]
        lang['family.jjkcraft.'+family]=title+' (Original)';save(D/'innate_family'/f'{family}.json',{'name':'family.jjkcraft.'+family,'clan':'','inherited_weight':0,'random_weight':10,'techniques':ids,'domain':''});guide+=['']
    for p in (D/'innate_family').glob('*.json'):
        f=json.loads(p.read_text(encoding='utf-8'))
        if f.get('random_weight',0)>0:f['random_weight']=10;save(p,f)
    corrections={'tooth_blast':'Fire a body-made explosive tooth. Every cast removes 2 health, independently of armor and CE discounts. RCT is needed to replenish the body safely.','eye_blast':'Fire explosive body ammunition. Every cast removes 5 health, independently of armor and CE discounts.','flesh_detonation':'Detonate sacrificed flesh in an area. Every cast removes 8 health. The cast refuses to leave you at one health or less.','flesh_regrow':'Requires learned Reverse Cursed Technique. Spend CE to regrow ammunition and heal the sacrificed body.','projection_sorcery':'Execute a saved movement/combat route, or a terrain-aware preset. Edit destinations and physical moves in the workshop route planner. Movement is collision-checked; breaking a frame against terrain briefly freezes you. Crouch to cancel.'}
    for key,desc in corrections.items():lang['technique.jjkcraft.'+key+'.desc']=desc
    p=D/'technique/projection_sorcery.json';d=json.loads(p.read_text());d['cast'].update(cooldown=180,cast_time=8,range=16);d['behavior']['blink']=12;save(p,d)
    save(L,lang);(ROOT/'docs/ORIGINAL_TECHNIQUES.md').write_text('\n'.join(guide),encoding='utf-8')
    print('Added 20 families and 120 fully defined moves; nonzero Random weights are balanced at 10. Inherit weights are preserved.')

if __name__=='__main__':main()
