package com.dragonminez.gametest;

import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class CapeMotionGameTests {
    @GameTest(template="port_empty")
    public static void clothPinsAndCollides(GameTestHelper helper) {
        for(boolean heavy:new boolean[]{false,true}) {
            var sheet=new com.dragonminez.client.render.cloth.CapeCloth.Sheet(heavy);
            for(int tick=0;tick<600;tick++) sheet.step(.15f,.025f,.095f,3.5f);
            for(int i=0;i<sheet.points.length;i++) {
                var point=sheet.points[i];
                helper.assertTrue(Float.isFinite(point.x+point.y+point.z),"Cloth simulation diverged");
                if(i<com.dragonminez.client.render.cloth.CapeCloth.COLUMNS)
                    helper.assertTrue(point.y==0 && point.z==0,"Shoulder attachment moved");
                else {
                    helper.assertTrue(point.z>=.119f && point.y<=0,"Cloth entered body/head");
                    if(point.y>-6 && Math.abs(point.x)<5.1f)
                        helper.assertTrue(point.z+0.001f>=3.5f*Math.max(0,1+point.y/6),"Cape entered tilted head");
                }
            }
        }
        helper.succeed();
    }

}
