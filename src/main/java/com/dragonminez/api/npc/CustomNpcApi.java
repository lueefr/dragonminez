package com.dragonminez.api.npc;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.questnpc.CustomNpcEntity;
import net.minecraft.server.level.ServerLevel;

/** Server-side spawn entry point for the additive JSON NPC system. */
public final class CustomNpcApi {
    private CustomNpcApi() {}

    public static CustomNpcEntity spawn(ServerLevel level, String id, double x, double y, double z, float yaw) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("NPC spawning requires the server thread");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw))
            throw new IllegalArgumentException("NPC position must be finite");
        var definition = CustomNpcRegistry.INSTANCE.get(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown custom NPC: " + id));
        var entity = MainEntities.CUSTOM_NPC.get().create(level);
        if (entity == null) throw new IllegalStateException("Could not create custom NPC: " + id);
        entity.setDefinition(definition);
        entity.moveTo(x, y, z, yaw, 0);
        entity.setHomePosition(x, z);
        entity.setPersistenceRequired();
        if (!level.addFreshEntity(entity)) throw new IllegalStateException("Could not spawn custom NPC: " + id);
        return entity;
    }
}
