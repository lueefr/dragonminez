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
        var batches = PENDING.get();
        var batch = batches.get(player.getUUID());
        boolean root = batch == null;
        if (root) {
            batch = new Pending(!data.getContentProgress().hasCompleted(contentId) && !com.dragonminez.common.config.ConfigManager.getRulesConfig().rewards(contentId).isEmpty() ? data.save() : null, new java.util.ArrayList<>());
            batches.put(player.getUUID(), batch);
        }
        int count;
        try {
            count = data.getContentProgress().complete(contentId);
            if (count == 1) com.dragonminez.api.rules.Rewards.apply(player, data, contentId,
                    com.dragonminez.common.config.ConfigManager.getRulesConfig().rewards(contentId));
            batch.events().add(new ContentCompletedEvent(player, contentId, kind, count));
        } catch (RuntimeException ex) {
            if (root) {
                try { if(batch.before()!=null)data.load(batch.before()); } catch (ClassNotFoundException restore) { ex.addSuppressed(restore); }
                batches.remove(player.getUUID());
                if (batches.isEmpty()) PENDING.remove();
            }
            throw ex;
        }
        if (root) {
            batches.remove(player.getUUID());
            if (batches.isEmpty()) PENDING.remove();
            StatsSyncS2C.sendRequested(player);
            batch.events().forEach(NeoForge.EVENT_BUS::post);
        }
        return count;
    }

    private record Pending(net.minecraft.nbt.CompoundTag before,java.util.List<net.neoforged.bus.api.Event> events) {}
    private static final ThreadLocal<java.util.Map<java.util.UUID,Pending>> PENDING = ThreadLocal.withInitial(java.util.HashMap::new);
    /** Reward events are emitted only when the entire completion/reward chain succeeds. */
    public static void afterCommit(ServerPlayer player,net.neoforged.bus.api.Event event) {
        var batch=PENDING.get().get(player.getUUID());
        if(batch==null)NeoForge.EVENT_BUS.post(event);else batch.events().add(event);
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
