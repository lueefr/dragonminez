package com.dragonminez.client.render.cloth;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.BodyShape;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.WeakHashMap;
import java.util.function.Function;

/** Animated body volumes in cape pixels: origin at shoulder, positive Z behind the back.
 * Render callbacks publish poses only; projection is allocation-free and never advances time. */
public final class CapeBodyColliders {
    private static final WeakHashMap<LivingEntity,CapeBodyColliders[]> CACHE=new WeakHashMap<>();
    private static final String[] PARTS={"body","head","right_leg","left_leg","right_arm","left_arm"};
    private static final float SKIN=.24f;
    private static final int TAIL_START=6, TAIL_COUNT=16;
    private final Volume[] volumes=new Volume[TAIL_START+TAIL_COUNT];
    private final Matrix4f bodyInverse=new Matrix4f(), boneMatrix=new Matrix4f(), relative=new Matrix4f();
    private final Vector3f local=new Vector3f(), normal=new Vector3f(), probe=new Vector3f();
    private final org.joml.Quaternionf orientation=new org.joml.Quaternionf();
    private long captured=Long.MIN_VALUE;
    private boolean ready;

    private static final class Volume {
        final Matrix4f matrix=new Matrix4f(), inverse=new Matrix4f();
        final Vector3f low=new Vector3f(),high=new Vector3f(),start=new Vector3f(),end=new Vector3f();
        float radius, outerZ;
        boolean capsule,enabled;
    }

    public CapeBodyColliders() {
        for(int i=0;i<volumes.length;i++)volumes[i]=new Volume();
        neutral(new BodyShape(),0,0);
    }

    public static void clear() {CACHE.clear();}

    /** Call once at root renderRecursively BEFORE BodyMesh.apply changes any live bone. */
    public static void capture(LivingEntity entity,Function<String,GeoBone> bones) {
        if(entity==null || !CapeCloth.enabled() || !CapeCloth.visible())return;
        GeoBone body=bones.apply("body");
        if(body==null)return;
        boolean preview=net.minecraft.client.Minecraft.getInstance().screen!=null;
        CapeBodyColliders[] pair=CACHE.computeIfAbsent(entity,key->new CapeBodyColliders[2]);
        int cacheIndex=preview?1:0;
        CapeBodyColliders collider=pair[cacheIndex];
        if(collider==null)collider=pair[cacheIndex]=new CapeBodyColliders();
        var stats=StatsProvider.get(StatsCapability.INSTANCE,entity).orElse(null);
        BodyShape shape=stats==null?new BodyShape():stats.getCharacter().getBodyShape();
        transform(body,shape,collider.bodyInverse);
        if(Math.abs(collider.bodyInverse.determinant())<.000001f)return;
        collider.bodyInverse.invert();
        for(int index=0;index<PARTS.length;index++) {
            GeoBone bone=bones.apply(PARTS[index]);
            Volume volume=collider.volumes[index];
            if(bone==null || bone.isHidden() || !bounds(bone,volume.low,volume.high)) {
                volume.enabled=false;
                continue;
            }
            transform(bone,shape,collider.boneMatrix);
            collider.relative.identity().translation(0,-24.25f,-2.3f)
                    .mul(collider.bodyInverse).mul(collider.boneMatrix);
            if(index<2)collider.box(index,collider.relative,volume.low,volume.high);
            else collider.limb(index,collider.relative,volume.low,volume.high);
        }
        boolean loose=stats!=null && com.dragonminez.client.util.SaiyanTailRules.hasTail(stats.getCharacter())
                && com.dragonminez.client.util.SaiyanTailRules.looseTailShown(entity,stats.getStatus().isTailVisible())
                && com.dragonminez.api.forms.FormVisuals.flag(stats,"tail",true);
        for(int index=0;index<TAIL_COUNT;index++) {
            Volume volume=collider.volumes[TAIL_START+index];
            GeoBone bone=loose?bones.apply("tail"+(index+1)):null;
            if(bone==null || hidden(bone) || !bounds(bone,volume.low,volume.high)) {volume.enabled=false;continue;}
            transform(bone,shape,collider.boneMatrix);
            collider.relative.identity().translation(0,-24.25f,-2.3f).mul(collider.bodyInverse).mul(collider.boneMatrix);
            collider.box(TAIL_START+index,collider.relative,volume.low,volume.high);
        }
        collider.captured=entity.level().getGameTime();
        collider.ready=true;
    }

