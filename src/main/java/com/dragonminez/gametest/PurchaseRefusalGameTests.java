package com.dragonminez.gametest;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.stats.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class PurchaseRefusalGameTests {
    private static String key(Component value){return value!=null&&value.getContents() instanceof TranslatableContents t?t.getKey():"";}
    @GameTest(template="port_empty")
    public static void refusalsExplainFailuresWithoutDebitingOrGranting(GameTestHelper helper){
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"PurchaseRefusal"));
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        data.getCharacter().setRace("human");data.getResources().setTrainingPointsExact(0);
        data.getSkills().registerDefaultSkill("jump",50);
        var purchase=new UpdateSkillC2S(UpdateSkillC2S.SkillAction.PURCHASE,"jump",-100);
        data.getStatus().setFusionPartnerUUID(UUID.randomUUID());
        helper.assertTrue(key(purchase.apply(player,data)).equals("message.dragonminez.fusion.action_blocked"),"Fusion refusal missing");
        data.getStatus().setFusionPartnerUUID(null);
        helper.assertTrue(key(purchase.apply(player,data)).equals("rules.dragonminez.purchase.master"),"Missing-master refusal missing");
        var upgrade=new UpdateSkillC2S(UpdateSkillC2S.SkillAction.UPGRADE,"jump",-100);
        helper.assertTrue(key(upgrade.apply(player,data)).equals("rules.dragonminez.purchase.tp"),"Insufficient-TP refusal missing");
        helper.assertTrue(data.getResources().getTrainingPointsExact()==0&&data.getSkills().getSkillLevel("jump")==0,"Refused request mutated progression");
        int maximum=ConfigManager.getSkillsConfig().getSkillCosts("jump").getCosts().size();
        data.getSkills().setSkillLevel("jump",maximum);
        helper.assertTrue(key(upgrade.apply(player,data)).equals("rules.dragonminez.purchase.maximum"),"Maximum-level refusal missing");
        helper.assertTrue(key(purchase.apply(player,data)).equals("rules.dragonminez.purchase.owned"),"Already-owned refusal missing");
        player.discard();helper.succeed();
    }
}
