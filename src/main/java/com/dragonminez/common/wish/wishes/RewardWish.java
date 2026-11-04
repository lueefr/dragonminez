package com.dragonminez.common.wish.wishes;

import com.dragonminez.common.wish.Wish;
import com.dragonminez.common.stats.*;
import com.dragonminez.api.rules.Rewards;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public final class RewardWish extends Wish {
    private List<JsonObject> rewards = new ArrayList<>();
    public RewardWish(String name,String description,List<JsonObject> rewards) {
        super(name,description,"rewards");this.rewards=List.copyOf(rewards);validate();
    }
    @Override public void validate() {
        super.validate();
        if(rewards==null||rewards.isEmpty())throw new IllegalArgumentException("Reward wish needs at least one reward");
        rewards.forEach(Rewards::validate);
    }
    @Override public void grant(ServerPlayer player) {
        validate();
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        Rewards.apply(player,data,getName(),rewards);
    }
}
