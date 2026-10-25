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
}
