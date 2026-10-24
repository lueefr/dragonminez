package com.dragonminez.client.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.io.ByteArrayInputStream;
import java.util.*;

/** file:name resolves a client-local config/dragonminez/form_textures/name.png; other IDs use resources. */
public final class FormTextures {
    private static final Map<String,Entry> CACHE=new LinkedHashMap<>(16,.75f,true);
    private static final class Entry {long checked,modified=Long.MIN_VALUE,size=-1;ResourceLocation texture;}
    public static ResourceLocation resolve(String id){
        if(!id.startsWith("file:")){
            var resource=ResourceLocation.tryParse(id);return resource!=null&&Minecraft.getInstance().getResourceManager().getResource(resource).isPresent()?resource:null;
        }
        String name=id.substring(5);com.dragonminez.api.npc.CustomNpcVisuals.texture(name);
        var entry=CACHE.computeIfAbsent(name,n->new Entry());long now=System.currentTimeMillis();
        if(CACHE.size()>128){var first=CACHE.entrySet().iterator();var old=first.next();if(old.getValue().texture!=null)Minecraft.getInstance().getTextureManager().release(old.getValue().texture);first.remove();}
        if(entry.checked!=0&&now-entry.checked<1000)return entry.texture;entry.checked=now;
        Path file=FMLPaths.CONFIGDIR.get().resolve("dragonminez/form_textures/"+name+".png");
        try{
            long modified=Files.exists(file)?Files.getLastModifiedTime(file).toMillis():-1,size=Files.exists(file)?Files.size(file):-1;
            if(modified==entry.modified&&size==entry.size)return entry.texture;entry.modified=modified;entry.size=size;
            if(size<24||size>4194304)throw new java.io.IOException("Expected PNG of at most 4 MiB");
            byte[] bytes;try(var input=Files.newInputStream(file)){bytes=input.readNBytes(4194305);}
            if(bytes.length<24||bytes.length>4194304)throw new java.io.IOException("Invalid PNG size");
            var header=ByteBuffer.wrap(bytes);if(header.getLong()!=0x89504e470d0a1a0aL||header.getInt()!=13||header.getInt()!=0x49484452)throw new java.io.IOException("Invalid PNG header");
            int w=header.getInt(),h=header.getInt();if(w<1||h<1||w>2048||h>2048)throw new java.io.IOException("PNG dimensions must be 1..2048");
            var image=NativeImage.read(new ByteArrayInputStream(bytes));if(entry.texture!=null)Minecraft.getInstance().getTextureManager().release(entry.texture);
            entry.texture=ResourceLocation.fromNamespaceAndPath("dragonminez","external_form/"+name);Minecraft.getInstance().getTextureManager().register(entry.texture,new DynamicTexture(image));
        }catch(Exception ex){if(entry.texture!=null)Minecraft.getInstance().getTextureManager().release(entry.texture);entry.texture=null;LogUtils.getLogger().warn("Cannot load form texture {}: {}",file,ex.getMessage());}
        return entry.texture;
    }
    private FormTextures(){}
}
