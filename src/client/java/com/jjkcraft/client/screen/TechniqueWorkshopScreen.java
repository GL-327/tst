package com.jjkcraft.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.minecraft.client.renderer.RenderPipelines;

import net.minecraft.network.chat.Component;

import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.jjkcraft.client.state.*;

import com.jjkcraft.client.ui.*;

import com.jjkcraft.core.technique.TechniqueMath;

import com.jjkcraft.network.payload.PersonalizationPayloads;

import com.jjkcraft.technique.*;

/** A draft is previewed locally; only a server acknowledgement makes it live. */
public final class TechniqueWorkshopScreen extends JjkScreen {
    private boolean choosingPage;
    private final Identifier id;
    private TechniqueTuning draft;
    private InkButton save;
    private int request,waitingTicks;
    private String feedback="";
    private boolean failed;
    private int page;
    private record Numeric(int axis,net.minecraft.client.gui.components.EditBox box){}
    private final java.util.List<Numeric> numbers=new java.util.ArrayList<>();
    private net.minecraft.client.gui.components.EditBox colorBox;
    private static final String[] PAGES={"Core","Delivery","Control","Trajectory","Form","Summons","Trade-offs","Style","Vows I","Vows II"};
    private static final int[][] AXES={{0,1,2,3,4},{6,7,8,9,10},{11,12,13,14,5},{16,17,18,19,20},{21,22,26,27,31},{23,24,25,3,0},{28,29,12,4,0}};
    private static final int[] COLORS={-1,0xB52E48,0x53DFFF,0x9870E8,0xF2C14E,0x5BE38C,0xF06028,0xEEEEEE};
    private static final String[] LABELS={"CE output","Reach / distance","Area / width","Effect duration","Cooldown"};
    public TechniqueWorkshopScreen(Identifier id){super(Component.literal("Technique workshop"));this.id=id;draft=ClientPersonalization.get(id);}
    public TechniqueWorkshopScreen(Identifier id,int page){this(id);this.page=Math.clamp(page,0,PAGES.length-1);}
    private TechniqueDefinition raw(){return ClientProgress.registries().flatMap(r->Techniques.get(r,id)).orElse(null);}
    private boolean usable(int axis){var d=raw();return d!=null&&switch(axis){case 0->TunedBehaviors.output(d.behavior());case 1->TunedBehaviors.reach(d);case 2->TunedBehaviors.area(d.behavior());case 3->TunedBehaviors.duration(d);case 5->d.behavior() instanceof com.jjkcraft.technique.behavior.InfinityBehavior;default->axis<6||TechniqueDetails.applicable(d,axis);};}
    private static String label(int a){return a<5?LABELS[a]:switch(a){case 5->"Infinity filter";case 6->"Projectile speed";case 7->"Shots per cast";case 8->"Spread angle";case 9->"Homing";case 10->"Armor penetration";case 11->"Push / pull / guard force";case 12->"Windup ticks";case 13->"Field placement";case 14->"Target selection";case 16->"Aim yaw offset";case 17->"Aim pitch offset";case 18->"Projectile gravity";case 19->"Homing turn rate %";case 20->"Projectile size %";case 21->"Volley pattern";case 22->"Pulse interval ticks";case 23->"Summons per cast";case 24->"Summon health %";case 25->"Summon movement %";case 26->"Impact lift / slam";case 27->"Edge damage falloff";case 28->"Health paid per cast";case 29->"Caster recoil %";case 31->"Step direction";default->"Visual trail";};}
    private String value(int a){int v=draft.get(a);if(!usable(a))return "Not applicable";return switch(a){case 5->v==0?"Projectiles":v==1?"Contact":"Both (extra CE)";case 7->v==0?"Original count":v+" projectiles";case 8->v<0?"Original spread":v+" degrees";case 9->v==0?"Original":v==1?"Straight flight":"Guided (extra CE)";case 10->v<0?"Original":v+"%";case 12->v<0?"Original":v+" ticks";case 13->v==0?"Aimed point":"Around yourself";case 14->switch(v){case 1->"Curses only";case 2->"Players only";case 3->"Mobs only";default->"All valid targets";};case 21->new String[]{"Fan","Full ring","Vertical fan","Cross fan"}[v];case 30->v==0?"Original trail":TechniqueDefinition.Trail.values()[v-1].getSerializedName();case 31->new String[]{"Forward","Backward","Left","Right","Up","Down"}[v];default->v+"%";};}
    private void edit(int axis,int value){draft=draft.with(axis,value);feedback="";failed=false;}
    private void preset(int p){String name=draft.details().title();draft=switch(p){case 0->new TechniqueTuning(75,100,75,75,200,0).with(12,30);case 1->new TechniqueTuning(250,100,150,100,175,0).with(11,175).with(12,20);case 2->new TechniqueTuning(100,275,75,100,150,0).with(6,175);default->new TechniqueTuning(75,100,100,100,175,0).with(7,5).with(8,24).with(9,2);};draft=draft.named(name).normalized(raw());feedback="";rebuildWidgets();}
    private static int min(int a){return switch(a){case 2->50;case 7,22,23,27,28,29->0;case 8,10,12,18->-1;case 16->-90;case 17->-60;case 26->-100;default->25;};}
    private static int max(int a){return switch(a){case 2,25->200;case 4->400;case 7->12;case 8,16->90;case 17->60;case 10,12,18,27,29->100;case 20->250;case 22->40;case 23->6;case 26->150;case 28->8;default->300;};}
    private static boolean choice(int a){return a==5||a==9||a==13||a==14||a==21||a==30||a==31;}
    private static int limit(int a){return switch(a){case 13->1;case 14,21->3;case 30->9;case 31->5;default->2;};}
    private static String help(int a){return switch(a){case 0->"More output increases damage and CE cost.";case 1->"Distance in blocks is shown in the preview.";case 2->"Wider effects cost more cursed energy.";case 3->"Longer effects increase upkeep and CE cost.";case 4->"100% is the original cooldown. Faster recovery costs more CE.";case 6->"Travel speed, relative to the original projectile.";case 7->"0 preserves the original projectile count.";case 8,10,12,18->"-1 preserves the original setting. Gravity is downward acceleration / 1000.";case 9->"Guided flight costs extra CE.";case 13->"Choose where your field is centered.";case 14->"Allies and protected players remain protected.";case 16,17->"Rotate your technique's server-checked aim by this many degrees.";case 19->"How sharply guided projectiles turn. Stronger guidance costs more CE.";case 20->"Relative projectile size and contact envelope. Larger projectiles cost more CE.";case 21->"Arrange the same projectile count in a fan, full ring, vertical fan or cross.";case 22->"0 keeps the original rhythm. Faster pulses cost more CE; field DPS is preserved.";case 23->"0 keeps the original count; more summons increase CE cost.";case 24,25->"Customize summoned allies' attributes, with a corresponding CE price.";case 26->"Positive values lift a hit target; negative values push it down.";case 27->"More edge falloff reduces damage at the edge and lowers CE cost.";case 28->"Pay this much health per cast for lower CE cost. A cast cannot leave you below 1 HP.";case 29->"Recoil pushes you backward on cast in exchange for a lower CE cost.";case 31->"Choose a collision-checked step direction, including up and down.";default->"Only settings supported by this technique can be edited.";};}

