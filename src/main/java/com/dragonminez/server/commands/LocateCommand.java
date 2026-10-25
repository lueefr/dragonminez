package com.dragonminez.server.commands;

import com.dragonminez.Reference;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import com.dragonminez.server.world.dimension.OtherworldTournamentGrounds;
import com.dragonminez.server.world.structure.helper.StructureLocator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.stream.Stream;

public class LocateCommand {

	public static final String OTHERWORLD_TOURNAMENT = "otherworld_tournament";

	private static final SuggestionProvider<CommandSourceStack> SUGGESTIONS = (context, builder) ->
			SharedSuggestionProvider.suggest(
					Stream.concat(
							context.getSource().registryAccess().registryOrThrow(Registries.STRUCTURE).keySet().stream()
									.filter(id -> id.getNamespace().equals(Reference.MOD_ID))
									.map(ResourceLocation::getPath),
							Stream.of(OTHERWORLD_TOURNAMENT)),
					builder);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzlocate")
				.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.LOCATE))
                .then(Commands.literal("npc").then(Commands.argument("id", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                    .suggests((context,builder) -> SharedSuggestionProvider.suggest(com.dragonminez.api.npc.CustomNpcRegistry.INSTANCE.definitions().keySet(),builder))
                    .executes(context -> locateNpc(context.getSource(),net.minecraft.commands.arguments.ResourceLocationArgument.getId(context,"id").toString()))))
				.then(Commands.argument("structure", StringArgumentType.string())
						.suggests(SUGGESTIONS)
						.executes(context -> locateByName(context.getSource(), StringArgumentType.getString(context, "structure")))));
	}

    /** Checks loaded entities, then configured placements; never forces chunks to load. */
    public static int locateNpc(CommandSourceStack source,String id) {
        var registry=com.dragonminez.api.npc.CustomNpcRegistry.INSTANCE;
        if(registry.get(id).isEmpty()) {source.sendFailure(Component.translatable("command.dragonminez.locate.invalid",id));return 0;}
        record Found(ServerLevel level,BlockPos position) {}
        java.util.List<Found> loaded=new java.util.ArrayList<>(),placed=new java.util.ArrayList<>();
        for(var level:source.getServer().getAllLevels()) for(var entity:level.getAllEntities()) {
            if(entity instanceof com.dragonminez.common.init.entities.questnpc.CustomNpcEntity npc && npc.isAlive() && npc.getNpcId().equals(id)) loaded.add(new Found(level,npc.blockPosition()));
        }
        for(var placement:registry.placements().values()) if(placement.npc().equals(id)) {
            var key=ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(placement.dimension()));
            var level=source.getServer().getLevel(key);
            if(level!=null)placed.add(new Found(level,BlockPos.containing(placement.x(),placement.y(),placement.z())));
        }
        var sameLoaded=loaded.stream().filter(f->f.level()==source.getLevel()).toList();
        var samePlaced=placed.stream().filter(f->f.level()==source.getLevel()).toList();
        var candidates=!sameLoaded.isEmpty()?sameLoaded:!samePlaced.isEmpty()?samePlaced:!loaded.isEmpty()?loaded:placed;
        var found=candidates.stream().min(java.util.Comparator.comparingDouble(f->f.position().distToCenterSqr(source.getPosition()))).orElse(null);
        if(found==null){source.sendFailure(Component.translatable("command.dragonminez.locate.not_found",id));return 0;}
        return sendFound(source,found.level(),BlockPos.containing(source.getPosition()),found.position(),id+" ("+found.level().dimension().location()+")",found.position().getY());
    }

	private static int locateByName(CommandSourceStack source, String name) {
		ResourceLocation id = name.indexOf(':') >= 0
				? ResourceLocation.tryParse(name)
				: ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name);
		if (id != null && id.getNamespace().equals(Reference.MOD_ID) && id.getPath().equals(OTHERWORLD_TOURNAMENT)) {
			return locateOtherworldTournament(source);
		}
		if (id == null || !source.registryAccess().registryOrThrow(Registries.STRUCTURE).containsKey(id)) {
			source.sendFailure(Component.translatable("command.dragonminez.locate.invalid", name));
			return 0;
		}
		return locate(source, ResourceKey.create(Registries.STRUCTURE, id));
	}

	public static int locate(CommandSourceStack source, ResourceKey<Structure> structureKey) {
		ServerLevel level = source.getLevel();
		BlockPos from = BlockPos.containing(source.getPosition());
		BlockPos foundPos = StructureLocator.locateStructure(level, structureKey, from);
		String structName = structureKey.location().getPath();

		if (foundPos == null) {
			StructureLocator.SpawnHint hint = StructureLocator.spawnHint(source.getServer(), structureKey);
			source.sendFailure(hint == null
					? Component.translatable("command.dragonminez.locate.not_found", structName)
					: Component.translatable("command.dragonminez.locate.not_found_hint", structName, hint.dimensions(), hint.biomes()));
			return 0;
		}

		return sendFound(source, level, from, foundPos, structName, (int) source.getPosition().y);
	}

	private static int locateOtherworldTournament(CommandSourceStack source) {
		ServerLevel level = source.getLevel();
		if (!level.dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) {
			source.sendFailure(Component.translatable("command.dragonminez.locate.wrong_dimension"));
			return 0;
		}
		BlockPos ring = OtherworldTournamentGrounds.RING_CENTER;
		return sendFound(source, level, BlockPos.containing(source.getPosition()), ring, OTHERWORLD_TOURNAMENT, ring.getY());
	}

	private static int sendFound(CommandSourceStack source, ServerLevel level, BlockPos from, BlockPos foundPos,
								 String structName, int tpY) {
		int distance = StructureLocator.getDistanceTo(from, foundPos);
		Component coordComponent = ComponentUtils.wrapInSquareBrackets(
				net.minecraft.network.chat.Component.translatable("text.dragonminez.locatecommand.308ce7a605" , foundPos.getX(), foundPos.getZ())
		).withStyle(style -> style
				.withColor(ChatFormatting.GREEN)
				.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/execute in " + level.dimension().location() + " run tp @s " + foundPos.getX() + " " + tpY + " " + foundPos.getZ()))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, net.minecraft.network.chat.Component.translatable("text.dragonminez.locatecommand.d88fc33a08")))
		);

		source.sendSuccess(() -> Component.translatable(
				"command.dragonminez.locate.success",
				com.dragonminez.api.lang.ConfigTranslations.text(structName).withStyle(ChatFormatting.YELLOW),
				coordComponent,
				distance), false);

		return 1;
	}
}
