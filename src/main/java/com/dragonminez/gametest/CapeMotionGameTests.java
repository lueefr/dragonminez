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
            var body=new com.dragonminez.client.render.cloth.CapeBodyColliders();
            body.neutral(new com.dragonminez.common.stats.character.BodyShape(),-1.1f,0);
            sheet.body(body);
            for(int tick=0;tick<600;tick++) sheet.step(.15f,.025f,.095f,3.5f);
            for(int i=0;i<sheet.points.length;i++) {
                var point=sheet.points[i];
                helper.assertTrue(Float.isFinite(point.x+point.y+point.z),"Cloth simulation diverged");
                if(i<com.dragonminez.client.render.cloth.CapeCloth.COLUMNS)
                    helper.assertTrue(point.y==0 && point.z==0,"Shoulder attachment moved");
                else {
                    var outside=new org.joml.Vector3f(point);body.projectSurface(outside);
                    helper.assertTrue(outside.distance(point)<.03f,"Cape entered animated body/head");
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
                low.surface(.5f,true);
                for(int frame=0;frame<24;frame++)high.surface(frame/24f,frame%2==0);
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
                    helper.assertTrue(Float.isFinite(point.x+point.y+point.z),"Fast cloth entered wearer or diverged");
                    helper.assertTrue(point.distance(sheet.points[i-com.dragonminez.client.render.cloth.CapeCloth.COLUMNS])<=segment*1.017f,"Cloth stretched at high speed");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template="port_empty")
    public static void chargingLiftsCapeAndDirectionChangesItsTrail(GameTestHelper helper) {
        for(boolean heavy:new boolean[]{false,true}) {
            var charged=new com.dragonminez.client.render.cloth.CapeCloth.Sheet(heavy);
            charged.dynamics(0,1);
            for(int tick=0;tick<100;tick++)charged.tick(heavy?.17f:.24f,0,.14f,0);
            var tip=charged.points[charged.points.length-com.dragonminez.client.render.cloth.CapeCloth.COLUMNS/2-1];
            float length=heavy?18:22;
            helper.assertTrue(tip.z>length*.8f&&tip.y>-length*.3f,"Charge did not raise cape almost horizontally: "+tip);
            var left=new com.dragonminez.client.render.cloth.CapeCloth.Sheet(heavy);
            var right=new com.dragonminez.client.render.cloth.CapeCloth.Sheet(heavy);
            for(int tick=0;tick<100;tick++) {left.tick(.05f,-.15f,0,0);right.tick(.05f,.15f,0,0);}
            int index=left.points.length-com.dragonminez.client.render.cloth.CapeCloth.COLUMNS/2-1;
            helper.assertTrue(left.points[index].x<-3&&right.points[index].x>3,"Cloth ignored movement direction");
        }
        helper.succeed();
    }
    @GameTest(template="port_empty")
    public static void bodyVolumesStopClothAndPreserveTangentialVelocity(GameTestHelper helper) {
        var body=new com.dragonminez.client.render.cloth.CapeBodyColliders();
        var shape=new com.dragonminez.common.stats.character.BodyShape();
        shape.set(com.dragonminez.common.stats.character.BodyShape.Part.LEG_WIDTH,1.4f);
        shape.set(com.dragonminez.common.stats.character.BodyShape.Part.HEAD,1.2f);
        body.neutral(shape,-.6f,.3f);
        for(var point:new org.joml.Vector3f[]{new org.joml.Vector3f(-2,-18,-2.3f),new org.joml.Vector3f(2,-18,-2.3f),new org.joml.Vector3f(0,3,0)}) {
            var inside=new org.joml.Vector3f(point);body.projectSurface(point);
            helper.assertTrue(point.distance(inside)>.1f,"Leg/head collision volume missing");
            var outside=new org.joml.Vector3f(point);body.projectSurface(outside);
            helper.assertTrue(outside.distance(point)<.001f,"Collision did not stop on surface");
        }
        var point=new org.joml.Vector3f(0,-7,-.4f);var previous=new org.joml.Vector3f(-1,-7,.2f);
        body.project(point,previous);
        helper.assertTrue(Math.abs(point.x-previous.x-1)<.001f&&point.z>=-.061f&&point.z-previous.z>=-.001f,"Collision reset pose or lost tangent velocity");
        helper.succeed();
    }


    @GameTest(template="port_empty")
    public static void flightStaysCloseWithWind(GameTestHelper helper) {
        for(boolean heavy:new boolean[]{false,true}) {
            var sheet=new com.dragonminez.client.render.cloth.CapeCloth.Sheet(heavy);
            sheet.flight(true);sheet.dynamics(.8f,0);
            float lowest=Float.POSITIVE_INFINITY,highest=Float.NEGATIVE_INFINITY;
            for(int tick=0;tick<400;tick++) {
                sheet.tick(.012f,(float)Math.sin(tick*.2)*.014f,0,0);
                var tip=sheet.points[sheet.points.length-3];
                if(tick>200) {lowest=Math.min(lowest,tip.x);highest=Math.max(highest,tip.x);}
                for(int index=5;index<sheet.points.length;index++)
                    helper.assertTrue(sheet.points[index].z<3,"Flight cape left the local back plane");
            }
            helper.assertTrue(highest-lowest>.1f,"Flight wind stopped fluttering");
        }
        helper.succeed();
    }
    @GameTest(template="port_empty")
    public static void movingTailAndHeadDeflectCloth(GameTestHelper helper) {
        var body=new com.dragonminez.client.render.cloth.CapeBodyColliders();
        var shape=new com.dragonminez.common.stats.character.BodyShape();
        var sheet=new com.dragonminez.client.render.cloth.CapeCloth.Sheet(false);sheet.body(body);sheet.flight(true);
        var low=new org.joml.Vector3f(-1,-1,-2);var high=new org.joml.Vector3f(1,1,2);
        for(int tick=0;tick<160;tick++) {
            body.neutral(shape,(float)Math.sin(tick*.08)*1.1f,(float)Math.sin(tick*.1)*.7f);
            var pose=new org.joml.Matrix4f().translation((float)Math.sin(tick*.05)*3,-13,1).rotateY((float)Math.sin(tick*.1));
            body.tail(0,pose,low,high);
            sheet.tick(.012f,.004f,0,0);
            var surface=sheet.surface(.5f,true);
            for(int index=9;index<surface.length;index++) {
                var projected=new org.joml.Vector3f(surface[index]);body.projectSurface(projected);
                helper.assertTrue(projected.distance(surface[index])<.02f,"Animated head/tail penetrated the rendered cape");
            }
        }
        var inside=new org.joml.Vector3f(0,-13,1);
        body.neutral(shape,0,0);body.tail(0,new org.joml.Matrix4f().translation(0,-13,1),low,high);
        var old=new org.joml.Vector3f(inside);body.project(inside,old);
        helper.assertTrue(inside.distance(new org.joml.Vector3f(0,-13,1))>.1f,"Loose tail did not deflect cloth");
        helper.succeed();
    }
}
