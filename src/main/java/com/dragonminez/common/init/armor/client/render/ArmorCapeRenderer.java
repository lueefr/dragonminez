package com.dragonminez.common.init.armor.client.render;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.client.util.ArmorTextureResolver;
import com.dragonminez.common.init.armor.DbzArmorCapeItem;
import com.dragonminez.common.init.armor.client.model.ArmorCapeModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ArmorCapeRenderer extends GeoArmorRenderer<DbzArmorCapeItem> {
    public ArmorCapeRenderer() {
        super(new ArmorCapeModel());
    }

    private float clothPartial, clothTextureU;
    private MultiBufferSource clothSource;

    public void clothSource(@Nullable MultiBufferSource source, float partial) {
        clothSource = source;
        clothPartial = partial;
    }

    @Override
    public void prepForRender(net.minecraft.world.entity.Entity entity, net.minecraft.world.item.ItemStack item,
            net.minecraft.world.entity.EquipmentSlot slot, net.minecraft.client.model.HumanoidModel<?> model) {
        // GeckoLib's four-argument overload replaces the layer source with the
        // global buffer, losing the player's nested outline/mask consumers.
        if (clothSource != null)
            super.prepForRender(entity, item, slot, model, clothSource, clothPartial, 0, 0, 0, 0);
        else
            super.prepForRender(entity, item, slot, model);
    }


    @Override
    public void renderRecursively(com.mojang.blaze3d.vertex.PoseStack stack, DbzArmorCapeItem item,
            software.bernie.geckolib.cache.object.GeoBone bone, RenderType type, MultiBufferSource source,
            com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean rerender, float partial, int light, int overlay, int color) {
        if (!com.dragonminez.client.render.cloth.CapeCloth.visible()) return;
        if (ConfigManager.getUserConfig().getCapePhysics() && bone.getName().startsWith("cape")) return;
        if ("armorBody".equals(bone.getName())) {
            clothPartial = partial;
            if (ConfigManager.getUserConfig().getCapePhysics())
                clothTextureU = com.dragonminez.client.render.cloth.CapeCloth.textureU(getTextureLocation(item));
            if (source instanceof com.dragonminez.client.render.shader.TransformationMaskBufferSource mask)
                buffer = mask.captureBuffer(type);
        }
        super.renderRecursively(stack, item, bone, type, source, buffer, rerender, partial, light, overlay, color);
    }

    @Override
    public void renderCubesOfBone(com.mojang.blaze3d.vertex.PoseStack stack, software.bernie.geckolib.cache.object.GeoBone bone,
            com.mojang.blaze3d.vertex.VertexConsumer buffer, int light, int overlay, int color) {
        if (!ConfigManager.getUserConfig().getCapePhysics()) {
            // Keep the original GeckoLib cape bones and their authored animation.
            if (bone.getName().startsWith("cape")) super.renderCubesOfBone(stack, bone, buffer, light, overlay, color);
            return;
        }
        if ("armorBody".equals(bone.getName()) && getCurrentEntity() instanceof net.minecraft.world.entity.LivingEntity wearer)
            com.dragonminez.client.render.cloth.CapeCloth.render(wearer, false, clothPartial, stack, buffer, light, overlay, color, clothTextureU);
    }

    @Override
    public ResourceLocation getTextureLocation(DbzArmorCapeItem animatable) {
        String modId = animatable.getModId();
        String itemId = animatable.getItemId();

        if (itemId.contains("pothala") || itemId.contains("scouter"))
            return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/armor/blank.png");

        return ArmorTextureResolver.resolve(modId, itemId, this.getCurrentSlot(), this.getCurrentStack());
    }

    @Override
    public RenderType getRenderType(DbzArmorCapeItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return ConfigManager.getUserConfig().getCapePhysics()
                ? RenderType.entityCutout(texture) : RenderType.armorCutoutNoCull(texture);
    }
}
