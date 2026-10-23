package com.dragonminez.compat.network.simple;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import com.dragonminez.compat.network.NetworkDirection;
import com.dragonminez.compat.network.NetworkEvent;
import com.dragonminez.platform.ServerLifecycleHooks;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import com.dragonminez.platform.network.ClientNetworking;
import com.dragonminez.platform.network.ProtocolHandshake;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Preserves the existing packet codecs and handlers using native Fabric payload registration.
 */
public final class SimpleChannel {
	private static final List<SimpleChannel> CHANNELS = new ArrayList<>();

	private final Identifier name;
    private final Supplier<String> protocolVersion;
    private final java.util.function.Predicate<String> clientAccepted;
    private final java.util.function.Predicate<String> serverAccepted;
	private final List<PendingRegistration<?>> pending = new ArrayList<>();
	private final Map<Class<?>, PayloadBinding<?>> bindings = new ConcurrentHashMap<>();
	private boolean registered;

	public SimpleChannel(Identifier name, Supplier<String> protocolVersion) {
        this(name, protocolVersion, remote -> protocolVersion.get().equals(remote), remote -> protocolVersion.get().equals(remote));
    }

    public SimpleChannel(Identifier name, Supplier<String> protocolVersion,
            java.util.function.Predicate<String> clientAccepted, java.util.function.Predicate<String> serverAccepted) {
		this.name = name;
        this.protocolVersion = protocolVersion;
        this.clientAccepted = clientAccepted;
        this.serverAccepted = serverAccepted;
		CHANNELS.add(this);
	}

	public <MSG> MessageBuilder<MSG> messageBuilder(Class<MSG> type, int id, NetworkDirection direction) {
		return new MessageBuilder<>(type, id, direction);
	}

	@SuppressWarnings("unchecked")
	public <MSG> void sendToServer(MSG message) {
		PayloadBinding<MSG> binding = (PayloadBinding<MSG>) bindings.get(message.getClass());
		if (binding == null) {
			throw new IllegalStateException("No payload binding for " + message.getClass().getName());
		}
		ClientNetworking.send(binding.wrap(message));
	}

