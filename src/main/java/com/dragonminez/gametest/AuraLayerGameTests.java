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
    public static void layerOperations(GameTestHelper helper) {
        var gson = new Gson();
        var values = new java.util.ArrayList<>(java.util.List.of("base/inner", "base/outer"));
        var replacement = gson.fromJson("{id:'replacement',operation:'replace',target:'base/inner'}", AuraLayers.Layer.class);
        AuraLayers.validate(java.util.List.of(replacement));
        AuraLayers.apply(values, replacement, "stack/replacement", value -> value);
        helper.assertTrue(values.equals(java.util.List.of("base/outer", "stack/replacement")), "Replacement affected unrelated layers");
        var removal = gson.fromJson("{id:'hide',operation:'remove',target:'base/outer'}", AuraLayers.Layer.class);
        AuraLayers.apply(values, removal, null, value -> value);
        AuraLayers.apply(values, removal, null, value -> value);
        helper.assertTrue(values.equals(java.util.List.of("stack/replacement")), "Removal added a shell or failed when target absent");
        AuraLayers.apply(values, replacement, "stack/another", value -> value);
        helper.assertTrue(values.contains("stack/another"), "Replacement without target failed to supply its shell");
        for (String invalid : java.util.List.of("{id:'bad',operation:'clear'}", "{id:'bad',operation:'replace'}", "{id:'bad',operation:'remove',target:'inner'}", "{id:'bad',target:'base/inner'}")) {
            boolean failed = false;
            try { AuraLayers.validate(java.util.List.of(gson.fromJson(invalid, AuraLayers.Layer.class))); } catch (IllegalArgumentException ex) { failed = true; }
            helper.assertTrue(failed, "Invalid operation accepted: " + invalid);
        }
        helper.succeed();
    }
    @GameTest(template="port_empty")
    public static void layerSchema(GameTestHelper helper) {
        var gson = new GsonBuilder().registerTypeAdapterFactory(new ContentAdapters()).create();
        var group = gson.fromJson("""
            {"identity":{"groupName":"example","formType":"superforms"},"forms":{"stage":{"name":"stage","aura":{"layers":[
              {"id":"inner","color":"#66CCFF","type3d":"god","scale":0.96,"order":0,"sealedWhen":{"not":{"chargingKi":true}},"sealSeconds":0.8},
              {"id":"outer","color":"#FF3344","type3d":"sparking","scale":1,"order":100,"when":{"chargingKi":true}}
            ]}}}}
            """, FormConfig.class);
        var layers = group.getForm("stage").getAuraLayers(); AuraLayers.validate(layers);
        helper.assertTrue(layers.size() == 2 && layers.get(1).getScale() == 1 && layers.get(1).getOrder() == 100, "Layer order changed explicit scale or nested schema was ignored");
        helper.assertTrue(layers.get(0).getSealedWhen() != null && layers.get(0).getSealSeconds() == 0.8, "Layer sealing settings lost");
        var restored = gson.fromJson(gson.toJson(group), FormConfig.class);
        helper.assertTrue(restored.getForm("stage").getAuraLayers().get(1).getId().equals("outer"), "Aura identity lost on config roundtrip");
        var legacy = gson.fromJson("{\"groupName\":\"legacy\",\"forms\":{\"old\":{\"name\":\"old\",\"auraLayer\":2}}}", FormConfig.class);
        helper.assertTrue(legacy.getForm("old").getAuraLayers() == null && legacy.getForm("old").getAuraLayer() == 2, "Legacy aura definition changed");
        helper.assertTrue(restored.getForm("stage").getAuraLayers().get(0).getSealSeconds() == 0.8, "Layer sealing lost on roundtrip");
        for (String invalid : java.util.List.of("{id:'bad',sealSeconds:0}", "{id:'bad',sealSeconds:11}", "{id:'bad',sealedWhen:{unknown:true}}")) {
            boolean failed = false;
            try { AuraLayers.validate(java.util.List.of(new Gson().fromJson(invalid, AuraLayers.Layer.class))); } catch (IllegalArgumentException ex) { failed = true; }
            helper.assertTrue(failed, "Invalid layer seal accepted: " + invalid);
        }
        boolean rejected = false;
        try { AuraLayers.validate(new Gson().fromJson("[{id:'same'},{id:'same'}]", new com.google.gson.reflect.TypeToken<java.util.List<AuraLayers.Layer>>(){}.getType())); } catch (IllegalArgumentException ex) { rejected = true; }
        helper.assertTrue(rejected, "Duplicate layer IDs accepted"); helper.succeed();
    }
}
