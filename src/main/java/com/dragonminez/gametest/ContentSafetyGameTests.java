package com.dragonminez.gametest;

import com.dragonminez.common.quest.*;
import com.dragonminez.common.config.schema.*;
import com.google.gson.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;
import java.util.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class ContentSafetyGameTests {
    private static JsonObject quest(String id) {
        return JsonParser.parseString("{id:'"+id+"',title:'example.quest',type:'SIDEQUEST',objectives:[],rewards:[]}").getAsJsonObject();
    }
    @GameTest(template="port_empty")
    public static void disabledDefaultGenerationStillProvidesFallbacks(GameTestHelper helper)throws Exception{
        ContentMigration.begin();
        Path root=Files.createTempDirectory("dmz-disabled-defaults-");
        var gameplay=com.dragonminez.common.config.ConfigManager.getServerConfig().getGameplay();
        var flag=gameplay.getClass().getDeclaredField("createDefaultSideQuests");flag.setAccessible(true);Object previous=flag.get(gameplay);
        try{
            flag.set(gameplay,false);
            var factory=Class.forName("com.dragonminez.common.quest.SideQuestDefaults").getDeclaredMethod("createDefaultSideQuestFiles",Path.class);
            factory.setAccessible(true);Path side=root.resolve("sidequests");factory.invoke(null,side);
            helper.assertTrue(!Files.exists(side),"Disabled generation created missing defaults");
            Path file=side.resolve("training/weighted_training.json");Files.createDirectories(file.getParent());Files.writeString(file,"{");
            var load=QuestRegistry.class.getDeclaredMethod("loadContent",Path.class,ContentSchema.Kind.class);load.setAccessible(true);
            helper.assertTrue(QuestParser.parseQuest((JsonObject)load.invoke(null,file,ContentSchema.Kind.QUEST))!=null,
                    "Disabling generation removed the in-memory default fallback");
            ContentMigration.commitPending();helper.assertTrue(Files.readString(file).equals("{"),"Fallback rewrote malformed default");
            helper.succeed();
        }finally{flag.set(gameplay,previous);try(var files=Files.walk(root)){for(Path file:files.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}}
    }
    @GameTest(template="port_empty")
    public static void nativeContentAdaptersPreserveDefaultMechanics(GameTestHelper helper){
        var gson=new GsonBuilder().registerTypeAdapterFactory(new ContentAdapters()).create();
        var config=com.dragonminez.common.config.ConfigManager.class;
        var fixtures=new ArrayList<Object>();
        fixtures.add(com.dragonminez.common.config.ConfigManager.getRaceCharacter("human"));
        fixtures.add(com.dragonminez.common.config.ConfigManager.getRaceStats("human"));
        fixtures.add(com.dragonminez.common.config.ConfigManager.getSkillsConfig());
        fixtures.add(com.dragonminez.common.config.ConfigManager.getTechniqueConfig());
        fixtures.add(com.dragonminez.common.config.ConfigManager.getEntitiesConfig());
        fixtures.addAll(com.dragonminez.common.config.RaidDefaults.create().values());
        fixtures.addAll(com.dragonminez.common.config.TournamentDefinition.Defaults.create().values());
        for(Object value:fixtures){
            String encoded=gson.toJson(value);
            var restored=gson.fromJson(encoded,value.getClass());
            helper.assertTrue(gson.toJsonTree(value).equals(gson.toJsonTree(restored)),"Native adapter changed "+value.getClass().getSimpleName());
        }
        helper.succeed();
    }
    @GameTest(template="port_empty")
    public static void questBackupsAreMandatoryAndInvalidDefaultsStayOnDisk(GameTestHelper helper)throws Exception {
        ContentMigration.begin();
        Path root=Files.createTempDirectory("dmz-quest-safety-");
        QuestUpgrader.setAutoUpdateEnabled(true);
        try {
            Path file=root.resolve("sidequests/example.json");Files.createDirectories(file.getParent());
            String original=new Gson().toJson(quest("example"));Files.writeString(file,original);
            Files.writeString(file.getParent().resolve("oldBackup"),"blocks backup directory");
            QuestUpgrader.upgradeOrWrite(root,file,quest("example"));
            try{ContentMigration.commitPending();throw new IllegalStateException("Backup failure did not abort migration");}catch(java.io.IOException expected){}
            helper.assertTrue(Files.readString(file).equals(original),"Quest overwritten after backup failed");
            Files.delete(file.getParent().resolve("oldBackup"));
            QuestUpgrader.upgradeOrWrite(root,file,quest("example"));
            ContentMigration.commitPending();
            Path first=file.getParent().resolve("oldBackup/example.json.legacy.bak");
            helper.assertTrue(Files.readString(first).equals(original),"Original quest bytes not backed up");
            Files.writeString(file,original+"\n");
            QuestUpgrader.upgradeOrWrite(root,file,quest("example"));
            ContentMigration.commitPending();
            helper.assertTrue(Files.readString(first).equals(original),"Previous backup overwritten");
            helper.assertTrue(Files.exists(file.getParent().resolve("oldBackup/example.json.legacy.1.bak")),"Numbered backup missing");
            Files.writeString(file,"{invalid");
            var load=QuestRegistry.class.getDeclaredMethod("loadContent",Path.class,ContentSchema.Kind.class);load.setAccessible(true);
            var fallback=(JsonObject)load.invoke(null,file,ContentSchema.Kind.QUEST);
            helper.assertTrue(QuestParser.parseQuest(fallback)!=null,"Invalid bundled quest did not fall back in memory");
            ContentMigration.commitPending();
            helper.assertTrue(Files.readString(file).equals("{invalid"),"Invalid file overwritten by fallback");
            var malformed=quest("example");
            malformed.add("objectives",JsonParser.parseString("[{type:'ITEM',item:'example:missing',count:1}]").getAsJsonArray());
            String malformedSource=new Gson().toJson(malformed);Files.writeString(file,malformedSource);
            var safe=(JsonObject)load.invoke(null,file,ContentSchema.Kind.QUEST);
            helper.assertTrue(QuestParser.parseQuest(safe).getObjectives().isEmpty(),"Invalid objective was silently accepted as completed");
            ContentMigration.commitPending();
            helper.assertTrue(Files.readString(file).equals(malformedSource),"Invalid objective file was migrated");
            helper.succeed();
        } finally {
            try(var paths=Files.walk(root)){for(Path file:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}
        }
    }
    @GameTest(template="port_empty")
    public static void duplicateQuestIsNeverMigrated(GameTestHelper helper)throws Exception {
        ContentMigration.begin();
        Path root=Files.createTempDirectory("dmz-quest-duplicate-");
        Path file=root.resolve("duplicate.json");String source=new Gson().toJson(quest("collision"));Files.writeString(file,source);
        try {
            QuestUpgrader.upgradeOrWrite(root,root.resolve(".").resolve("duplicate.json"),quest("collision"));
            var load=QuestRegistry.class.getDeclaredMethod("loadSingleSideQuestFile",Path.class,Path.class,Map.class);load.setAccessible(true);
            load.invoke(null,root,file,new HashMap<>(Map.of("collision","existing.json")));
            ContentMigration.commitPending();
            helper.assertTrue(Files.readString(file).equals(source),"Rejected duplicate was migrated");
            helper.assertTrue(!Files.exists(file.getParent().resolve("oldBackup")),"Rejected duplicate generated migration backup");
            helper.succeed();
        } finally {Files.deleteIfExists(file);Files.deleteIfExists(root);}
    }
}