    /** Tick-side lookup. Unseen players still have proportion-aware neutral body volumes. */
    public static CapeBodyColliders update(LivingEntity entity,boolean preview) {
        CapeBodyColliders[] pair=CACHE.computeIfAbsent(entity,key->new CapeBodyColliders[2]);
        int cacheIndex=preview?1:0;
        CapeBodyColliders collider=pair[cacheIndex];
        if(collider==null)collider=pair[cacheIndex]=new CapeBodyColliders();
        if(!collider.ready || entity.level().getGameTime()-collider.captured>20) {
            var stats=StatsProvider.get(StatsCapability.INSTANCE,entity).orElse(null);
            collider.neutral(stats==null?new BodyShape():stats.getCharacter().getBodyShape(),
                    entity.getXRot()*Mth.DEG_TO_RAD,Mth.wrapDegrees(entity.getYHeadRot()-entity.yBodyRot)*Mth.DEG_TO_RAD);
        }
        return collider;
    }

    /** Rotate a world-space force into the actual animated torso frame, discarding scale.
     * The outer flight attitude and the animated prone waist are separate transforms. */
    public Vector3f worldVector(LivingEntity entity,float x,float y,float z,Vector3f destination) {
        boolean fast=entity instanceof net.minecraft.client.player.AbstractClientPlayer player
                && com.dragonminez.client.events.FlySkillEvent.getInstance().isFlyingFast(player);
        if(fast && entity==net.minecraft.client.Minecraft.getInstance().player
                && com.dragonminez.client.flight.FlightOrientationHandler.isActive())
            orientation.set(com.dragonminez.client.flight.FlightOrientationHandler.getModelRotation());
        else {
            orientation.rotationY((180-entity.yBodyRot)*Mth.DEG_TO_RAD);
            if(fast)orientation.rotateX(-entity.getXRot()*Mth.DEG_TO_RAD);
        }
        orientation.conjugate().transform(destination.set(x,y,z));
        float dx=destination.x,dy=destination.y,dz=destination.z;
        float rx=(float)Math.sqrt(bodyInverse.m00()*bodyInverse.m00()+bodyInverse.m10()*bodyInverse.m10()+bodyInverse.m20()*bodyInverse.m20());
        float ry=(float)Math.sqrt(bodyInverse.m01()*bodyInverse.m01()+bodyInverse.m11()*bodyInverse.m11()+bodyInverse.m21()*bodyInverse.m21());
        float rz=(float)Math.sqrt(bodyInverse.m02()*bodyInverse.m02()+bodyInverse.m12()*bodyInverse.m12()+bodyInverse.m22()*bodyInverse.m22());
        destination.set((bodyInverse.m00()*dx+bodyInverse.m10()*dy+bodyInverse.m20()*dz)/Math.max(.00001f,rx),
                (bodyInverse.m01()*dx+bodyInverse.m11()*dy+bodyInverse.m21()*dz)/Math.max(.00001f,ry),
                (bodyInverse.m02()*dx+bodyInverse.m12()*dy+bodyInverse.m22()*dz)/Math.max(.00001f,rz));
        float length=destination.length(),original=(float)Math.sqrt(x*x+y*y+z*z);
        if(length>.000001f)destination.mul(original/length);
        return destination;
    }

