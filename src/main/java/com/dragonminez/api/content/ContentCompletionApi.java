package com.dragonminez.api.content;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

public final class ContentCompletionApi {
    private ContentCompletionApi() {}
    @FunctionalInterface
    public interface Callback { void completed(Completion event); }
    public record Completion(ServerPlayer player, String contentId, String kind, int timesCompleted) {
        public boolean firstCompletion() { return timesCompleted == 1; }
    }
    public static final Event<Callback> COMPLETED = EventFactory.createArrayBacked(Callback.class,
            callbacks -> event -> { for (Callback callback : callbacks) callback.completed(event); });

    /** Must be called on the server thread after the content's successful completion. */
    public static void complete(ServerPlayer player, String contentId, String kind) {
        ContentProgress.requireId(contentId);
        if (!player.level().getServer().isSameThread()) throw new IllegalStateException("Content completion requires the server thread");
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            int count = data.getContentProgress().complete(contentId);
            COMPLETED.invoker().completed(new Completion(player, contentId, kind, count));
            com.dragonminez.common.network.NetworkHandler.sendToPlayer(
                    new com.dragonminez.common.network.S2C.StatsSyncS2C(player), player);
        });
    }
    public static boolean hasCompleted(ServerPlayer player, String contentId) {
        return StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(data -> data.getContentProgress().hasCompleted(contentId)).orElse(false);
    }
}
