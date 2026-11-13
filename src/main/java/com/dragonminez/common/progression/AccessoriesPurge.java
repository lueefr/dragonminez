package com.dragonminez.common.progression;

import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;

/** Optional native Accessories API; its cosmetic inventory is separate from the Curios bridge. */
public final class AccessoriesPurge {
    private AccessoriesPurge() {}
    public static void clear(LivingEntity player) {
        if(!net.neoforged.fml.ModList.get().isLoaded("accessories"))return;
        try {
            var type=Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
            Object capability=type.getMethod("get",LivingEntity.class).invoke(null,player);
            if(capability==null)return;
            Object raw=type.getMethod("getContainers").invoke(capability);
            if(raw instanceof Map<?,?> containers)for(Object container:containers.values()) {
                clear(container.getClass().getMethod("getAccessories").invoke(container));
                clear(container.getClass().getMethod("getCosmeticAccessories").invoke(container));
            }
            Object holder=type.getMethod("getHolder").invoke(capability);
            if(holder!=null) {
                Object invalid=holder.getClass().getField("invalidStacks").get(holder);
                if(invalid instanceof Collection<?> stacks)stacks.clear();
            }
        } catch(ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot clear native Accessories inventory",error instanceof InvocationTargetException invocation?invocation.getCause():error);
        }
    }
    private static void clear(Object inventory) {
        if(!(inventory instanceof Container container))throw new IllegalStateException("Unsupported Accessories inventory");
        for(int slot=0;slot<container.getContainerSize();slot++)container.setItem(slot,ItemStack.EMPTY);
        container.setChanged();
    }
}
