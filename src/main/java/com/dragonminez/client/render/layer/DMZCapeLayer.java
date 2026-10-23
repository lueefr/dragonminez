package com.dragonminez.client.render.layer;

import com.dragonminez.client.render.compat.CosmeticArmorCompat;
import com.dragonminez.common.init.armor.DbzArmorCapeItem;
import com.dragonminez.common.init.armor.client.render.ArmorCapeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.builtin.ItemArmorGeoLayer;

import org.jetbrains.annotations.Nullable;

public class DMZCapeLayer<T extends AbstractClientPlayer & GeoAnimatable> extends ItemArmorGeoLayer<T, Void, net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {

    private final ArmorCapeRenderer capeRenderer = new ArmorCapeRenderer();

    public DMZCapeLayer(GeoRenderer<T, Void, net.minecraft.client.renderer.entity.state.LivingEntityRenderState> geoRenderer) {
        super(geoRenderer);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (animatable.isSpectator()) return;
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
    }

    @Override
    protected @Nullable ItemStack getArmorItemForBone(GeoBone bone, T animatable) {
        if (!"armorBody".equals(bone.getName())) return null;

        ItemStack stack = animatable.getItemBySlot(EquipmentSlot.CHEST);
        if (CosmeticArmorCompat.isLoaded()) {
            ItemStack cosStack = CosmeticArmorCompat.getCosmeticStack(animatable, EquipmentSlot.CHEST);
            if (cosStack != null) {
                if (cosStack.isEmpty()) return null;
                stack = cosStack;
            }
        }

        if (stack.isEmpty() || !(stack.getItem() instanceof DbzArmorCapeItem)) return null;
        return stack;
    }

    @Override
    protected @NotNull EquipmentSlot getEquipmentSlotForBone(GeoBone bone, ItemStack stack, T animatable) {
        return EquipmentSlot.CHEST;
    }

    @Override
    protected @NotNull HumanoidModel<?> getModelForItem(GeoBone bone, EquipmentSlot slot, ItemStack stack, T animatable) {
        return capeRenderer;
    }
}
