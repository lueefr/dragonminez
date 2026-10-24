package com.dragonminez.server.world.npc;

import com.dragonminez.Reference;
import com.dragonminez.api.npc.CustomNpcRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Custom placements are additive and never force distant chunks to load. */
@EventBusSubscriber(modid = Reference.MOD_ID)
public final class CustomNpcPlacements {
    private CustomNpcPlacements() {}

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 100 != 0) return;
        String dimension = level.dimension().location().toString();
        CustomNpcRegistry.INSTANCE.placements().forEach((key, placement) -> {
            if (!dimension.equals(placement.dimension())) return;
            BlockPos pos = BlockPos.containing(placement.x(), placement.y(), placement.z());
            if (!level.hasChunkAt(pos)) return;
            String identity = "dragonminez:custom_npc_placement/" + dimension + "/" + key;
            UUID uuid = UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
            if (level.getEntity(uuid) != null) return;
            // Set the persistent identity before adding to the level, so reloads cannot duplicate it.
            var definition = CustomNpcRegistry.INSTANCE.get(placement.npc()).orElse(null);
            if (definition == null) return;
            var npc = com.dragonminez.common.init.MainEntities.CUSTOM_NPC.get().create(level);
            if (npc == null) return;
            npc.setUUID(uuid);
            npc.setDefinition(definition);
            npc.moveTo(placement.x(), placement.y(), placement.z(), placement.yaw(), 0);
            npc.setHomePosition(placement.x(), placement.z());
            npc.setPersistenceRequired();
            level.addFreshEntity(npc);
        });
    }
}
