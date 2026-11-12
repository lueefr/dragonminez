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

    @GameTest(template="port_empty")
    public static void renderingNeverAdvancesPhysics(GameTestHelper helper) {
        for(boolean heavy:new boolean[]{false,true}) {
            var low=new com.dragonminez.client.render.cloth.CapeCloth.Sheet(heavy);
            var high=new com.dragonminez.client.render.cloth.CapeCloth.Sheet(heavy);
            for(int tick=0;tick<300;tick++) {
                float side=(float)Math.sin(tick*.1)*.08f;
                low.tick(.2f,side,.08f,0);high.tick(.2f,side,.08f,0);
                low.sample(.5f);
                for(int frame=0;frame<24;frame++)high.sample(frame/24f);
            }
            for(int i=0;i<low.points.length;i++)
                helper.assertTrue(low.points[i].equals(high.points[i]),"Render frame count changed cloth physics");
        }
        helper.succeed();
    }
    @GameTest(template="port_empty")
    public static void extremeSpeedCannotStretchOrResetCloth(GameTestHelper helper) {
        for(boolean heavy:new boolean[]{false,true}) {
            var sheet=new com.dragonminez.client.render.cloth.CapeCloth.Sheet(heavy);
            float length=heavy?18:22,segment=length/(com.dragonminez.client.render.cloth.CapeCloth.ROWS-1);
            for(int tick=0;tick<500;tick++) {
                sheet.tick(10000,tick%40<20?10000:-10000,.14f,0);
                for(int i=com.dragonminez.client.render.cloth.CapeCloth.COLUMNS;i<sheet.points.length;i++) {
                    var point=sheet.points[i];
                    helper.assertTrue(Float.isFinite(point.x+point.y+point.z)&&point.z>=.119f,"Fast cloth entered wearer or diverged");
                    helper.assertTrue(point.distance(sheet.points[i-com.dragonminez.client.render.cloth.CapeCloth.COLUMNS])<=segment*1.017f,"Cloth stretched at high speed");
                }
            }
        }
        helper.succeed();
    }

}
