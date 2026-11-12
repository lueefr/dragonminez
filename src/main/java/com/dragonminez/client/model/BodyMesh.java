package com.dragonminez.client.model;

import com.dragonminez.common.stats.character.BodyShape;
import software.bernie.geckolib.cache.object.GeoBone;

/** Applied inside a renderer snapshot, so proportions never accumulate across frames. */
public final class BodyMesh {
    private BodyMesh() {}
    public static void apply(GeoBone bone, BodyShape shape) {
        float x=1,y=1,z=1,dx=0,dy=0;
        float legs=shape.get(BodyShape.Part.LEGS), torso=shape.get(BodyShape.Part.TORSO_HEIGHT);
        float width=shape.get(BodyShape.Part.TORSO_WIDTH);
        switch(bone.getName()) {
            case "head" -> { x=y=z=shape.get(BodyShape.Part.HEAD); dy=12*(torso-1); }
            case "body" -> { x=width; y=torso; dy=12*(torso-1); }
            case "waist" -> dy=12*(legs-1);
            case "right_leg", "left_leg" -> { y=legs; dy=12*(legs-1); }
            case "right_arm" -> { y=shape.get(BodyShape.Part.ARMS); dx=-5*(width-1); dy=12*(torso-1); }
            case "left_arm" -> { y=shape.get(BodyShape.Part.ARMS); dx=5*(width-1); dy=12*(torso-1); }
            default -> { return; }
        }
        if ((bone.getName().equals("right_arm") || bone.getName().equals("left_arm")) && shape.frame() >= 0) {
            float low=Float.POSITIVE_INFINITY, high=Float.NEGATIVE_INFINITY;
            for (var cube:bone.getCubes()) for(var quad:cube.quads()) if(quad!=null) for(var vertex:quad.vertices()) { low=Math.min(low,vertex.position().x); high=Math.max(high,vertex.position().x); }
            if (high-low>=.15f && high-low<=.26f) x *= (shape.frame()==1 ? 3f : 4f)/16/(high-low);
        }
        bone.setScaleX(bone.getScaleX()*x); bone.setScaleY(bone.getScaleY()*y); bone.setScaleZ(bone.getScaleZ()*z);
        bone.setPosX(bone.getPosX()+dx); bone.setPosY(bone.getPosY()+dy);
    }
}
