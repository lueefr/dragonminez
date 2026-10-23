package com.dragonminez.platform.resource;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;

public final class PackMetadata {
    private PackMetadata() {}
    public static void addCurrentFormat(JsonObject metadata, PackType type) {
        var format = SharedConstants.getCurrentVersion().packVersion(type);
        metadata.add("min_format", PackFormat.BOTTOM_CODEC.encodeStart(JsonOps.INSTANCE, format).getOrThrow());
        metadata.add("max_format", PackFormat.TOP_CODEC.encodeStart(JsonOps.INSTANCE, format).getOrThrow());
    }
}
