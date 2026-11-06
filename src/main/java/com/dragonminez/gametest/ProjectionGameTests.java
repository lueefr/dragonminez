package com.dragonminez.gametest;

import com.dragonminez.common.config.*;
import com.dragonminez.common.init.entities.ki.EnergyProjection;
import com.dragonminez.common.stats.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class ProjectionGameTests {
    @GameTest(template="port_empty")
    public static void energyAvatars(GameTestHelper helper) throws Exception {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"EnergyAvatars"));
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        var field=ConfigManager.class.getDeclaredField("runtime");field.setAccessible(true);var previous=field.get(null);
        var projections=new ArrayList<EnergyProjection>();var target=new Zombie(helper.getLevel());
        try {
            player.setPos(helper.absoluteVec(new Vec3(1.5,32,1.5)));data.getStatus().setHasCreatedCharacter(true);data.getStats().setKiPower(10);
            data.getResources().setCurrentEnergy(data.getMaxEnergy());data.getResources().setCurrentStamina(data.getMaxStamina());
            for(var block:net.minecraft.core.BlockPos.betweenClosed(player.blockPosition().offset(-4,0,-4),player.blockPosition().offset(4,5,4)))helper.getLevel().setBlockAndUpdate(block,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            field.set(null,RuntimeConfig.parse("""
                {projections:{'example:avatar':{when:{content:'example:avatar'},durationTicks:20,cooldownTicks:0,costs:{energy:0.1,energyPerTick:0.01},appearance:{scale:2,color:'#ABCDEF',opacity:0.4},attack:{radius:4,range:8,intervalTicks:1,factor:0.1,effects:[{id:'dragonminez:stun',ticks:10}]}}}}
                """));
            helper.assertTrue(!EnergyProjection.spawn(player,"example:avatar"),"Projection ignored activation conditions");
            com.dragonminez.api.content.ContentCompletionApi.complete(player,"example:avatar","projection-test");
            float energy=data.getResources().getCurrentEnergy();
            helper.assertTrue(EnergyProjection.spawn(player,"example:avatar"),"Configured projection did not spawn");
            helper.assertTrue(!EnergyProjection.spawn(player,"example:avatar"),"Repeated action stacked the same projection");
            projections.addAll(helper.getLevel().getEntitiesOfClass(EnergyProjection.class,player.getBoundingBox().inflate(16)));
            helper.assertTrue(projections.size()==1,"Projection entered the wrong level");var avatar=projections.getFirst();
            helper.assertTrue(avatar.owner()==player&&avatar.definition().appearance().color()==0xABCDEF&&data.getResources().getCurrentEnergy()<energy,"Projection owner/appearance/spawn cost lost");
            helper.assertTrue(!avatar.save(new net.minecraft.nbt.CompoundTag())&&!avatar.isPickable()&&!avatar.isPushable(),"Transient projection persisted or obstructed gameplay");
            target.setPos(player.position().add(2,0,0));target.setNoAi(true);helper.getLevel().addFreshEntity(target);
            float health=target.getHealth();avatar.tick();
            helper.assertTrue(target.getHealth()<health&&target.hasEffect(com.dragonminez.common.init.MainEffects.STUN),"Projection did not apply configured native damage/effect: health="+target.getHealth()+" base="+data.getStats().getKiPower()+" factor="+data.getTotalMultiplier("PWR"));
            target.removeAllEffects();target.invulnerableTime=0;health=target.getHealth();
            for(var block:net.minecraft.core.BlockPos.betweenClosed(player.blockPosition().offset(1,0,-2),player.blockPosition().offset(1,4,2)))helper.getLevel().setBlockAndUpdate(block,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            avatar.tick();helper.assertTrue(target.getHealth()==health&&!target.hasEffect(com.dragonminez.common.init.MainEffects.STUN),"Projection attacked through a wall");
            var before=avatar.position();player.setPos(player.position().add(0,0,1));avatar.tick();
            helper.assertTrue(avatar.position().distanceToSqr(before)>.5,"Projection stopped following its caster");
            data.getResources().setCurrentEnergy(0);avatar.tick();helper.assertTrue(avatar.isRemoved(),"Projection survived unpaid upkeep");
            data.getResources().setCurrentEnergy(data.getMaxEnergy());helper.assertTrue(EnergyProjection.spawn(player,"example:avatar"),"Removed projection retained its active slot");
            var next=helper.getLevel().getEntitiesOfClass(EnergyProjection.class,player.getBoundingBox().inflate(16)).stream().filter(value->!value.isRemoved()).findFirst().orElseThrow();projections.add(next);
            data.getStatus().setHasCreatedCharacter(false);next.tick();helper.assertTrue(next.isRemoved(),"Character reset retained an active projection");
            data.getStatus().setHasCreatedCharacter(true);data.getResources().setCurrentEnergy(data.getMaxEnergy());
            var limits=new com.google.gson.JsonObject();var definitions=new com.google.gson.JsonObject();
            for(int index=0;index<5;index++){var definition=new com.google.gson.JsonObject();definition.addProperty("cooldownTicks",0);definitions.add("example:limit_"+index,definition);}limits.add("projections",definitions);field.set(null,RuntimeConfig.parse(limits.toString()));
            for(int index=0;index<4;index++)helper.assertTrue(EnergyProjection.spawn(player,"example:limit_"+index),"Independent projection slots were not available");
            helper.assertTrue(!EnergyProjection.spawn(player,"example:limit_4"),"Projection exceeded the per-caster budget");
            projections.addAll(helper.getLevel().getEntitiesOfClass(EnergyProjection.class,player.getBoundingBox().inflate(16)));
            field.set(null,RuntimeConfig.empty());for(var projection:projections)if(!projection.isRemoved())projection.tick();
            helper.assertTrue(projections.stream().allMatch(EnergyProjection::isRemoved),"Removing configuration retained live avatars");
            for(String invalid:List.of("{durationTicks:0}","{costs:{energy:2}}","{appearance:{scale:0}}","{appearance:{offset:[1,2]}}","{attack:{intervalTicks:0}}","{attack:{stat:'all'}}","{attack:{targeting:'everyone'}}","{attack:{effects:[{id:'dragonminez:stun',ticks:0}]}}")) {
                boolean rejected=false;try{RuntimeConfig.parse("{projections:{'example:bad':"+invalid+"}}");}catch(RuntimeException ex){rejected=true;}
                helper.assertTrue(rejected,"Invalid projection accepted: "+invalid);
            }
            helper.succeed();
        } finally {projections.forEach(EnergyProjection::discard);target.discard();field.set(null,previous);player.discard();}
    }
}
