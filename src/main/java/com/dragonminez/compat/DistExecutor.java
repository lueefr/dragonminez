package com.dragonminez.compat;

import net.fabricmc.api.EnvType;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Compatibility shim for Forge DistExecutor.
 */
public final class DistExecutor {
	private DistExecutor() {}

	public static void unsafeRunWhenOn(EnvType dist, Supplier<Runnable> toRun) {
		if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == dist) {
			toRun.get().run();
		}
	}

	public static void runWhenOn(EnvType dist, Supplier<Runnable> toRun) {
		unsafeRunWhenOn(dist, toRun);
	}

	public static <T> T unsafeCallWhenOn(EnvType dist, Supplier<Callable<T>> toRun) {
		if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == dist) {
			try {
				return toRun.get().call();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	public static <T> T callWhenOn(EnvType dist, Supplier<Callable<T>> toRun) {
		return unsafeCallWhenOn(dist, toRun);
	}
}