    /** Neutral geometry is public for deterministic headless collision tests. Angles are radians. */
    public void neutral(BodyShape shape,float pitch,float yaw) {
        bodyInverse.identity();
        for(int i=TAIL_START;i<volumes.length;i++)volumes[i].enabled=false;
        float torso=shape.get(BodyShape.Part.TORSO_HEIGHT),width=shape.get(BodyShape.Part.TORSO_WIDTH);
        float depth=shape.get(BodyShape.Part.TORSO_DEPTH);
        float head=shape.get(BodyShape.Part.HEAD),legs=shape.get(BodyShape.Part.LEGS),arms=shape.get(BodyShape.Part.ARMS);
        // The render attachment inherits torso scaling, so collider coordinates divide it out.
        relative.identity().translation(0,-24.25f,-2.3f);
        low(0,-4,12,-2,4,24,2);box(0,relative,volumes[0].low,volumes[0].high);
        relative.identity().translation(0,-.25f,-2.3f).scale(1/width,1/torso,1/depth)
                .rotateY(yaw).rotateX(-pitch).scale(head*shape.get(BodyShape.Part.HEAD_WIDTH),head*shape.get(BodyShape.Part.HEAD_HEIGHT),head*shape.get(BodyShape.Part.HEAD_DEPTH));
        low(1,-4,0,-4,4,8,4);box(1,relative,volumes[1].low,volumes[1].high);
        for(int side=0;side<2;side++) {
            float x=side==0?-2:2;
            relative.identity().translation(x/width,-12.25f,-2.3f)
                    .scale(shape.get(BodyShape.Part.LEG_WIDTH)/width,legs/torso,shape.get(BodyShape.Part.LEG_DEPTH)/depth);
            low(2+side,-2,-12,-2,2,0,2);limb(2+side,relative,volumes[2+side].low,volumes[2+side].high);
            float armWidth=shape.get(BodyShape.Part.ARM_WIDTH);
            relative.identity().translation((side==0?-5:5),-2.25f,-2.3f)
                    .scale(armWidth/width,arms/torso,shape.get(BodyShape.Part.ARM_DEPTH)/depth);
            low(4+side,-(shape.frame()==1?1.5f:2),-10,-2,shape.frame()==1?1.5f:2,2,2);
            limb(4+side,relative,volumes[4+side].low,volumes[4+side].high);
        }
    }

    private void low(int index,float x,float y,float z,float xx,float yy,float zz) {
        volumes[index].low.set(x,y,z);volumes[index].high.set(xx,yy,zz);
    }
    private void box(int index,Matrix4f matrix,Vector3f low,Vector3f high) {
        Volume volume=volumes[index];
        if(!Float.isFinite(matrix.determinant()) || Math.abs(matrix.determinant())<.000001f) {volume.enabled=false;return;}
        volume.matrix.set(matrix);volume.inverse.set(matrix).invert();
        volume.low.set(low);volume.high.set(high);volume.capsule=false;volume.enabled=true;
        volume.outerZ=Float.NEGATIVE_INFINITY;
        for(int corner=0;corner<8;corner++) {
            matrix.transformPosition((corner&1)==0?low.x-SKIN:high.x+SKIN,
                    (corner&2)==0?low.y-SKIN:high.y+SKIN,(corner&4)==0?low.z-SKIN:high.z+SKIN,probe);
            volume.outerZ=Math.max(volume.outerZ,probe.z);
        }
    }
    private void limb(int index,Matrix4f matrix,Vector3f low,Vector3f high) {
        Volume volume=volumes[index];
        float x=(low.x+high.x)*.5f,z=(low.z+high.z)*.5f;
        float radius=Math.max(high.x-low.x,high.z-low.z)*.5f;
        matrix.transformPosition(x,low.y+radius,z,volume.start);
        matrix.transformPosition(x,high.y-radius,z,volume.end);
        float sx=(float)Math.sqrt(matrix.m00()*matrix.m00()+matrix.m01()*matrix.m01()+matrix.m02()*matrix.m02());
        float sz=(float)Math.sqrt(matrix.m20()*matrix.m20()+matrix.m21()*matrix.m21()+matrix.m22()*matrix.m22());
        volume.radius=radius*Math.max(sx,sz)+SKIN;volume.outerZ=Math.max(volume.start.z,volume.end.z)+volume.radius;volume.capsule=true;volume.enabled=true;
    }

    /** Resolve the UNION of volumes, not one isolated volume at a time. */
    public void project(Vector3f point,Vector3f previous) { resolve(point,previous); }

    /** Same geometry for visual detail, without altering solver velocities. */
    public void projectSurface(Vector3f point) { resolve(point,null); }

