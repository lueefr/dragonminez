package com.dragonminez.client.init.entities.model.ki;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.ki.SPDragonFistEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.geckolib.loading.math.MolangQueries;
import net.minecraft.core.registries.BuiltInRegistries;
import com.geckolib.constant.DataTickets;
import com.geckolib.cache.object.GeoBone;
import com.geckolib.animation.AnimationState;
import com.geckolib.model.GeoModel;

public class SPDragonFistModel<T extends SPDragonFistEntity> extends GeoModel<T> {

    public static final int STRIKE_TICKS = 8;

    @Override
    public Identifier getModelResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/dragon/shenron.geo.json");
    }

    @Override
    public Identifier getTextureResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/skills/sp_dragonfist_shenron.png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/skills/sp_dragonfist_shenron.animation.json");
    }

    @Override
    public void applyMolangQueries(com.geckolib.animation.AnimationState<T> state, double animTime) {
        super.applyMolangQueries(state, animTime);
        T animatable = state.getAnimatable();

        float age = animatable.tickCount + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        float strike = Mth.clamp((age - (animatable.getMaxLife() - STRIKE_TICKS)) / STRIKE_TICKS, 0.0f, 1.0f);
        float eased = strike * strike * (3.0f - 2.0f * strike);
        MolangQueries.setActorVariable("variable.dmz_df_strike", actor -> eased);
    }
}
