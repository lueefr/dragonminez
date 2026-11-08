package com.dragonminez.gametest;

import com.dragonminez.common.wish.*;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.util.gson.GsonUtils;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;
import java.util.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class WishConfigGameTests {
    @GameTest(template="port_empty")
    public static void legacyBackup(GameTestHelper helper)throws Exception {
        Path directory=Files.createTempDirectory("dmz-wish-migration-");Path file=directory.resolve("example.json");
        String original="""
                [{"type":"rewards","name":"wish.example.potential","description":"wish.example.potential.desc","repeatable":false,
                  "rewards":[{"content":"example:potential"},{"item":{"id":"minecraft:stone","count":3}}]}]
                """;
        Files.writeString(file,original);Map<String,List<Wish>> wishes=new LinkedHashMap<>();Map<String,Integer> counts=new LinkedHashMap<>();
        var method=WishManager.class.getDeclaredMethod("loadWishConfig",Path.class,Map.class,Map.class);method.setAccessible(true);method.invoke(null,file,wishes,counts);
        helper.assertTrue(Files.readString(directory.resolve("oldBackup/example.json.legacy.bak")).equals(original),"Legacy wish backup was not exact");
        var root=com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        helper.assertTrue(root.getAsJsonObject("summoning").get("wishCount").getAsInt()==1&&wishes.get("example").size()==1,"Wish envelope was not loaded after migration");
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"WishJson"));
        var data=StatsProvider.get(StatsCapability.INSTANCE,player).orElseThrow(IllegalStateException::new);
        try {
            data.getStatus().setHasCreatedCharacter(true);
            Wish wish=wishes.get("example").getFirst();
            var copy=GsonUtils.NETWORK.fromJson(GsonUtils.NETWORK.toJson(wish,Wish.class),Wish.class);
            helper.assertTrue(copy.available(data,"example"),"Fresh modular wish was unavailable");
            copy.grant(player);
            helper.assertTrue(data.getContentProgress().hasCompleted("example:potential"),"Composite wish did not grant content");
            helper.assertTrue(player.getInventory().countItem(net.minecraft.world.item.Items.STONE)==3,"Composite wish did not honor item quantity");
            com.dragonminez.api.content.ContentCompletionApi.complete(player,copy.key("example"),"wish");
            helper.assertTrue(!copy.available(data,"example"),"Non-repeatable wish ignored lifetime completion");
            String invalid="{\"summoning\":{\"wishCount\":-1},\"wishes\":[]}";Files.writeString(file,invalid);
            method.invoke(null,file,wishes,counts);
            helper.assertTrue(Files.readString(file).equals(invalid),"Invalid wish file was overwritten");
            helper.succeed();
        } finally {player.discard();}
    }
    @GameTest(template="port_empty")
    public static void translatedWish(GameTestHelper helper) {
        String source="""
                {"type":"attributes","id":"example:power_boost",
                  "name":"Power Boost","description":"Receive attribute points.","amount":100}
                """;
        Wish wish=GsonUtils.GSON.fromJson(source,Wish.class);
        wish.validate();
        helper.assertTrue("example:power_boost".equals(wish.getId()),"Wish ID was not loaded from JSON");
        helper.assertTrue("Power Boost".equals(wish.getName()),"Wish name fallback was lost");
        helper.assertTrue("Receive attribute points.".equals(wish.getDescription()),"Wish description fallback was lost");
        helper.assertTrue("wish.example.power_boost.name".equals(WishText.key(wish.getId(),"name")),
                "Wish title translation key did not use the ID");
        var copy=GsonUtils.NETWORK.fromJson(GsonUtils.NETWORK.toJson(wish,Wish.class),Wish.class);
        helper.assertTrue("example:power_boost".equals(copy.getId()),"Network synchronization lost the wish ID");

        Wish unnamed=GsonUtils.GSON.fromJson("""
                {"type":"attributes","id":"example:bonus","amount":10}
                """,Wish.class);
        unnamed.validate();
        helper.assertTrue("bonus".equals(unnamed.getName()) && unnamed.getDescription().isEmpty(),
                "A wish with an ID must allow omitted JSON labels");
        helper.succeed();
    }

    @GameTest(template="port_empty")
    public static void builtinWishIds(GameTestHelper helper) {
        Map<String,List<Wish>> dragons=Map.of(
                "shenron",DefaultWishes.shenron(),
                "porunga",DefaultWishes.porunga(),
                "porunga_daima",DefaultWishes.porungaDaima());
        for (var entry : dragons.entrySet()) {
            Set<String> ids=new HashSet<>();
            for (Wish wish : entry.getValue()) {
                wish.validate();
                helper.assertTrue(wish.getId()!=null && wish.getId().startsWith("dragonminez:"+entry.getKey()+"/"),
                        "Default wish is missing an ID for "+entry.getKey());
                helper.assertTrue(ids.add(wish.getId()),"Duplicate default wish ID: "+wish.getId());
            }
        }
        helper.succeed();
    }

    @GameTest(template="port_empty")
    public static void syncedCount(GameTestHelper helper) {
        var dragon=com.dragonminez.common.init.MainEntities.SHENRON.get().create(helper.getLevel());
        int original=com.dragonminez.common.dragonball.DragonBallDefinitions.defaultCount("shenron");
        try {
            dragon.setDragonDefinitionId("shenron");
            com.dragonminez.common.dragonball.DragonBallDefinitions.wishCounts(Map.of("shenron",7));
            dragon.tick();
            var remote=com.dragonminez.common.init.MainEntities.SHENRON.get().create(helper.getLevel());
            remote.setDragonDefinitionId("shenron");
            remote.getEntityData().assignValues(dragon.getEntityData().getNonDefaultValues());
            com.dragonminez.common.dragonball.DragonBallDefinitions.wishCounts(Map.of());
            helper.assertTrue(remote.wishCount()==7,"Client wish count depended on local dragon defaults");
            helper.assertTrue(com.dragonminez.common.dragonball.DragonBallDefinitions.defaultCount("shenron")==original,"Wish override changed its own default");
            helper.succeed();
        } finally {com.dragonminez.common.dragonball.DragonBallDefinitions.wishCounts(Map.of());dragon.discard();}
    }

}
