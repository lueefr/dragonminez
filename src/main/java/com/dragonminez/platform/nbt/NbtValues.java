package com.dragonminez.platform.nbt;

import com.mojang.serialization.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.stream.Stream;

/** Flat-key bridge to the native entity ValueInput/ValueOutput serialization API. */
public final class NbtValues {
    private NbtValues() {}
    private static final MapCodec<CompoundTag> ROOT = new MapCodec<>() {
        @Override public <T> Stream<T> keys(DynamicOps<T> ops) { return Stream.empty(); }
        @Override public <T> DataResult<CompoundTag> decode(DynamicOps<T> ops, MapLike<T> input) {
            return CompoundTag.CODEC.parse(ops, ops.createMap(input.entries()));
        }
        @Override public <T> RecordBuilder<T> encode(CompoundTag input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            for (String key : input.keySet()) prefix.add(ops.createString(key), NbtOps.INSTANCE.convertTo(ops, input.get(key)));
            return prefix;
        }
    };
    public static CompoundTag read(ValueInput input) { return input.read(ROOT).orElseGet(CompoundTag::new).copy(); }
    public static void write(ValueOutput output, CompoundTag tag) { output.store(ROOT, tag); }
}
