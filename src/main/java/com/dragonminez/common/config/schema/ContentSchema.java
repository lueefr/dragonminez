package com.dragonminez.common.config.schema;

import com.google.gson.*;
import java.util.*;

/** Human-facing content layout. Unknown keys are retained for addons and manual rollback. */
public final class ContentSchema {
    public enum Kind { FORMS, FORM, SKILLS, SKILL, NPC, QUEST, SAGA, RULES }
    private static final Map<Kind,LinkedHashMap<String,String>> FIELDS=new EnumMap<>(Kind.class);
    private static final Map<Kind,Map<String,String>> PREVIOUS_PATHS=new EnumMap<>(Kind.class);
    static {
        map(Kind.FORMS,"identity", "groupName formType");
        map(Kind.FORM,"appearance.hair","hairType forcedHairCode hairColor editHair");
        map(Kind.FORM,"appearance.eyes","eye1Color eye2Color");
        map(Kind.FORM,"appearance.body","customModel keepBaseFormHeadBones modelScaling bodyColor1 bodyColor2 bodyColor3 extraFormLayer extraFormColor tintColor tintIntensity");
        map(Kind.FORM,"appearance.fur","furUsesHairColor");map(Kind.FORM,"appearance.tail","hideTail");
        map(Kind.FORM,"aura","auraTransitionSeconds auraSealSeconds auraType auraType3D auraLayer auraColor aura3DStyle extraAuraLayer extraAuraColor extraAuraType extraAuraType3D extraAura3DStyle");
        map(Kind.FORM,"effects","hasLightnings lightningColor outlineShader visual mobEffects transformationAnimation");
        map(Kind.FORM,"stats.multipliers","strMultiplier skpMultiplier stmMultiplier defMultiplier vitMultiplier pwrMultiplier eneMultiplier speedMultiplier");
        map(Kind.FORM,"stats.composition","baseForm inheritsMultipliersFrom multiplierMode");map(Kind.FORM,"stats","attackSpeed");
        map(Kind.FORM,"progression.mastery","maxMastery masteryPerHitDealt masteryPerHitReceived passiveMasteryEveryFiveSeconds maxCostMultiplier maxStatsMultiplier shareMasteryWith shareMasteryMultiplier instantTransformOnMastery allowFreeTransformOnMastery");
        map(Kind.FORM,"requirements.unlock","unlockOnSkillLevel formRequisite formRequisiteType unlockOnMastery levelRequisite sagaRequisite completedContentRequisites");
        put(Kind.FORM,"buy","requirements.buy");put(Kind.FORM,"use","requirements.use");
        map(Kind.FORM,"stacking","formStackable stackOnMastery incompatibleWith");
        map(Kind.FORM,"economy.drains","staminaDrainMultiplier energyDrain staminaDrain healthDrain otherworldTimeDrain otherworldTimeDrainMastered stackDrainMultiplier");
        map(Kind.FORM,"economy.items","triggerItemCosts durationItemCosts");map(Kind.FORM,"controls","formCombo");
        map(Kind.FORM,"legacy","accessoriesColors allowAlwaysTransformOnMastery canAlwaysTransform colorMixSpeed costDecreasePerMasteryPoint directTransformIfUsedOnMastery directTransformation directTransformationIfUsed excemptFromUltimate extraLayersColor isLayered layerAmount noiseScale racialAccessories statMultPerMasteryPoint transformationPostShader");
        map(Kind.SKILLS,"categories","kiSkills formSkills stackSkills strikeSkills evasionSkills androidBlacklistedForms");
        put(Kind.SKILLS,"skillOfferings","masters.offerings");
        put(Kind.SKILL,"costs","economy.prices");put(Kind.SKILL,"allowedRaces","requirements.races");
        map(Kind.NPC,"appearance","baseTexture baseModel armor hairType forcedHairCode hairPreset");
        map(Kind.NPC,"identity","name alignment");map(Kind.NPC,"interaction","dialogues offers");
        map(Kind.QUEST,"text","title description");map(Kind.QUEST,"identity","type category");
        map(Kind.QUEST,"interaction","quest_giver turn_in claim_mode");
        map(Kind.QUEST,"progression","repeatable repeat_cooldown_seconds completionId time_limit_seconds parallel_objectives party_scaling secret");
        map(Kind.SAGA,"identity","name");map(Kind.SAGA,"progression","requirements questFolder");
        put(Kind.RULES,"content","completion.rewards");put(Kind.RULES,"buy","purchase.conditions");
        FIELDS.forEach((kind,fields)->PREVIOUS_PATHS.put(kind,Map.copyOf(fields)));
        put(Kind.FORM,"hairType","appearance.hair.slot");
        put(Kind.FORM,"forcedHairCode","appearance.hair.code");
        put(Kind.FORM,"hairColor","appearance.hair.color");
        put(Kind.FORM,"editHair","appearance.hair.editable");
        put(Kind.FORM,"eye1Color","appearance.eyes.primary");
        put(Kind.FORM,"eye2Color","appearance.eyes.secondary");
        put(Kind.FORM,"customModel","appearance.body.model");
        put(Kind.FORM,"keepBaseFormHeadBones","appearance.body.keepBaseHead");
        put(Kind.FORM,"modelScaling","appearance.body.scale");
        put(Kind.FORM,"bodyColor1","appearance.body.primaryColor");
        put(Kind.FORM,"bodyColor2","appearance.body.secondaryColor");
        put(Kind.FORM,"bodyColor3","appearance.body.tertiaryColor");
        put(Kind.FORM,"extraFormLayer","appearance.body.layer");
        put(Kind.FORM,"extraFormColor","appearance.body.layerColor");
        put(Kind.FORM,"furUsesHairColor","appearance.fur.useHairColor");
        put(Kind.FORM,"hideTail","appearance.tail.hidden");
        put(Kind.FORM,"auraTransitionSeconds","aura.transitionSeconds");
        put(Kind.FORM,"auraSealSeconds","aura.absorptionSeconds");
        put(Kind.FORM,"auraType","aura.main.type2d");
        put(Kind.FORM,"auraType3D","aura.main.type3d");
        put(Kind.FORM,"auraLayer","aura.main.layer");
        put(Kind.FORM,"auraColor","aura.main.color");
        put(Kind.FORM,"aura3DStyle","aura.main.style");
        put(Kind.FORM,"extraAuraType","aura.extra.type2d");
        put(Kind.FORM,"extraAuraType3D","aura.extra.type3d");
        put(Kind.FORM,"extraAuraLayer","aura.extra.layer");
        put(Kind.FORM,"extraAuraColor","aura.extra.color");
        put(Kind.FORM,"extraAura3DStyle","aura.extra.style");
        put(Kind.FORM,"hasLightnings","effects.lightning.enabled");
        put(Kind.FORM,"lightningColor","effects.lightning.color");
        put(Kind.FORM,"outlineShader","effects.outline");
        put(Kind.FORM,"transformationAnimation","effects.animation");
        put(Kind.FORM,"baseForm","stats.composition.base");
        put(Kind.FORM,"inheritsMultipliersFrom","stats.composition.inherits");
        put(Kind.FORM,"multiplierMode","stats.composition.mode");
        put(Kind.FORM,"formStackable","stacking.allowed");
        put(Kind.FORM,"stackOnMastery","stacking.mastery");
        put(Kind.FORM,"incompatibleWith","stacking.incompatible");
        put(Kind.FORM,"formCombo","controls.combo");
        put(Kind.FORM,"unlockOnSkillLevel","requirements.unlock.skillLevel");
        put(Kind.FORM,"formRequisite","requirements.unlock.masteryOf");
        put(Kind.FORM,"formRequisiteType","requirements.unlock.masteryMode");
        put(Kind.FORM,"unlockOnMastery","requirements.unlock.masteryMinimum");
        put(Kind.FORM,"levelRequisite","requirements.unlock.level");
        put(Kind.FORM,"sagaRequisite","requirements.unlock.saga");
        put(Kind.FORM,"completedContentRequisites","requirements.unlock.completedContent");
        put(Kind.FORM,"maxMastery","progression.mastery.maximum");
        put(Kind.FORM,"masteryPerHitDealt","progression.mastery.perHitDealt");
        put(Kind.FORM,"masteryPerHitReceived","progression.mastery.perHitReceived");
        put(Kind.FORM,"passiveMasteryEveryFiveSeconds","progression.mastery.passivePerFiveSeconds");
        put(Kind.FORM,"maxCostMultiplier","progression.mastery.costAtMaximum");
        put(Kind.FORM,"maxStatsMultiplier","progression.mastery.statsAtMaximum");
        put(Kind.FORM,"shareMasteryWith","progression.mastery.shareWith");
        put(Kind.FORM,"shareMasteryMultiplier","progression.mastery.shareMultiplier");
        put(Kind.FORM,"instantTransformOnMastery","progression.mastery.instantAt");
        put(Kind.FORM,"allowFreeTransformOnMastery","progression.mastery.freeAt");
        put(Kind.FORM,"energyDrain","economy.drains.energy");
        put(Kind.FORM,"staminaDrain","economy.drains.stamina");
        put(Kind.FORM,"healthDrain","economy.drains.health");
        put(Kind.FORM,"staminaDrainMultiplier","economy.drains.staminaMultiplier");
        put(Kind.FORM,"otherworldTimeDrain","economy.drains.otherworldTime");
        put(Kind.FORM,"otherworldTimeDrainMastered","economy.drains.otherworldTimeAtMaximum");
        put(Kind.FORM,"stackDrainMultiplier","economy.drains.stackMultiplier");
        put(Kind.FORM,"strMultiplier","stats.multipliers.strength");
        put(Kind.FORM,"skpMultiplier","stats.multipliers.strikePower");
        put(Kind.FORM,"stmMultiplier","stats.multipliers.stamina");
        put(Kind.FORM,"defMultiplier","stats.multipliers.defense");
        put(Kind.FORM,"vitMultiplier","stats.multipliers.vitality");
        put(Kind.FORM,"pwrMultiplier","stats.multipliers.kiPower");
        put(Kind.FORM,"eneMultiplier","stats.multipliers.energy");
        put(Kind.FORM,"speedMultiplier","stats.multipliers.speed");
        put(Kind.NPC,"baseModel","appearance.body.model");
        put(Kind.NPC,"baseTexture","appearance.body.texture");
        put(Kind.NPC,"hairType","appearance.hair.slot");
        put(Kind.NPC,"forcedHairCode","appearance.hair.code");
        put(Kind.NPC,"hairPreset","appearance.hair.preset");
        FIELDS.get(Kind.FORM).remove("visual");
        put(Kind.FORM,"visual.aura","aura.state");
        put(Kind.FORM,"visual.parts","appearance.parts");
        put(Kind.FORM,"visual.fur","appearance.fur.override");
        put(Kind.FORM,"visual.tail","appearance.tail.override");
        put(Kind.FORM,"visual.eyes","appearance.eyes.outline");
        put(Kind.FORM,"visual.hair","appearance.hair.visible");
        put(Kind.FORM,"visual.lightning","effects.lightning.visible");
        put(Kind.FORM,"visual.outline","effects.outline.visible");
        put(Kind.FORM,"visual.rules","effects.rules");

    }
    private static void map(Kind kind,String section,String fields){for(String field:fields.split(" "))put(kind,field,section+"."+field);}
    private static void put(Kind kind,String field,String path){FIELDS.computeIfAbsent(kind,k->new LinkedHashMap<>()).put(field,path);}
    public static Map<String,String> paths(Kind kind){return Collections.unmodifiableMap(FIELDS.getOrDefault(kind,new LinkedHashMap<>()));}
    private static JsonElement take(JsonObject object,String path){
        String[] parts=path.split("\\.");JsonObject current=object;
        for(int i=0;i<parts.length-1;i++){
            if(!current.has(parts[i])||current.get(parts[i]).isJsonNull())return null;
            if(!current.get(parts[i]).isJsonObject())throw new IllegalArgumentException("Expected object at "+path);
            current=current.getAsJsonObject(parts[i]);
        }
        return current.remove(parts[parts.length-1]);
    }
    private static void insert(JsonObject object,String path,JsonElement value){
        String[] parts=path.split("\\.");JsonObject current=object;
        for(int i=0;i<parts.length-1;i++){
            if(!current.has(parts[i]))current.add(parts[i],new JsonObject());
            if(!current.get(parts[i]).isJsonObject())throw new IllegalArgumentException("Conflicting field at "+path);
            current=current.getAsJsonObject(parts[i]);
        }
        if(current.has(parts[parts.length-1]))throw new IllegalArgumentException("Duplicate field at "+path);
        current.add(parts[parts.length-1],value);
    }
    private static void prune(JsonObject object){
        for(String key:new ArrayList<>(object.keySet()))if(object.get(key).isJsonObject()){
            prune(object.getAsJsonObject(key));if(object.getAsJsonObject(key).isEmpty())object.remove(key);
        }
    }
    public static JsonObject decode(Kind kind,JsonObject input){
        JsonObject result=input.deepCopy();
        if(result.has("schema")){
            JsonElement version=result.get("schema");
            if(!version.isJsonPrimitive()||!version.getAsJsonPrimitive().isNumber()
                    ||version.getAsBigDecimal().compareTo(java.math.BigDecimal.ONE)!=0)
                throw new IllegalArgumentException("Unsupported content schema: "+version);
            result.remove("schema");
        }
        if(kind==Kind.FORM){
            JsonElement legacyVisual=take(result,"effects.visual");
            if(legacyVisual!=null){if(result.has("visual"))throw new IllegalArgumentException("Duplicate visual configuration");result.add("visual",legacyVisual);}
        }
        var ordered=new ArrayList<>(paths(kind).entrySet());
        ordered.sort(java.util.Comparator.comparingInt((Map.Entry<String,String> entry)->entry.getValue().split("\\.").length).reversed());
        for(var field:ordered){
            JsonElement value=take(result,field.getValue());
            String previous=PREVIOUS_PATHS.getOrDefault(kind,Map.of()).get(field.getKey());
            if(previous!=null&&!previous.equals(field.getValue())){
                JsonElement old=take(result,previous);
                if(old!=null){if(value!=null)throw new IllegalArgumentException("Duplicate nested alias: "+field.getKey());value=old;}
            }
            if(value!=null){if(get(result,field.getKey())!=null)throw new IllegalArgumentException("Mixed legacy and nested field: "+field.getKey());insert(result,field.getKey(),value);}
        }
        var sectionPaths=new ArrayList<>(paths(kind).values());sectionPaths.addAll(PREVIOUS_PATHS.getOrDefault(kind,Map.of()).values());
        for(String path:sectionPaths){
            String parent=path.contains(".")?path.substring(0,path.lastIndexOf('.')):"";
            while(!parent.isEmpty()){
                JsonElement node=get(result,parent);
                if(node!=null&&node.isJsonObject()&&node.getAsJsonObject().isEmpty())take(result,parent);
                parent=parent.contains(".")?parent.substring(0,parent.lastIndexOf('.')):"";
            }
        }
        if(kind==Kind.FORMS){
            legacyAliases(result,Map.of("group_name","groupName","form_type","formType"));
            if(result.has("formType")){
                String type=result.get("formType").getAsString();
                String modern=switch(type){case "super"->"superforms";case "legendary"->"legendaryforms";case "god"->"godforms";case "android"->"androidforms";default->type;};
                result.addProperty("formType",modern);
            }
            transformMap(result,"forms",Kind.FORM,false);
        }
        if(kind==Kind.FORM)normalizeLegacyForm(result);
        if(kind==Kind.NPC)legacyAliases(result,Map.of("hairCode","forcedHairCode"));
        if(kind==Kind.SKILLS)transformMap(result,"skills",Kind.SKILL,false);
        return result;
    }
    public static JsonObject encode(Kind kind,JsonObject flat){
        JsonObject result=new JsonObject();if(kind!=Kind.FORM&&kind!=Kind.SKILL)result.addProperty("schema",1);
        JsonObject remaining=flat.deepCopy();remaining.remove("schema");
        if(kind==Kind.FORMS)transformMap(remaining,"forms",Kind.FORM,true);
        if(kind==Kind.SKILLS)transformMap(remaining,"skills",Kind.SKILL,true);
        for(String key:List.of("configVersion","defaultsVersion","id","name"))if(remaining.has(key)&&!paths(kind).containsKey(key))result.add(key,remaining.remove(key));
        for(var field:paths(kind).entrySet()){
            JsonElement value=take(remaining,field.getKey());if(value!=null)insert(result,field.getValue(),value);
        }
        if(remaining.has("visual")&&remaining.get("visual").isJsonObject()&&remaining.getAsJsonObject("visual").isEmpty())remaining.remove("visual");
        mergeUnknown(result,remaining);return result;
    }
    private static JsonElement get(JsonObject root,String path){JsonElement value=root;for(String key:path.split("\\.")){if(!value.isJsonObject()||!value.getAsJsonObject().has(key))return null;value=value.getAsJsonObject().get(key);}return value;}
    private static void mergeUnknown(JsonObject target,JsonObject source){source.entrySet().forEach(e->{
        if(!target.has(e.getKey()))target.add(e.getKey(),e.getValue());
        else if(target.get(e.getKey()).isJsonObject()&&e.getValue().isJsonObject())mergeUnknown(target.getAsJsonObject(e.getKey()),e.getValue().getAsJsonObject());
        else throw new IllegalArgumentException("Conflicting content field: "+e.getKey());
    });}
    private static void transformMap(JsonObject root,String key,Kind kind,boolean encode){
        if(!root.has(key))return;JsonObject values=root.getAsJsonObject(key);
        for(String id:new ArrayList<>(values.keySet())){
            JsonObject value=values.getAsJsonObject(id);
            if(kind==Kind.FORM&&!value.has("name"))value.addProperty("name",id);
            values.add(id,encode?encode(kind,value):decode(kind,value));
        }
    }
    private static void legacyAliases(JsonObject object,Map<String,String> aliases){aliases.forEach((old,current)->{
        if(object.has(old)){if(object.has(current))throw new IllegalArgumentException("Duplicate legacy alias: "+old);object.add(current,object.remove(old));}
    });}
    private static void normalizeLegacyForm(JsonObject object){
        Map<String,String> aliases=new HashMap<>();
        for(String key:paths(Kind.FORM).keySet())aliases.put(key.replaceAll("([a-z0-9])([A-Z])","$1_$2").toLowerCase(Locale.ROOT),key);
        aliases.entrySet().removeIf(e->e.getKey().equals(e.getValue()));
        for(String stat:List.of("str","skp","stm","def","vit","pwr","ene","speed"))aliases.put(stat+"_mult",stat+"Multiplier");
        aliases.put("unlockOnSuperformLevel","unlockOnSkillLevel");
        aliases.put("masteryPerHit","masteryPerHitDealt");aliases.put("masteryPerDamageReceived","masteryPerHitReceived");
        aliases.put("passiveMasteryGainEveryFiveSeconds","passiveMasteryEveryFiveSeconds");
        aliases.put("kaiokenStackable","formStackable");aliases.put("kaiokenDrainMultiplier","stackDrainMultiplier");
        aliases.put("hairCode","forcedHairCode");aliases.put("hair_code","forcedHairCode");
        aliases.put("body_color_1","bodyColor1");aliases.put("body_color_2","bodyColor2");aliases.put("body_color_3","bodyColor3");
        legacyAliases(object,aliases);
        double max=object.has("maxMastery")?object.get("maxMastery").getAsDouble():100;
        if(object.has("statMultPerMasteryPoint")&&!object.has("maxStatsMultiplier"))
            object.addProperty("maxStatsMultiplier",1+max*object.get("statMultPerMasteryPoint").getAsDouble());
        if(object.has("costDecreasePerMasteryPoint")&&!object.has("maxCostMultiplier"))
            object.addProperty("maxCostMultiplier",1/(1+max*object.get("costDecreasePerMasteryPoint").getAsDouble()));
        if(object.has("canAlwaysTransform")&&object.get("canAlwaysTransform").getAsBoolean()&&!object.has("allowFreeTransformOnMastery"))
            object.addProperty("allowFreeTransformOnMastery",object.has("allowAlwaysTransformOnMastery")?object.get("allowAlwaysTransformOnMastery").getAsDouble():0);
        if(object.has("directTransformation")&&object.get("directTransformation").getAsBoolean()&&!object.has("instantTransformOnMastery"))
            object.addProperty("instantTransformOnMastery",0);
        if(object.has("excemptFromUltimate")&&object.get("excemptFromUltimate").getAsBoolean()&&!object.has("incompatibleWith")){
            JsonArray list=new JsonArray();list.add("ultimate.ultimate");object.add("incompatibleWith",list);
        }
        if(object.has("modelScaling")&&object.get("modelScaling").isJsonPrimitive()){
            JsonArray scale=new JsonArray();for(int i=0;i<3;i++)scale.add(object.get("modelScaling"));object.add("modelScaling",scale);
        }
        if(object.has("multiplierMode")&&object.get("multiplierMode").getAsString().equals("add"))
            for(String stat:List.of("str","skp","stm","def","vit","pwr","ene","speed"))if(!object.has(stat+"Multiplier"))object.addProperty(stat+"Multiplier",0);
        if(object.has("hairType")&&object.get("hairType").isJsonPrimitive()&&object.getAsJsonPrimitive("hairType").isNumber())
            {
                int legacy=object.get("hairType").getAsInt();
                if(legacy==-1)object.addProperty("hairType","");
                else if(legacy==1)object.addProperty("hairType","ssj3");
                else throw new IllegalArgumentException("Unknown historical numeric hairType: "+legacy+"; original left untouched");
            }
    }
    private ContentSchema(){}
}
