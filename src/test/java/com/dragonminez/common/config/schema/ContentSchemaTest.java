package com.dragonminez.common.config.schema;

import com.google.gson.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.dragonminez.common.config.schema.ContentSchema.*;

class ContentSchemaTest {
    @TempDir Path directory;
    static JsonObject json(String text){return JsonParser.parseString(text).getAsJsonObject();}
    @BeforeEach void reset(){ContentMigration.begin();}
    @Test void formsKeepUnknownFieldsWhileMovingEveryKnownField(){
        JsonObject old=json("{groupName:'custom',formType:'superforms',forms:{stage:{name:'stage',hairColor:'#FFFFFF',strMultiplier:4,custom:{empty:{},'addon.key':9},use:{mastery:{form:'custom.base',min:100}}}}}");
        JsonObject encoded=encode(Kind.FORMS,decode(Kind.FORMS,old));
        assertEquals("#FFFFFF",encoded.getAsJsonObject("forms").getAsJsonObject("stage").getAsJsonObject("appearance").getAsJsonObject("hair").get("color").getAsString());
        assertEquals(old,decode(Kind.FORMS,encoded));
    }
    @Test void migratesHistoricalSnakeCaseScalarScaleAndNumericHair(){
        var flat=decode(Kind.FORMS,json("{group_name:'custom',formType:'super',forms:{stage:{hair_type:1,model_scaling:0.9,str_mult:16}}}"));
        var form=flat.getAsJsonObject("forms").getAsJsonObject("stage");
        assertEquals("superforms",flat.get("formType").getAsString());
        assertEquals("ssj3",form.get("hairType").getAsString());
        assertEquals(3,form.getAsJsonArray("modelScaling").size());
        assertEquals(16,form.get("strMultiplier").getAsInt());
    }
    @Test void additiveOmittedStatsAreZeroAndInvalidMixedFieldsFail(){
        assertThrows(IllegalArgumentException.class,()->decode(Kind.FORMS,json("{schema:1.5}")));
        assertThrows(IllegalArgumentException.class,()->decode(Kind.FORMS,json("{schema:'1'}")));
        var form=decode(Kind.FORM,json("{multiplierMode:'add',strMultiplier:5}"));
        assertEquals(0,form.get("defMultiplier").getAsInt());
        assertThrows(IllegalArgumentException.class,()->decode(Kind.FORM,json("{hairColor:'#000000',appearance:{hair:{hairColor:'#FFFFFF'}}}")));
    }
    @Test void validatesBeforeBackupAndCommitsOnlyAfterWholeLoad()throws Exception{
        Path file=directory.resolve("forms.json");String source="{groupName:'custom',forms:{}}";Files.writeString(file,source);
        ContentMigration.load(file,Kind.FORMS,flat->assertEquals("custom",flat.get("groupName").getAsString()));
        assertEquals(source,Files.readString(file));
        ContentMigration.commitPending();
        assertEquals(source,Files.readString(directory.resolve("oldBackup/forms.json.legacy.bak")));
        assertEquals(1,json(Files.readString(file)).get("schema").getAsInt());
        String converted=Files.readString(file);ContentMigration.load(file,Kind.FORMS,flat->{});ContentMigration.commitPending();
        assertEquals(converted,Files.readString(file));
        try(var backups=Files.list(directory.resolve("oldBackup"))){assertEquals(1,backups.count());}
    }
    @Test void malformedOrRejectedFilesAndConcurrentEditsRemainUntouched()throws Exception{
        Path file=directory.resolve("bad.json");Files.writeString(file,"{");
        assertThrows(RuntimeException.class,()->ContentMigration.load(file,Kind.FORM,flat->{}));
        assertEquals("{",Files.readString(file));assertFalse(Files.exists(directory.resolve("oldBackup")));
        Files.writeString(file,"{name:'x'}");
        assertThrows(IllegalArgumentException.class,()->ContentMigration.load(file,Kind.FORM,flat->{throw new IllegalArgumentException("invalid");}));
        assertFalse(Files.exists(directory.resolve("oldBackup")));
        ContentMigration.load(file,Kind.FORM,flat->{});Files.writeString(file,"{name:'edited'}");
        assertThrows(java.io.IOException.class,ContentMigration::commitPending);
        assertEquals("{name:'edited'}",Files.readString(file));
    }
    @Test void everyRecordedHistoricalLayoutConvertsAndRoundTrips()throws Exception{
        try(var input=getClass().getResourceAsStream("/content-history/forms.json")){
            assertNotNull(input);
            var versions=JsonParser.parseString(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();
            assertTrue(versions.size()>=40);
            for(var version:versions){
                String source=version.getAsJsonObject().get("source").getAsString();
                var flat=decode(Kind.FORMS,version.getAsJsonObject().getAsJsonObject("document"));
                assertEquals(flat,decode(Kind.FORMS,encode(Kind.FORMS,flat)),source);
            }
        }
    }
    @Test void allOtherContentKindsRoundTripWithoutChangingPayloads(){
        for(var fixture:java.util.Map.of(
            Kind.SKILLS,"{skills:{training:{costs:[100,200],allowedRaces:['custom']}},skillOfferings:{guide:['training']}}",
            Kind.NPC,"{id:'example:guide',name:'example.guide',baseTexture:'master_base',hairType:'base',offers:{skills:['training']}}",
            Kind.QUEST,"{id:'trial',type:'side',title:'example.title',description:'example.description',completionId:'example:trial',objectives:[],rewards:[]}",
            Kind.SAGA,"{id:'path',name:'example.path',questFolder:'custom',requirements:{previousSaga:'earlier'}}",
            Kind.RULES,"{content:{'example:trial':[{tp:100}]},buy:{training:{'1':{content:'example:trial'}}}}"
        ).entrySet()){
            var flat=decode(fixture.getKey(),json(fixture.getValue()));
            assertEquals(flat,decode(fixture.getKey(),encode(fixture.getKey(),flat)),fixture.getKey().toString());
        }
    }
    @Test void addedSkillDefaultsKeepOriginalBackupAndUnknownFields()throws Exception{
        Path file=directory.resolve("skills.json");
        String original="{skills:{custom:{costs:[7],addon:{enabled:true}}},addonRoot:9}";
        Files.writeString(file,original);
        ContentMigration.load(file,Kind.SKILLS,flat->{});
        ContentMigration.update(file,Kind.SKILLS,json("{skills:{custom:{costs:[7]},new_default:{costs:[10]}}}"));
        assertEquals(original,Files.readString(file));
        ContentMigration.commitPending();
        assertEquals(original,Files.readString(directory.resolve("oldBackup/skills.json.legacy.bak")));
        var flat=decode(Kind.SKILLS,json(Files.readString(file)));
        assertTrue(flat.getAsJsonObject("skills").getAsJsonObject("custom").has("addon"));
        assertTrue(flat.getAsJsonObject("skills").has("new_default"));
        assertEquals(9,flat.get("addonRoot").getAsInt());
    }
    @Test void unsetOptionalVisualSectionsRemainSerializable(){
        var flat=json("{name:'stage',visual:null}");
        assertEquals(flat,decode(Kind.FORM,encode(Kind.FORM,flat)));
        var nested=json("{schema:1,forms:{stage:{name:'stage',appearance:{hair:null},effects:{lightning:null},aura:null}}}");
        assertDoesNotThrow(()->decode(Kind.FORMS,nested));
    }
    @Test void historicalContentLayoutsAcrossSubsystemsRoundTrip()throws Exception{
        try(var input=getClass().getResourceAsStream("/content-history/content.json")){
            assertNotNull(input);
            var cases=JsonParser.parseString(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();
            var kinds=new java.util.HashSet<Kind>();
            for(var item:cases){
                var entry=item.getAsJsonObject();Kind kind=Kind.valueOf(entry.get("kind").getAsString());kinds.add(kind);
                var flat=decode(kind,entry.getAsJsonObject("document"));
                assertEquals(flat,decode(kind,encode(kind,flat)),entry.get("source").getAsString());
            }
            assertTrue(kinds.containsAll(java.util.Set.of(Kind.SKILLS,Kind.RACE_CHARACTER,Kind.RACE_STATS,Kind.ENTITIES,Kind.TECHNIQUES,Kind.QUEST,Kind.SAGA)));
        }
    }
    @Test void questPayloadsAndLegacyPricesConvertToCanonicalSections(){
        var quest=decode(Kind.QUEST,json("{id:'trial',type:'SIDEQUEST',title:'example.title',prerequisites:{conditions:[]},requirements:{conditions:[]},objectives:[{type:'KILL',entity:'example:enemy',count:2,health:30}],rewards:[{type:'hard:TPS',amount:5}]}"));
        var nested=encode(Kind.QUEST,quest);
        assertTrue(nested.getAsJsonObject("requirements").has("start"));
        assertEquals("example:enemy",nested.getAsJsonArray("objectives").get(0).getAsJsonObject().getAsJsonObject("target").get("entity").getAsString());
        assertEquals("TPS",nested.getAsJsonArray("rewards").get(0).getAsJsonObject().get("type").getAsString());
        var character=decode(Kind.RACE_CHARACTER,json("{formSkillsCosts:{training:[100,-1]}}"));
        assertEquals(-1,character.getAsJsonObject("formSkillsCosts").getAsJsonObject("training").getAsJsonArray("prices").get(1).getAsInt());
        assertFalse(character.getAsJsonObject("formSkillsCosts").getAsJsonObject("training").get("buyFromMaster").getAsBoolean());
        assertEquals(quest,decode(Kind.QUEST,nested));
    }
    @Test void npcDialoguePlacementAndRuleLayoutsRemainAdditive(){
        for(var entry:java.util.Map.of(
            Kind.NPC_PLACEMENT,"{npc:'example:guide',dimension:'minecraft:overworld',x:1,y:80,z:3,yaw:0,addon:2}",
            Kind.DIALOGUE,"{npc:'example:guide',start:'greet',nodes:{greet:{line:'example.line',choices:[{text:'example.choice',goto:'greet',conditions:{operator:'AND',conditions:[]},actions:[]}]}}}",
            Kind.NPC,"{id:'example:guide',baseTexture:'master_base',hairCode:'',name:'example.guide',offers:{skills:['training']}}",
            Kind.RULES,"{buy:{training:{'1':{content:'example:trial'}}},content:{'example:trial':[{tp:10}]}}"
        ).entrySet()){
            var flat=decode(entry.getKey(),json(entry.getValue()));
            assertEquals(flat,decode(entry.getKey(),encode(entry.getKey(),flat)),entry.getKey().name());
        }
    }
    @Test void visualAppearanceAndAuraStateAreGroupedWithoutLosingRules(){
        var flat=decode(Kind.FORM,json("{name:'stage',furUsesHairColor:true,outlineShader:{enabled:true},visual:{aura:'sealed',parts:{left_forearm:[1.5,1,1.5]},fur:{color:'#AA3344'},eyes:'#222222',tail:'#AA3344',outline:true,rules:[{if:{flying:true},set:{aura:true}}]}}"));
        var nested=encode(Kind.FORM,flat);
        assertEquals("sealed",nested.getAsJsonObject("aura").get("state").getAsString());
        assertTrue(nested.getAsJsonObject("appearance").has("parts"));
        assertTrue(nested.getAsJsonObject("effects").has("rules"));
        assertFalse(nested.has("visual"));
        assertEquals(flat,decode(Kind.FORM,nested));
    }
}
