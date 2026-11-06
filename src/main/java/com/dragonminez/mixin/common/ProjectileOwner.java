package com.dragonminez.mixin.common;

import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Projectile.class)
public interface ProjectileOwner {
    @Accessor("leftOwner")
    void leaveOwner(boolean value);
}
