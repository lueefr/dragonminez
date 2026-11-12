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

/** A pinned, distance-constrained cloth sheet. Coordinates are pixels in the torso frame. */
public final class CapeCloth {
    public static final int COLUMNS = 7, ROWS = 12;
    private static final WeakHashMap<LivingEntity, Sheet[]> SHEETS = new WeakHashMap<>();
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

    public static final class Sheet {
        public final Vector3f[] points = new Vector3f[COLUMNS * ROWS];
        private final Vector3f[] previous = new Vector3f[points.length];
        private final float width, length;
        private double time = -1, pending;
        private Vec3 position;
        private float yaw;
        public Sheet(boolean heavy) {
            width = heavy ? 10 : 14;
            length = heavy ? 18 : 22;
            for (int row=0; row<ROWS; row++) for (int column=0; column<COLUMNS; column++) {
                int index=row*COLUMNS+column;
                points[index]=new Vector3f((column/(float)(COLUMNS-1)-.5f)*width, -row*length/(ROWS-1), .1f);
                previous[index]=new Vector3f(points[index]);
            }
        }
        private void pin() {
            for(int column=0;column<COLUMNS;column++) {
                points[column].set((column/(float)(COLUMNS-1)-.5f)*width, 0, 0);
                previous[column].set(points[column]);
            }
        }
        public void step(float backward, float side, float lift, float head) {
            for(int index=COLUMNS;index<points.length;index++) {
                Vector3f point=points[index], old=previous[index];
                float x=point.x,y=point.y,z=point.z;
                point.add((x-old.x)*.97f+side, (y-old.y)*.97f-.11f+lift, (z-old.z)*.97f+backward);
                old.set(x,y,z);
            }
            for(int pass=0;pass<10;pass++) {
                pin();
                for(int row=0;row<ROWS;row++) for(int column=0;column<COLUMNS;column++) {
                    int index=row*COLUMNS+column;
                    if(column+1<COLUMNS) constrain(index,index+1,width/(COLUMNS-1));
                    if(row+1<ROWS) constrain(index,index+COLUMNS,length/(ROWS-1));
                    if(row+1<ROWS && column+1<COLUMNS) constrain(index,index+COLUMNS+1,(float)Math.hypot(width/(COLUMNS-1),length/(ROWS-1)));
                    if(row+1<ROWS && column>0) constrain(index,index+COLUMNS-1,(float)Math.hypot(width/(COLUMNS-1),length/(ROWS-1)));
                }
                // A continuous back plane prevents thin cloth tunnelling through the torso.
                // Upper rows also respect the swept volume of a head tilted backwards.
                for(int index=COLUMNS;index<points.length;index++) {
                    Vector3f point=points[index];
                    point.z=Math.max(point.z,.12f);
                    if(point.y>-6 && Math.abs(point.x)<5.1f) {
                        float clearance=head*Math.max(0,1+point.y/6);
                        point.z=Math.max(point.z,clearance);
                    }
                    point.y=Math.min(point.y,-.05f); // never fold through the collar/head
                }
            }
            pin();
        }
        private void constrain(int first,int second,float distance) {
            Vector3f a=points[first],b=points[second];
            float dx=b.x-a.x,dy=b.y-a.y,dz=b.z-a.z;
            float magnitude=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
            if(magnitude<.00001f)return;
            float error=(magnitude-distance)/magnitude;
            float wa=first<COLUMNS?0:1,wb=second<COLUMNS?0:1,total=wa+wb;
            if(total==0)return;
            a.add(dx*error*wa/total,dy*error*wa/total,dz*error*wa/total);
            b.add(-dx*error*wb/total,-dy*error*wb/total,-dz*error*wb/total);
        }
    }

