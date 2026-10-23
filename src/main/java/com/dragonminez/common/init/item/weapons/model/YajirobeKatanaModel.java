package com.dragonminez.common.init.item.weapons.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.weapons.YajirobeKatanaItem;
import net.minecraft.resources.Identifier;
import com.dragonminez.platform.client.model.ExtractedGeoModel;

public class YajirobeKatanaModel extends ExtractedGeoModel<YajirobeKatanaItem> {
    @Override
    public Identifier getModelResource(YajirobeKatanaItem animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/yajirobe_katana.geo.json");
    }

    @Override
    public Identifier getTextureResource(YajirobeKatanaItem animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/yajirobe_katana.png");
    }

    @Override
    public Identifier getAnimationResource(YajirobeKatanaItem animatable) {
        return null;
    }
}
