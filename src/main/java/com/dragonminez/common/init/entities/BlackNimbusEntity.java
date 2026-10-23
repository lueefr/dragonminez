package com.dragonminez.common.init.entities;

import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.entities.vehicle.VehicleEntity;
import com.dragonminez.common.init.entities.vehicle.VehicleProperties;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

public class BlackNimbusEntity extends VehicleEntity {

    private static final VehicleProperties PROPERTIES = VehicleProperties.builder(VehicleProperties.Mobility.FLYING)
            .speed(1.2D)
            .verticalSpeed(0.4D)
            .seat(0.0D, 0.9D, 0.0D)
            .boost(45, 7.2D, 0.65D)
            .build();

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    public BlackNimbusEntity(EntityType<? extends VehicleEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel, PROPERTIES);
    }

    @Override
    protected Item getVehicleItem() {
        return MainItems.NUBE_NEGRA_ITEM.get();
    }
    @Override
    protected boolean canBeMountedBy(Player player) {
        int alignment = StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(data -> data.getResources().getAlignment())
                .orElse(100);
        if (alignment < 67) return true;
        player.displayClientMessage(Component.translatable("message.dragonminez.black_nimbus.not_evil"), true);
        return false;
    }

    @Override
    protected void onBoostStart() {
        spawnBoostBurst(MainParticles.KINTON.get(), 0x272d67);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>("controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationTest<T> tAnimationState) {
        tAnimationState.controller().setAnimation(IDLE);
        return PlayState.CONTINUE;
    }
}
