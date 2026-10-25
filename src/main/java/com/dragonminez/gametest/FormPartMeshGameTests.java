package com.dragonminez.gametest;

import com.dragonminez.client.model.FormPartMesh;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

import com.google.gson.JsonParser;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import software.bernie.geckolib.cache.object.*;


@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class FormPartMeshGameTests {
    @GameTest(template="port_empty")
    public static void forearmKeepsShoulderAndUvsAndDoesNotMutateSharedMesh(GameTestHelper helper) {
        var quad=new GeoQuad(new GeoVertex[]{v(-1,0,0,1),v(1,0,1,1),v(1,12,1,0),v(-1,12,0,0)},new Vector3f(0,0,1),Direction.SOUTH);
        var cube=new GeoCube(new GeoQuad[]{quad},Vec3.ZERO,Vec3.ZERO,new Vec3(2,12,0),0,false);
        var arm=new GeoBone(null,"left_arm",false,0d,false,false);arm.getCubes().add(cube);
        var scale=JsonParser.parseString("[2,1,2]");var result=FormPartMesh.deform(cube,arm,scale);
        helper.assertTrue(result==FormPartMesh.deform(cube,arm,scale),"Repeated forearm geometry was not cached");
        boolean wrist=false,shoulder=false,seam=false;
        for(var face:result.quads())for(var vertex:face.vertices()){
            float y=vertex.position().y;
            if(y==0){wrist=true;helper.assertTrue(Math.abs((2)-(Math.abs(vertex.position().x)))<1e-6,"Forearm geometry or UV changed unexpectedly");helper.assertTrue(Math.abs((1)-(vertex.texV()))<1e-6,"Forearm geometry or UV changed unexpectedly");}
            if(y==12){shoulder=true;helper.assertTrue(Math.abs((1)-(Math.abs(vertex.position().x)))<1e-6,"Forearm geometry or UV changed unexpectedly");helper.assertTrue(Math.abs((0)-(vertex.texV()))<1e-6,"Forearm geometry or UV changed unexpectedly");}
            if(y==6){seam=true;helper.assertTrue(Math.abs((1)-(Math.abs(vertex.position().x)))<1e-6,"Forearm geometry or UV changed unexpectedly");helper.assertTrue(Math.abs((.5)-(vertex.texV()))<1e-6,"Forearm geometry or UV changed unexpectedly");}
        }
        helper.assertTrue(wrist&&shoulder&&seam,"Missing wrist, shoulder or elbow seam");helper.assertTrue(quad.vertices()[0].position().x==-1&&quad.vertices().length==4,"Shared source mesh was mutated");helper.succeed();
    }
    private static GeoVertex v(float x,float y,float u,float v){return new GeoVertex(new Vector3f(x,y,0),u,v);}
}
