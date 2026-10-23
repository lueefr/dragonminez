package com.dragonminez.compat.network;

import net.minecraft.resources.Identifier;
import com.dragonminez.compat.network.simple.SimpleChannel;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Compatibility shim for Forge NetworkRegistry.ChannelBuilder.
 */
public final class NetworkRegistry {
	private NetworkRegistry() {}

	public static ChannelBuilder newSimpleChannel(Identifier name, Supplier<String> networkProtocolVersion,
												  Predicate<String> clientAcceptedVersions, Predicate<String> serverAcceptedVersions) {
		return ChannelBuilder.named(name)
				.networkProtocolVersion(networkProtocolVersion)
				.clientAcceptedVersions(clientAcceptedVersions)
				.serverAcceptedVersions(serverAcceptedVersions);
	}

	public static final class ChannelBuilder {
		private final Identifier name;
        private Supplier<String> version = () -> "1";

		private ChannelBuilder(Identifier name) {
			this.name = name;
		}

		public static ChannelBuilder named(Identifier name) {
			return new ChannelBuilder(name);
		}

		public ChannelBuilder networkProtocolVersion(Supplier<String> version) {
            this.version = version;
			return this;
		}

		public ChannelBuilder clientAcceptedVersions(Predicate<String> accepted) {
			return this;
		}

		public ChannelBuilder serverAcceptedVersions(Predicate<String> accepted) {
			return this;
		}

		public SimpleChannel simpleChannel() {
			return new SimpleChannel(name, version);
		}
	}
}
