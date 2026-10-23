package com.dragonminez.mixin.common;
import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.event.level.ExplosionEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.List;
import java.util.ArrayList;
@Mixin(ServerExplosion.class)
public abstract class ExplosionBridgeMixin {
    @ModifyExpressionValue(method = "explode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/ServerExplosion;calculateExplodedPositions()Ljava/util/List;"))
    private List<BlockPos> dmz$detonate(List<BlockPos> positions) {
        var mutable = new ArrayList<>(positions);
        GameEvents.BUS.post(new ExplosionEvent.Detonate((ServerExplosion) (Object) this, mutable));
        return mutable;
    }
}
