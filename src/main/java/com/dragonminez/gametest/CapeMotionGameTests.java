package com.dragonminez.gametest;

import com.dragonminez.client.render.cloth.CapeMotion;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class CapeMotionGameTests {
    @GameTest(template="port_empty")
    public static void capeClearsTorso(GameTestHelper helper) {
        // Actual clothing cape: 14 pixels wide, segments 6/6/10, seam Z=2.3.
        // The weighted cape is narrower and shorter, so this also bounds it.
        for(float root:new float[]{.04f,.18f,.6f,1.45f})
        for(float middle:new float[]{-.25f,0,.45f})
        for(float tip:new float[]{-.25f,0,.45f})
        for(float side:new float[]{-.35f,0,.35f}) {
            float[] angles={root,middle,tip};
            float total=0;
            for(int i=0;i<3;i++){angles[i]=Math.max(angles[i],.025f-total);total+=angles[i];}
            Matrix4f transform=new Matrix4f().translation(0,24.25f,2.3f);
            for(int segment=0;segment<3;segment++) {
                float[] motion=CapeMotion.rotation(angles,side,segment);
                if(segment==0) helper.assertTrue(motion[0]<0 && motion[1]==0,"Shoulder seam swings toward torso");
                transform.rotateZ(motion[1]).rotateX(motion[0]);
                float length=segment==2?10:6;
                for(float x:new float[]{-7,7}) for(float y:new float[]{0,-length}) {
                    Vector3f vertex=transform.transformPosition(new Vector3f(x,y,0));
                    helper.assertTrue(vertex.z>=2,"Cape vertex entered torso: segment="+segment+" z="+vertex.z);
                }
                transform.translate(0,-length,0);
            }
        }
        helper.succeed();
    }
}
