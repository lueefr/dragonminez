package com.dragonminez.client.init.entities.renderer.sagas.layer;

import com.dragonminez.client.render.util.ArmorPieceInflation;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import com.dragonminez.platform.item.ArmorData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.builtin.ItemArmorGeoLayer;

import org.jetbrains.annotations.Nullable;

public class DMZSagaArmorLayer<T extends DBSagasEntity> extends ItemArmorGeoLayer<T, Void, net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {

	public DMZSagaArmorLayer(GeoRenderer<T, Void, net.minecraft.client.renderer.entity.state.LivingEntityRenderState> geoRenderer) {
		super(geoRenderer);
	}

	@Override
	public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		if (animatable.isSpectator()) return;
		super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
	}

	@Override
	protected void prepModelPartForRender(PoseStack poseStack, GeoBone bone, ModelPart sourcePart) {
		super.prepModelPartForRender(poseStack, bone, sourcePart);
		ArmorPieceInflation.inflateFittedPart(poseStack, bone, sourcePart, getReferenceCubeForModel(bone, sourcePart));
	}

    @Override
    protected @Nullable ItemStack getArmorItemForBone(GeoBone bone, T animatable) {
        final String boneName = bone.getName();
        EquipmentSlot slot = switch (boneName) {
            case "armorHead", "armor_head" -> EquipmentSlot.HEAD;
            case "armorBody", "armor_body",
                 "armorRightArm", "armor_right_arm",
                 "armorLeftArm", "armor_left_arm" -> EquipmentSlot.CHEST;
            case "armorLeggingsBody", "armor_leggings_body",
                 "armorLeftLeg", "armor_left_leg",
                 "armorRightLeg", "armor_right_leg" -> EquipmentSlot.LEGS;
            case "armorRightBoot", "armor_right_boot",
                 "armorLeftBoot", "armor_left_boot" -> EquipmentSlot.FEET;
            default -> null;
        };
        if (slot == null) return null;

        ItemStack stack = animatable.getItemBySlot(slot);

        if (stack.isEmpty()) return null;
        if (!(ArmorData.isArmor(stack)) && !(stack.getItem() instanceof DbzArmorItem)) return null;
        if (!stack.canEquip(slot, animatable) && !(stack.getItem() instanceof DbzArmorItem)) return null;

        return stack;
    }

	@Override
	protected @NotNull EquipmentSlot getEquipmentSlotForBone(GeoBone bone, ItemStack stack, T animatable) {
		String boneName = bone.getName();
		return switch (boneName) {
			case "armorHead" -> EquipmentSlot.HEAD;
			case "armorBody", "armorRightArm", "armorLeftArm" -> EquipmentSlot.CHEST;
			case "armorLeggingsBody", "armorRightLeg", "armorLeftLeg" -> EquipmentSlot.LEGS;
			case "armorRightBoot", "armorLeftBoot" -> EquipmentSlot.FEET;
			default -> super.getEquipmentSlotForBone(bone, stack, animatable);
		};
	}

	@Override
	protected @NotNull ModelPart getModelPartForBone(GeoBone bone, EquipmentSlot slot, ItemStack stack, T animatable, HumanoidModel<?> baseModel) {
		String boneName = bone.getName();

		return switch (boneName) {
			case "armorHead" -> baseModel.head;
			case "armorBody", "armorLeggingsBody" -> baseModel.body;
			case "armorRightArm" -> baseModel.rightArm;
			case "armorLeftArm" -> baseModel.leftArm;
			case "armorRightLeg", "armorRightBoot" -> baseModel.rightLeg;
			case "armorLeftLeg", "armorLeftBoot" -> baseModel.leftLeg;
			default -> super.getModelPartForBone(bone, slot, stack, animatable, baseModel);
		};
	}


}
