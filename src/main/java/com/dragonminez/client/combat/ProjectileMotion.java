package com.dragonminez.client.combat;

import com.dragonminez.common.network.S2C.ProjectileMotionS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.projectile.Projectile;

public final class ProjectileMotion {
    private ProjectileMotion(){}
    public static void receive(ProjectileMotionS2C message){
        var level=Minecraft.getInstance().level;
        if(level==null||!level.dimension().location().equals(message.dimension()))return;
        if(!(level.getEntity(message.entity()) instanceof Projectile projectile)||!projectile.getUUID().equals(message.uuid()))return;
        var owner=level.getEntity(message.owner());
        if(owner==null||!owner.getUUID().equals(message.ownerUuid()))return;
        projectile.setOwner(owner);projectile.setPos(message.position());projectile.setDeltaMovement(message.motion());
        ((com.dragonminez.mixin.common.ProjectileOwner)projectile).leaveOwner(false);
        if(projectile instanceof com.dragonminez.common.init.entities.ki.AbstractKiProjectile ki)ki.setHomingTarget(-1);
    }
}
