package com.dragonminez.server.commands;

import com.dragonminez.Reference;
import com.dragonminez.api.npc.CustomNpcApi;
import com.dragonminez.api.npc.CustomNpcRegistry;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Reference.MOD_ID)
public final class CustomNpcCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dmzcustomnpc")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        CustomNpcRegistry.INSTANCE.definitions().keySet(), builder))
                                .executes(context -> {
                                    var source = context.getSource();
                                    var pos = source.getPosition();
                                    CustomNpcApi.spawn(source.getLevel(), ResourceLocationArgument.getId(context, "id").toString(),
                                            pos.x, pos.y, pos.z, source.getRotation().y);
                                    return 1;
                                })
                                .then(Commands.argument("pos", Vec3Argument.vec3()).executes(context -> {
                                    var source = context.getSource();
                                    var pos = Vec3Argument.getVec3(context, "pos");
                                    CustomNpcApi.spawn(source.getLevel(), ResourceLocationArgument.getId(context, "id").toString(),
                                            pos.x, pos.y, pos.z, source.getRotation().y);
                                    return 1;
                                })))));
    }
}
