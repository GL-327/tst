package com.jjkcraft.client.vfx;
import net.minecraft.world.phys.Vec3;
import com.jjkcraft.client.state.ClientDomains;
import com.jjkcraft.domain.DomainDefinition;
/** Bounded interior set pieces. No terrain is replaced; closure removes all geometry immediately. */
final class DomainScenery {
    private DomainScenery(){}
    static void draw(WorldFx.Frame f,ClientDomains.Domain d,DomainDefinition def,float alpha){
        Vec3 c=d.center();double r=d.radius()*0.92;int main=0xFF000000|def.visuals().color();f.opacity(alpha);
        String id=d.id().getPath();int floor=id.contains("horizon")?0xD0124858:id.contains("mountain")?0xF02A1716:id.contains("void")?0xF0080B18:0xEF17121F;
        for(int n=0;n<48;n++){
            double a=n*Math.PI/24,b=(n+1)*Math.PI/24;
            f.quad(c.add(0,0.015,0),c.add(Math.cos(a)*r,0.015,Math.sin(a)*r),c.add(Math.cos(b)*r,0.015,Math.sin(b)*r),c.add(0,0.015,0),WorldFx.fade(floor,alpha),WorldFx.fade(floor,alpha));
        }
        if(id.contains("shrine")){
            Vec3 s=c.add(0,0,r*0.35);box(f,s.add(0,0.2,0),5,0.5,4,0xFF2C242A);
            for(int side:new int[]{-1,1})box(f,s.add(side*2,2,0),0.45,4,0.5,0xFF9A393B);
            box(f,s.add(0,4.2,0),5.8,0.55,3,0xFF443B45);box(f,s.add(0,4.7,0),4.4,0.5,2.4,0xFFDED0AD);
            box(f,s.add(0,2.6,0.65),2.4,1.9,1.4,0xFF392D39);box(f,s.add(0,2.8,-0.1),1.5,0.35,0.2,main);
            for(int n=0;n<4;n++){double a=n*Math.PI/2;Vec3 p=c.add(Math.cos(a)*r*0.72,0,Math.sin(a)*r*0.72);box(f,p.add(0,2.7,0),3.6,0.3,0.35,0xFF9E3C45);for(int side:new int[]{-1,1})box(f,p.add(side*1.3,1.4,0),0.3,2.8,0.3,0xFF69303A);}
        }else if(id.contains("void")){
            for(int n=0;n<36;n++){double a=n*2.39996;double rr=r*(0.35+(n%7)/12.0);Vec3 p=c.add(Math.cos(a)*rr,2+(n%9)*0.65,Math.sin(a)*rr);f.beam(p.add(-0.15,0,0),p.add(0.15,0,0),0.04,0xFFDCEAFF,0xFFECE7FF);f.beam(p.add(0,-0.25,0),p.add(0,0.25,0),0.03,0xFF8BC8FF,0xFFDDEEFF);}
        }else if(id.contains("arsenal")||id.contains("mutual")){
            for(int n=0;n<25;n++){int x=n%5-2,z=n/5-2;Vec3 p=c.add(x*2.5,0.1,z*2.5);f.beam(p,p.add(0,1.4+(n%3)*0.2,0),0.08,0xFFCBC4DA,0xFFF8E7AC);f.beam(p.add(-0.25,0.95,0),p.add(0.25,0.95,0),0.06,main,main);}
        }else if(id.contains("gamble")){
            for(int n=-1;n<=1;n++){Vec3 p=c.add(n*2.5,2,r*0.4);box(f,p,2,4,0.6,0xFF33434B);box(f,p.add(0,0,-0.34),1.4,1.2,0.08,0xFFDFCD9F);box(f,p.add(0,0.03,-0.41),0.5,0.75,0.06,main);}
            for(int n=-1;n<=1;n+=2)f.beam(c.add(n*1.3,0.06,-r),c.add(n*1.3,0.06,r),0.08,0xFFABB8C6,0xFFABB8C6);
        }else if(id.contains("perfection")){
            for(int n=0;n<8;n++){double a=n*Math.PI/4;Vec3 p=c.add(Math.cos(a)*r*0.7,0,Math.sin(a)*r*0.7);box(f,p.add(0,1.2,0),1.5,2.4,0.8,0xFFBDAA98);for(int finger=0;finger<4;finger++)box(f,p.add((finger-1.5)*0.4,3+(finger==0?0:0.3),0),0.3,2.4,0.35,0xFFD9C4AD);}
        }else{
            boolean sea=id.contains("horizon"),fire=id.contains("mountain"),shadow=id.contains("shadow");
            for(int n=0;n<10;n++){double a=n*Math.PI/5;Vec3 p=c.add(Math.cos(a)*r*0.75,0,Math.sin(a)*r*0.75);double h=2+(n%3);
                box(f,p.add(0,h/2,0),0.8,h,0.8,sea?0xFF456C71:fire?0xFF342932:0xFF312638);
                f.beam(p.add(0,h,0),p.add(0,h+0.6,0),0.1,main,0xFFFFFFFF);
                if(shadow)f.beam(p.add(-0.15,h-0.2,-0.45),p.add(0.15,h-0.2,-0.45),0.05,0xFFDFCFF8,0xFFDFCFF8);
            }
            for(int n=0;n<12;n++){double a=n*Math.PI/6;Vec3 p=c.add(Math.cos(a)*r,0.03,Math.sin(a)*r);f.beam(c.add(0,0.03,0),p,fire?0.13:0.035,WorldFx.fade(main,0.6f),WorldFx.fade(main,0.05f));}
        }
        // Layered boundary seals and suspended filaments keep the interior readable in motion.
        double time=f.time();int segments=com.jjkcraft.client.config.ClientConfig.lowVfx()?16:32;
        for(int ring=0;ring<3;ring++){
            double rr=r*(0.38+ring*0.28),height=0.055+ring*0.02;
            for(int n=0;n<segments;n++){
                double a=n*Math.PI*2/segments+time*0.002*(ring%2==0?1:-1),b=a+Math.PI*1.55/segments;
                f.beam(c.add(Math.cos(a)*rr,height,Math.sin(a)*rr),c.add(Math.cos(b)*rr,height,Math.sin(b)*rr),0.018,WorldFx.fade(main,0.48f),WorldFx.fade(main,0.14f));
            }
        }
        if(!com.jjkcraft.client.config.ClientConfig.lowVfx())for(int n=0;n<12;n++){
            double a=n*2.39996+time*0.0015,rr=r*(0.55+(n%4)*0.09),h=1+(n%5)*0.6+Math.sin(time*0.035+n)*0.3;
            Vec3 p=c.add(Math.cos(a)*rr,h,Math.sin(a)*rr);
            f.beam(p.add(0,-0.18,0),p.add(0,0.25,0),0.025,WorldFx.fade(main,0.1f),WorldFx.fade(main,0.75f));
        }
    }
    private static void box(WorldFx.Frame f,Vec3 c,double w,double h,double d,int color){
        double x=w/2,y=h/2,z=d/2;Vec3 a=c.add(-x,-y,-z),b=c.add(x,-y,-z),e=c.add(-x,y,-z),g=c.add(x,y,-z);
        Vec3 aa=c.add(-x,-y,z),bb=c.add(x,-y,z),ee=c.add(-x,y,z),gg=c.add(x,y,z);
        f.quad(a,b,g,e,color,color);f.quad(bb,aa,ee,gg,color,color);f.quad(aa,a,e,ee,color,color);f.quad(b,bb,gg,g,color,color);f.quad(e,g,gg,ee,color,color);f.quad(aa,bb,b,a,color,color);
    }
}
