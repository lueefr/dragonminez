package com.dragonminez.common.init.armor.client.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.armor.DbzArmorCapeItem;
import net.minecraft.resources.Identifier;
import com.dragonminez.platform.client.model.ExtractedGeoModel;

public class ArmorCapeModel extends ExtractedGeoModel<DbzArmorCapeItem> {
    @Override
    public Identifier getModelResource(DbzArmorCapeItem animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/armor/armorcape.geo.json");
    }

    @Override
    public Identifier getTextureResource(DbzArmorCapeItem animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/armor/blank.png");
    }

    @Override
    public Identifier getAnimationResource(DbzArmorCapeItem animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/armorcape.animation.json");
    }
}
