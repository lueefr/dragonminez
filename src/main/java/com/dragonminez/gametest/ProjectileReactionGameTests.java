package com.dragonminez.gametest;

import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.dragonminez.server.events.players.combat.ReactionEvents;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class ProjectileReactionGameTests {
    @GameTest(template="port_empty")
    public static void projectileResponses(GameTestHelper helper) throws Exception {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"ProjectileRules"));
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        var field=ConfigManager.class.getDeclaredField("runtime");field.setAccessible(true);var original=field.get(null);
        try{
            data.getStatus().setHasCreatedCharacter(true);player.setPos(helper.absoluteVec(new Vec3(1.5,3,1.5)));
            data.getResources().setCurrentEnergy(data.getMaxEnergy());data.getResources().setCurrentStamina(data.getMaxStamina());
            var attacker=new Zombie(helper.getLevel());attacker.setPos(player.position().add(3,0,0));
            var arrow=EntityType.ARROW.create(helper.getLevel());arrow.setOwner(attacker);arrow.setPos(player.position());arrow.setDeltaMovement(-1,0,0);
            field.set(null,RuntimeConfig.parse("{reactions:[{id:'example:destroy',response:'destroy',attacks:['projectile'],hitChance:0,energyCost:0.1,staminaCost:0,cooldownTicks:0}]}"));
            float energy=data.getResources().getCurrentEnergy();
            var hit=new net.neoforged.neoforge.event.entity.ProjectileImpactEvent(arrow,new EntityHitResult(player));
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(hit);
            helper.assertTrue(hit.isCanceled()&&arrow.isRemoved(),"Projectile impact did not destroy incoming arrow");
            helper.assertTrue(data.getResources().getCurrentEnergy()<energy,"Destruction did not pay its resource cost");
            field.set(null,RuntimeConfig.parse("{reactions:[{id:'example:deflect',response:'deflect',attacks:['projectile'],projectiles:['minecraft:arrow'],hitChance:0,staminaCost:0,cooldownTicks:0}]}"));
            var reflected=EntityType.ARROW.create(helper.getLevel());reflected.setOwner(attacker);reflected.setPos(player.position());reflected.setDeltaMovement(-1,0,0);
            helper.assertTrue(ConfigManager.getRuntime().reactions().select(data,"projectile",reflected)!=null,"Deflection type filter did not select arrow");
            var clear=net.minecraft.core.BlockPos.containing(player.position());
            for(var block:net.minecraft.core.BlockPos.betweenClosed(clear.offset(-1,0,-1),clear.offset(3,3,1)))helper.getLevel().setBlockAndUpdate(block,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            boolean changed=ReactionEvents.intercept(player,reflected);
            helper.assertTrue(changed&&reflected.getOwner()==player&&reflected.getDeltaMovement().x>0,"Deflection failed: result="+changed+" motion="+reflected.getDeltaMovement()+" box="+player.getBoundingBox()+" player="+player.position());
            var method=net.minecraft.world.entity.projectile.Projectile.class.getDeclaredMethod("canHitEntity",net.minecraft.world.entity.Entity.class);method.setAccessible(true);
            helper.assertTrue(!(boolean)method.invoke(reflected,player),"Deflected projectile could immediately hit its new owner");
            var snowball=EntityType.SNOWBALL.create(helper.getLevel());snowball.setOwner(attacker);snowball.setDeltaMovement(1,0,0);
            helper.assertTrue(!ReactionEvents.intercept(player,snowball),"Projectile type filter was ignored");
            var packet=new com.dragonminez.common.network.S2C.ProjectileMotionS2C(reflected.getId(),reflected.getUUID(),player.getId(),player.getUUID(),player.level().dimension().location(),reflected.position(),reflected.getDeltaMovement());
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try{packet.encode(buffer);helper.assertTrue(new com.dragonminez.common.network.S2C.ProjectileMotionS2C(buffer).equals(packet),"Deflection motion packet changed on wire");}finally{buffer.release();}
            field.set(null,RuntimeConfig.parse("{reactions:[{id:'example:ki_destroy',response:'destroy',attacks:['projectile'],hitChance:0,staminaCost:0,cooldownTicks:0}]}"));
            var ki=com.dragonminez.common.init.MainEntities.KI_BLAST.get().create(helper.getLevel());ki.setup(attacker,8,.5f,1,0,0);ki.setFiring(true);
            float health=player.getHealth();
            helper.assertTrue(!ki.applyDamageOrHeal(player,8)&&ki.isRemoved()&&player.getHealth()==health,"Native Ki interception left damage behind");
            var victim=new Zombie(helper.getLevel());float life=victim.getHealth();ki.applyDamageOrHeal(victim,8);
            helper.assertTrue(victim.getHealth()==life,"Destroyed Ki projectile damaged another target in the same tick");
            for(String invalid:List.of("{response:'destroy',attacks:['melee']}","{response:'deflect',attacks:['projectile'],counterFactor:1}")){
                boolean rejected=false;
                try{RuntimeConfig.parse("{reactions:[{id:'example:invalid',hitChance:0,"+invalid.substring(1)+"]}");}catch(RuntimeException ex){rejected=true;}
                helper.assertTrue(rejected,"Invalid projectile policy accepted");
            }
            helper.succeed();
        }finally{field.set(null,original);player.discard();}
    }
}
