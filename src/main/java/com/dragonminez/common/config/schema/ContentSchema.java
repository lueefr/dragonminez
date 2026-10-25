package com.dragonminez.common.config.schema;

import com.google.gson.*;
import java.util.*;

/** Human-facing content layout. Unknown keys are retained for addons and manual rollback. */
public final class ContentSchema {
    public enum Kind { FORMS, FORM, SKILLS, SKILL, NPC, QUEST, SAGA, RULES, RACE_CHARACTER, RACE_STATS, CLASS_STATS, OBJECTIVE, REWARD, TECHNIQUES, TECHNIQUE, ENTITIES, RAID, TOURNAMENT, FIGHTER, NPC_PLACEMENT, DIALOGUE, DIALOGUE_NODE, DIALOGUE_CHOICE }
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
        map(Kind.RACE_CHARACTER,"identity","racialSkill hasGender");
        map(Kind.RACE_CHARACTER,"appearance.body","useVanillaSkin customModel isLayered headBones defaultModelScaling defaultBodyType slimBodyTypes defaultBodyColor defaultBodyColor2 defaultBodyColor3");
        map(Kind.RACE_CHARACTER,"appearance.hair","defaultHairType defaultHairColor");
        map(Kind.RACE_CHARACTER,"appearance.face","defaultEyesType defaultNoseType defaultMouthType defaultTattooType defaultEye1Color defaultEye2Color");
        map(Kind.RACE_CHARACTER,"appearance.tail","hasSaiyanTail");
        map(Kind.RACE_CHARACTER,"aura","auraType auraType3D aura3DStyle defaultAuraColor");
        put(Kind.RACE_CHARACTER,"formSkillsCosts","economy.formSkills");
        FIELDS.put(Kind.RACE_STATS,new LinkedHashMap<>());
        map(Kind.CLASS_STATS,"stats","baseStats statScaling");
        map(Kind.CLASS_STATS,"regeneration.health","baseHp5 hp5VitScaling");
        map(Kind.CLASS_STATS,"regeneration.energy","baseEp5 ep5EneScaling");
        map(Kind.CLASS_STATS,"regeneration.stamina","baseSp5 sp5StmScaling sp5ResScaling");
        map(Kind.CLASS_STATS,"economy","tpCostMultiplier tpGainMultiplier");
        map(Kind.CLASS_STATS,"racial","passive");
        map(Kind.QUEST,"requirements","prerequisites");
        put(Kind.QUEST,"requirements","requirements.start");
        // Type-specific payloads keep extension keys while grouping native fields by subject.
        map(Kind.OBJECTIVE,"target","entity entityName item biome dimension structure npcId skill");
        map(Kind.OBJECTIVE,"progress","count waves duration seconds ticks milliseconds mode count_mode checkpoints radius x y z");
        map(Kind.OBJECTIVE,"combat","health meleeDamage kiDamage AITier canTransform CanTransform TransformHealth TransformMeleeDamage TransformKiDamage TransformHealthMultiplier TransformMeleeDamageMultiplier TransformKiMultiplier TransformTriggerPercent");
        map(Kind.OBJECTIVE,"appearance","TextureVariant");
        map(Kind.OBJECTIVE,"spawning","spawn");
        map(Kind.OBJECTIVE,"navigation","hint");
        map(Kind.REWARD,"grant","item itemReward count amount command skill level group form mastery stack technique");
        map(Kind.REWARD,"availability","difficulty difficulties difficultyType minDifficulty");
        map(Kind.REWARD,"text","translationKey");
        map(Kind.TECHNIQUES,"types","kiAttacks strikeAttacks evasionAttacks");
        map(Kind.TECHNIQUE,"progression","minXPCost maxXPCost xpCostMultiplier xpGainMultiplier xpGainPerHit xpGainPerKill");
        map(Kind.TECHNIQUE,"economy","kiCostMultiplier");
        map(Kind.TECHNIQUE,"combat","damageMultiplier destructionMultiplier maxDestructionRadius");
        map(Kind.TECHNIQUE,"timing","castTimeTicks cooldownTicks");
        map(Kind.TECHNIQUE,"appearance.size","baseSizeCap transformedSizeCap sizePowerFloor sizePowerCeiling");
        put(Kind.ENTITIES,"defaultEntityStats","stats.byEntity");
        put(Kind.ENTITIES,"transformDefaults","transformation.defaults");
        map(Kind.RAID,"identity","enabled displayName");
        map(Kind.RAID,"encounter","activationRadius leashDistance interWaveDelaySeconds trigger waves defeat");
        map(Kind.RAID,"appearance","music");map(Kind.RAID,"economy","rewards");
        map(Kind.TOURNAMENT,"identity","enabled displayName format difficultyStars");
        map(Kind.TOURNAMENT,"rules","qualifierSlots lethal");
        map(Kind.TOURNAMENT,"timing","reentryCooldownSeconds victoryCooldownSeconds matchTimeoutSeconds nextRoundSeconds rulesAcceptSeconds returnSeconds arrivalSeconds");
        map(Kind.TOURNAMENT,"arena","ring");map(Kind.TOURNAMENT,"fighters","champion semifinalist finalist contenders");
        map(Kind.TOURNAMENT,"economy","rewards");
        map(Kind.FIGHTER,"identity","entityId");
        map(Kind.FIGHTER,"combat","health meleeDamage kiDamage aiTier perRoundScaling");
        map(Kind.FIGHTER,"appearance","halo supervillain");
        map(Kind.FIGHTER,"spawning","count dimension spawnChance preparationSeconds dormantUntilEscortDead");
        map(Kind.FIGHTER,"text","omenMessage startMessage announceMessage");
        map(Kind.NPC_PLACEMENT,"identity","npc");map(Kind.NPC_PLACEMENT,"location","dimension x y z yaw");
        map(Kind.DIALOGUE,"identity","npc");map(Kind.DIALOGUE,"interaction","start");
        map(Kind.DIALOGUE_NODE,"text","line");map(Kind.DIALOGUE_NODE,"interaction","choices");
        put(Kind.DIALOGUE_CHOICE,"text","text.label");put(Kind.DIALOGUE_CHOICE,"goto","navigation.next");
        put(Kind.DIALOGUE_CHOICE,"conditions","requirements");put(Kind.DIALOGUE_CHOICE,"actions","effects.actions");
        map(Kind.FIGHTER,"appearance","textureVariant");
        map(Kind.FIGHTER,"transformation","canTransform transformHealth transformMeleeDamage transformKiDamage");
        map(Kind.OBJECTIVE,"target","npc_id npc dragon dragon_id dragonId ball_set ballSet ball_set_id ballSetId set skillId id");
        map(Kind.OBJECTIVE,"progress","mobs_per_wave mobsPerWave wave_delay_seconds waveDelaySeconds level minLevel required");
        map(Kind.REWARD,"grant","formGroup form_group formName form_name code techniqueCode technique_code itemType itemId enchantments potion mobEffects material pattern");
        FIELDS.forEach((kind,fields)->PREVIOUS_PATHS.put(kind,Map.copyOf(fields)));
        put(Kind.RACE_CHARACTER,"defaultHairType","appearance.hair.slot");
        put(Kind.RACE_CHARACTER,"defaultHairColor","appearance.hair.color");
        put(Kind.RACE_CHARACTER,"customModel","appearance.body.model");
        put(Kind.RACE_CHARACTER,"useVanillaSkin","appearance.body.usePlayerSkin");
        put(Kind.RACE_CHARACTER,"isLayered","appearance.body.layered");
        put(Kind.RACE_CHARACTER,"defaultModelScaling","appearance.body.scale");
        put(Kind.RACE_CHARACTER,"defaultBodyType","appearance.body.type");
        put(Kind.RACE_CHARACTER,"defaultBodyColor","appearance.body.primaryColor");
        put(Kind.RACE_CHARACTER,"defaultBodyColor2","appearance.body.secondaryColor");
        put(Kind.RACE_CHARACTER,"defaultBodyColor3","appearance.body.tertiaryColor");
        put(Kind.RACE_CHARACTER,"defaultEyesType","appearance.face.eyes");
        put(Kind.RACE_CHARACTER,"defaultNoseType","appearance.face.nose");
        put(Kind.RACE_CHARACTER,"defaultMouthType","appearance.face.mouth");
        put(Kind.RACE_CHARACTER,"defaultTattooType","appearance.face.tattoo");
        put(Kind.RACE_CHARACTER,"defaultEye1Color","appearance.face.primaryEyeColor");
        put(Kind.RACE_CHARACTER,"defaultEye2Color","appearance.face.secondaryEyeColor");
        put(Kind.RACE_CHARACTER,"auraType","aura.main.type2d");
        put(Kind.RACE_CHARACTER,"auraType3D","aura.main.type3d");
        put(Kind.RACE_CHARACTER,"aura3DStyle","aura.main.style");
        put(Kind.RACE_CHARACTER,"defaultAuraColor","aura.main.color");
        put(Kind.CLASS_STATS,"baseStats","stats.base");
        put(Kind.CLASS_STATS,"statScaling","stats.scaling");
        put(Kind.CLASS_STATS,"baseHp5","regeneration.health.basePerFiveSeconds");
        put(Kind.CLASS_STATS,"hp5VitScaling","regeneration.health.perVitality");
        put(Kind.CLASS_STATS,"baseEp5","regeneration.energy.basePerFiveSeconds");
        put(Kind.CLASS_STATS,"ep5EneScaling","regeneration.energy.perEnergy");
        put(Kind.CLASS_STATS,"baseSp5","regeneration.stamina.basePerFiveSeconds");
        put(Kind.CLASS_STATS,"sp5StmScaling","regeneration.stamina.perVitality");
        put(Kind.CLASS_STATS,"sp5ResScaling","regeneration.stamina.perResistance");
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
        var extracted=new LinkedHashMap<String,JsonElement>();
        for(var field:ordered){
            String head=field.getValue().split("\\.")[0];
            boolean legacyScalar=head.equals(field.getKey())&&result.has(head)&&!result.get(head).isJsonObject();
            JsonElement value=legacyScalar?null:take(result,field.getValue());
            String previous=PREVIOUS_PATHS.getOrDefault(kind,Map.of()).get(field.getKey());
            if(previous!=null&&!previous.equals(field.getValue())){
                JsonElement old=take(result,previous);
                if(old!=null){if(value!=null)throw new IllegalArgumentException("Duplicate nested alias: "+field.getKey());value=old;}
            }
            if(value!=null)extracted.put(field.getKey(),value);
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
        extracted.forEach((field,value)->{
            if(get(result,field)!=null)throw new IllegalArgumentException("Mixed legacy and nested field: "+field);
            insert(result,field,value);
        });
        if(kind==Kind.DIALOGUE)transformMap(result,"nodes",Kind.DIALOGUE_NODE,false);
        if(kind==Kind.DIALOGUE_NODE)transformList(result,"choices",Kind.DIALOGUE_CHOICE,false);
        if(kind==Kind.RACE_STATS)transformMap(result,"classes",Kind.CLASS_STATS,false);
        if(kind==Kind.TECHNIQUES)for(String key:List.of("kiAttacks","strikeAttacks","evasionAttacks"))transformMap(result,key,Kind.TECHNIQUE,false);
        if(kind==Kind.QUEST){transformList(result,"objectives",Kind.OBJECTIVE,false);transformList(result,"rewards",Kind.REWARD,false);}
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
        if(kind==Kind.RACE_CHARACTER&&result.has("formSkillsCosts")){
            var costs=result.getAsJsonObject("formSkillsCosts");
            for(String key:new ArrayList<>(costs.keySet()))if(costs.get(key).isJsonArray()){
                var entry=new JsonObject();entry.addProperty("buyFromMaster",false);entry.add("prices",costs.get(key));costs.add(key,entry);
            }
        }
        if(kind==Kind.REWARD&&result.has("type")){
            String type=result.get("type").getAsString();
            int colon=type.indexOf(':');
            if(colon>0&&Set.of("hard","normal").contains(type.substring(0,colon).toLowerCase(Locale.ROOT))){
                boolean explicit=List.of("difficulty","difficulties","difficultyType","minDifficulty").stream().anyMatch(result::has);
                if(!explicit){var levels=new JsonArray();if(type.substring(0,colon).equalsIgnoreCase("hard"))levels.add("HARD");else{levels.add("EASY");levels.add("NORMAL");}result.add("difficulties",levels);}
                result.addProperty("type",type.substring(colon+1));
            }
        }
        if(kind==Kind.NPC)legacyAliases(result,Map.of("hairCode","forcedHairCode"));
        if(kind==Kind.SKILLS)transformMap(result,"skills",Kind.SKILL,false);
        return result;
    }
    public static JsonObject encode(Kind kind,JsonObject flat){
        JsonObject result=new JsonObject();if(!Set.of(Kind.FORM,Kind.SKILL,Kind.CLASS_STATS,Kind.OBJECTIVE,Kind.REWARD,Kind.TECHNIQUE,Kind.FIGHTER,Kind.DIALOGUE_NODE,Kind.DIALOGUE_CHOICE).contains(kind))result.addProperty("schema",1);
        JsonObject remaining=flat.deepCopy();remaining.remove("schema");
        if(kind==Kind.DIALOGUE)transformMap(remaining,"nodes",Kind.DIALOGUE_NODE,true);
        if(kind==Kind.DIALOGUE_NODE)transformList(remaining,"choices",Kind.DIALOGUE_CHOICE,true);
        if(kind==Kind.RACE_STATS)transformMap(remaining,"classes",Kind.CLASS_STATS,true);
        if(kind==Kind.TECHNIQUES)for(String key:List.of("kiAttacks","strikeAttacks","evasionAttacks"))transformMap(remaining,key,Kind.TECHNIQUE,true);
        if(kind==Kind.QUEST){transformList(remaining,"objectives",Kind.OBJECTIVE,true);transformList(remaining,"rewards",Kind.REWARD,true);}
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
    private static void transformList(JsonObject root,String key,Kind kind,boolean encode){
        if(!root.has(key)||root.get(key).isJsonNull())return;
        JsonArray values=root.getAsJsonArray(key);
        for(int i=0;i<values.size();i++){
            if(!values.get(i).isJsonObject())continue;
            JsonObject value=values.get(i).getAsJsonObject();
            values.set(i,encode?encode(kind,value):decode(kind,value));
        }
    }
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
