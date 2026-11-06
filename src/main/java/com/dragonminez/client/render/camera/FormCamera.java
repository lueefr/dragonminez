package com.dragonminez.client.render.camera;

import com.dragonminez.Reference;
import com.dragonminez.api.forms.*;
import com.dragonminez.common.stats.*;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Form camera presentation does not rotate the authoritative aim direction. */
@EventBusSubscriber(modid=Reference.MOD_ID,value=Dist.CLIENT)
public final class FormCamera {
    private FormCamera() {}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void roll(ViewportEvent.ComputeCameraAngles event) {
        apply(event.getCamera().getEntity(),(data,camera)->event.setRoll(event.getRoll()+(float)VisualValue.evaluate(camera.get("roll"),data.getRuntime().numbers(data),0,-180,180)));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void fov(ViewportEvent.ComputeFov event) {
        if(!event.usedConfiguredFov())return;
        apply(event.getCamera().getEntity(),(data,camera)->{
            if(camera.has("fov"))event.setFOV(Math.clamp(event.getFOV()*VisualValue.evaluate(camera.get("fov"),data.getRuntime().numbers(data),1,.25,4),15,170));
        });
    }
    private static void apply(net.minecraft.world.entity.Entity entity,java.util.function.BiConsumer<StatsData,com.google.gson.JsonObject> action) {
        var client=Minecraft.getInstance();
        if(client.player==null||entity!=client.player||client.screen!=null||client.player.isSpectator())return;
        StatsProvider.get(StatsCapability.INSTANCE,client.player).ifPresent(data->{
            if(!data.getStatus().isHasCreatedCharacter())return;
            var camera=FormVisuals.resolve(data).getAsJsonObject("camera");
            if(camera!=null)action.accept(data,camera);
        });
    }
}
