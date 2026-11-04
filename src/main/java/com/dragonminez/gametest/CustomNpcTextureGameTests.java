package com.dragonminez.gametest;

import com.dragonminez.api.npc.CustomNpcRegistry;
import com.dragonminez.api.npc.CustomNpcVisuals;
import com.dragonminez.common.init.MainEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class CustomNpcTextureGameTests {
    @GameTest(template="port_empty")
    public static void namedTextureAndModelPersistIndependently(GameTestHelper helper) {
        var definition = CustomNpcRegistry.fromJson("""
            {"id":"example:visitor","baseModel":"master_female","baseTexture":"custom_outfit","name":"Visitor"}
            """);
        var entity = MainEntities.CUSTOM_NPC.get().create(helper.getLevel());
        entity.setDefinition(definition);
        var tag = new CompoundTag(); entity.addAdditionalSaveData(tag);
        var restored = MainEntities.CUSTOM_NPC.get().create(helper.getLevel());
        restored.readAdditionalSaveData(tag);
        helper.assertTrue(restored.definition().baseModel().equals("master_female")
                && restored.definition().baseTexture().equals("custom_outfit"), "Model or external texture name lost during entity persistence");
        helper.assertTrue(CustomNpcRegistry.fromJson(CustomNpcRegistry.toJson(definition)).equals(definition), "Definition synchronization changed appearance");
        for (String model : new String[]{"master_male", "master_female", "humanoid_slim"}) {
            var legacy = CustomNpcRegistry.fromJson("{\"id\":\"example:legacy\",\"baseTexture\":\"" + model + "\",\"name\":\"Visitor\"}");
            helper.assertTrue(legacy.baseModel().equals(model) && legacy.baseTexture().equals("master_base"), "Legacy alias migration lost the body model");
        }
        for (String invalid : new String[]{"../outside", "/absolute", "folder/texture", "Texture", "texture.png", ".."}) {
            boolean rejected = false;
            try { CustomNpcVisuals.texture(invalid); } catch (IllegalArgumentException expected) { rejected = true; }
            helper.assertTrue(rejected, "Invalid external texture name accepted: " + invalid);
        }
        helper.succeed();
    }
    @GameTest(template="port_empty")
    public static void groupedFiles(GameTestHelper helper)throws Exception {
        var root=java.nio.file.Files.createTempDirectory("dmz-npc-layout-");
        var legacy=root.resolve("custom_npcs/guide.json");
        var target=root.resolve("npcs/definitions/guide.json");
        java.nio.file.Files.createDirectories(legacy.getParent());
        String original="{\"id\":\"example:grouped\",\"baseTexture\":\"master_base\",\"name\":\"Visitor\"}";
        java.nio.file.Files.writeString(legacy,original);
        com.dragonminez.common.config.schema.ContentMigration.begin();
        try {
            CustomNpcRegistry.INSTANCE.reload(root);
            helper.assertTrue(java.nio.file.Files.exists(legacy),"NPC moved before validation transaction committed");
            com.dragonminez.common.config.schema.ContentMigration.commitPending();
            helper.assertTrue(java.nio.file.Files.exists(target)&&!java.nio.file.Files.exists(legacy),"Validated NPC was not moved into grouped folder");
            helper.assertTrue(java.nio.file.Files.readString(legacy.getParent().resolve("oldBackup/guide.json.legacy.bak")).equals(original),"NPC migration lost exact original backup");
            CustomNpcRegistry.INSTANCE.reload(root);
            helper.assertTrue(CustomNpcRegistry.INSTANCE.get("example:grouped").isPresent(),"Grouped NPC did not reload");
            helper.succeed();
        } finally {
            com.dragonminez.common.config.schema.ContentMigration.begin();
            CustomNpcRegistry.INSTANCE.reload(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("dragonminez"));
        }
    }

}
