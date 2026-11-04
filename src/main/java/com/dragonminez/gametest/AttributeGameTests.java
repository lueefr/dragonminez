package com.dragonminez.gametest;

import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.dragonminez.api.content.ContentCompletionApi;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class AttributeGameTests {
    @GameTest(template="port_empty")
    public static void basePotential(GameTestHelper helper)throws Exception {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"AttributeBudget"));
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        var field=ConfigManager.class.getDeclaredField("attributes");field.setAccessible(true);var original=field.get(null);
        var config=AttributeConfig.parse("""
                {"attributes":{"example:potential":{"when":{"content":"example:potential"},"base":{"all":1.5},"tp":1.5}}}
                """);
        boolean rejected=false;
        try{AttributeConfig.parse("{\"attributes\":{\"example:bad\":{\"base\":{\"STR\":-1}}}}");}catch(IllegalArgumentException ex){rejected=true;}
        helper.assertTrue(rejected,"Negative attribute multiplier was accepted");
        field.set(null,config);
        try {
            data.getStatus().setHasCreatedCharacter(true);
            int level=data.getLevel();int strength=data.getStats().getStrength();
            double before=data.getTotalMultiplier("STR");
            ContentCompletionApi.complete(player,"example:potential","test");
            helper.assertTrue(data.getTotalMultiplier("STR")==before*1.5,"Potential did not multiply the base");
            helper.assertTrue(data.getLevel()==level&&data.getStats().getStrength()==strength,"Potential changed purchased stats or progression");
            data.getResources().setTrainingPointsExact(0);data.getResources().addTrainingPoints(100,false,true);
            helper.assertTrue(data.getResources().getTrainingPointsExact()==150,"Configured attribute TP bonus did not apply");
            data.getCharacter().setRace("human");
            data.getCharacter().setRace("saiyan");
            helper.assertTrue(data.getRebirth().originRace().equals("human")&&data.getRebirth().previousRace().equals("human"),"Race origin was not retained");
            var recursive=AttributeConfig.parse("{\"attributes\":{\"example:health\":{\"when\":{\"healthPercent\":{\"min\":0}},\"base\":{\"all\":1.5}}}}");
            field.set(null,recursive);
            helper.assertTrue(Double.isFinite(data.getMaxHealth()),"Health condition caused recursive attribute evaluation");
            helper.succeed();
        } finally {field.set(null,original);player.discard();}
    }
}
