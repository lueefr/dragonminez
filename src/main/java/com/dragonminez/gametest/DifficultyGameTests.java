package com.dragonminez.gametest;

import com.dragonminez.common.init.MainEntities;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class DifficultyGameTests {
    @GameTest(template="port_empty")
    public static void npcDamage(GameTestHelper helper)throws Exception {
        var server=helper.getLevel().getServer();var difficulty=helper.getLevel().getDifficulty();
        var player=new Player(helper.getLevel(),helper.absolutePos(net.minecraft.core.BlockPos.ZERO),0,new GameProfile(UUID.randomUUID(),"DifficultyCheck")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
        };
        var npc=MainEntities.CUSTOM_NPC.get().create(helper.getLevel());
        var zombie=EntityType.ZOMBIE.create(helper.getLevel());
        try {
            player.getAbilities().invulnerable=false;
            server.setDifficulty(Difficulty.NORMAL,true);
            player.setHealth(20);player.invulnerableTime=0;player.hurt(player.damageSources().mobAttack(npc),4);
            float normal=20-player.getHealth();
            server.setDifficulty(Difficulty.HARD,true);
            player.setHealth(20);player.invulnerableTime=0;player.hurt(player.damageSources().mobAttack(npc),4);
            float hard=20-player.getHealth();
            helper.assertTrue(normal>0&&Math.abs(hard-normal)<0.001,"DMZ NPC damage mismatch: normal="+normal+", hard="+hard);
            player.setHealth(20);player.invulnerableTime=0;player.hurt(player.damageSources().mobAttack(zombie),4);
            helper.assertTrue(20-player.getHealth()>hard,"Vanilla mob difficulty scaling was disabled");
            helper.succeed();
        } finally {server.setDifficulty(difficulty,true);player.discard();npc.discard();zombie.discard();}
    }
}
