package com.dragonminez.common.init.armor;

import lombok.*;
import com.dragonminez.Reference;
import com.dragonminez.client.util.ArmorTextureResolver;
import com.dragonminez.common.init.armor.client.model.ArmorBaseModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class DbzArmorCapeItem extends ArmorItem implements GeoItem, DbzArmorTextured {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
@Getter
    private final String modId;
@Getter
    private final String itemId;

    public DbzArmorCapeItem(Holder<ArmorMaterial> pMaterial, Type pType, Properties pProperties, String itemId) {
        this(pMaterial, pType, pProperties, Reference.MOD_ID, itemId);
    }

    public DbzArmorCapeItem(Holder<ArmorMaterial> pMaterial, Type pType, Properties pProperties, String modId, String itemId) {
        super(pMaterial, pType, pProperties);
        this.modId = modId;
        this.itemId = itemId;
    }

    @Nullable
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return ArmorTextureResolver.resolve(modId, itemId, slot, stack).toString();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, event -> {
            if (event.isMoving()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }
        }));
    }

    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ArmorBaseModel model;
            @Override
            @NotNull
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (model == null) model = new ArmorBaseModel(Minecraft.getInstance().getEntityModels().bakeLayer(ArmorBaseModel.LAYER_LOCATION));
                return model;
            }
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
