package com.dragonminez.common.util;

import com.dragonminez.compat.DistExecutor;
import net.minecraft.core.RegistryAccess;
import net.fabricmc.api.EnvType;
import com.dragonminez.platform.ServerLifecycleHooks;

/** Resolves dynamic registries on the side currently materializing a configured item. */
public final class RuntimeRegistries {
    private RuntimeRegistries() {}

    public static RegistryAccess access() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isSameThread()) return server.registryAccess();
        RegistryAccess client = DistExecutor.unsafeCallWhenOn(EnvType.CLIENT, () -> ClientAccess::get);
        return client != null ? client : server != null ? server.registryAccess() : RegistryAccess.EMPTY;
    }

    private static final class ClientAccess {
        private static RegistryAccess get() {
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            return connection == null ? null : connection.registryAccess();
        }
    }
}