    private void resolve(Vector3f point,Vector3f previous) {
        float vx=previous==null?0:point.x-previous.x;
        float vy=previous==null?0:point.y-previous.y;
        float vz=previous==null?0:point.z-previous.z;
        boolean moved=false,settled=false;
        for(int pass=0;pass<4;pass++) {
            boolean changed=false;
            for(Volume volume:volumes)if(volume.enabled && project(point,volume)) {
                changed=true;moved=true;
                float incoming=vx*normal.x+vy*normal.y+vz*normal.z;
                if(incoming<0){vx-=normal.x*incoming;vy-=normal.y*incoming;vz-=normal.z*incoming;}
            }
            if(!changed){settled=true;break;}
        }
        // Escape a trapped overlap along the local rear ray at this exact X/Y.
        // Only volumes actually containing that ray position participate: a head
        // or raised arm elsewhere must never displace cloth around the legs.
        if(!settled)for(int pass=0;pass<volumes.length;pass++) {
            boolean escaped=false;
            for(Volume volume:volumes)if(volume.enabled) {
                probe.set(point);
                if(!project(probe,volume))continue;
                float inside=point.z,outside=Math.max(inside,volume.outerZ)+.003f;
                // Each volume is convex, so its intersection with this ray is one
                // interval. Starting inside gives a monotonic rear exit search.
                for(int iteration=0;iteration<18;iteration++) {
                    float middle=(inside+outside)*.5f;
                    probe.set(point.x,point.y,middle);
                    if(project(probe,volume))inside=middle;else outside=middle;
                }
                point.z=outside+.003f;
                vz=Math.max(0,vz);moved=true;escaped=true;
            }
            // Recheck the union after each correction. Z only increases, so each
            // of the six convex intervals can be exited at most once; no cycles.
            if(!escaped && pass>0)break;
        }
        if(moved && previous!=null)previous.set(point.x-vx,point.y-vy,point.z-vz);
    }
    private boolean project(Vector3f point,Volume volume) {
        if(volume.capsule) {
            float dx=volume.end.x-volume.start.x,dy=volume.end.y-volume.start.y,dz=volume.end.z-volume.start.z;
            float length=dx*dx+dy*dy+dz*dz;
            float t=length>.000001f?Mth.clamp(((point.x-volume.start.x)*dx+(point.y-volume.start.y)*dy+(point.z-volume.start.z)*dz)/length,0,1):0;
            float cx=volume.start.x+t*dx,cy=volume.start.y+t*dy,cz=volume.start.z+t*dz;
            normal.set(point.x-cx,point.y-cy,point.z-cz);
            float square=normal.lengthSquared();
            if(square>=volume.radius*volume.radius)return false;
            if(square<.000001f)normal.set(0,0,1);else normal.mul(1/(float)Math.sqrt(square));
            float outside=volume.radius+.002f;
            point.set(cx+normal.x*outside,cy+normal.y*outside,cz+normal.z*outside);
            return true;
        }
        volume.inverse.transformPosition(point,local);
        if(local.x<volume.low.x-SKIN || local.x>volume.high.x+SKIN
                || local.y<volume.low.y-SKIN || local.y>volume.high.y+SKIN
                || local.z<volume.low.z-SKIN || local.z>volume.high.z+SKIN)return false;
        float distance=local.x-volume.low.x+SKIN;int axis=0;float sign=-1;
        float candidate=volume.high.x+SKIN-local.x;
        if(candidate<distance){distance=candidate;axis=0;sign=1;}
        candidate=local.y-volume.low.y+SKIN;if(candidate<distance){distance=candidate;axis=1;sign=-1;}
        candidate=volume.high.y+SKIN-local.y;if(candidate<distance){distance=candidate;axis=1;sign=1;}
        candidate=local.z-volume.low.z+SKIN;if(candidate<distance){distance=candidate;axis=2;sign=-1;}
        candidate=volume.high.z+SKIN-local.z;if(candidate<distance){distance=candidate;axis=2;sign=1;}
        local.setComponent(axis,local.get(axis)+distance*sign);
        volume.matrix.transformPosition(local,point);
        // Inverse-transpose handles customized nonuniform head/torso scaling.
        normal.set(axis==0?volume.inverse.m00()*sign:axis==1?volume.inverse.m01()*sign:volume.inverse.m02()*sign,
                axis==0?volume.inverse.m10()*sign:axis==1?volume.inverse.m11()*sign:volume.inverse.m12()*sign,
                axis==0?volume.inverse.m20()*sign:axis==1?volume.inverse.m21()*sign:volume.inverse.m22()*sign).normalize();
        point.fma(.002f,normal);
        return true;
    }