    @Override protected void initContent(){

        numbers.clear();colorBox=null;
        int x=panelX()+10,y=contentY()+27;int controlW=(panelW()-28)*3/5;
        if(id.getPath().equals("projection_sorcery"))addRenderableWidget(new InkButton(x+controlW+8,contentY()-3,panelW()-controlW-28,17,Component.literal("Movement / combat route"),b->minecraft.gui.setScreen(new ProjectionPlannerScreen())));
        addRenderableWidget(new InkButton(x+controlW-85,contentY()-3,85,17,Component.literal(PAGES[page]+" >"),b->{choosingPage=!choosingPage;rebuildWidgets();}));
        if(page<AXES.length)for(int row=0;row<5;row++){
            final int n=AXES[page][row];int yy=y+row*25;
            if(choice(n)){
                var b=addRenderableWidget(new InkButton(x,yy+9,controlW,14,Component.literal(value(n)),button->{edit(n,(draft.get(n)+1)%(limit(n)+1));button.setMessage(Component.literal(value(n)));}));b.active=usable(n);b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(help(n))));

            }else{
                var number=addRenderableWidget(new net.minecraft.client.gui.components.EditBox(Ink.font(),x+controlW-43,yy+8,43,16,Component.literal(label(n))));

                number.setMaxLength(4);number.setValue(Integer.toString(draft.get(n)));number.active=usable(n);

                numbers.add(new Numeric(n,number));
                var slider=addRenderableWidget(new InkSlider(x,yy+10,controlW-50,12,Component.literal(label(n)),min(n),max(n),draft.get(n),v->{edit(n,v);number.setValue(Integer.toString(v));}));slider.active=usable(n);

                number.setResponder(s->{try{edit(n,Integer.parseInt(s));slider.setInt(draft.get(n));}catch(NumberFormatException ignored){}});

                number.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(help(n))));

            }

        }
        else if(page>=8){
            for(int row=0;row<5;row++){
                final int flag=(page-8)*5+row;
                String text=TechniquePact.NAMES[flag]+(draft.pact().has(flag)?" · ON":" · off");
                var button=addRenderableWidget(new InkButton(x,y+row*25,controlW,21,Component.literal(text),b->{draft=draft.withPact(draft.pact().toggle(flag));feedback="";rebuildWidgets();}));
                button.active=raw()!=null&&TechniquePact.compatible(raw(),flag);

                button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(TechniquePact.TERMS[flag]+" Bonuses combine up to x12. Daily restrictions persist while this learned power has the vow. After casting, vows are sealed for at least 60 seconds, or the lifetime of a lasting summon.")));

            }

        }
        else{
            var name=addRenderableWidget(new net.minecraft.client.gui.components.EditBox(Ink.font(),x,y+17,controlW,19,Component.literal("Custom name")));name.setMaxLength(32);name.setValue(draft.details().title());name.setResponder(s->{draft=draft.named(s);feedback="";});
            addRenderableWidget(new InkButton(x,y+43,controlW,19,Component.literal("Cycle technique color"),b->{int at=0;for(int n=0;n<COLORS.length;n++)if(COLORS[n]==draft.details().color())at=n;edit(15,COLORS[(at+1)%COLORS.length]);}));
            var color=addRenderableWidget(new net.minecraft.client.gui.components.EditBox(Ink.font(),x,y+66,controlW/2-3,17,Component.literal("RGB hex color")));colorBox=color;color.setMaxLength(7);color.setValue(draft.details().color()<0?"Default":String.format("#%06X",draft.details().color()));color.setResponder(s->{try{String hex=s.replace("#","");if(hex.length()==6)edit(15,Integer.parseInt(hex,16));}catch(NumberFormatException ignored){}});
            addRenderableWidget(new InkButton(x+controlW/2,y+66,controlW/2,17,Component.literal(value(30)),b->{edit(30,(draft.get(30)+1)%10);b.setMessage(Component.literal(value(30)));}));
            String[] names={"Efficient","Heavy","Long range","Volley"};for(int n=0;n<4;n++){final int p=n;addRenderableWidget(new InkButton(x+(n%2)*controlW/2,y+90+(n/2)*23,controlW/2-3,18,Component.literal(names[n]),b->preset(p)));}

        }
        if(choosingPage){
            int rx=x+controlW+14,rw=panelX()+panelW()-rx-10;
            for(int n=0;n<PAGES.length;n++){final int target=n;var section=addRenderableWidget(new InkButton(rx+(n%2)*(rw/2),contentY()+29+(n/2)*25,rw/2-2,22,Component.literal(PAGES[n]),b->{page=target;choosingPage=false;rebuildWidgets();}).selected(page==n));section.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(PAGES[n])));}
        }
        int bottom=panelY()+panelH()-25;
        addRenderableWidget(new InkButton(x,bottom,61,18,Component.literal("Back"),b->minecraft.gui.setScreen(new LoadoutScreen())));
        addRenderableWidget(new InkButton(x+65,bottom,65,18,Component.literal("Defaults"),b->{draft=TechniqueTuning.DEFAULT;feedback="";rebuildWidgets();}));

        save=addRenderableWidget(new InkButton(panelX()+panelW()-110,bottom,100,18,Component.literal("Apply tuning"),b->{if(raw()==null)return;draft=draft.normalized(raw());request=ClientPersonalization.nextRequest();waitingTicks=0;feedback="Applying…";ClientPlayNetworking.send(new PersonalizationPayloads.Tune(id,draft,request));}));

    }

    @Override public void tick(){
        for(var n:numbers)if(!n.box().isFocused()){String value=Integer.toString(draft.get(n.axis()));if(!value.equals(n.box().getValue()))n.box().setValue(value);}
        if(colorBox!=null&&!colorBox.isFocused()){String value=draft.details().color()<0?"Default":String.format("#%06X",draft.details().color());if(!value.equals(colorBox.getValue()))colorBox.setValue(value);}
        if(request!=0){var reply=ClientPersonalization.reply(id);if(reply!=null&&reply.request()==request){

            failed=reply.result()!=0;feedback=switch(reply.result()){case 0->"Saved";case 1->"Learn this move first";case 2->"Too fast · retry";case 4->"Vow sealed / sole-power conflict";default->"Unavailable · retry";};request=0;

        }else if(++waitingTicks>100){request=0;failed=true;feedback="No reply · retry";}}

        save.active=request==0&&ClientTechniques.known().contains(id)&&!draft.equals(ClientPersonalization.get(id));

        save.setMessage(Component.literal(request!=0?"Applying…":draft.equals(ClientPersonalization.get(id))?"Saved":"Apply tuning"));

    }

    @Override protected void extractContent(GuiGraphicsExtractor g,int mx,int my,float a){
        var raw=raw();if(raw==null)return;var d=draft.apply(raw);int x=panelX()+10,y=contentY();
        int controlW=(panelW()-28)*3/5,rx=x+controlW+14,rw=panelX()+panelW()-rx-10;
        Ink.text(g,Ink.fit(Component.translatable(d.name()),controlW-92),x,y,0xFF000000|d.visuals().color(),1);
        Ink.text(g,"Drag to tune · type exact values · "+PAGES[page],x,y+12,Palette.TEXT_DIM,1);
        if(page<AXES.length)for(int row=0;row<5;row++){
            int axis=AXES[page][row],yy=y+27+row*25;
            Ink.text(g,Ink.fit(Component.literal(label(axis)+(usable(axis)?"":" · unavailable")),controlW),x,yy,usable(axis)?Palette.TEXT:Palette.muted(),1);

        }else if(page==7)Ink.text(g,"Custom name",x,y+27,Palette.TEXT_DIM,1);

        double modifier=ClientAwakening.view().record().sixEyes()?0.75:1;

        double cost=TechniqueMath.cost(d.cast().cost(),ClientProgress.get().growth().control(),ClientTechniques.mastered(id),modifier);

        g.fill(rx-5,y+25,rx+rw+3,panelY()+panelH()-32,0x70000000);
        Ink.text(g,"EXPRESSION CE COST",rx,y+30,Palette.TEXT_DIM,1);

        double budget=ClientEnergy.uncapped()?ClientEnergy.predicted():ClientEnergy.max();
        Ink.text(g,String.format(java.util.Locale.ROOT,"%.1f / %.0f",cost,budget),rx,y+44,cost>budget?Palette.danger():Palette.energyHigh(),1);

        bar(g,rx,y+59,rw,budget>0?cost/budget:1,Palette.energy());
        String[] preview={String.format(java.util.Locale.ROOT,"Cooldown %.2fs",TechniqueMath.cooldown(d.cast().cooldown(),ClientProgress.get().growth().efficiency())/20.0),
            String.format(java.util.Locale.ROOT,"Reach %.1f blocks",d.behavior() instanceof com.jjkcraft.technique.behavior.DashBehavior b?b.distance():d.cast().range()),
            String.format(java.util.Locale.ROOT,"Upkeep %.1f CE/s",d.cast().upkeep()),

            "Output "+draft.output()+"% x"+String.format(java.util.Locale.ROOT,"%.2f",draft.pact().output()),feedback.isEmpty()?(draft.equals(ClientPersonalization.get(id))?"Server: saved":"Unsaved draft"):feedback};
        for(int i=0;i<preview.length;i++)Ink.text(g,Ink.fit(Component.literal(preview[i]),rw),rx,y+72+i*13,Palette.TEXT_DIM,1);
        Ink.text(g,Ink.fit(Component.literal(page>=8?"Hover for terms - 60s seal after cast - x12 cap":page==6?"Health / recoil reduce CE cost; health cannot kill you.":page==7?"RGB hex color · custom trail · editable presets":"100 = original % · 0 pulse/count = original"),controlW),x,y+27+135,Palette.TEXT_DIM,1);
        if(failed)g.fill(rx,y+136,rx+rw,y+137,Palette.danger());

    }

}
