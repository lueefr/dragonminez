package com.dragonminez.gametest;

import com.dragonminez.api.forms.FormAccess;
import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("dragonminez") @PrefixGameTestTemplate(false)
public final class MasteryNoticeGameTests {
    @GameTest(template="port_empty")
    public static void masteryNoticesRetainPerFormColorsWithoutLanguageFormatting(GameTestHelper helper) {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"MasteryNotice"));
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        var groups=ConfigManager.getAllFormsForRace("saiyan");
        var fixture=new com.google.gson.Gson().fromJson("{groupName:'notice_fixture',forms:{black:{name:'black',hairColor:'#000000'},blue:{name:'blue',hairColor:'#55CCFF'}}}",FormConfig.class);
        groups.put("notice_fixture",fixture);
        try {
            data.getCharacter().setRace("saiyan");
            var black=Component.translatable("race.dragonminez.saiyan.form.notice_fixture.black");
            var blue=Component.translatable("race.dragonminez.saiyan.form.notice_fixture.blue");
            Component message=Component.translatable("rules.dragonminez.use_denied",blue,
                Component.translatable("rules.dragonminez.mastery",black,100));
            helper.assertTrue(FormAccess.isMasteryNotice(message),"Nested mastery failure still opens a modal");
            helper.assertTrue(FormAccess.isMasteryNotice(Component.translatable("message.dragonminez.form.free_transform_mastery",100,blue)),"Legacy mastery refusal was not recognized");
            helper.assertTrue(!FormAccess.isMasteryNotice(Component.translatable("message.dragonminez.form.no_ki",50)),"Unrelated popup behavior changed");
            var colored=FormAccess.coloredNames(message,data);
            var args=((TranslatableContents)colored.getContents()).getArgs();
            helper.assertTrue(((Component)args[0]).getStyle().getColor().getValue()==0x55CCFF,"Target form lost its hair color");
            var required=((TranslatableContents)((Component)args[1]).getContents()).getArgs();
            helper.assertTrue(((Component)required[0]).getStyle().getColor().getValue()==0,"Black hair treated as missing color");
            helper.assertTrue(black.getStyle().getColor()==null,"Formatting mutated the original component");
            int builtIns=0;
            for(var group:groups.entrySet())if(!group.getKey().equals("notice_fixture"))for(var form:group.getValue().getForms().entrySet()) {
                var hex=form.getValue().getHairColor();
                if(hex==null||!hex.matches("#?[0-9a-fA-F]{6}"))continue;
                var key="race.dragonminez.saiyan.form."+group.getKey()+"."+form.getKey();
                helper.assertTrue(FormAccess.colorForKey(data,key)==Integer.parseInt(hex.replace("#",""),16),"Bundled form color lookup failed: "+key);builtIns++;
            }
            helper.assertTrue(builtIns>0,"No bundled forms were checked");helper.succeed();
        } finally {groups.remove("notice_fixture");player.discard();}
    }
}
