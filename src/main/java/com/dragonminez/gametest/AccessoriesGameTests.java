package com.dragonminez.gametest;

import com.dragonminez.common.progression.RebirthService;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** Run with native Accessories installed; the ordinary distribution has no hard dependency. */
@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class AccessoriesGameTests {
    @GameTest(template="port_empty")
    public static void nativeInventories(GameTestHelper helper) throws Exception {
        if(!net.neoforged.fml.ModList.get().isLoaded("accessories")) {helper.succeed();return;}
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"NativeAccessory"));
        try {
            var api=Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
            Object capability=api.getMethod("get",net.minecraft.world.entity.LivingEntity.class).invoke(null,player);
            helper.assertTrue(capability!=null,"Native Accessories capability missing");
            var containerType=Class.forName("io.wispforest.accessories.api.AccessoriesContainer");
            Map<?,?> containers=(Map<?,?>)api.getMethod("getContainers").invoke(capability);
            int slots=0;
            for(Object holder:containers.values())for(String method:new String[]{"getAccessories","getCosmeticAccessories"}) {
                Container inventory=(Container)containerType.getMethod(method).invoke(holder);
                for(int slot=0;slot<inventory.getContainerSize();slot++) {inventory.setItem(slot,new ItemStack(Items.DIAMOND));slots++;}
            }
            Object holder=api.getMethod("getHolder").invoke(capability);
            @SuppressWarnings("unchecked") var invalid=(List<ItemStack>)holder.getClass().getField("invalidStacks").get(holder);
            invalid.add(new ItemStack(Items.APPLE));
            player.getInventory().setItem(0,new ItemStack(Items.DIAMOND));
            RebirthService.clearInventory(player);
            for(Object entry:containers.values())for(String method:new String[]{"getAccessories","getCosmeticAccessories"}) {
                Container inventory=(Container)containerType.getMethod(method).invoke(entry);
                helper.assertTrue(inventory.isEmpty(),"Native functional/cosmetic inventory survived reincarnation");
            }
            helper.assertTrue(player.getInventory().isEmpty() && invalid.isEmpty(),"Pending Accessories stacks could return after reincarnation");
            helper.assertTrue(slots>0,"Native Accessories integration fixture had no functional/cosmetic slots");
            com.dragonminez.LogUtil.info(com.dragonminez.Env.SERVER,"Native Accessories purge verified across {} slots",slots);
            helper.succeed();
        } finally {player.discard();}
    }
}
