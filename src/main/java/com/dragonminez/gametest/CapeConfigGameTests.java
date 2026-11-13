package com.dragonminez.gametest;

import com.dragonminez.common.config.GeneralUserConfig;
import com.google.gson.Gson;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class CapeConfigGameTests {
    @GameTest(template = "port_empty")
    public static void capePreference(GameTestHelper helper) {
        Gson gson = new Gson();
        helper.assertTrue(gson.fromJson("{}", GeneralUserConfig.class).getCapePhysics(),
                "Existing user config must keep cape physics enabled");
        helper.assertTrue(gson.fromJson("{\"capePhysics\":null}", GeneralUserConfig.class).getCapePhysics(),
                "Null preferences must use the enabled default");
        var config = gson.fromJson("{\"capePhysics\":false}", GeneralUserConfig.class);
        helper.assertTrue(!config.getCapePhysics(), "Disabled cape physics must load");
        helper.assertTrue(!gson.fromJson(gson.toJson(config), GeneralUserConfig.class).getCapePhysics(),
                "Disabled cape physics must survive a config save/reload");
        helper.succeed();
    }
}
