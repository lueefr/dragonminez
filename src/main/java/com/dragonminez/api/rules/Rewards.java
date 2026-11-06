package com.dragonminez.api.rules;

import com.dragonminez.api.content.ContentCompletionApi;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsData;
import com.google.gson.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Rewards {
    public interface Type {
        void validate(JsonElement value);
        void apply(ServerPlayer player, StatsData data, JsonElement value);
    }
    /** Extensions can observe each successfully applied JSON action and its originating content ID. */
    public static final class Applied extends Event {
        private final ServerPlayer player; private final String content; private final JsonObject reward;
        public Applied(ServerPlayer player,String content,JsonObject reward){this.player=player;this.content=content;this.reward=reward.deepCopy();}
        public ServerPlayer getPlayer(){return player;}
        public String getContent(){return content;}
        public JsonObject getReward(){return reward.deepCopy();}
    }
    private static final Map<String,Type> TYPES=new ConcurrentHashMap<>();
    public static void register(String key,Type type){if(TYPES.putIfAbsent(key,Objects.requireNonNull(type))!=null)throw new IllegalArgumentException("Duplicate reward: "+key);}
    private interface Apply {void apply(ServerPlayer player,StatsData data,JsonElement value);}
    private static void add(String key,java.util.function.Consumer<JsonElement> validate,Apply apply){register(key,new Type(){public void validate(JsonElement v){validate.accept(v);}public void apply(ServerPlayer p,StatsData d,JsonElement v){apply.apply(p,d,v);}});}
    static {
        add("effect",v->{
            var o=v.getAsJsonObject();
            com.dragonminez.api.content.ContentProgress.requireId(o.get("id").getAsString());integer(o.get("ticks"));
            if(o.has("amplifier"))integer(o.get("amplifier"));
            int ticks=o.get("ticks").getAsInt(), amplifier=o.has("amplifier")?o.get("amplifier").getAsInt():0;
            if(ticks<1||ticks>72000||amplifier<0||amplifier>255)throw new IllegalArgumentException("Invalid effect duration/amplifier");
            if(Conditions.ready()&&!net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.containsKey(net.minecraft.resources.ResourceLocation.parse(o.get("id").getAsString())))throw new IllegalArgumentException("Unknown effect");
        },(p,d,v)->{
            var o=v.getAsJsonObject();var effect=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(net.minecraft.resources.ResourceLocation.parse(o.get("id").getAsString())).orElseThrow();
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect,o.get("ticks").getAsInt(),o.has("amplifier")?o.get("amplifier").getAsInt():0));
        });
        add("resource",v->{
            var o=v.getAsJsonObject();if(!Set.of("energy","stamina","health").contains(o.get("id").getAsString()))throw new IllegalArgumentException("Unknown resource");
            double amount=o.get("add").getAsDouble();if(!Double.isFinite(amount)||Math.abs(amount)>1e9)throw new IllegalArgumentException("Invalid resource delta");
        },(p,d,v)->{
            var o=v.getAsJsonObject();float amount=o.get("add").getAsFloat();
            switch(o.get("id").getAsString()){
                case "energy" -> d.getResources().addEnergy(amount);
                case "stamina" -> d.getResources().addStamina(amount);
                case "health" -> p.setHealth(Math.clamp(p.getHealth()+amount,0,p.getMaxHealth()));
            }
        });
        add("activate",v->{var o=v.getAsJsonObject();Conditions.form(o.get("form").getAsString());if(o.has("stack")&&(!o.get("stack").isJsonPrimitive()||!o.getAsJsonPrimitive("stack").isBoolean()))throw new IllegalArgumentException("activate stack must be boolean");},
            (p,d,v)->{var o=v.getAsJsonObject();String[] form=Conditions.form(o.get("form").getAsString());if(o.has("stack")&&o.get("stack").getAsBoolean())d.getCharacter().setActiveStackForm(form[0],form[1]);else d.getCharacter().setActiveForm(form[0],form[1]);});

        add("race",com.dragonminez.common.progression.RaceChange::validate,com.dragonminez.common.progression.RaceChange::apply);
        add("item",v->{
            var object=v.getAsJsonObject();com.dragonminez.api.content.ContentProgress.requireId(object.get("id").getAsString());integer(object.get("count"));
            if(Conditions.ready()&&!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.ResourceLocation.parse(object.get("id").getAsString())))
                throw new IllegalArgumentException("Unknown item reward: "+object.get("id"));
        },(p,d,v)->{
            var object=v.getAsJsonObject();String id=object.get("id").getAsString();
            if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.ResourceLocation.parse(id)))throw new IllegalArgumentException("Unknown item reward: "+id);
            new com.dragonminez.common.wish.wishes.ItemWish("","",id,object.get("count").getAsInt()).grant(p);
        });
        add("customize",v->{if(!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isBoolean()||!v.getAsBoolean())throw new IllegalArgumentException("customize must be true");},
                (p,d,v)->new com.dragonminez.common.wish.wishes.ReCustomizeWish("","").grant(p));
        add("appearance",v->{
            var object=v.getAsJsonObject();
            for(var entry:object.entrySet()) {
                if(!Set.of("bodyColor","bodyColor2","bodyColor3","hairColor","eye1Color","eye2Color","auraColor").contains(entry.getKey()))throw new IllegalArgumentException("Unknown appearance property: "+entry.getKey());
                if(!entry.getValue().getAsString().matches("#?[a-fA-F0-9]{6}"))throw new IllegalArgumentException("Expected an RGB appearance color");
            }
        },(p,d,v)->{
            var character=d.getCharacter();
            v.getAsJsonObject().entrySet().forEach(entry->{
                String color=entry.getValue().getAsString();
                switch(entry.getKey()) {
                    case "bodyColor" -> character.setBodyColor(color);
                    case "bodyColor2" -> character.setBodyColor2(color);
                    case "bodyColor3" -> character.setBodyColor3(color);
                    case "hairColor" -> character.setHairColor(color);
                    case "eye1Color" -> character.setEye1Color(color);
                    case "eye2Color" -> character.setEye2Color(color);
                    case "auraColor" -> character.setAuraColor(color);
                }
            });
        });
        add("form",v -> Conditions.form(v.getAsString()),(p,d,v) -> {
            String[] ref=Conditions.form(v.getAsString());var group=group(d,ref[0]);
            if(group==null || !group.getForms().containsKey(ref[1]))throw new IllegalArgumentException("Reward form not found for player race: "+v);
            var form=group.getForms().get(ref[1]);String skill=group.getFormType().toLowerCase(Locale.ROOT);int level=form.getUnlockOnSkillLevel();
            if(level<1 || com.dragonminez.common.util.TransformationsHelper.isScriptedFormType(skill))throw new IllegalArgumentException("Permanent reward requires a skill-backed form: "+v);
            grant(d,skill,level);
        });
        add("skill",v -> {var o=v.getAsJsonObject();Conditions.id(o.get("id").getAsString());integer(o.get("level"));},(p,d,v) -> {var o=v.getAsJsonObject();grant(d,o.get("id").getAsString(),o.get("level").getAsInt());});
        add("mastery",v -> {var o=v.getAsJsonObject();Conditions.form(o.get("form").getAsString());Conditions.positive(o.get("add").getAsDouble());},(p,d,v) -> {
            var o=v.getAsJsonObject();String[] ref=Conditions.form(o.get("form").getAsString());var group=group(d,ref[0]);
            if(group==null || !group.getForms().containsKey(ref[1]))throw new IllegalArgumentException("Reward mastery form not found: "+o.get("form"));
            var masteries=ConfigManager.getAllStackForms().containsKey(ref[0])?d.getCharacter().getStackFormMasteries():d.getCharacter().getFormMasteries();
            masteries.addMastery(ref[0],ref[1],o.get("add").getAsDouble(),group.getForms().get(ref[1]).getMaxMastery());
        });
        add("content",v -> com.dragonminez.api.content.ContentProgress.requireId(v.getAsString()),(p,d,v) -> ContentCompletionApi.complete(p,v.getAsString(),"reward"));
        add("tp",v -> {double n=v.getAsDouble();Conditions.positive(n);if(n!=Math.rint(n))throw new IllegalArgumentException("TP rewards must be whole points");},(p,d,v) -> d.getResources().addTrainingPoints(v.getAsDouble(),false,true));
    }
    private static com.dragonminez.common.config.FormConfig group(StatsData data,String id){
        var group=ConfigManager.getFormGroup(data.getCharacter().getRaceName(),id);return group!=null?group:ConfigManager.getStackFormGroup(id);
    }
    private static void integer(JsonElement v){double n=v.getAsDouble();Conditions.positive(n);if(n!=Math.rint(n)||n>Integer.MAX_VALUE)throw new IllegalArgumentException("Expected nonnegative integer");}
    private static void grant(StatsData data,String skill,int level){
        var race=ConfigManager.getRaceCharacter(data.getCharacter().getRaceName());
        int max;
        if(ConfigManager.getSkillsConfig().getFormSkills().contains(skill))max=race==null?0:race.getFormSkillTpCosts(skill).length;
        else {var costs=ConfigManager.getSkillsConfig().getSkillCosts(skill);max=costs==null||costs.getCosts()==null?0:costs.getCosts().size();}
        if(level>max || max==0)throw new IllegalArgumentException("Reward exceeds configured skill levels: "+skill+" -> "+level);
        data.getSkills().registerDefaultSkill(skill,max);data.getSkills().setSkillLevel(skill,Math.max(data.getSkills().getSkillLevel(skill),level));
    }
    public static void validate(JsonObject reward){
        if(reward.size()!=1)throw new IllegalArgumentException("Each reward must contain exactly one action");
        var entry=reward.entrySet().iterator().next();var type=TYPES.get(entry.getKey());if(type==null){if(Conditions.ready())throw new IllegalArgumentException("Unknown reward: "+entry.getKey());return;}type.validate(entry.getValue());
    }
    public static void apply(ServerPlayer player,StatsData data,String content,List<JsonObject> rewards){
        for(var reward:rewards){var action=reward.entrySet().iterator().next();TYPES.get(action.getKey()).apply(player,data,action.getValue());ContentCompletionApi.afterCommit(player,new Applied(player,content,reward));}
    }
    private Rewards(){}
}
