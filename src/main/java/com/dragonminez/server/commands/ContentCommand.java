package com.dragonminez.server.commands;

import com.dragonminez.Reference;
import com.dragonminez.api.content.ContentCompletionApi;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Administrative bridge for configurable events implemented with command functions. */
@EventBusSubscriber(modid = Reference.MOD_ID)
public final class ContentCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dmzcontent").requires(source -> source.hasPermission(2))
                .then(Commands.literal("complete").then(Commands.argument("players", EntityArgument.players())
                        .then(Commands.argument("id", ResourceLocationArgument.id()).executes(context -> {
                            String id = ResourceLocationArgument.getId(context,"id").toString();
                            var players = EntityArgument.getPlayers(context,"players");
                            for (var player : players) ContentCompletionApi.complete(player,id,"command");
                            context.getSource().sendSuccess(() -> Component.translatable("command.dragonminez.content.completed",id,players.size()),true);
                            return players.size();
                        }))))
                .then(Commands.literal("has").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("id", ResourceLocationArgument.id()).executes(context ->
                                ContentCompletionApi.hasCompleted(EntityArgument.getPlayer(context,"player"),
                                        ResourceLocationArgument.getId(context,"id").toString()) ? 1 : 0)))));
    }
}
