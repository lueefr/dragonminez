package com.dragonminez.platform.entity;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/** Persistent, entity-owned tags for combat, quests, raids, NPC placement and tournaments. */
public final class EntityData {
    private static final AttachmentType<State> TYPE = AttachmentRegistry.create(
            Identifier.parse("dragonminez:entity_data"), builder -> builder
                    .initializer(() -> new State(new CompoundTag()))
                    .persistent(State.CODEC).copyOnDeath());
    private EntityData() {}

    public static CompoundTag get(Entity entity) {
        AttachmentTarget target = (AttachmentTarget) entity;
        State state = target.getAttachedOrCreate(TYPE);
        // Fabric copies attachment values at respawn; mutable tags must not stay shared.
        if (state.owner != null && state.owner != entity) {
            state = new State(state.tag.copy());
            target.setAttached(TYPE, state);
        }
        state.owner = entity;
        return state.tag;
    }
    private static final class State {
        static final Codec<State> CODEC = CompoundTag.CODEC.xmap(State::new, state -> state.tag);
        final CompoundTag tag;
        Entity owner;
        State(CompoundTag tag) { this.tag = tag; }
    }
}
