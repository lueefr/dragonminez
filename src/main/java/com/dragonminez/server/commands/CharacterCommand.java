package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.combat.logic.player.KiChargeTracker;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.OpenCreationS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.progression.RebirthService;
import com.dragonminez.common.racial.RacialReset;
import com.dragonminez.common.stats.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Administrative character operations use the same authoritative data and reset services. */
public final class CharacterCommand {
    private CharacterCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dmzcharacter").requires(s -> s.hasPermission(2))
                .then(Commands.literal("reset")
                        .executes(c -> reset(c.getSource(), List.of(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(c -> reset(c.getSource(), EntityArgument.getPlayers(c,"targets"))))));
        dispatcher.register(Commands.literal("dmzreset").requires(s -> s.hasPermission(2))
                .executes(c -> reset(c.getSource(),List.of(c.getSource().getPlayerOrException())))
                .then(Commands.argument("targets",EntityArgument.players())
                        .executes(c -> reset(c.getSource(),EntityArgument.getPlayers(c,"targets")))));
        dispatcher.register(Commands.literal("dmzrace").requires(s -> s.hasPermission(2))
                .then(Commands.literal("set").then(Commands.argument("race",StringArgumentType.word())
                        .suggests((c,b) -> SharedSuggestionProvider.suggest(ConfigManager.getLoadedRaces(),b))
                        .executes(c -> race(c.getSource(),StringArgumentType.getString(c,"race"),List.of(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets",EntityArgument.players())
                                .executes(c -> race(c.getSource(),StringArgumentType.getString(c,"race"),EntityArgument.getPlayers(c,"targets")))))));
    }
    private static void stop(ServerPlayer player) {
        for(var attack:KiChargeTracker.owned(player)) attack.discard();
    }
    public static void reset(ServerPlayer player, StatsData data) {
        stop(player);
        RacialReset.reset(player,data);
        data.resetPlayerProgress(player,0,false,false);
        RebirthService.clearInventory(player);
        data.copyFrom(new StatsData(player));
        data.getStatus().setAlive(true);
        data.getStatus().setHasCreatedCharacter(false);
        player.refreshDimensions();
    }
    private static int reset(CommandSourceStack source,Collection<ServerPlayer> targets) {
        int count=0;
        for(var player:targets) {
            var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElse(null);
            if(data==null)continue;
            reset(player,data);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player),player);
            NetworkHandler.sendToPlayer(new OpenCreationS2C(),player);
            count++;
        }
        int total=count;
        source.sendSuccess(() -> Component.translatable("command.dragonminez.character.reset",total),true);
        return count;
    }
    public static boolean race(ServerPlayer player,StatsData data,String input) {
        String race=input.toLowerCase(Locale.ROOT);
        if(race.equals("namek")&&!ConfigManager.getLoadedRaces().contains(race))race="namekian";
        if(!ConfigManager.getLoadedRaces().contains(race))return false;
        if(race.equals(data.getCharacter().getRaceName()))return true;
        var config=ConfigManager.getRaceStats(race);
        if(config==null || config.getAllClasses().isEmpty())return false;
        float[] resources=data.snapshotMultiplierResources();
        stop(player);
        data.getCharacter().clearActiveForm(player);
        data.getCharacter().clearActiveStackForm(player);
        RacialReset.reset(player,data);
        data.getTechniques().clearTechniqueCharge();
        var character=data.getCharacter();
        character.setRace(race);
        if(!config.getAllClasses().contains(character.getCharacterClass()))
            character.setCharacterClass(new TreeSet<>(config.getAllClasses()).first());
        character.setSelectedFormGroup("");character.setSelectedForm("");
        character.setSelectedStackFormGroup("");character.setSelectedStackForm("");
        data.updateTransformationSkillLimits(race);
        data.restoreMultiplierGains(player,resources);
        player.refreshDimensions();
        return true;
    }
    private static int race(CommandSourceStack source,String race,Collection<ServerPlayer> targets) {
        int count=0;
        for(var player:targets) {
            var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElse(null);
            if(data!=null && race(player,data,race)) {
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player),player);count++;
            }
        }
        int total=count;
        if(count==0)source.sendFailure(Component.translatable("command.dragonminez.race.invalid",race));
        else source.sendSuccess(() -> Component.translatable("command.dragonminez.race.set",race,total),true);
        return count;
    }
}
