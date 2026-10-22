package com.jjkcraft.client.screen;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.*;
import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.state.*;
import com.jjkcraft.client.ui.*;
import com.jjkcraft.domain.Domains;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.network.payload.PersonalizationPayloads;
import com.jjkcraft.sorcerer.SorcererProgress.Growth;
import com.jjkcraft.technique.*;

/** One draft across seven workspaces. Only Apply changes the server character. */
public final class CreativeTechniqueScreen extends JjkScreen {
    private static final String[] PAGES={"Family","Potential","Training","Attributes","Powers","Domain","Skills"};
    private final Set<String> invalid=new HashSet<>();
    private final Map<Identifier,Integer> mastery=new HashMap<>();
    private final Set<Identifier> learned=new LinkedHashSet<>(),traits=new LinkedHashSet<>();
    private Identifier family=JjkCraft.id("limitless"),grade=JjkCraft.id("grade_4");
    private int page,offset,request,waiting,xp,birth=100,trained,regen=1,output=100,healing=100,threshold=60;
    private int[] growth={0,0,0,0};
    private double stored=100,bodyHealth=20;
    private boolean loaded,readSent,reinforcement,active,rct,autoRct=true,heavenly,special;
    private String search="",lastSearch="",form="",domain="",feedback="Reading character…";
    private List<String> slots=List.of();
    private InkButton apply;
    public CreativeTechniqueScreen(){super(Component.literal("Creative character workshop"));}
    private int x(){return panelX()+10;} private int y(){return contentY()+42;} private int w(){return panelW()-20;}
    private int rows(){return Math.max(2,(panelH()-126)/25);}
    private InnateFamily family(Identifier id){var registry=minecraft.level.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES);var found=registry.getValue(id);if(found!=null)return found;return registry.keySet().stream().sorted().map(registry::getValue).findFirst().orElse(null);}
    private boolean matches(Identifier id,String text){return (id.getPath()+" "+text).toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));}
    private void button(String label,int px,int py,int width,Runnable action){addRenderableWidget(new InkButton(px,py,width,20,Component.literal(label),b->action.run()));}
    private void toggle(String label,int px,int py,int width,boolean on,Consumer<Boolean> action){addRenderableWidget(new InkButton(px,py,width,20,Component.literal(label+": "+(on?"ON":"off")),b->{action.accept(!on);rebuildWidgets();}).selected(on));}
    private record Label(String text,int x,int y){}
    private final List<Label> labels=new ArrayList<>();
    private void number(String label,int px,int py,int width,int value,int min,int max,IntConsumer action){
        labels.add(new Label(label,px,py));var box=addRenderableWidget(new EditBox(Ink.font(),px,py+12,width,16,Component.literal(label)));
        box.setMaxLength(10);box.setValue(""+value);box.setTooltip(Tooltip.create(Component.literal(label+" · "+min+"–"+max)));
        box.setResponder(s->{try{int v=Integer.parseInt(s);if(v<min||v>max)throw new NumberFormatException();invalid.remove(label);box.setTextColor(Palette.TEXT);action.accept(v);}catch(NumberFormatException e){invalid.add(label);box.setTextColor(Palette.danger());}});
    }
    private void read(CreativeSettings s){
        grade=s.grade();xp=s.xp();growth=new int[]{s.growth().output(),s.growth().control(),s.growth().efficiency(),s.growth().domain()};var e=s.energy();birth=e.birth();trained=e.trained();regen=e.regen();stored=e.stored();reinforcement=e.reinforcement();active=e.active();output=e.output();healing=e.healing();threshold=e.threshold();rct=e.rct();autoRct=e.autoRct();traits.clear();traits.addAll(s.traits());form=s.form();bodyHealth=s.bodyHealth();heavenly=s.heavenly();special=s.special();learned.clear();learned.addAll(s.learned());mastery.clear();mastery.putAll(s.mastery());slots=s.slots();domain=s.domain();invalid.clear();
    }
    private CreativeSettings settings(){return new CreativeSettings(grade,xp,new Growth(growth[0],growth[1],growth[2],growth[3]),new CreativeSettings.Energy(birth,trained,regen,stored,reinforcement,active,output,healing,threshold,rct,autoRct),List.copyOf(traits),form,bodyHealth,heavenly,List.copyOf(learned),Map.copyOf(mastery),slots,domain,special);}
    private void autoSlot(){slots=learned.stream().limit(12).map(Object::toString).toList();}
    private void maxPreset(){grade=JjkCraft.id("calamity");xp=10000;growth=new int[]{20,20,20,20};special=true;heavenly=false;birth=traits.contains(JjkCraft.id("mechamaru_restriction"))?0:3000;trained=10000;regen=10;stored=traits.contains(JjkCraft.id("mechamaru_restriction"))?0:1e9;reinforcement=true;active=false;rct=true;autoRct=false;learned.addAll(family(family).techniques());for(var id:learned)mastery.put(id,ClientTechniques.definition(id).orElseThrow().masteryUses()*4);domain=family(family).domain();autoSlot();}
    private void select(Identifier id){
        if(!loaded||id.equals(family))return;
        var old=family(family);if(old!=null){learned.removeAll(old.techniques());old.techniques().forEach(mastery::remove);}family=id;
        for(var move:family(id).techniques())if(ClientTechniques.definition(move).map(d->Grades.rank(minecraft.level.registryAccess(),d.grade())<=Grades.rank(minecraft.level.registryAccess(),grade)).orElse(false))learned.add(move);
        if(ClientConfig.get().creativeAutoCalamity)maxPreset();else autoSlot();feedback="Draft · apply to save";rebuildWidgets();
    }
    private void query(int width){var box=addRenderableWidget(new EditBox(Ink.font(),x(),y(),width,18,Component.literal("Search")));box.setMaxLength(48);box.setHint(Component.literal("Search…"));box.setValue(search);box.setResponder(s->{search=s;offset=0;});}
    private void pager(int total,int count){int py=panelY()+panelH()-47;button("<",x(),py,28,()->{offset=Math.max(0,offset-count);rebuildWidgets();});button(">",x()+w()-28,py,28,()->{offset=Math.min(Math.max(0,total-count),offset+count);rebuildWidgets();});}
    @Override protected void initContent(){
        labels.clear();invalid.clear();int tw=w()/PAGES.length;
        for(int n=0;n<PAGES.length;n++){final int at=n;addRenderableWidget(new InkButton(x()+n*tw,contentY()+18,tw-2,18,Component.literal(PAGES[n]),b->{page=at;offset=0;search=lastSearch="";rebuildWidgets();}).selected(page==n)).setTooltip(Tooltip.create(Component.literal(PAGES[n])));}
        if(!readSent){readSent=true;request=ClientPersonalization.nextRequest();ClientPlayNetworking.send(new PersonalizationPayloads.CreativeEdit(family,CreativeSettings.DEFAULT,request,false));}
        int col=(w()-8)/2;
        switch(page){
            case 0->{
                query(col);var ids=minecraft.level.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES).keySet().stream().sorted().filter(id->matches(id,Component.translatable(family(id).name()).getString())).toList();offset=Math.clamp(offset,0,Math.max(0,ids.size()-rows()));
                for(int n=offset;n<Math.min(ids.size(),offset+rows());n++){var id=ids.get(n);addRenderableWidget(new InkButton(x(),y()+24+(n-offset)*25,col,22,Component.translatable(family(id).name()),b->select(id)).selected(id.equals(family)));}
                int rx=x()+col+8;toggle("Auto Calamity",rx,y(),col,ClientConfig.get().creativeAutoCalamity,v->{ClientConfig.get().creativeAutoCalamity=v;ClientConfig.save();});
                button("Learn whole family",rx,y()+26,col,()->{learned.addAll(family(family).techniques());autoSlot();feedback="Whole family added to draft";});pager(ids.size(),rows());
            }
            case 1->{
                var ladder=Grades.ladder(minecraft.level.registryAccess());int idx=0;for(int n=0;n<ladder.size();n++)if(ladder.get(n).id().equals(grade))idx=n;final int next=(idx+1)%ladder.size();
                button(Component.translatable(Grades.get(minecraft.level.registryAccess(),grade).orElseThrow().name()).getString(),x(),y(),col,()->{grade=ladder.get(next).id();rebuildWidgets();});
                toggle("Physical HR",x()+col+8,y(),col,heavenly,v->{heavenly=v;if(v){birth=0;stored=0;traits.remove(JjkCraft.id("mechamaru_restriction"));}});
                number("Birth CE",x(),y()+26,col,birth,0,3000,v->birth=v);number("Trained CE",x()+col+8,y()+26,col,trained,0,10000,v->trained=v);
                number("Regeneration level",x(),y()+56,col,regen,1,10,v->regen=v);number("Stored CE",x()+col+8,y()+56,col,(int)stored,0,1000000000,v->stored=v);
                number("Grade XP",x(),y()+86,col,xp,0,1000000,v->xp=v);number("Body health",x()+col+8,y()+86,col,(int)bodyHealth,4,100,v->bodyHealth=v);
            }
            case 2->{
                String[] names={"Output growth","Control growth","Efficiency growth","Domain growth"};for(int n=0;n<4;n++){final int at=n;number(names[n],x()+(n%2)*(col+8),y()+(n/2)*36,col,growth[n],0,20,v->growth[at]=v);}
                toggle("Special unlock",x(),y()+80,col,special,v->special=v);button("Reset growth",x()+col+8,y()+80,col,()->{growth=new int[4];rebuildWidgets();});
            }
            case 3->{
                var registry=minecraft.level.registryAccess().lookupOrThrow(CharacterTraits.DEFINITIONS);var ids=registry.keySet().stream().sorted().toList();int count=rows()*2;offset=Math.clamp(offset,0,Math.max(0,ids.size()-count));
                for(int n=offset;n<Math.min(ids.size(),offset+count);n++){var id=ids.get(n);var d=registry.getValue(id);int at=n-offset;var b=addRenderableWidget(new InkButton(x()+(at%2)*(col+8),y()+(at/2)*25,col,22,Component.literal((traits.contains(id)?"✓ ":"+ ")+Component.translatable(d.name()).getString()),button->{if(!traits.remove(id)){traits.add(id);if(id.equals(JjkCraft.id("mechamaru_restriction"))){heavenly=false;birth=0;stored=0;}}rebuildWidgets();}).selected(traits.contains(id)));b.setTooltip(Tooltip.create(Component.translatable(d.description())));}pager(ids.size(),count);
            }
            case 4->{
                query(w()-112);button("Auto-slot known",x()+w()-106,y(),106,()->{autoSlot();feedback="First twelve learned moves slotted";});
                var ids=Techniques.all(minecraft.level.registryAccess()).stream().filter(e->matches(e.id(),Component.translatable(e.definition().name()).getString())).toList();int count=rows();offset=Math.clamp(offset,0,Math.max(0,ids.size()-count));
                for(int n=offset;n<Math.min(ids.size(),offset+count);n++){var e=ids.get(n);var id=e.id();int yy=y()+24+(n-offset)*25;
                    addRenderableWidget(new InkButton(x(),yy,w()-88,22,Component.literal((learned.contains(id)?"✓ ":"+ ")+Component.translatable(e.definition().name()).getString()),b->{if(!learned.remove(id))learned.add(id);else{mastery.remove(id);slots=slots.stream().map(s->s.equals(id.toString())?"":s).toList();}rebuildWidgets();}).selected(learned.contains(id)));
                    var box=addRenderableWidget(new EditBox(Ink.font(),x()+w()-82,yy+2,82,18,Component.literal("Mastery casts")));box.setMaxLength(7);box.setValue(""+mastery.getOrDefault(id,0));box.active=learned.contains(id);box.setTooltip(Tooltip.create(Component.literal("Exact landed casts · mastered at "+e.definition().masteryUses())));box.setResponder(s->{try{int v=Integer.parseInt(s);if(v<0||v>1000000)throw new NumberFormatException();mastery.put(id,v);invalid.remove(id.toString());box.setTextColor(Palette.TEXT);}catch(NumberFormatException ex){invalid.add(id.toString());box.setTextColor(Palette.danger());}});
                }pager(ids.size(),count);
            }
            case 5->{
                var ids=Domains.all(minecraft.level.registryAccess());addRenderableWidget(new InkButton(x(),y(),col,20,Component.literal("No domain"),b->{domain="";rebuildWidgets();}).selected(domain.isEmpty()));
                for(int n=0;n<ids.size();n++){var e=ids.get(n);int at=n+1;addRenderableWidget(new InkButton(x()+(at%2)*(col+8),y()+(at/2)*23,col,20,Component.translatable(e.definition().name()),b->{domain=e.id().toString();rebuildWidgets();}).selected(domain.equals(e.id().toString())));}
            }
            case 6->{
                toggle("Learn reinforcement",x(),y(),col,reinforcement,v->{reinforcement=v;if(!v)active=false;});toggle("Learn RCT",x()+col+8,y(),col,rct,v->rct=v);
                toggle("Reinforcement active",x(),y()+24,col,active,v->{active=v;if(v)reinforcement=true;});toggle("Automatic RCT",x()+col+8,y()+24,col,autoRct,v->autoRct=v);
                number("Reinforcement output %",x(),y()+50,col,output,25,300,v->output=v);number("RCT output %",x()+col+8,y()+50,col,healing,25,300,v->healing=v);
                number("Auto RCT below %",x(),y()+85,col,threshold,25,100,v->threshold=v);
                String[] forms={"","spirit","transplanted"};int at=Arrays.asList(forms).indexOf(form);button("Body: "+(form.isEmpty()?"human":form),x()+col+8,y()+97,col,()->{form=forms[(at+1)%3];if(form.equals("spirit"))traits.add(JjkCraft.id("vengeful_spirit"));else traits.remove(JjkCraft.id("vengeful_spirit"));rebuildWidgets();});
            }
        }
        int bottom=panelY()+panelH()-24;button("Back",x(),bottom,54,()->minecraft.gui.setScreen(new LoadoutScreen()));
        button("Calamity preset",x()+58,bottom,108,()->{maxPreset();feedback="Preset loaded · review and apply";rebuildWidgets();});
        apply=addRenderableWidget(new InkButton(panelX()+panelW()-125,bottom,115,18,Component.literal("Apply character"),b->{request=ClientPersonalization.nextRequest();waiting=0;feedback="Applying…";ClientPlayNetworking.send(new PersonalizationPayloads.CreativeEdit(family,settings(),request,true));}));
    }
    @Override public void tick(){
        if(request!=0){var reply=ClientPersonalization.creative();if(reply!=null&&reply.request()==request){boolean first=!loaded;request=0;loaded=true;feedback=reply.result()==0?(first?"Character loaded · edits are a draft":"Saved on server"):reply.result()==2?"Too fast · retry":"Invalid combination · review values";if(reply.result()==0){family=minecraft.level.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES).getValue(reply.family())!=null?reply.family():minecraft.level.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES).keySet().stream().sorted().findFirst().orElse(family);read(reply.settings());rebuildWidgets();}}else if(++waiting>100){request=0;feedback="No reply · reopen to retry";}}
        if(apply!=null)apply.active=loaded&&request==0&&invalid.isEmpty();
        if((page==0||page==4)&&!search.equals(lastSearch)){lastSearch=search;rebuildWidgets();children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).findFirst().ifPresent(this::setFocused);}
    }
    @Override protected void extractContent(GuiGraphicsExtractor g,int mx,int my,float a){
        Ink.text(g,"CREATIVE CHARACTER WORKSHOP",x(),contentY()+2,Palette.energyHigh(),1);
        if(page==0){int rx=x()+(w()-8)/2+8;var f=family(family);if(f==null){Ink.text(g,"No technique families loaded",rx,y()+60,Palette.danger(),1);return;}Ink.text(g,Ink.fit(Component.translatable(f.name()),w()/2-12),rx,y()+60,Palette.gold(),1);Ink.text(g,f.techniques().size()+" moves · "+f.clan(),rx,y()+76,Palette.TEXT_DIM,1);Ink.text(g,"Family selection edits a draft.",rx,y()+86,Palette.TEXT_DIM,1);Ink.text(g,"Calamity is optional.",rx,y()+109,Palette.TEXT_DIM,1);}
        for(var label:labels)Ink.text(g,label.text(),label.x(),label.y(),Palette.TEXT_DIM,1);
        Ink.text(g,Ink.fit(Component.literal(invalid.isEmpty()?feedback:"Invalid value: "+invalid.iterator().next()),w()-66),x()+33,panelY()+panelH()-38,invalid.isEmpty()?Palette.TEXT_DIM:Palette.danger(),1);
    }
}
