package com.dragonminez.compat.capabilities;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Compatibility shim for Forge AttachCapabilitiesEvent.
 * Providers are stored in-memory and resolved via entity-side lookups in StatsProvider helpers.
 */
public class AttachCapabilitiesEvent<T> extends Event {
	private final T object;
	private final Map<Identifier, ICapabilityProvider> caps = new HashMap<>();

	public AttachCapabilitiesEvent(T object) {
		this.object = object;
	}

	public T getObject() {
		return object;
	}

	public void addCapability(Identifier key, ICapabilityProvider cap) {
		caps.put(key, cap);
	}

	public Map<Identifier, ICapabilityProvider> getCapabilities() {
		return Collections.unmodifiableMap(caps);
	}
}