    private static boolean hidden(GeoBone bone) {
        for(GeoBone current=bone;current!=null;current=current.getParent())if(current.isHidden())return true;
        return false;
    }
    /** Tail segments share the exact animated bone transform; exposed for deterministic collision tests. */
    public void tail(int index,Matrix4f matrix,Vector3f low,Vector3f high) {
        if(index<0 || index>=TAIL_COUNT)throw new IllegalArgumentException("Tail segment out of range");
        box(TAIL_START+index,matrix,low,high);
    }
    private static boolean bounds(GeoBone bone,Vector3f low,Vector3f high) {
        low.set(Float.POSITIVE_INFINITY);high.set(Float.NEGATIVE_INFINITY);
        for(var cube:bone.getCubes())for(var quad:cube.quads())if(quad!=null)for(var vertex:quad.vertices()) {
            var p=vertex.position();
            low.x=Math.min(low.x,p.x*16);low.y=Math.min(low.y,p.y*16);low.z=Math.min(low.z,p.z*16);
            high.x=Math.max(high.x,p.x*16);high.y=Math.max(high.y,p.y*16);high.z=Math.max(high.z,p.z*16);
        }
        return Float.isFinite(low.x+low.y+low.z+high.x+high.y+high.z);
    }
    private static void transform(GeoBone bone,BodyShape shape,Matrix4f matrix) {
        if(bone.getParent()==null)matrix.identity();else transform(bone.getParent(),shape,matrix);
        float sx=bone.getScaleX(),sy=bone.getScaleY(),sz=bone.getScaleZ();
        float x=bone.getPosX(),y=bone.getPosY(),z=bone.getPosZ();
        float torso=shape.get(BodyShape.Part.TORSO_HEIGHT),width=shape.get(BodyShape.Part.TORSO_WIDTH);
        switch(bone.getName()) {
            case "body" -> {sx*=width;sy*=torso;sz*=shape.get(BodyShape.Part.TORSO_DEPTH);y+=12*(torso-1);}
            case "head" -> {float size=shape.get(BodyShape.Part.HEAD);sx*=size*shape.get(BodyShape.Part.HEAD_WIDTH);sy*=size*shape.get(BodyShape.Part.HEAD_HEIGHT);sz*=size*shape.get(BodyShape.Part.HEAD_DEPTH);y+=12*(torso-1);}
            case "waist" -> y+=12*(shape.get(BodyShape.Part.LEGS)-1);
            case "right_leg","left_leg" -> {sy*=shape.get(BodyShape.Part.LEGS);sx*=shape.get(BodyShape.Part.LEG_WIDTH);sz*=shape.get(BodyShape.Part.LEG_DEPTH);y+=12*(shape.get(BodyShape.Part.LEGS)-1);}
            case "right_arm","left_arm" -> {
                sy*=shape.get(BodyShape.Part.ARMS);sx*=shape.get(BodyShape.Part.ARM_WIDTH);sz*=shape.get(BodyShape.Part.ARM_DEPTH);
                x+=(bone.getName().equals("right_arm")?-5:5)*(width-1);y+=12*(torso-1);
                if(shape.frame()>=0) {
                    float low=Float.POSITIVE_INFINITY,high=Float.NEGATIVE_INFINITY;
                    for(var cube:bone.getCubes())for(var quad:cube.quads())if(quad!=null)for(var vertex:quad.vertices()) {low=Math.min(low,vertex.position().x);high=Math.max(high,vertex.position().x);}
                    if(high-low>=.15f && high-low<=.26f)sx*=(shape.frame()==1?3f:4f)/16/(high-low);
                }
            }
            default -> {}
        }
        matrix.translate(-x,y,z).translate(bone.getPivotX(),bone.getPivotY(),bone.getPivotZ())
                .rotateZYX(bone.getRotZ(),bone.getRotY(),bone.getRotX()).scale(sx,sy,sz)
                .translate(-bone.getPivotX(),-bone.getPivotY(),-bone.getPivotZ());
    }
}
