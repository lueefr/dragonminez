package com.dragonminez.api.content;

import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class ContentCompletionApi {
    private ContentCompletionApi() {}

    /** Server thread only. Call once after successful completion; repeated runs increment the count. */
    public static int complete(ServerPlayer player, String contentId, String kind) {
        ContentProgress.requireId(contentId);
        Objects.requireNonNull(kind, "kind");
        if (!player.serverLevel().getServer().isSameThread()) {
            throw new IllegalStateException("Content completion requires the server thread");
        }
        var data = StatsProvider.get(StatsCapability.INSTANCE, player).orElseThrow(IllegalStateException::new);
        int count = data.getContentProgress().complete(contentId);
        NeoForge.EVENT_BUS.post(new ContentCompletedEvent(player, contentId, kind, count));
        StatsSyncS2C.sendRequested(player);
        return count;
    }

    public static boolean hasCompleted(ServerPlayer player, String contentId) {
        ContentProgress.requireId(contentId);
        return StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(data -> data.getContentProgress().hasCompleted(contentId)).orElse(false);
    }

    /** Stable automatic ID for every quest, including legacy IDs outside resource-location syntax. */
    public static String questContentId(String questKey) {
        Objects.requireNonNull(questKey, "questKey");
        String path = questKey.replace(':', '/');
        if (path.length() > 230 || !path.matches("[a-z0-9_./-]+")) {
            path = "encoded/" + java.util.UUID.nameUUIDFromBytes(questKey.getBytes(StandardCharsets.UTF_8)).toString();
        }
        return ContentProgress.requireId("dragonminez:quest/" + path);
    }

    public static void questCompleted(ServerPlayer player, String questKey) {
        String automatic = questContentId(questKey);
        complete(player, automatic, "quest");
        var quest = QuestRegistry.getQuest(questKey);
        if (quest != null && quest.getCompletionId() != null && !quest.getCompletionId().equals(automatic)) {
            complete(player, quest.getCompletionId(), "quest");
        }
    }
}
