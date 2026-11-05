package com.dragonminez.gametest;

import com.dragonminez.common.config.*;
import com.dragonminez.common.config.schema.ContentAdapters;
import com.google.gson.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class AuraLayerGameTests {
    @GameTest(template="port_empty")
    public static void layerSchema(GameTestHelper helper) {
        var gson = new GsonBuilder().registerTypeAdapterFactory(new ContentAdapters()).create();
        var group = gson.fromJson("""
            {"identity":{"groupName":"example","formType":"superforms"},"forms":{"stage":{"name":"stage","aura":{"layers":[
              {"id":"inner","color":"#66CCFF","type3d":"god","scale":0.96,"order":0},
              {"id":"outer","color":"#FF3344","type3d":"sparking","scale":1,"order":100,"when":{"chargingKi":true}}
            ]}}}}
            """, FormConfig.class);
        var layers = group.getForm("stage").getAuraLayers(); AuraLayers.validate(layers);
        helper.assertTrue(layers.size() == 2 && layers.get(1).getScale() == 1 && layers.get(1).getOrder() == 100, "Layer order changed explicit scale or nested schema was ignored");
        var restored = gson.fromJson(gson.toJson(group), FormConfig.class);
        helper.assertTrue(restored.getForm("stage").getAuraLayers().get(1).getId().equals("outer"), "Aura identity lost on config roundtrip");
        var legacy = gson.fromJson("{\"groupName\":\"legacy\",\"forms\":{\"old\":{\"name\":\"old\",\"auraLayer\":2}}}", FormConfig.class);
        helper.assertTrue(legacy.getForm("old").getAuraLayers() == null && legacy.getForm("old").getAuraLayer() == 2, "Legacy aura definition changed");
        boolean rejected = false;
        try { AuraLayers.validate(new Gson().fromJson("[{id:'same'},{id:'same'}]", new com.google.gson.reflect.TypeToken<java.util.List<AuraLayers.Layer>>(){}.getType())); } catch (IllegalArgumentException ex) { rejected = true; }
        helper.assertTrue(rejected, "Duplicate layer IDs accepted"); helper.succeed();
    }
}
