package com.dragonminez.api.content;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/** Posted on NeoForge.EVENT_BUS after a successful, persisted server-side completion. */
public final class ContentCompletedEvent extends Event {
    private final ServerPlayer player;
    private final String contentId;
    private final String kind;
    private final int timesCompleted;

    public ContentCompletedEvent(ServerPlayer player, String contentId, String kind, int timesCompleted) {
        this.player = player;
        this.contentId = contentId;
        this.kind = kind;
        this.timesCompleted = timesCompleted;
    }
    public ServerPlayer getPlayer() { return player; }
    public String getContentId() { return contentId; }
    public String getKind() { return kind; }
    public int getTimesCompleted() { return timesCompleted; }
    public boolean isFirstCompletion() { return timesCompleted == 1; }
}