	@SuppressWarnings("unchecked")
	public <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
		PayloadBinding<MSG> binding = (PayloadBinding<MSG>) bindings.get(message.getClass());
		if (binding == null) {
			throw new IllegalStateException("No payload binding for " + message.getClass().getName());
		}
		ServerPlayNetworking.send(player, binding.wrap(message));
	}

	@SuppressWarnings("unchecked")
	public <MSG> void sendToAllPlayers(MSG message) {
		PayloadBinding<MSG> binding = (PayloadBinding<MSG>) bindings.get(message.getClass());
		if (binding == null) {
			throw new IllegalStateException("No payload binding for " + message.getClass().getName());
		}
		var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer player : PlayerLookup.all(server)) ServerPlayNetworking.send(player, binding.wrap(message));
	}

	@SuppressWarnings("unchecked")
	public <MSG> void sendToTrackingEntityAndSelf(MSG message, Entity entity) {
		PayloadBinding<MSG> binding = (PayloadBinding<MSG>) bindings.get(message.getClass());
		if (binding == null) {
			throw new IllegalStateException("No payload binding for " + message.getClass().getName());
		}
		var recipients = new java.util.HashSet<>(PlayerLookup.tracking(entity));
        if (entity instanceof ServerPlayer self) recipients.add(self);
        for (ServerPlayer player : recipients) ServerPlayNetworking.send(player, binding.wrap(message));
	}

	@SuppressWarnings("unchecked")
	public <MSG> void sendToTrackingEntity(MSG message, Entity entity) {
		PayloadBinding<MSG> binding = (PayloadBinding<MSG>) bindings.get(message.getClass());
		if (binding == null) {
			throw new IllegalStateException("No payload binding for " + message.getClass().getName());
		}
		for (ServerPlayer player : PlayerLookup.tracking(entity)) ServerPlayNetworking.send(player, binding.wrap(message));
	}

    public void register() {
        if (registered) return;
        for (PendingRegistration<?> reg : pending) reg.register(name, bindings);
        ProtocolHandshake.register(name, protocolVersion, serverAccepted);
        registered = true;
    }

    public static void registerClientReceivers() {
        for (SimpleChannel channel : List.copyOf(CHANNELS)) {
            if (!channel.registered) throw new IllegalStateException("Channel not registered: " + channel.name);
            for (PendingRegistration<?> reg : channel.pending) reg.registerClient(channel.name);
            ProtocolHandshake.registerClient(channel.name, channel.protocolVersion, channel.clientAccepted);
        }
    }

	public final class MessageBuilder<MSG> {
		private final Class<MSG> type;
		private final int id;
		private final NetworkDirection direction;
        private boolean networkThread;
		private BiConsumer<MSG, FriendlyByteBuf> encoder;
		private Function<FriendlyByteBuf, MSG> decoder;
		private BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer;

		MessageBuilder(Class<MSG> type, int id, NetworkDirection direction) {
			this.type = type;
			this.id = id;
			this.direction = direction;
		}

		public MessageBuilder<MSG> encoder(BiConsumer<MSG, FriendlyByteBuf> encoder) {
			this.encoder = encoder;
			return this;
		}

		public MessageBuilder<MSG> decoder(Function<FriendlyByteBuf, MSG> decoder) {
			this.decoder = decoder;
			return this;
		}

		public MessageBuilder<MSG> consumerMainThread(BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer) {
			this.consumer = consumer;
			return this;
		}

        public MessageBuilder<MSG> consumerNetworkThread(BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer) {
            this.consumer = consumer;
            this.networkThread = true;
            return this;
        }

        public void add() {
            if (registered) throw new IllegalStateException("Channel already registered: " + name);
            if (encoder == null || decoder == null || consumer == null) throw new IllegalStateException("Incomplete packet: " + type.getName());
            if (pending.stream().anyMatch(reg -> reg.id() == id || reg.type() == type)) throw new IllegalArgumentException("Duplicate packet ID or class: " + id + " / " + type.getName());
			pending.add(new PendingRegistration<>(type, id, direction, encoder, decoder, consumer, networkThread));
		}
	}

	private record PendingRegistration<MSG>(
			Class<MSG> type,
			int id,
			NetworkDirection direction,
			BiConsumer<MSG, FriendlyByteBuf> encoder,
			Function<FriendlyByteBuf, MSG> decoder,
			BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer,
            boolean networkThread
	) {
        private CustomPacketPayload.Type<WrappedPayload<MSG>> payloadType(Identifier channelName) {
            return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(channelName.getNamespace(),
                    channelName.getPath() + "/" + id + "_" + type.getSimpleName().toLowerCase(java.util.Locale.ROOT)));
        }

        void register(Identifier channelName, Map<Class<?>, PayloadBinding<?>> bindings) {
            var payloadType = payloadType(channelName);
            StreamCodec<RegistryFriendlyByteBuf, WrappedPayload<MSG>> codec = StreamCodec.of(
                    (buf, wrapped) -> encoder.accept(wrapped.message(), buf),
                    buf -> new WrappedPayload<>(payloadType, decoder.apply(buf), System.nanoTime()));
            if (direction == NetworkDirection.PLAY_TO_SERVER) {
                PayloadTypeRegistry.serverboundPlay().register(payloadType, codec);
                ServerPlayNetworking.registerGlobalReceiver(payloadType, (payload, context) ->
                        consumer.accept(payload.message(), NetworkEvent.wrap(context.player(), context.server(), payload.receivedNanos())));
            } else {
                PayloadTypeRegistry.clientboundPlay().register(payloadType, codec);
            }
            bindings.put(type, new PayloadBinding<>(payloadType));
        }

        void registerClient(Identifier channelName) {
            if (direction == NetworkDirection.PLAY_TO_CLIENT) ClientNetworking.register(payloadType(channelName), consumer);
        }
    }

	private record PayloadBinding<MSG>(CustomPacketPayload.Type<WrappedPayload<MSG>> payloadType) {
		WrappedPayload<MSG> wrap(MSG message) {
			return new WrappedPayload<>(payloadType, message, System.nanoTime());
		}
	}

	public record WrappedPayload<MSG>(CustomPacketPayload.Type<WrappedPayload<MSG>> payloadType, MSG message, long receivedNanos) implements CustomPacketPayload {
		@Override
		public Type<? extends CustomPacketPayload> type() {
			return payloadType;
		}
	}
}