    public static void render(LivingEntity entity, boolean heavy, float partial, PoseStack stack,
                              VertexConsumer buffer, int light,int overlay,int color,float textureU) {
        if(entity==null)return;
        boolean preview = net.minecraft.client.Minecraft.getInstance().screen != null;
        int sheetIndex = (preview ? 2 : 0) + (heavy ? 1 : 0);
        Sheet sheet=SHEETS.computeIfAbsent(entity,key->new Sheet[]{new Sheet(false),new Sheet(true),new Sheet(false),new Sheet(true)})[sheetIndex];
        double now=preview ? net.minecraft.Util.getMillis()/50d : entity.level().getGameTime()+partial;
        Vec3 location=entity.getPosition(partial);
        float yaw=Mth.rotLerp(partial,entity.yBodyRotO,entity.yBodyRot);
        if(sheet.time<0 || now<sheet.time || now-sheet.time>10 || sheet.position!=null && sheet.position.distanceToSqr(location)>64) {
            sheet=new Sheet(heavy);
            SHEETS.get(entity)[sheetIndex]=sheet;
            sheet.time=now;
        }
        // Move free particles against the shoulder's translation and rotation before solving.
        if(sheet.position!=null && !preview) {
            Vec3 shift=sheet.position.subtract(location).yRot((float)Math.toRadians(yaw));
            float turn=(float)Math.toRadians(Mth.wrapDegrees(yaw-sheet.yaw));
            for(int i=COLUMNS;i<sheet.points.length;i++) {
                sheet.points[i].add((float)shift.x*16,(float)shift.y*16,(float)-shift.z*16).rotateY(turn);
                sheet.previous[i].add((float)shift.x*16,(float)shift.y*16,(float)-shift.z*16).rotateY(turn);
            }
        }
        sheet.position=location;sheet.yaw=yaw;
        sheet.pending+=Math.min(.25,(now-sheet.time)/20);sheet.time=now;
        var stats=StatsProvider.get(StatsCapability.INSTANCE,entity).orElse(null);
        boolean charging=stats!=null && (stats.getStatus().isChargingKi() || stats.getStatus().isActionCharging());
        boolean aura=stats!=null && (stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura() || stats.getStatus().isForcedAura());
        boolean firing=entity instanceof com.dragonminez.client.animation.IPlayerAnimatable player && !player.dragonminez$getCurrentKiAnimation().isEmpty();
        Vec3 motion=entity.getDeltaMovement().yRot((float)Math.toRadians(yaw));
        float wind=Mth.clamp((float)(entity.getY()-96)/160,0,1);
        float force=heavy?.65f:1;
        while(sheet.pending>=1d/60) {
            float flutter=Mth.sin((float)(now-sheet.pending*20)*.3f)*.012f;
            sheet.step((.012f+wind*.025f+(float)Math.abs(motion.z)*.14f+(firing?.15f:0)+(aura?.035f:0))*force,
                    ((float)-motion.x*.05f+flutter)*force,
                    (charging?.095f:aura?.045f:0)*force,
                    Mth.clamp(-entity.getXRot()/90,0,1)*3.5f);
            sheet.pending-=1d/60;
        }
        // Rotation can advance on a frame shorter than the fixed simulation step.
        // Enforce the collision envelope before submitting any geometry as well.
        for(int index=COLUMNS;index<sheet.points.length;index++) {
            Vector3f point=sheet.points[index];
            point.z=Math.max(.12f,point.z);
            point.y=Math.min(-.05f,point.y);
            if(point.y>-6 && Math.abs(point.x)<5.1f)
                point.z=Math.max(point.z,Mth.clamp(-entity.getXRot()/90,0,1)*3.5f*Math.max(0,1+point.y/6));
        }
        sheet.pin();
        for(int row=0;row<ROWS-1;row++) for(int column=0;column<COLUMNS-1;column++) {
            int a=row*COLUMNS+column,b=a+1,c=b+COLUMNS,d=a+COLUMNS;
            // Baked geometry uses positive Y upwards; back is positive Z. The torso
            // bone already carries animation, body customization and flight rotation.
            Vector3f normal=new Vector3f(sheet.points[b]).sub(sheet.points[a]).cross(new Vector3f(sheet.points[d]).sub(sheet.points[a])).normalize();
            vertex(buffer,stack,sheet,a,column,row,light,overlay,color,normal,textureU);
            vertex(buffer,stack,sheet,b,column+1,row,light,overlay,color,normal,textureU);
            vertex(buffer,stack,sheet,c,column+1,row+1,light,overlay,color,normal,textureU);
            vertex(buffer,stack,sheet,d,column,row+1,light,overlay,color,normal,textureU);
        }
    }
    private static void vertex(VertexConsumer buffer,PoseStack stack,Sheet sheet,int index,int column,int row,int light,int overlay,int color,Vector3f normal,float textureU) {
        Vector3f point=sheet.points[index];
        buffer.addVertex(stack.last().pose(),point.x/16,(24.25f+point.y)/16,(2.3f+point.z)/16)
                .setColor(color).setUv((textureU+.02f+column/(float)(COLUMNS-1)*9.96f)/64f,(32.02f+row/(float)(ROWS-1)*(sheet.length==18?17.96f:19.96f))/64f)
                .setOverlay(overlay).setLight(light).setNormal(stack.last(),normal.x,normal.y,normal.z);
    }
}
