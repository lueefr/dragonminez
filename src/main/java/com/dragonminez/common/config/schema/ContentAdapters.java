package com.dragonminez.common.config.schema;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.*;
import java.io.IOException;

/** Retains Java APIs while replacing the JSON layout at the serialization boundary. */
public final class ContentAdapters implements TypeAdapterFactory {
    public static ContentSchema.Kind kind(Class<?> type){return switch(type.getName()){
        case "com.dragonminez.common.config.FormConfig"->ContentSchema.Kind.FORMS;
        case "com.dragonminez.common.config.FormConfig$FormData"->ContentSchema.Kind.FORM;
        case "com.dragonminez.common.config.SkillsConfig"->ContentSchema.Kind.SKILLS;
        case "com.dragonminez.common.config.SkillsConfig$SkillCosts"->ContentSchema.Kind.SKILL;
        case "com.dragonminez.api.npc.CustomNpcRegistry$Definition"->ContentSchema.Kind.NPC;
        case "com.dragonminez.common.config.RuleConfig"->ContentSchema.Kind.RULES;
        default->null;
    };}
    @Override public <T> TypeAdapter<T> create(Gson gson,TypeToken<T> type){
        var kind=kind(type.getRawType());if(kind==null)return null;
        TypeAdapter<T> delegate=gson.getDelegateAdapter(this,type);
        return new TypeAdapter<T>() {
            @Override public T read(JsonReader reader)throws IOException{
                JsonElement tree=JsonParser.parseReader(reader);
                if(tree.isJsonNull())return null;
                return delegate.fromJsonTree(ContentSchema.decode(kind,tree.getAsJsonObject()));
            }
            @Override public void write(JsonWriter writer,T value)throws IOException{
                JsonElement tree=delegate.toJsonTree(value);
                gson.getAdapter(JsonElement.class).write(writer,tree.isJsonNull()?tree:ContentSchema.encode(kind,tree.getAsJsonObject()));
            }
        };
    }
}
