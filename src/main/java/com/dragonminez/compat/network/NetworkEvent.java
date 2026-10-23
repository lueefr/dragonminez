package com.dragonminez.compat.network;

import net.minecraft.server.level.ServerPlayer;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/** Packet context backed by the receiving Fabric executor. */
public final class NetworkEvent {
    private NetworkEvent() {}

    public static final class Context {
        private final Executor executor;
        private final ServerPlayer sender;
        private final long receivedNanos;
        private boolean handled;

        public Context(ServerPlayer sender) {
            this(sender, sender == null ? Runnable::run : sender.level().getServer(), System.nanoTime());
        }
        public Context(ServerPlayer sender, Executor executor, long receivedNanos) {
            this.sender = sender;
            this.executor = Objects.requireNonNull(executor);
            this.receivedNanos = receivedNanos;
        }
        public void enqueueWork(Runnable task) { executor.execute(task); }
        public void setPacketHandled(boolean handled) { this.handled = handled; }
        public boolean isPacketHandled() { return handled; }
        public ServerPlayer getSender() { return sender; }
        /** Captured during decoding, before Fabric transfers handling to the game thread. */
        public long receivedNanos() { return receivedNanos; }
    }

    public static Supplier<Context> wrap(ServerPlayer sender, Executor executor, long receivedNanos) {
        Context context = new Context(sender, executor, receivedNanos);
        return () -> context;
    }
}
