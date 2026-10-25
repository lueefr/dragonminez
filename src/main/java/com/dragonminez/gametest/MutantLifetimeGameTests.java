package com.dragonminez.gametest;

import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.dragonminez.server.util.MutantManager;
import com.dragonminez.server.world.data.MutantSavedData;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class MutantLifetimeGameTests {
    private static void set(Object value,String field,Object replacement)throws Exception{
        var member=value.getClass().getDeclaredField(field);member.setAccessible(true);member.set(value,replacement);
    }
    @GameTest(template="port_empty")
    public static void singleAttemptSurvivesResetsAndOnlyExplicitRevocationRemovesOwnership(GameTestHelper helper)throws Exception{
        var player=new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),new GameProfile(UUID.randomUUID(),"MutantLifetime")){
            @Override public boolean hasPermissions(int level){return level<=4;}
        };
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        data.getStatus().setHasCreatedCharacter(true);
        var config=ConfigManager.getServerConfig();var original=config.getMutant();var test=new GeneralServerConfig.MutantConfig();
        set(test,"maxHolders",Integer.MAX_VALUE);set(test,"chance",0d);set(config,"mutant",test);
        try{
            helper.assertTrue(!test.getRetryEnabled(),"Retry must be disabled by default");
            MutantManager.rollForPlayer(player,data);
            var saved=MutantSavedData.get(helper.getLevel().getServer());
            helper.assertTrue(saved.hasAttempted(player.getUUID())&&!saved.isHolder(player.getUUID()),"Failed initial attempt was not persisted");
            set(test,"chance",1d);MutantManager.rollForPlayer(player,data);MutantManager.runLottery(helper.getLevel().getServer());
            helper.assertTrue(!saved.isHolder(player.getUUID()),"Initial attempt or default periodic lottery rerolled a failed player");
            MutantManager.grant(player,data);
            data.getEffects().removeEffect("mutant");data.getEffects().removeAllEffects();data.getEffects().clear();
            helper.assertTrue(data.getEffects().hasEffect("mutant"),"Normal removal/reset stripped permanent ownership");
            var stale=data.getEffects().save();
            data.getEffects().load(new net.minecraft.nbt.CompoundTag());
            MutantManager.reconcileHolder(player,data);
            helper.assertTrue(data.getEffects().hasEffect("mutant"),"World ownership did not restore recreated character effects");
            var copy=MutantSavedData.load(saved.save(new net.minecraft.nbt.CompoundTag(),helper.getLevel().registryAccess()),helper.getLevel().registryAccess());
            helper.assertTrue(copy.isHolder(player.getUUID())&&copy.hasAttempted(player.getUUID()),"Lifetime ownership/attempt did not persist");
            var source=player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
            var commands=new com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>();
            com.dragonminez.server.commands.EffectsCommand.register(commands);
            commands.execute("dmzeffect remove mutant",source);
            helper.assertTrue(saved.isHolder(player.getUUID()),"Removal happened without confirmation");
            commands.execute("dmzeffect confirm_mutant",source);
            helper.assertTrue(!saved.isHolder(player.getUUID()),"Confirmed administrative removal failed");
            data.getEffects().load(stale);MutantManager.reconcileHolder(player,data);
            helper.assertTrue(!saved.isHolder(player.getUUID())&&!data.getEffects().hasEffect("mutant"),"Stale effect resurrected explicitly revoked ownership");
            helper.succeed();
        }finally{MutantManager.revoke(player,data);set(config,"mutant",original);player.discard();}
    }
}
