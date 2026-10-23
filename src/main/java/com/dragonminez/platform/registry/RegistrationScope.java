package com.dragonminez.platform.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.function.Supplier;

/** Supplies the IDs now required by item/block constructors during deferred factory evaluation. */
public final class RegistrationScope {
    private static final ThreadLocal<Identifier> CURRENT = new ThreadLocal<>();
    private RegistrationScope() {}
    static <T> T withId(Identifier id, Supplier<T> factory) {
        Identifier previous = CURRENT.get();
        CURRENT.set(id);
        try { return factory.get(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
    public static Item.Properties itemProperties() {
        Item.Properties properties = new Item.Properties();
        Identifier id = CURRENT.get();
        return id != null ? properties.setId(ResourceKey.create(Registries.ITEM, id)) : properties;
    }
    public static BlockBehaviour.Properties blockProperties() { return withBlockId(BlockBehaviour.Properties.of()); }
    public static BlockBehaviour.Properties withBlockId(BlockBehaviour.Properties properties) {
        Identifier id = CURRENT.get();
        return id != null ? properties.setId(ResourceKey.create(Registries.BLOCK, id)) : properties;
    }
    public static ResourceKey<EntityType<?>> entityKey(String id) {
        Identifier scoped = CURRENT.get();
        Identifier identifier = id.contains(":") ? Identifier.parse(id)
                : Identifier.fromNamespaceAndPath(scoped != null ? scoped.getNamespace() : "dragonminez", id);
        return ResourceKey.create(Registries.ENTITY_TYPE, identifier);
    }
}
