package com.dragonminez.common.wish;

import lombok.*;
import com.dragonminez.common.stats.StatsData;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

@Setter
@Getter
public abstract class Wish {
    private final String name;
    private final String description;
    private final String type;
    private Boolean repeatable;
    private String id;
    private com.google.gson.JsonObject requirements;

    protected Wish(String name, String description, String type) {
        this.name = name;
        this.description = description;
        this.type = type;
    }

    public String key(String dragon) {
        String identity=id==null?name:id;
        return "dragonminez:wish/"+java.util.UUID.nameUUIDFromBytes((dragon+"\0"+identity).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    public boolean available(StatsData data,String dragon) {
        return (isRepeatable()||!data.getContentProgress().hasCompleted(key(dragon)))
                &&(requirements==null||com.dragonminez.api.rules.Conditions.test(new com.dragonminez.api.rules.Conditions.Context(data,null),requirements));
    }
    public void validate() {
        if(id!=null)com.dragonminez.api.content.ContentProgress.requireId(id);
        if(requirements!=null)com.dragonminez.api.rules.Conditions.validate(requirements);
    }

    public abstract void grant(ServerPlayer player);

    public void grant(ServerPlayer wisher, List<ServerPlayer> targets) {
        grant(wisher);
    }

    public boolean isRepeatable() {
        return repeatable == null || repeatable;
    }

    public int getMaxTargets() {
        return 0;
    }

    public List<Component> getTooltipExtras(StatsData data) {
        return List.of();
    }

}
