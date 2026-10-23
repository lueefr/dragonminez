package com.dragonminez.client.init.entities.model;


import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.MastersEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import net.minecraft.util.Mth;
import net.minecraft.core.registries.BuiltInRegistries;
import com.geckolib.constant.DataTickets;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.animation.state.AnimationTest;
import com.dragonminez.platform.client.model.HeadTrackingGeoModel;

import java.util.HashMap;
import java.util.Map;

public class MasterGlobalModel<T extends MastersEntity> extends HeadTrackingGeoModel<T> {

    private static final Map<String, Boolean> MODEL_CACHE = new HashMap<>();
    private static final Map<String, Boolean> TEXTURE_CACHE = new HashMap<>();
    private static final Map<String, Boolean> ANIM_CACHE = new HashMap<>();

    private static final Map<String, String> ASSET_ALIASES = Map.of(
            "master_uranai_earth", "master_uranai",
            "saga_world_tournament_announcer", "master_world_tournament_announcer"
    );

    private static String assetName(MastersEntity animatable) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return ASSET_ALIASES.getOrDefault(name, name);
    }

    @Override
    public Identifier getModelResource(T animatable) {
        String name = assetName(animatable);
        Identifier original = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/master/" + name + ".geo.json");

        boolean exists = MODEL_CACHE.computeIfAbsent(name, k -> resourceExists(original));
        return exists ? original : Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/enemies/robotxv.geo.json");
    }

    @Override
    public Identifier getTextureResource(T animatable) {
        String name = assetName(animatable);
        Identifier original = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/master/" + name + ".png");

        boolean exists = TEXTURE_CACHE.computeIfAbsent(name, k -> resourceExists(original));
        return exists ? original : Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/enemies/robotxv.png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        String name = assetName(animatable);
        Identifier original = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/master/masters.animation.json");

        boolean exists = ANIM_CACHE.computeIfAbsent(name, k -> resourceExists(original));
        return exists ? original : Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/master/master_goku.animation.json");
    }


    private boolean resourceExists(Identifier location) {
        return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
    }
}
