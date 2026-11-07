package com.dragonminez.gametest;

import com.dragonminez.common.config.*;
import com.dragonminez.common.config.schema.ContentAdapters;
import com.google.gson.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class FormSizeGameTests {
    @GameTest(template="port_empty")
    public static void dimensions(GameTestHelper helper) throws Exception {
        var gson = new GsonBuilder().registerTypeAdapterFactory(new ContentAdapters()).create();
        var group = gson.fromJson("""
            {"identity":{"groupName":"example","formType":"superforms"},"forms":{"large":{
              "name":"large","appearance":{"body":{"scale":[1,1,1]},"size":{
                "reference":"dragonminez:saga_saiyan_oozaru","eyeRatio":0.8}}
            }}}
            """, FormConfig.class);
        var form = group.getForm("large"); form.getSize().validate();
        var character = new com.dragonminez.common.stats.character.Character();
        var scale = character.getModelScalingFor(form, null);
        float height = com.dragonminez.common.init.MainEntities.SAGA_SAIYAN_OOZARU.get().getHeight();
        helper.assertTrue(Math.abs(1.9f*scale[1]-height)<0.0001, "Form height differs from reference entity");
        var restored = gson.fromJson(gson.toJson(group), FormConfig.class).getForm("large");
        helper.assertTrue(restored.getSize().getReference().equals("dragonminez:saga_saiyan_oozaru"), "Size lost on nested config roundtrip");
        var method = com.dragonminez.server.events.players.StatsEvents.class.getDeclaredMethod("formDimensions", Float[].class, net.minecraft.world.entity.Pose.class, float.class);
        method.setAccessible(true);
        var standing = (float[])method.invoke(null, scale, net.minecraft.world.entity.Pose.STANDING, character.eyeRatio(form,null));
        helper.assertTrue(standing[1] == height && Math.abs(standing[2]-height*.8f)<.051, "Physical height or eye ratio disagrees with model");
        var crouched = (float[])method.invoke(null, scale, net.minecraft.world.entity.Pose.CROUCHING, character.eyeRatio(form,null));
        helper.assertTrue(crouched[1]<standing[1] && crouched[2]<standing[2], "Pose dimensions ignored form size");
        form.getSize().setMultiplier(.5f);
        helper.assertTrue(Math.abs(character.getModelScalingFor(form,null)[1]*1.9f-height*.5f)<.001, "Reference size cannot be reduced");
        form.getSize().setHeight(2f); form.getSize().setWidth(1f);
        scale=character.getModelScalingFor(form,null);
        helper.assertTrue(Math.abs(scale[1]*1.9f-1)<.001 && Math.abs(scale[0]*.6f-.5)<.001, "Explicit dimensions or multiplier precedence failed");
        for(String invalid:java.util.List.of("{multiplier:0}","{height:100}","{width:-1}","{eyeRatio:2}","{reference:'example:missing'}")){
            boolean rejected=false;
            try {new Gson().fromJson(invalid,FormSize.class).validate();}catch(IllegalArgumentException ex){rejected=true;}
            helper.assertTrue(rejected,"Invalid form size accepted: "+invalid);
        }
        var legacy = new FormConfig.FormData(); legacy.setModelScaling(new Float[]{1f,2f,3f});
        helper.assertTrue(java.util.Arrays.equals(character.getModelScalingFor(legacy,null),legacy.getModelScaling()),"Legacy scale changed without size settings");
        var tiny = (float[])method.invoke(null, new Float[]{0.01f,0.01f,0.01f}, net.minecraft.world.entity.Pose.SWIMMING, .05f);
        helper.assertTrue(tiny[0]>=.1f && tiny[1]>=.1f && tiny[2]>0 && tiny[2]<tiny[1], "Small body collapsed or eyes escaped it");
        var wide = (float[])method.invoke(null, new Float[]{1f,1f,3f}, net.minecraft.world.entity.Pose.STANDING, .8f);
        helper.assertTrue(Math.abs(wide[0]-1.8f)<.001, "Depth scale escaped physical bounds");
        var ape = new FormConfig.FormData();
        ape.setCustomModel("oozaru"); ape.setModelScaling(new Float[]{3.8f,3.8f,3.8f});
        var apes = new FormConfig(); apes.setGroupName("oozaru");
        apes.setForms(new java.util.LinkedHashMap<>(java.util.Map.of("oozaru",ape)));
        OozaruSize.upgrade(apes);
        var apeScale = character.getModelScalingFor(ape,null);
        helper.assertTrue(Math.abs(apeScale[1]*OozaruSize.MODEL_HEIGHT-OozaruSize.TARGET_HEIGHT)<.001,
                "Existing Oozaru form does not match classic saga NPC rendered height");
        var apeBody = (float[])method.invoke(null,apeScale,net.minecraft.world.entity.Pose.STANDING,.8f);
        helper.assertTrue(Math.abs(apeBody[1]-ape.getSize().getHeight())<.051,
                "Oozaru collision size did not follow the resolved form dimensions");
        ape.getSize().setHeight(6f); OozaruSize.upgrade(apes);
        helper.assertTrue(ape.getSize().getHeight()==6f,"Custom Oozaru size was overwritten");
        helper.succeed();
    }
}
