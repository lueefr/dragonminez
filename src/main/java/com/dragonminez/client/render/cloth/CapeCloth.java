package com.dragonminez.client.render.cloth;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.WeakHashMap;

/** Tick-driven cloth. Rendering and extra shadow/outline passes never advance physics. */
@net.neoforged.fml.common.EventBusSubscriber(modid="dragonminez",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class CapeCloth {
    public static final int COLUMNS=5, ROWS=9;
    public static final int DETAIL_COLUMNS=9, DETAIL_ROWS=17;
    private static final WeakHashMap<LivingEntity,Sheet[]> SHEETS=new WeakHashMap<>();
    private static int clientTick;
    private static final java.util.Map<net.minecraft.resources.ResourceLocation,Float> TEXTURE_U = new java.util.HashMap<>();
    public static void clearTextures() { TEXTURE_U.clear(); }
    public static float textureU(net.minecraft.resources.ResourceLocation texture) {
        return TEXTURE_U.computeIfAbsent(texture,key -> {
            try(var input=net.minecraft.client.Minecraft.getInstance().getResourceManager().open(key);
                var image=com.mojang.blaze3d.platform.NativeImage.read(input)) {
                int first=0, second=0;
                for(int y=32;y<Math.min(52,image.getHeight());y++) for(int x=0;x<10;x++) {
                    if((image.getPixelRGBA(x,y)>>>24)!=0) first++;
                    if((image.getPixelRGBA(x+10,y)>>>24)!=0) second++;
                }
                return second>first?10f:0f;
            } catch(java.io.IOException ignored) { return 0f; }
        });
    }
    private CapeCloth() {}


    private record Link(int first,int second,float distance) {}
    public static final class Sheet {
        public final Vector3f[] points=new Vector3f[COLUMNS*ROWS];
        private final Vector3f[] previous=new Vector3f[points.length];
        private final Vector3f[] tickStart=new Vector3f[points.length];
        private final Vector3f[] sampled=new Vector3f[points.length];
        private final Vector3f[] normals=new Vector3f[points.length];
        private final Vector3f[] surface=new Vector3f[DETAIL_COLUMNS*DETAIL_ROWS];
        private final Vector3f[] surfaceNormals=new Vector3f[surface.length];
        private final Link[] links;
        private CapeBodyColliders colliders=new CapeBodyColliders();
        public void body(CapeBodyColliders colliders) { this.colliders=colliders; }
        private float phase,oldPhase,wavePower=.1f,oldWavePower=.1f,speed;
        public void dynamics(float speed,float energy) {
            this.speed=Mth.clamp(speed,0,1);
            oldWavePower=wavePower;
            wavePower=.1f+this.speed*.55f+Mth.clamp(energy,0,1)*.3f;
        }
        private final float width,length;
        private Vec3 position;
        private final Vector3f airflow=new Vector3f(),gravity=new Vector3f(),windVector=new Vector3f();
        private float previousFlowZ;
        private float yaw;
        private int lastRendered;
        private boolean initialized;
        public Sheet(boolean heavy) {
            width=heavy?10:14;length=heavy?18:22;
            float dx=width/(COLUMNS-1),dy=length/(ROWS-1);
            java.util.List<Link> constraints=new java.util.ArrayList<>();
            for(int row=0;row<ROWS;row++)for(int column=0;column<COLUMNS;column++) {
                int index=row*COLUMNS+column;
                points[index]=new Vector3f((column/(float)(COLUMNS-1)-.5f)*width,-row*dy,row==0?0:.12f);
                previous[index]=new Vector3f(points[index]);tickStart[index]=new Vector3f(points[index]);
                sampled[index]=new Vector3f(points[index]);normals[index]=new Vector3f();
                if(column+1<COLUMNS)constraints.add(new Link(index,index+1,dx));
                if(row+1<ROWS)constraints.add(new Link(index,index+COLUMNS,dy));
                if(row+1<ROWS&&column+1<COLUMNS)constraints.add(new Link(index,index+COLUMNS+1,(float)Math.hypot(dx,dy)));
                if(row+1<ROWS&&column>0)constraints.add(new Link(index,index+COLUMNS-1,(float)Math.hypot(dx,dy)));
                if(row+2<ROWS)constraints.add(new Link(index,index+2*COLUMNS,2*dy));
            }
            links=constraints.toArray(Link[]::new);
            for(int i=0;i<surface.length;i++) {surface[i]=new Vector3f();surfaceNormals[i]=new Vector3f();}
        }
        private void pin() {
            for(int column=0;column<COLUMNS;column++) {
                points[column].set((column/(float)(COLUMNS-1)-.5f)*width,0,0);
                previous[column].set(points[column]);
            }
        }
        private void collide(int index,float head) {
            Vector3f point=points[index],old=previous[index];
            if(point.y>-.05f) {point.y=-.05f;old.y=Math.min(old.y,point.y);}
            colliders.project(point,old);
        }
        public void step(float backward,float side,float lift,float head) {
            backward=Mth.clamp(backward,-.24f,.3f);side=Mth.clamp(side,-.18f,.18f);lift=Mth.clamp(lift,-.1f,.16f);
            for(int index=COLUMNS;index<points.length;index++) {
                Vector3f point=points[index],old=previous[index];
                float x=point.x,y=point.y,z=point.z;
                float trail=(index/COLUMNS)/(float)(ROWS-1);
                float flutter=Mth.sin(phase-trail*8+(index%COLUMNS)*.7f)*wavePower*.035f*trail;
                point.add(Mth.clamp((x-old.x)*.94f,-.65f,.65f)+side+flutter*.4f,
                    Mth.clamp((y-old.y)*.94f,-.65f,.65f)-.11f+lift+flutter*.5f,
                    Mth.clamp((z-old.z)*.94f,-.65f,.65f)+backward+flutter);
                old.set(x,y,z);
            }
            for(int pass=0;pass<6;pass++) {
                pin();
                for(Link link:links)constrain(link);
                for(int index=COLUMNS;index<points.length;index++)collide(index,head);
            }
            // Ordered rows prevent folds from turning through the sheet at extreme speeds.
            // A hard maximum segment length prevents finite solver error accumulating as stretch.
            float segment=length/(ROWS-1);
            for(int row=1;row<ROWS;row++)for(int column=0;column<COLUMNS;column++) {
                int index=row*COLUMNS+column;
                Vector3f anchor=points[index-COLUMNS],point=points[index];
                point.y=Math.min(point.y,anchor.y-segment*.025f);
                float dx=point.x-anchor.x,dy=point.y-anchor.y,dz=point.z-anchor.z;
                float distance=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
                if(distance>segment*1.015f)point.set(anchor.x+dx*segment*1.015f/distance,anchor.y+dy*segment*1.015f/distance,anchor.z+dz*segment*1.015f/distance);
                collide(index,head);
            }
            pin();
        }
        private void constrain(Link link) {
            Vector3f a=points[link.first],b=points[link.second];
            float dx=b.x-a.x,dy=b.y-a.y,dz=b.z-a.z,magnitude=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
            if(magnitude<.00001f)return;
            float error=(magnitude-link.distance)/magnitude;
            float wa=link.first<COLUMNS?0:1,wb=link.second<COLUMNS?0:1,total=wa+wb;
            if(total==0)return;
            a.add(dx*error*wa/total,dy*error*wa/total,dz*error*wa/total);
            b.add(-dx*error*wb/total,-dy*error*wb/total,-dz*error*wb/total);
        }
        public void tick(float backward,float side,float lift,float head) {
            for(int i=0;i<points.length;i++)tickStart[i].set(points[i]);
            oldPhase=phase;phase+=.24f+speed*.2f;
            for(int step=0;step<3;step++)step(backward,side,lift,head);
        }
        /** Pure sampling: any number of render passes produces the same simulation. */
        public Vector3f[] sample(float partial) {
            partial=Mth.clamp(partial,0,1);
            for(int i=0;i<points.length;i++)sampled[i].set(tickStart[i]).lerp(points[i],partial);
            for(int row=0;row<ROWS;row++)for(int column=0;column<COLUMNS;column++) {
                int index=row*COLUMNS+column;
                Vector3f left=sampled[row*COLUMNS+Math.max(0,column-1)],right=sampled[row*COLUMNS+Math.min(COLUMNS-1,column+1)];
                Vector3f top=sampled[Math.max(0,row-1)*COLUMNS+column],bottom=sampled[Math.min(ROWS-1,row+1)*COLUMNS+column];
                Vector3f normal=normals[index];
                normal.set(right.x-left.x,right.y-left.y,right.z-left.z).cross(bottom.x-top.x,bottom.y-top.y,bottom.z-top.z);
                if(normal.lengthSquared()<.00001f)normal.set(0,0,-1);else normal.normalize();
            }
            return sampled;
        }
        private float spline(int row,int column,float t,int axis) {
            row=Mth.clamp(row,0,ROWS-1);
            float p0=sampled[row*COLUMNS+Math.max(0,column-1)].get(axis);
            float p1=sampled[row*COLUMNS+column].get(axis);
            float p2=sampled[row*COLUMNS+Math.min(COLUMNS-1,column+1)].get(axis);
            float p3=sampled[row*COLUMNS+Math.min(COLUMNS-1,column+2)].get(axis);
            return cubic(p0,p1,p2,p3,t);
        }
        private static float cubic(float a,float b,float c,float d,float t) {
            return b+.5f*t*(c-a+t*(2*a-5*b+4*c-d+t*(3*(b-c)+d-a)));
        }
        /** Smooth visual subdivisions and traveling folds without more simulated particles. */
        public Vector3f[] surface(float partial,boolean detail) {
            sample(partial);
            int columns=detail?DETAIL_COLUMNS:COLUMNS,rows=detail?DETAIL_ROWS:ROWS;
            float time=Mth.lerp(partial,oldPhase,phase),amplitude=Mth.lerp(partial,oldWavePower,wavePower);
            for(int row=0;row<rows;row++)for(int column=0;column<columns;column++) {
                float u=column/(float)(columns-1),v=row/(float)(rows-1);
                float x=u*(COLUMNS-1),y=v*(ROWS-1);int ix=(int)x,iy=(int)y;
                Vector3f vertex=surface[row*columns+column];
                for(int axis=0;axis<3;axis++) vertex.setComponent(axis,cubic(spline(iy-1,ix,x-ix,axis),spline(iy,ix,x-ix,axis),spline(iy+1,ix,x-ix,axis),spline(iy+2,ix,x-ix,axis),y-iy));
                float ripple=(Mth.sin(time*1.3f-v*11+u*3)+Mth.sin(time*1.9f-v*18-u*5)*.3f)*amplitude*v*Mth.sin(u*Mth.PI);
                Vector3f normal=normals[iy*COLUMNS+ix];
                vertex.add(normal.x*ripple,normal.y*ripple,normal.z*ripple);
                // No visual fold may move through the collar even between simulation ticks.
                if(row>0)vertex.y=Math.min(vertex.y,-.02f);
                colliders.projectSurface(vertex);
            }
            for(int row=0;row<rows;row++)for(int column=0;column<columns;column++) {
                Vector3f left=surface[row*columns+Math.max(0,column-1)],right=surface[row*columns+Math.min(columns-1,column+1)];
                Vector3f top=surface[Math.max(0,row-1)*columns+column],bottom=surface[Math.min(rows-1,row+1)*columns+column];
                Vector3f normal=surfaceNormals[row*columns+column];
                normal.set(right.x-left.x,right.y-left.y,right.z-left.z).cross(bottom.x-top.x,bottom.y-top.y,bottom.z-top.z);
                if(normal.lengthSquared()<.00001f)normal.set(0,0,-1);else normal.normalize();
            }
            return surface;
        }

    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void clientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        var mc=net.minecraft.client.Minecraft.getInstance();clientTick++;
        if(mc.level==null) {SHEETS.clear();return;}
        var iterator=SHEETS.entrySet().iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next();LivingEntity wearer=entry.getKey();
            if(wearer.isRemoved()||wearer.level()!=mc.level) {iterator.remove();continue;}
            for(int index=0;index<4;index++) {
                Sheet sheet=entry.getValue()[index];
                if(sheet==null||clientTick-sheet.lastRendered>40||(index<2&&mc.isPaused()))continue;
                update(sheet,wearer,index>=2,(index&1)!=0);
            }
        }
    }
    private static void update(Sheet sheet,LivingEntity entity,boolean preview,boolean heavy) {
        sheet.body(CapeBodyColliders.update(entity,preview));
        Vec3 location=entity.position();float yaw=entity.yBodyRot;
        Vec3 velocity=preview||sheet.position==null?Vec3.ZERO:location.subtract(sheet.position);
        // Teleports change the attachment frame; they never reset the cloth pose.
        if(velocity.lengthSqr()>4096)velocity=Vec3.ZERO;
        if(preview) {sheet.airflow.zero();sheet.gravity.set(0,-.11f,0);}
        else {
            sheet.colliders.worldVector(entity,(float)-velocity.x,(float)-velocity.y,(float)-velocity.z,sheet.airflow);
            sheet.colliders.worldVector(entity,0,-.11f,0,sheet.gravity);
        }
        float speed=(float)Math.tanh(velocity.length()*.4);
        float forward=(float)Math.tanh(sheet.airflow.z*.55);
        float turn=preview?0:Mth.clamp(Mth.wrapDegrees(yaw-sheet.yaw)*.0015f,-.08f,.08f);
        float lateral=(float)Math.tanh(sheet.airflow.x*.65)*.13f;
        float vertical=(float)Math.tanh(sheet.airflow.y*.5)*.065f;
        float acceleration=(float)Math.tanh(sheet.airflow.z-sheet.previousFlowZ)*.045f;
        sheet.position=location;sheet.yaw=yaw;sheet.previousFlowZ=sheet.airflow.z;
        var stats=StatsProvider.get(StatsCapability.INSTANCE,entity).orElse(null);
        boolean transform=stats!=null&&stats.getStatus().isActionCharging();
        boolean charge=stats!=null&&stats.getStatus().isChargingKi();
        boolean aura=stats!=null&&(stats.getStatus().isAuraActive()||stats.getStatus().isPermanentAura()||stats.getStatus().isForcedAura());
        boolean firing=entity instanceof com.dragonminez.client.animation.IPlayerAnimatable player&&!player.dragonminez$getCurrentKiAnimation().isEmpty();
        float wind=Mth.clamp((float)(entity.getY()-96)/160,0,1),weight=heavy?.7f:1;
        float gust=Mth.sin(clientTick*.13f)*(.009f+speed*.025f+wind*.01f);
        if(preview)sheet.windVector.set(wind*.015f,0,wind*.01f);
        else sheet.colliders.worldVector(entity,wind*.025f,0,wind*.01f,sheet.windVector);
        boolean flightFrame=entity instanceof net.minecraft.client.player.AbstractClientPlayer player
            && com.dragonminez.client.events.FlySkillEvent.getInstance().isFlyingFast(player);
        float headClearance=flightFrame?0:Mth.clamp(-entity.getXRot()/90,0,1)*3.5f;
        sheet.dynamics(speed,transform?1:charge?.85f:aura?.35f:0);
        float blast=transform?.28f:charge?.23f:aura?.045f:0;
        float lift=transform?.15f:charge?.135f:aura?.065f:0;
        // Lift offsets gravity even for a heavier cape; weight slows the sideways response.
        sheet.tick((.01f+sheet.windVector.z+forward*.17f+acceleration+blast+(firing?.09f:0))*weight+sheet.gravity.z,
            (lateral+turn+gust+sheet.windVector.x)*weight+sheet.gravity.x,
            lift+vertical+.11f+sheet.gravity.y,headClearance);
        sheet.initialized=true;
    }
    public static void render(LivingEntity entity,boolean heavy,float partial,PoseStack stack,VertexConsumer buffer,int light,int overlay,int color,float textureU) {
        if(entity==null)return;
        boolean preview=net.minecraft.client.Minecraft.getInstance().screen!=null;
        int index=(preview?2:0)+(heavy?1:0);
        Sheet[] sheets=SHEETS.computeIfAbsent(entity,unused->new Sheet[4]);
        Sheet sheet=sheets[index];if(sheet==null)sheet=sheets[index]=new Sheet(heavy);
        sheet.lastRendered=clientTick;
        // Lazy initialization has no integration; all simulation runs in the tick listener.
        if(!sheet.initialized) {sheet.position=entity.position();sheet.yaw=entity.yBodyRot;sheet.initialized=true;}
        var camera=net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        float apparentScale=Math.max(1,entity.getBbHeight()/1.8f);
        boolean detail=preview||entity.position().distanceToSqr(camera.getPosition())<1024*apparentScale*apparentScale;
        int columns=detail?DETAIL_COLUMNS:COLUMNS,rows=detail?DETAIL_ROWS:ROWS;
        sheet.body(CapeBodyColliders.update(entity,preview));
        sheet.surface(partial,detail);
        for(int row=0;row<rows-1;row++)for(int column=0;column<columns-1;column++) {
            int a=row*columns+column,b=a+1,c=b+columns,d=a+columns;
            vertex(buffer,stack,sheet,a,column,row,light,overlay,color,textureU,1,columns,rows);
            vertex(buffer,stack,sheet,b,column+1,row,light,overlay,color,textureU,1,columns,rows);
            vertex(buffer,stack,sheet,c,column+1,row+1,light,overlay,color,textureU,1,columns,rows);
            vertex(buffer,stack,sheet,d,column,row+1,light,overlay,color,textureU,1,columns,rows);
            vertex(buffer,stack,sheet,d,column,row+1,light,overlay,color,textureU,-1,columns,rows);
            vertex(buffer,stack,sheet,c,column+1,row+1,light,overlay,color,textureU,-1,columns,rows);
            vertex(buffer,stack,sheet,b,column+1,row,light,overlay,color,textureU,-1,columns,rows);
            vertex(buffer,stack,sheet,a,column,row,light,overlay,color,textureU,-1,columns,rows);
        }
    }
    private static void vertex(VertexConsumer buffer,PoseStack stack,Sheet sheet,int index,int column,int row,int light,int overlay,int color,float textureU,int face,int columns,int rows) {
        Vector3f point=sheet.surface[index],normal=sheet.surfaceNormals[index];
        buffer.addVertex(stack.last().pose(),point.x/16,(24.25f+point.y)/16,(2.3f+point.z)/16)
            .setColor(color).setUv((textureU+.02f+column/(float)(columns-1)*9.96f)/64f,(32.02f+row/(float)(rows-1)*(sheet.length==18?17.96f:19.96f))/64f)
            .setOverlay(overlay).setLight(light).setNormal(stack.last(),normal.x*face,normal.y*face,normal.z*face);
    }
}
