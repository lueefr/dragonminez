package com.dragonminez.common.init.item.weapons.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.weapons.ZSwordItem;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;

public class ZSwordModel extends GeoModel<ZSwordItem> {
    @Override
    public Identifier getModelResource(ZSwordItem animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/z_sword.geo.json");
    }

    @Override
    public Identifier getTextureResource(ZSwordItem animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/z_sword.png");
    }

    @Override
    public Identifier getAnimationResource(ZSwordItem animatable) {
        return null;
    }
}
