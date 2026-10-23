package com.dragonminez.common.loot;

import com.dragonminez.common.init.MainItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import java.util.List;

/** Adds one mod disc to chest loot that actually rolled a playable disc. */
public record AddMusicDiscModifier(List<LootItemCondition> conditions, float chance) {
    public static final MapCodec<AddMusicDiscModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LootItemCondition.DIRECT_CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(AddMusicDiscModifier::conditions),
            Codec.floatRange(0, 1).fieldOf("chance").forGetter(AddMusicDiscModifier::chance)
    ).apply(instance, AddMusicDiscModifier::new));
    public AddMusicDiscModifier { conditions = List.copyOf(conditions); }
    public void apply(Identifier table, List<ItemStack> loot, LootContext context) {
        if (!table.getPath().startsWith("chests/") || MainItems.MUSIC_DISCS.isEmpty()) return;
        if (conditions.stream().anyMatch(condition -> !condition.test(context))) return;
        if (loot.stream().noneMatch(stack -> stack.has(DataComponents.JUKEBOX_PLAYABLE))) return;
        if (context.getRandom().nextFloat() >= chance) return;
        var disc = MainItems.MUSIC_DISCS.get(context.getRandom().nextInt(MainItems.MUSIC_DISCS.size()));
        loot.add(new ItemStack(disc.get()));
    }
}
