package com.dragonminez.mixin.common;

import com.dragonminez.platform.event.GameEvents;
import com.dragonminez.platform.resource.AddPackFindersEvent;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Consumer;

@Mixin(BuiltInPackSource.class)
public abstract class RuntimePacksBridgeMixin {
    @Shadow @Final private PackType packType;
    @Inject(method = "loadPacks", at = @At("RETURN"))
    private void dmz$loadRuntimePacks(Consumer<Pack> consumer, CallbackInfo callback) {
        GameEvents.BUS.post(new AddPackFindersEvent(packType, consumer));
    }
}
