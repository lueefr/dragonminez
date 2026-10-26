package com.dragonminez.server.world.dimension;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.server.world.npc.NPCPlacementManager;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class OtherworldTournamentGrounds {

	public static final ResourceLocation SCHEMATIC = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "schematics/otherworld_tournament.schem");
	public static final int REVISION = 1;
	public static final BlockPos RING_CENTER = new BlockPos(0, 355, -1200);
	public static final Vec3 ARRIVAL = new Vec3(-25.5, 352, -1161.5);
	public static final float ARRIVAL_YAW = 180.0F;
	public static final Vec3 GRAND_KAI_RETURN = new Vec3(-30.5, 150, 10.5);
	public static final float GRAND_KAI_RETURN_YAW = 0.0F;
	public static final String ANNOUNCER_PLACEMENT_ID = "master_otherworld_announcer";

	private static final long TICK_BUDGET_NANOS = 4_000_000L;
	private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
	private static final int LOOKAHEAD_COLUMNS = 4;
	private static final TicketType<ChunkPos> BUILD_TICKET =
			TicketType.create(Reference.MOD_ID + "_tournament_build", Comparator.comparingLong(ChunkPos::toLong));

	private static Build active;
	private static ServerLevel failedLevel;

	private OtherworldTournamentGrounds() {
	}

    /** Worldgen placement has no SavedData, entity, ticket or active-player dependency. */
    public static boolean placeInGeneratedChunk(net.minecraft.world.level.WorldGenLevel world, ChunkPos chunk) {
        return OtherworldTournamentSchematic.load(world.getLevel().getServer().getResourceManager())
                .map(schematic -> schematic.place(world, chunk)).orElse(false);
    }

	public static boolean isBuilt(ServerLevel level) {
		return Data.get(level).revision >= REVISION;
	}

	public static BoundingBox protectedBounds(Level level) {
		if (!(level instanceof ServerLevel serverLevel) || !serverLevel.dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) return null;
		return Data.get(serverLevel).bounds;
	}

	public static boolean isProtected(Level level, BlockPos pos) {
		BoundingBox bounds = protectedBounds(level);
		return bounds != null && bounds.isInside(pos);
	}

	public static void tick(ServerLevel level) {
		if (!level.dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) return;
		if (!ConfigManager.getServerConfig().getWorldGen().getOtherworldActive()) return;
		Data data = Data.get(level);
		if (failedLevel == level) return;
		if (data.revision >= REVISION) {
			if (data.bounds == null) {
				Build built = Build.load(level);
				if (built == null) {
					failedLevel = level;
					return;
				}
				data.bounds = built.bounds;
				data.setDirty();
			}
			return;
		}
		if (level.players().isEmpty()) return;

		if (active == null || active.level != level) {
			active = Build.load(level);
			if (active == null) {
				failedLevel = level;
				return;
			}
			active.ticketedEnd = data.nextChunk;
			data.bounds = active.bounds;
			data.setDirty();
			LogUtil.info(Env.SERVER, "Otherworld tournament: building {} chunk column(s) around {}, starting at column {}",
					active.chunks.size(), RING_CENTER, data.nextChunk);
		}

		active.requestAhead(data.nextChunk);
		long deadline = System.nanoTime() + TICK_BUDGET_NANOS;
		while (System.nanoTime() < deadline) {
			if (data.nextChunk >= active.chunks.size()) {
				finish(level, data);
				return;
			}
			if (!active.isReady(data.nextChunk)) return;
			if (!active.place(data.nextChunk, deadline)) return;
			active.release(data.nextChunk);
			data.nextChunk++;
			data.setDirty();
			active.requestAhead(data.nextChunk);
		}
	}

	private static int buildPercent(ServerLevel level) {
		Build build = active;
		if (build == null || build.level != level || build.chunks.isEmpty()) return 0;
		return Math.min(99, Data.get(level).nextChunk * 100 / build.chunks.size());
	}

	private static void finish(ServerLevel level, Data data) {
		data.revision = REVISION;
		data.nextChunk = 0;
		data.setDirty();
		active = null;
		LogUtil.info(Env.SERVER, "Otherworld tournament: grounds built around {}", RING_CENTER);
		NPCPlacementManager.spawnForLevel(level);
	}

    /** Release unfinished build tickets and level references before a server closes. */
    public static void reset() {
        Build pending = active;
        active = null;
        failedLevel = null;
        if (pending != null) {
            int first = Data.get(pending.level).nextChunk;
            for (int i = first; i < pending.ticketedEnd; i++) pending.release(i);
        }
    }

	public static void teleportFromGrandKai(ServerPlayer player) {
		ServerLevel otherworld = player.server.getLevel(OtherworldDimension.OTHERWORLD_KEY);
		if (otherworld == null) return;
		if (!isBuilt(otherworld)) {
			player.displayClientMessage(Component.translatable("message.dragonminez.grandkai.tournament_not_ready", buildPercent(otherworld)), true);
			return;
		}
		player.teleportTo(otherworld, ARRIVAL.x, ARRIVAL.y, ARRIVAL.z, ARRIVAL_YAW, 0.0F);
	}

	public static void teleportBackToGrandKai(ServerPlayer player) {
		ServerLevel otherworld = player.server.getLevel(OtherworldDimension.OTHERWORLD_KEY);
		if (otherworld == null) return;
		player.teleportTo(otherworld, GRAND_KAI_RETURN.x, GRAND_KAI_RETURN.y, GRAND_KAI_RETURN.z, GRAND_KAI_RETURN_YAW, 0.0F);
	}

	private static final class Build {
		private final ServerLevel level;
		private final OtherworldTournamentSchematic schematic;
		private final int minY;
		private final BoundingBox bounds;
		private final List<OtherworldTournamentSchematic.ChunkColumn> chunks;
		private int ticketedEnd;
		private int columnCursor;


		private Build(ServerLevel level, OtherworldTournamentSchematic schematic) {
            this.level = level;
            this.schematic = schematic;
            this.bounds = schematic.bounds();
            this.minY = bounds.minY();
            this.chunks = schematic.chunks();
        }

		private static Build load(ServerLevel level) {
            return OtherworldTournamentSchematic.load(level.getServer().getResourceManager())
                    .map(schematic -> new Build(level, schematic)).orElse(null);
        }

		private void requestAhead(int from) {
			int end = Math.min(chunks.size(), from + LOOKAHEAD_COLUMNS);
			while (ticketedEnd < end) {
				ChunkPos pos = chunkPos(ticketedEnd++);
				level.getChunkSource().addRegionTicket(BUILD_TICKET, pos, 0, pos);
			}
		}

		private void release(int chunkIndex) {
			ChunkPos pos = chunkPos(chunkIndex);
			level.getChunkSource().removeRegionTicket(BUILD_TICKET, pos, 0, pos);
		}

		private boolean isReady(int chunkIndex) {
			OtherworldTournamentSchematic.ChunkColumn column = chunks.get(chunkIndex);
			return level.getChunkSource().getChunkNow(column.chunkX(), column.chunkZ()) != null;
		}

		private ChunkPos chunkPos(int chunkIndex) {
			OtherworldTournamentSchematic.ChunkColumn column = chunks.get(chunkIndex);
			return new ChunkPos(column.chunkX(), column.chunkZ());
		}

		private boolean place(int chunkIndex, long deadline) {
			OtherworldTournamentSchematic.ChunkColumn column = chunks.get(chunkIndex);
			int baseX = column.chunkX() << 4;
			int baseZ = column.chunkZ() << 4;
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			IntList entries = column.entries();
			while (columnCursor < entries.size()) {
				int packed = entries.getInt(columnCursor++);
				pos.set(baseX + (packed & 15), minY + (packed >>> 8 & 0x3FF), baseZ + (packed >>> 4 & 15));
				level.setBlock(pos, schematic.state(packed >>> 18), PLACE_FLAGS);
				if ((columnCursor & 511) == 0 && columnCursor < entries.size() && System.nanoTime() >= deadline) return false;
			}
			columnCursor = 0;
			return true;
		}
	}

	public static final class Data extends SavedData {
		private static final String FILE_NAME = "dragonminez_otherworld_tournament";

		private int revision;
		private int nextChunk;
		private BoundingBox bounds;

		public static Data get(ServerLevel level) {
			return level.getDataStorage().computeIfAbsent(new net.minecraft.world.level.saveddata.SavedData.Factory<>(Data::new, (tag, registries) -> Data.load(tag)), FILE_NAME);
		}

		public static Data load(CompoundTag tag) {
			Data data = new Data();
			data.revision = tag.getInt("Revision");
			data.nextChunk = tag.getInt("NextChunk");
			int[] bounds = tag.getIntArray("Bounds");
			if (bounds.length == 6) data.bounds = new BoundingBox(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
			return data;
		}

		@Override
		public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
			tag.putInt("Revision", revision);
			tag.putInt("NextChunk", nextChunk);
			if (bounds != null) {
				tag.putIntArray("Bounds", new int[]{bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()});
			}
			return tag;
		}
	}
}
