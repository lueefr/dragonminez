package com.dragonminez.gametest;

import com.dragonminez.api.forms.*;
import com.dragonminez.common.config.*;
import com.dragonminez.common.config.schema.ContentAdapters;
import com.google.gson.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class PresentationGameTests {
    @GameTest(template="port_empty")
    public static void dynamicPresentation(GameTestHelper helper) throws Exception {
        var visual=JsonParser.parseString("""
            {parts:{right_arm:{scale:[1,{source:'meter:example:pose'},1],rotation:[0,90,999],offset:[2,0,-999]}},
             camera:{fov:{source:'attack.charge'},roll:{source:'state.formTicks'}}}
            """).getAsJsonObject();
        FormVisuals.validate(visual);
        var pose=FormVisuals.pose(visual.getAsJsonObject("parts").getAsJsonObject("right_arm"),Map.of("meter:example:pose",2d));
        helper.assertTrue(pose[0]==1&&pose[1]==2&&pose[4]==90&&pose[5]==180&&pose[6]==2&&pose[8]==-32,"Dynamic part pose or bounds were lost");
        var gson=new GsonBuilder().registerTypeAdapterFactory(new ContentAdapters()).create();
        var form=new FormConfig.FormData();form.setName("pose");form.setVisual(visual);
        var encoded=gson.toJsonTree(form).getAsJsonObject();
        helper.assertTrue(encoded.getAsJsonObject("appearance").has("camera"),"Camera settings were not organized under appearance");
        var restored=gson.fromJson(gson.toJson(form),FormConfig.FormData.class);
        helper.assertTrue(restored.getVisual().equals(visual),"Presentation did not survive form synchronization schema");
        var source=JsonParser.parseString("{source:'meter:example:missing'}");
        helper.assertTrue(VisualValue.evaluate(source,Map.of(),1,.05,8)==1,"Missing presentation input did not fall back");
        helper.assertTrue(VisualValue.evaluate(source,Map.of("meter:example:missing",3d),1,.05,8)==1,"Failed presentation expression retried before reload");
        var generation=ConfigManager.class.getDeclaredField("formConfigGeneration");generation.setAccessible(true);long previous=generation.getLong(null);
        try {
            generation.setLong(null,previous+1);
            helper.assertTrue(VisualValue.evaluate(source,Map.of("meter:example:missing",3d),1,.05,8)==3,"Reload did not clear failed presentation expression");
        } finally {generation.setLong(null,previous+2);}
        for(String invalid:List.of("{camera:{fov:{source:'event.damage'}}}","{camera:{position:1}}","{parts:{right_arm:{rotation:[1,2]}}}","{parts:{right_arm:{unknown:[1,2,3]}}}","{parts:{right_arm:{scale:[1,{source:'java:object'},1]}}}")) {
            boolean rejected=false;
            try {FormVisuals.validate(JsonParser.parseString(invalid).getAsJsonObject());}catch(RuntimeException ex){rejected=true;}
            helper.assertTrue(rejected,"Invalid presentation accepted: "+invalid);
        }
        helper.succeed();
    }
}
