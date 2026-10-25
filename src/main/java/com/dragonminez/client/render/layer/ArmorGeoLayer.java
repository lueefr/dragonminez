package com.dragonminez.client.render.layer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer;

/** GeckoLib's material-only texture lookup does not invoke NeoForge's item override. */
public class ArmorGeoLayer<T extends LivingEntity & GeoAnimatable> extends ItemArmorGeoLayer<T> {
    public ArmorGeoLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    protected VertexConsumer getVanillaArmorBuffer(MultiBufferSource source, T entity, ItemStack stack,
            EquipmentSlot slot, GeoBone bone, ArmorMaterial.Layer layer, int light, int overlay, boolean glint) {
        if (glint) return super.getVanillaArmorBuffer(source, entity, stack, slot, bone, layer, light, overlay, true);
        return source.getBuffer(RenderType.armorCutoutNoCull(
                ClientHooks.getArmorTexture(entity, stack, layer, slot == EquipmentSlot.LEGS, slot)));
    }
}
