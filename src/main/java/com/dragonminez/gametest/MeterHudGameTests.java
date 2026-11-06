package com.dragonminez.gametest;

import com.dragonminez.common.config.RuntimeConfig;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class MeterHudGameTests {
    @GameTest(template="port_empty")
    public static void meterViews(GameTestHelper helper){
        String meters="\"meters\":{\"example:spirit\":{\"min\":-10,\"max\":90,\"initial\":0}}";
        var config=RuntimeConfig.parse("{"+meters+",\"hud\":[{\"meter\":\"example:spirit\",\"label\":\"hud.example.spirit\",\"anchor\":\"bottom_right\",\"width\":120,\"height\":8}]}");
        var bar=config.hud().getFirst();
        helper.assertTrue(bar.fraction(40)==.5 && bar.fraction(-20)==0 && bar.fraction(100)==1,"HUD did not normalize meter bounds");
        helper.assertTrue(bar.fraction(Double.NaN)==0,"Nonfinite HUD value accepted");
        int[] box=bar.bounds(100,40);
        helper.assertTrue(box[0]==0 && box[1]==24 && box[2]==100 && box[3]==8,"HUD escaped small-screen bounds");
        var restored=RuntimeConfig.parse(config.json()).hud().getFirst();
        helper.assertTrue(restored.meter().equals(bar.meter()) && restored.color()==bar.color(),"HUD changed on config synchronization");
        for(String invalid:java.util.List.of(
                "[{meter:'example:unknown'}]","[{meter:'example:spirit',width:0}]","[{meter:'example:spirit',x:0.5}]",
                "[{meter:'example:spirit',anchor:'invalid'}]","[{meter:'example:spirit',color:'red'}]",
                "[{meter:'example:spirit',when:{meter:{id:'example:unknown',min:1}}}]")){
            boolean failed=false;
            try{RuntimeConfig.parse("{"+meters+",hud:"+invalid+"}");}catch(RuntimeException ex){failed=true;}
            helper.assertTrue(failed,"Invalid HUD accepted: "+invalid);
        }
        helper.assertTrue(RuntimeConfig.empty().hud().isEmpty(),"Default config added unwanted HUD");
        helper.succeed();
    }
}
