package com.dragonminez.gametest;

import com.dragonminez.common.init.MainEnchants;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.util.types.items.EnchantedBookDTO;
import com.dragonminez.server.recipes.PatternRecipe;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Integration checks run only by the development GameTest server. */
@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class PortGameTests {
    @GameTest(template = "port_empty")
    public static void registeredEntityConstruction(GameTestHelper helper) {
        int count = 0;
        for (var type : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (!id.getNamespace().equals("dragonminez")) continue;
            var entity = type.create(helper.getLevel());
            helper.assertTrue(entity != null, "Unable to construct registered entity: " + id);
            entity.discard();
            count++;
        }
        helper.assertTrue(count > 100, "Entity registry is unexpectedly incomplete: " + count);
        helper.succeed();
    }

    @GameTest(template = "port_empty")
    public static void staticDatapackContent(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        for (String name : List.of("cooked_dino", "cooked_dino_tail", "cooked_baby_dino",
                "cooked_dino_from_smoking", "cooked_dino_tail_from_smoking", "cooked_baby_dino_from_smoking",
                "cooked_dino_from_campfire_cooking", "cooked_dino_tail_from_campfire_cooking", "cooked_baby_dino_from_campfire_cooking")) {
            helper.assertTrue(server.getRecipeManager().byKey(Identifier.fromNamespaceAndPath("dragonminez", name)).isPresent(),
                    "Missing cooking recipe: " + name);
        }
        for (String name : List.of("bandit", "saga_friezasoldier01", "saga_friezasoldier02", "saga_friezasoldier03",
                "giant_fish", "sabertooth", "dino1", "dino2", "dino3", "giant_turtle", "robot1", "robot2", "robot3",
                "namek_frog", "namek_frog_ginyu", "dinokid", "giran")) {
            var key = net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath("dragonminez", "entities/" + name));
            helper.assertTrue(server.reloadableRegistries().getLootTable(key) != net.minecraft.world.level.storage.loot.LootTable.EMPTY,
                    "Missing or invalid entity loot table: " + name);
        }
        helper.succeed();
    }

    @GameTest(template = "port_empty")
    public static void enchantmentsAndConfiguredBooks(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var keys = List.of(MainEnchants.VITALITY_RECOVERY, MainEnchants.RESISTANCE_RECOVERY,
                MainEnchants.ENERGY_RECOVERY, MainEnchants.DEFENSE_PENETRATION, MainEnchants.HEALING_REDUCTION,
                MainEnchants.CRIT_CHANCE, MainEnchants.CRIT_DAMAGE, MainEnchants.KI_CONDUCTIVITY, MainEnchants.GRAVITY_FORGED);
        for (var key : keys) helper.assertTrue(registry.getHolder(key).isPresent(), "Missing enchantment: " + key.location());
        var book = new EnchantedBookDTO(Map.of(MainEnchants.KI_CONDUCTIVITY.location(), 2)).getItemStack();
        var enchantment = registry.getHolderOrThrow(MainEnchants.KI_CONDUCTIVITY);
        helper.assertTrue(book.getOrDefault(DataComponents.STORED_ENCHANTMENTS,
                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).getLevel(enchantment) == 2,
                "Configured enchanted books must preserve stored enchantments");
        helper.succeed();
    }

    @GameTest(template = "port_empty")
    public static void sixBySixRecipeSerialization(GameTestHelper helper) {
        var recipe = PatternRecipe.create("port_test", List.of("AAAAAA", "AAAAAA", "AAAAAA", "AAAAAA", "AAAAAA", "AAAAAA"),
                Map.of("A", Ingredient.of(Items.IRON_INGOT)), new ItemStack(Items.DIAMOND, 2), true);
        var ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        var json = PatternRecipe.Serializer.CODEC.codec().encodeStart(ops, recipe).getOrThrow();
        var decoded = PatternRecipe.Serializer.CODEC.codec().parse(ops, json).getOrThrow();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            PatternRecipe.Serializer.INSTANCE.streamCodec().encode(buffer, decoded);
            decoded = PatternRecipe.Serializer.INSTANCE.streamCodec().decode(buffer);
        } finally { buffer.release(); }
        var stacks = new ArrayList<ItemStack>();
        for (int i = 0; i < 36; i++) stacks.add(new ItemStack(Items.IRON_INGOT));
        var input = CraftingInput.of(6, 6, stacks);
        helper.assertTrue(decoded.matches(input, helper.getLevel()), "6x6 recipe must survive JSON and network serialization");
        helper.assertTrue(decoded.assemble(input, helper.getLevel().registryAccess()).getCount() == 2, "Recipe result count changed");
        stacks.set(35, ItemStack.EMPTY);
        helper.assertTrue(!decoded.matches(CraftingInput.of(6, 6, stacks), helper.getLevel()), "Incomplete recipe must not match");
        helper.succeed();
    }

    @GameTest(template = "port_empty")
    public static void playerAttachmentPersistence(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(
                UUID.fromString("7dcfc954-0765-4a28-93dd-e988c327e12a"), "DMZPortTest"));
        var provider = player.getData(StatsCapability.PLAYER_STATS.get());
        var data = StatsProvider.get(StatsCapability.INSTANCE, player).orElseThrow(IllegalStateException::new);
        data.getStats().setStrength(123);
        data.getResources().setTrainingPointsExact(1234567890123D);
        data.getCooldowns().setCooldown(Cooldowns.DASH_CD, 73);
        var saved = provider.serializeNBT(helper.getLevel().registryAccess());
        data.getStats().setStrength(1);
        data.getResources().setTrainingPointsExact(0);
        data.getCooldowns().clearCooldowns();
        provider.deserializeNBT(helper.getLevel().registryAccess(), saved);
        helper.assertTrue(data.getStats().getStrength() == 123, "Player strength was not restored");
        helper.assertTrue(data.getResources().getTrainingPointsExact() == 1234567890123D, "Training point precision was lost");
        helper.assertTrue(data.getCooldowns().getCooldown(Cooldowns.DASH_CD) == 73, "Player cooldown was not restored");
        helper.succeed();
    }
}
