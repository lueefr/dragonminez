package com.dragonminez.platform.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TypedEntityData;
import java.util.List;
import java.util.function.Supplier;

/** Native spawn egg whose entity component is resolved after registry construction. */
public final class DeferredSpawnEggItem extends SpawnEggItem {
    private final int primaryColor, secondaryColor;
    public DeferredSpawnEggItem(Supplier<? extends EntityType<?>> entity, int primary, int secondary, Properties properties) {
        super(properties.delayedComponent(DataComponents.ENTITY_DATA,
                        registries -> TypedEntityData.of(entity.get(), new CompoundTag()))
                .component(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(),
                        List.of(0xFF000000 | primary, 0xFF000000 | secondary))));
        primaryColor = primary;
        secondaryColor = secondary;
    }
    public int primaryColor() { return primaryColor; }
    public int secondaryColor() { return secondaryColor; }
}
