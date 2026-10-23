package com.dragonminez.common.init.armor.client.render;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ArmorTextureResolver;
import com.dragonminez.common.init.armor.DbzArmorCapeItem;
import com.dragonminez.common.init.armor.client.model.ArmorCapeModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.cache.object.GeoBone;
import com.geckolib.renderer.GeoArmorRenderer;

public class ArmorCapeRenderer extends GeoArmorRenderer<DbzArmorCapeItem> {
    public ArmorCapeRenderer() {
        super(new ArmorCapeModel());
    }

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight,
                                  int packedOverlay, int colour) {
        String name = bone.getName();
        if (name == null || !name.startsWith("cape")) return;
        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);
    }

    @Override
    public Identifier getTextureLocation(DbzArmorCapeItem animatable) {
        String modId = animatable.getModId();
        String itemId = animatable.getItemId();

        if (itemId.contains("pothala") || itemId.contains("scouter"))
            return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/armor/blank.png");

        return ArmorTextureResolver.resolve(modId, itemId, this.getCurrentSlot(), this.getCurrentStack());
    }

    @Override
    public RenderType getRenderType(DbzArmorCapeItem animatable, Identifier texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.armorCutoutNoCull(texture);
    }
}
