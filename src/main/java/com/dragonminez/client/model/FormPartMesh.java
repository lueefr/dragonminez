package com.dragonminez.client.model;

import com.dragonminez.api.forms.FormVisuals;
import com.dragonminez.common.stats.StatsData;
import com.google.gson.JsonElement;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.*;
import java.util.*;

/** Adds a continuous lower-arm deformation to unsplit models, retaining UVs and the original shared mesh. */
public final class FormPartMesh {
    private record Shape(float x,float y,float z,float low,float mid,float centerX,float centerZ,boolean hide) {}
    private static final Map<GeoCube,Map<Shape,GeoCube>> CACHE=new WeakHashMap<>();
    public static GeoBone arm(GeoBone bone){
        for(var parent=bone;parent!=null;parent=parent.getParent())if(parent.getName().equals("left_arm")||parent.getName().equals("right_arm"))return parent;return null;
    }
    public static JsonElement scale(StatsData data,GeoBone arm){
        if(arm==null)return null;
        String part=arm.getName().startsWith("left")?"left_forearm":"right_forearm";
        if(hasBone(arm,part))return null;return FormVisuals.part(data,part);
    }
    private static boolean hasBone(GeoBone bone,String name){for(var child:bone.getChildBones())if(child.getName().equals(name)||hasBone(child,name))return true;return false;}
    public static GeoCube deform(GeoCube cube,GeoBone arm,JsonElement value){
        float low=Float.POSITIVE_INFINITY,high=Float.NEGATIVE_INFINITY,left=Float.POSITIVE_INFINITY,right=Float.NEGATIVE_INFINITY,back=Float.POSITIVE_INFINITY,front=Float.NEGATIVE_INFINITY;
        for(var original:arm.getCubes())for(var quad:original.quads())if(quad!=null)for(var vertex:quad.vertices()){
            var p=vertex.position();low=Math.min(low,p.y);high=Math.max(high,p.y);left=Math.min(left,p.x);right=Math.max(right,p.x);back=Math.min(back,p.z);front=Math.max(front,p.z);
        }
        if(!Float.isFinite(low)||high-low<0.00001f)return cube;
        boolean hide=value.isJsonPrimitive()&&!value.getAsBoolean();
        if(value.isJsonPrimitive()&&!hide)return cube;
        var s=value.isJsonArray()?value.getAsJsonArray():null;
        Shape shape=new Shape(s==null?1:s.get(0).getAsFloat(),s==null?1:s.get(1).getAsFloat(),s==null?1:s.get(2).getAsFloat(),low,(low+high)/2,(left+right)/2,(back+front)/2,hide);
        var variants=CACHE.computeIfAbsent(cube,k->new LinkedHashMap<>());var cached=variants.get(shape);if(cached!=null)return cached;
        List<GeoQuad> quads=new ArrayList<>();
        for(var quad:cube.quads())if(quad!=null){
            add(quads,clip(quad.vertices(),shape.mid(),false),quad,shape);
            if(!hide)add(quads,clip(quad.vertices(),shape.mid(),true),quad,shape);
        }
        var result=new GeoCube(quads.toArray(GeoQuad[]::new),cube.pivot(),cube.rotation(),cube.size(),cube.inflate(),cube.mirror());
        if(variants.size()>=64)variants.clear();variants.put(shape,result);return result;
    }
    private static List<GeoVertex> clip(GeoVertex[] vertices,float mid,boolean lower){
        List<GeoVertex> result=new ArrayList<>();
        for(int i=0;i<vertices.length;i++){
            var a=vertices[i];var b=vertices[(i+1)%vertices.length];boolean inside=lower?a.position().y<=mid:a.position().y>=mid,other=lower?b.position().y<=mid:b.position().y>=mid;
            if(inside)result.add(a);
            if(inside!=other){float t=(mid-a.position().y)/(b.position().y-a.position().y);result.add(new GeoVertex(new Vector3f(a.position()).lerp(b.position(),t),a.texU()+(b.texU()-a.texU())*t,a.texV()+(b.texV()-a.texV())*t));}
        }return result;
    }
    private static GeoVertex point(GeoVertex v,Shape s){
        var p=new Vector3f(v.position());if(p.y<s.mid()){
            float t=Math.min(1,(s.mid()-p.y)/(s.mid()-s.low()));p.x=s.centerX()+(p.x-s.centerX())*(1+(s.x()-1)*t);p.z=s.centerZ()+(p.z-s.centerZ())*(1+(s.z()-1)*t);p.y=s.mid()+(p.y-s.mid())*s.y();
        }return new GeoVertex(p,v.texU(),v.texV());
    }
    private static void add(List<GeoQuad> target,List<GeoVertex> polygon,GeoQuad original,Shape shape){
        if(polygon.size()<3)return;
        GeoVertex[] vertices=polygon.stream().map(v->point(v,shape)).toArray(GeoVertex[]::new);
        if(vertices.length==4){target.add(quad(vertices,original));return;}
        for(int i=1;i<vertices.length-1;i++)target.add(quad(new GeoVertex[]{vertices[0],vertices[i],vertices[i+1],vertices[i+1]},original));
    }
    private static GeoQuad quad(GeoVertex[] vertices,GeoQuad original){
        var a=new Vector3f(vertices[1].position()).sub(vertices[0].position());var b=new Vector3f(vertices[2].position()).sub(vertices[0].position());var normal=a.cross(b);
        if(normal.lengthSquared()<1e-10f)normal.set(original.normal());else{normal.normalize();if(normal.dot(original.normal())<0)normal.negate();}
        return new GeoQuad(vertices,normal,original.direction());
    }
    private FormPartMesh(){}
}
