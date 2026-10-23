package com.dragonminez.common.init.item.weapons.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.weapons.PowerPoleItem;
import net.minecraft.resources.Identifier;
import com.dragonminez.platform.client.model.ExtractedGeoModel;

public class PowerPoleModel extends ExtractedGeoModel<PowerPoleItem> {
    @Override
    public Identifier getModelResource(PowerPoleItem animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/power_pole.geo.json");
    }

    @Override
    public Identifier getTextureResource(PowerPoleItem animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/power_pole.png");
    }

    @Override
    public Identifier getAnimationResource(PowerPoleItem animatable) {
        return null;
    }
}
